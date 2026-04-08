package core.symbolicExecution;

import com.microsoft.z3.*;
import core.FilePath;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.Array.ArrayCreationNode;
import core.ast.Expression.Array.ArrayCreationWithNewKeyWord;
import core.ast.Expression.Array.ArrayNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.BooleanLiteralNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Name.NameNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.ast.Expression.OperationExpression.PrefixExpressionNode;
import core.ast.Type.AnnotatableType.PrimitiveTypeNode;
import core.ast.additionalNodes.Node;
import core.cfg.CfgBoolExprNode;
import core.cfg.CfgNode;
import core.path.Path;
import core.variable.ArrayTypeVariable;
import core.variable.PrimitiveTypeVariable;
import core.variable.Variable;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.*;

public class SymbolicExecutionRewrite {
    private MemoryModel symbolicMap;
    private List<Z3VariableWrapper> Z3Vars;
    private Model model;
    private Path testPath;
    private List<ASTNode> parameters;
    private static CfgNode currentCfgNode;
    public static Map<String, PrimitiveType.Code> variableTypeMap = new HashMap<>();
    public String globalZ3Result = "";
    // Map để đếm số lần một hàm được gọi --> tạo biến gỉa không bị trùng lặp
    private Map<String, Integer> mockMethodCounter = new HashMap<>();
    private final Map<String, Integer> parameterArrayLengths = new HashMap<>();
    // Map này lưu các biểu thức index symbolic theo từng tên mảng.
    // Điểm quan trọng
    // - không solve riêng index bằng một solver/context phụ
    // - chỉ thu thập index symbolic ngay trong luồng solve chính
    // - sau khi model chính đã có, mới evaluate các biểu thức này bằng chính model đó
    // Nhờ vậy luồng solve chính và luồng suy kích thước mảng được tách trách nhiệm rõ ràng
    // nhưng vẫn nhất quán tuyệt đối về nghiệm.
    private final Map<String, List<Expr>> symbolicArrayIndexExpressions = new HashMap<>();
    public static List<MockInfo> currentMockInfos = new ArrayList<>();

    public SymbolicExecutionRewrite(Path testPath, List<ASTNode> parameters) {
        this.testPath = testPath;
        this.parameters = parameters;
    }

    public List<Z3VariableWrapper> execute() {
        symbolicMap = new MemoryModel();
        Z3Vars = new ArrayList<>();
        currentCfgNode = null;
        parameterArrayLengths.clear();
        symbolicArrayIndexExpressions.clear();

        HashMap<String, String> cfg = new HashMap();
        cfg.put("model", "true");
        Context ctx = new Context(cfg);

        executeParameters(ctx);

        for (MockInfo info : currentMockInfos) {
            Sort z3Sort = ctx.mkIntSort();
            Expr mockExpr = ctx.mkConst(info.mockVarName, z3Sort);
            Z3VariableWrapper wrapper = new Z3VariableWrapper(mockExpr);

            if (!haveDuplicateVariable(wrapper)) {
                Z3Vars.add(wrapper);
            }
        }

        mockMethodCounter.clear();
        currentMockInfos.clear();

        Node currentNode = testPath.getCurrentFirst();

        BoolExpr finalZ3Expression = null;

        if (this.parameters != null) {
            for (ASTNode param : this.parameters) {
                if (param instanceof SingleVariableDeclaration) {
                    SingleVariableDeclaration decl = (SingleVariableDeclaration) param;
                    String name = decl.getName().getIdentifier();

                    if (decl.getType().toString().equals("char")) {
                        Variable v = symbolicMap.getVariable(name);
                        Expr z3Var = Variable.createZ3Variable(v, ctx);

                        BoolExpr charConstraint = ctx.mkAnd(
                                ctx.mkGe((ArithExpr) z3Var, ctx.mkInt(48)),
                                ctx.mkLe((ArithExpr) z3Var, ctx.mkInt(126))
                        );

                        if (finalZ3Expression == null) finalZ3Expression = charConstraint;
                        else finalZ3Expression = ctx.mkAnd(finalZ3Expression, charConstraint);
                    } else if (decl.getType().isArrayType()) {
                        ArrayType arrayType = (ArrayType) decl.getType();
                        System.out.println(" Phát hiện Parameter là Mảng: " + name);
                        int inferredLength  = inferArrayParameterLength(name);
                        parameterArrayLengths.put(name, inferredLength);
                        ArrayNode virtualArray = createVirtualArray(arrayType, inferredLength);
                        symbolicMap.declareArrayTypeVariable(arrayType, name, arrayType.getDimensions(), virtualArray);
                    }
                }
            }
        }

        int limit = 0;
        while (currentNode != null) {
            if (++limit > 400) break;
            currentCfgNode = currentNode.getData();
            System.out.println(currentCfgNode.getContentReport());
            ASTNode astNode = currentCfgNode.getAst();

            if (astNode != null) {
                // Thu các index symbolic ngay tại thời điểm node sắp được symbolic execution.
                // Ở đây ta dùng chính symbolicMap/Z3Vars/ctx của luồng solve chính để chuyển
                // index như i, a + b, i + 1... thành Expr của cùng một Context.
                // Các Expr này sẽ được evaluate lại sau khi model chính được solve xong.
                collectSymbolicArrayIndexesFromAst(astNode, ctx);

                // BẮT, NỘI SOI KIỂU VÀ TẠO BIẾN GIẢ Z3
                astNode.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation methodInvocation) {
                        String methodName = methodInvocation.getName().getIdentifier();
                        String className = "";
                        if (methodInvocation.getExpression() != null) {
                            className = methodInvocation.getExpression().toString();
                        }

                        // Ta đã xử lý các hàm thư viện ở phía trước rồi nên bỏ qua
                        java.util.List<String> blackList = java.util.Arrays.asList("Math", "String",
                                "System", "Integer", "Double", "Thread");
                        if (blackList.contains(className)) {
                            System.out.println("không mock class thư viện: " + className);
                            return super.visit(methodInvocation); // Trả về bình thường
                        }

                        String methodKey = className.isEmpty() ? methodName : className + "_" + methodName;
                        int count = mockMethodCounter.getOrDefault(methodKey, 0) + 1;
                        mockMethodCounter.put(methodKey, count);
                        String mockVariableName = "mock_" + methodKey + "_" + count;

                        MockInfo info = new MockInfo(className, methodName, mockVariableName);
                        boolean alreadyExists = false;
                        for (MockInfo existingMock : currentMockInfos) {
                            // Nếu đã từng mock hàm này rồi thì bỏ qua, không add thêm
                            if (existingMock.className.equals(info.className) &&
                                    existingMock.methodName.equals(info.methodName)) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists) {
                            currentMockInfos.add(info); // Chỉ add khi chưa tồn tại
                        }

                        int argCount = methodInvocation.arguments().size();

                        String clonedDirPath = "D:\\projectLAB\\backend\\jcia-backend\\core-engine\\cfg\\src\\main\\java\\data\\clonedProject";

                        Class<?> returnType = ReflectionStubHelper.getReturnType(methodInvocation, className, methodName, argCount, clonedDirPath);

                        if (returnType == null) {
                            System.out.println("class lạ (" + className + "), không thể tìm được kiểu trả về, tự động ép về int");
                            returnType = int.class;
                        }

                        if (returnType != null) {
                            Sort z3Sort = ReflectionStubHelper.getZ3Sort(returnType, ctx);

                            Expr mockExpr = ctx.mkConst(mockVariableName, z3Sort);

                            Z3VariableWrapper wrapper = new Z3VariableWrapper(mockExpr);
                            if (!haveDuplicateVariable(wrapper)) {
                                Z3Vars.add(wrapper);
                            }

                            AST ast = methodInvocation.getAST();
                            PrimitiveType.Code typeCode = getPrimitiveTypeCode(returnType.getSimpleName());

                            if (typeCode != null) {
                                // Mapping kiểu dữ liệu cho hàm in kết quả Z3
                                variableTypeMap.put(mockVariableName, typeCode);

                                try {
                                    SingleVariableDeclaration fakeParam = ast.newSingleVariableDeclaration();
                                    fakeParam.setName(ast.newSimpleName(mockVariableName));
                                    fakeParam.setType(ast.newPrimitiveType(typeCode));

                                    // Đưa vào memory map dưới dạng Parameter để Tool nhận nó là biến Symbolic
                                    AstNode.executeASTNode(fakeParam, symbolicMap);
                                } catch (Exception e) {
                                    System.out.println("   ---> Lỗi tạo fake parameter: " + e.getMessage());
                                }
                            }

                            // THAY THẾ AST NODE: Cắt bỏ MethodInvocation, thế bằng SimpleName
                            try {
                                SimpleName mockNameNode = ast.newSimpleName(mockVariableName);
                                org.eclipse.jdt.core.dom.StructuralPropertyDescriptor location = methodInvocation.getLocationInParent();
                                if (location != null) {
                                    methodInvocation.getParent().setStructuralProperty(location, mockNameNode);
                                    System.out.println("   ---> Đã thay thế thành công lời gọi hàm thành biến" + mockVariableName);
                                }
                            } catch (Exception e) {
                                System.out.println("   ---> Lỗi thay thế AST: " + e.getMessage());
                            }
                        }
                        return super.visit(methodInvocation);
                    }
                });

                AstNode executedAstNode = Rewrite.reStm(astNode, symbolicMap);

                if (currentNode.getData() instanceof CfgBoolExprNode) { // Condition
                    CfgBoolExprNode boolNode = (CfgBoolExprNode) currentCfgNode;

                    boolean isGoingToFalseBranch = false;

                    if (currentNode.getNext() != null) {
                        CfgNode nextCfgNode = currentNode.getNext().getData(); // Node tiếp theo trong đường đi

                        // kiểm tra: Node tiếp theo có phải là con ở nhánh False của Node hiện tại không
                        if (nextCfgNode == boolNode.getFalseNode()) {
                            isGoingToFalseBranch = true;
                        }
                    }
                    // Nếu xác định là đi nhánh False -> Phủ định biểu thức
                    if (isGoingToFalseBranch) {
                        PrefixExpressionNode newAstNode = new PrefixExpressionNode();
                        newAstNode.setOperator(PrefixExpression.Operator.NOT);
                        newAstNode.setOperand((ExpressionNode) executedAstNode);

                        executedAstNode = PrefixExpressionNode.executePrefixExpressionNode(newAstNode, symbolicMap);
                    }

                    if (executedAstNode instanceof BooleanLiteralNode) {
                        currentNode = currentNode.getNext();
                        continue;
                    }

                    Expr expr = OperationExpressionNode.createZ3Expression(
                            (ExpressionNode) executedAstNode, ctx, Z3Vars, symbolicMap);

                    BoolExpr constraint;
                    if (expr instanceof BoolExpr) {
                        constraint = (BoolExpr) expr;
                    } else if (expr instanceof BitVecExpr) {
                        constraint = ctx.mkNot(ctx.mkEq((BitVecExpr) expr, ctx.mkBV(0, ((BitVecExpr) expr).getSortSize())));
                    } else {
                        throw new RuntimeException("Unsupported constraint type: " + expr);
                    }

                    if (finalZ3Expression == null) finalZ3Expression = constraint;
                    else {
                        finalZ3Expression = ctx.mkAnd(finalZ3Expression, constraint);
                    }
                } else if (astNode instanceof VariableDeclarationStatement) {
                    VariableDeclarationStatement stm = (VariableDeclarationStatement) astNode;
                    List<VariableDeclarationFragment> fragments = stm.fragments();
                    for (VariableDeclarationFragment fragment : fragments) {
                        String name = fragment.getName().getIdentifier();
                        Expression initializer = fragment.getInitializer();
                        if (initializer != null) { //Declaration with initialization
                            if (stm.getType() instanceof PrimitiveType) {
                                PrimitiveType type = (PrimitiveType) stm.getType();

                                symbolicMap.declarePrimitiveTypeVariable(type, name,
                                        ExpressionNode.executeExpression(initializer, symbolicMap));

                            } else if (stm.getType() instanceof ArrayType) {
                                ArrayType type = (ArrayType) stm.getType();
                                ArrayCreation arrayCreation = (ArrayCreation) initializer;
                                ArrayCreationWithNewKeyWord strategy = new ArrayCreationWithNewKeyWord();
                                AstNode element = ArrayCreationNode.executeArrayCreation(arrayCreation,
                                        symbolicMap, strategy);
                                ArrayNode arrayNode = (ArrayNode) element;
                                int numberOfDimensions = arrayNode.getNumberOfDimensions();
                                for (int i = 0; i < numberOfDimensions; i++) {
                                    ExpressionNode lengthOfArray = arrayNode.getLengthOfDimensions();
                                    if (lengthOfArray instanceof NameNode) {
                                        String nameNode = NameNode.getStringNameNode((NameNode) lengthOfArray);
                                        Expr nameNodeExpr = Variable.createZ3Variable(symbolicMap.getVariable(nameNode), ctx);

                                        BoolExpr constraint;
                                        if (nameNodeExpr instanceof BitVecExpr) {
                                            constraint = ctx.mkBVSGE((BitVecExpr) nameNodeExpr, ctx.mkBV(0, ((BitVecExpr) nameNodeExpr).getSortSize()));
                                        } else if (nameNodeExpr instanceof ArithExpr) {
                                            constraint = ctx.mkGe((ArithExpr) nameNodeExpr, ctx.mkInt(0));
                                        } else {
                                            throw new RuntimeException("Unexpected type for array length: " + nameNodeExpr);
                                        }

                                        if (finalZ3Expression == null) finalZ3Expression = constraint;
                                        else finalZ3Expression = ctx.mkAnd(finalZ3Expression, constraint);
                                    }
                                }
                                symbolicMap.declareArrayTypeVariable(type, name, type.getDimensions(), element);
                            } else {
                                throw new RuntimeException(stm.getType().getClass() + " is invalid!!");
                            }
                        } else { // Declaration without initialization
                            if (stm.getType() instanceof PrimitiveType) {
                                PrimitiveType type = (PrimitiveType) stm.getType();

                                symbolicMap.declarePrimitiveTypeVariable(type, name,
                                        PrimitiveTypeNode.changePrimitiveTypeToLiteralInitialization(type));

                            } else {
                                throw new RuntimeException("Only deal with PrimitiveType!!");
                            }
                        }
                    }

                }
            }

            if (astNode instanceof ThrowStatement) {
                break;
            }
            currentNode = currentNode.getNext();
        }

        currentCfgNode = null;
        System.out.println("=== Final Z3 Constraint ===");
        System.out.println(finalZ3Expression.simplify());
        System.out.println(finalZ3Expression.toString());

        model = createModel(ctx, (BoolExpr) finalZ3Expression);
        // Sau khi đã có model chính, mới evaluate các index symbolic đã thu được trước đó.
        // Đây là phần cốt lõi của "mức 1": index không có solver riêng, mà dùng trực tiếp
        // nghiệm của solver chính để suy ra maxIndex cho từng tham số mảng.
        updateArrayParameterLengthsFromModel();
        evaluateAndSaveTestDataCreated(ctx);
        return Z3Vars;
    }

    private void executeParameters(Context ctx) {
        Z3Vars = new ArrayList<>();
        for (ASTNode astNode : parameters) {
            AstNode.executeASTNode(astNode, symbolicMap);
            createZ3ParameterVariable(astNode, ctx, symbolicMap, Z3Vars);
        }
    }

    private void createZ3ParameterVariable(ASTNode parameter, Context ctx) {
        createZ3ParameterVariable(parameter, ctx, symbolicMap, Z3Vars);
    }

    // Dùng chung cho cả luồng solve chính và luồng suy kích thước mảng.
    // Việc tách hàm này giúp pass suy maxIndex có thể dựng đúng biến Z3 cho parameter
    // mà không phải làm bẩn trạng thái solve thật của object hiện tại.
    private void createZ3ParameterVariable(ASTNode parameter, Context ctx,
                                           MemoryModel memoryModel,
                                           List<Z3VariableWrapper> z3Vars) {
        if (parameter instanceof SingleVariableDeclaration) {
            SingleVariableDeclaration declaration = (SingleVariableDeclaration) parameter;
            String name = declaration.getName().toString();

            Variable variable = memoryModel.getVariable(name);

            if (variable instanceof PrimitiveTypeVariable) {
                Expr z3Variable = Variable.createZ3Variable(variable, ctx);
                if (z3Variable != null) {
                    Z3VariableWrapper z3VariableWrapper = new Z3VariableWrapper(z3Variable);
                    if (!haveDuplicateVariable(z3VariableWrapper, z3Vars)) {
                        z3Vars.add(z3VariableWrapper);
                    }
                }
            } else if (variable instanceof ArrayTypeVariable) {
                ArrayTypeVariable arrayTypeVariable = (ArrayTypeVariable) variable;
                Z3VariableWrapper z3VariableWrapper = new Z3VariableWrapper(arrayTypeVariable);
                if (!haveDuplicateVariable(z3VariableWrapper, z3Vars)) {
                    z3Vars.add(z3VariableWrapper);
                }
            } else {
                throw new RuntimeException("Invalid type variable");
            }
        } else {
            throw new RuntimeException("Invalid parameter");
        }
    }

    private boolean haveDuplicateVariable(Z3VariableWrapper z3Variable) {
        return haveDuplicateVariable(z3Variable, Z3Vars);
    }

    // Hàm overload này cho phép pass suy index symbolic kiểm tra duplicate trên
    // một danh sách biến Z3 tạm, thay vì chỉ dựa vào danh sách solve chính.
    private boolean haveDuplicateVariable(Z3VariableWrapper z3Variable, List<Z3VariableWrapper> z3Vars) {
        for (Z3VariableWrapper i : z3Vars) {
            if (i.equals(z3Variable)) {
                return true;
            }
        }
        return false;
    }

    private Model createModel(Context ctx, BoolExpr f) {
        Solver s = ctx.mkSolver();
        if (f != null) {
            s.add(f);
        }
//        System.out.println(s);

        Status satisfaction = s.check();
        if (satisfaction != Status.SATISFIABLE) {
            throw new RuntimeException("Expression is UNSATISFIABLE");
        } else {
            return s.getModel();
        }
    }

    private void evaluateAndSaveTestDataCreated(Context ctx) {
        if (model != null) {
            StringBuilder result = new StringBuilder();
            Map<String, String> evaluatedValues = new HashMap<>();

            // Quét tất cả các biến Z3 đã giải được và lưu vào Map tạm
            for (Z3VariableWrapper z3VariableWrapper : Z3Vars) {
                if (z3VariableWrapper.getPrimitiveVar() != null) {
                    Expr primitiveVar = z3VariableWrapper.getPrimitiveVar();
                    Expr evaluateResult = model.evaluate(primitiveVar, true);
                    String name = primitiveVar.toString();
                    String stringValue = "0";

                    PrimitiveType.Code originalTypeCode = variableTypeMap.get(name);
                    String typeName = (originalTypeCode != null) ? originalTypeCode.toString() : "";

                    if (evaluateResult instanceof BitVecNum) {
                        BitVecNum bvNum = (BitVecNum) evaluateResult;
                        BigInteger val = bvNum.getBigInteger();
                        switch (typeName) {
                            case "byte":
                                stringValue = String.valueOf(val.byteValue());
                                break;
                            case "short":
                                stringValue = String.valueOf(val.shortValue());
                                break;
                            case "char":
                                stringValue = String.valueOf(val.intValue() & 0xFFFF);
                                break;
                            case "long":
                                stringValue = String.valueOf(val.longValue());
                                break;
                            default:
                                stringValue = String.valueOf(val.intValue());
                                break;
                        }
                    } else if (evaluateResult instanceof FPNum) {
                        FPNum fpNum = (FPNum) evaluateResult;
                        if (fpNum.isNaN()) {
                            stringValue = "NaN";
                        } else if (fpNum.isInf()) {
                            stringValue = fpNum.isNegative() ? "-Infinity" : "Infinity";
                        } else {
                            Expr bvExpr = ctx.mkFPToIEEEBV(fpNum).simplify();
                            if (bvExpr instanceof BitVecNum) {
                                BigInteger bits = ((BitVecNum) bvExpr).getBigInteger();
                                if (fpNum.getEBits() == 8 && fpNum.getSBits() == 24) {
                                    stringValue = String.valueOf(Float.intBitsToFloat(bits.intValue()));
                                } else {
                                    stringValue = String.valueOf(Double.longBitsToDouble(bits.longValue()));
                                }
                            } else {
                                stringValue = fpNum.toString();
                            }
                        }
                    } else if (evaluateResult instanceof BoolExpr) {
                        stringValue = evaluateResult.isTrue() ? "true" : "false";
                    } else {
                        stringValue = evaluateResult.toString();
                    }

                    evaluatedValues.put(name, stringValue);

                    for (MockInfo info : currentMockInfos) {
                        if (info.mockVarName.equals(name)) {
                            info.solveValue = stringValue;
                        }
                    }
                }
            }

            // Lắp ráp lại dữ liệu theo đúng định dạng Parameter đầu vào
            if (this.parameters != null) {
                for (int i = 0; i < parameters.size(); i++) {
                    ASTNode param = parameters.get(i);
                    if (param instanceof SingleVariableDeclaration) {
                        SingleVariableDeclaration decl = (SingleVariableDeclaration) param;
                        String paramName = decl.getName().getIdentifier();

                        if (decl.getType().isArrayType()) {
                            int arrayLength = parameterArrayLengths.getOrDefault(paramName, 1);
                            StringBuilder arrStr = new StringBuilder();
                            for (int k = 0; k < arrayLength; k++) {

                                String flatName = paramName + "_" + k;
                                arrStr.append(evaluatedValues.getOrDefault(flatName, "0"));
                                if (k < arrayLength - 1) arrStr.append(",");
                            }
                            result.append(arrStr.toString());
                        } else {
                            result.append(evaluatedValues.getOrDefault(paramName, "0"));
                        }
                    }
                    if (i != parameters.size() - 1) {
                        result.append("\n");
                    }
                }
            }

            this.globalZ3Result = result.toString();
            writeDataToFile(result.toString());
        }
    }

    public Object[] getEvaluatedTestData(Class<?>[] parameterClasses) {
        List<Object> result = new ArrayList<>();

        String[] lines = this.globalZ3Result.split("\\r?\\n");

        for (int i = 0; i < parameterClasses.length; i++) {
            // nếu z3 ko giải được, bỏ qua
            if (i >= lines.length || lines[i].trim().isEmpty()) {
                result.add(null);
                continue;
            }

            Class<?> parameterClass = parameterClasses[i];
            String lineData = lines[i].trim();

            // tham số là biến đơn
            if (parameterClass.isPrimitive()) {
                result.add(parsePrimitiveString(lineData, parameterClass.getName()));
            }
            // tham số là mảng
            else if (parameterClass.isArray()) {
                String[] strElements = lineData.split(",");

                // Lấy kiểu dữ liệu bên trong mảng
                Class<?> componentType = parameterClass.getComponentType();

                // tạo mảng
                Object arrayInstance = Array.newInstance(componentType, strElements.length);

                // Nhét từng con số vào mảng
                for (int j = 0; j < strElements.length; j++) {
                    // Ép chuỗi thành số
                    Object val = parsePrimitiveString(strElements[j].trim(), componentType.getName());
                    // Lưu vào mảng
                    Array.set(arrayInstance, j, val);
                }

                // Thêm mảng hoàn chỉnh vào danh sách kết quả
                result.add(arrayInstance);
            } else {
                result.add(null);
            }
        }

        return result.toArray();
    }

    private Object parsePrimitiveString(String valStr, String type) {
        if ("int".equals(type)) return Integer.parseInt(valStr);
        if ("boolean".equals(type)) return Boolean.parseBoolean(valStr);
        if ("byte".equals(type)) return Byte.parseByte(valStr);
        if ("short".equals(type)) return Short.parseShort(valStr);
        if ("char".equals(type)) return (char) Integer.parseInt(valStr);
        if ("long".equals(type)) return Long.parseLong(valStr);
        if ("float".equals(type)) return Float.parseFloat(valStr);
        if ("double".equals(type)) return Double.parseDouble(valStr);
        throw new RuntimeException("Chưa hỗ trợ ép kiểu Z3 cho: " + type);
    }

    private ArrayNode createVirtualArray(ArrayType arrayType, int length) {
        ArrayNode virtualArray = new ArrayNode();
        virtualArray.setNumberOfDimensions(arrayType.getDimensions());
        virtualArray.setLengthOfDimensions(IntegerLiteralNode.executeIntegerLiteral(length));

        Type elementType = arrayType.getElementType();
        if (elementType.isPrimitiveType()) {
            PrimitiveType primitiveType = (PrimitiveType) elementType;
            PrimitiveTypeNode primitiveTypeNode = new PrimitiveTypeNode();
            primitiveTypeNode.setTypeCode(primitiveType.getPrimitiveTypeCode());
            virtualArray.setType(primitiveTypeNode);

            LiteralNode[] defaultElements =
                    PrimitiveTypeNode.changePrimitiveTypeToLiteralInitializationArray(primitiveType, length);
            virtualArray.setElements(0, defaultElements);
        }

        return virtualArray;
    }

    private int inferArrayParameterLength(String arrayName) {
        int maxIndex = -1;
        Node currentPathNode = testPath != null ? testPath.getCurrentFirst() : null;
        // bước suy kích thước sớm này chỉ lấy index concrete.
        // Các index symbolic sẽ được xử lý tách riêng sau khi model chính đã có.
        // Mục tiêu ở đây chỉ là tạo được virtual array tối thiểu để luồng solve chính tiếp tục chạy.
        while (currentPathNode != null) {
            ASTNode astNode = currentPathNode.getData().getAst();
            if (astNode != null) {
                ArrayParameterAccessVisitor visitor = new ArrayParameterAccessVisitor(arrayName);
                astNode.accept(visitor);
                maxIndex = Math.max(maxIndex, visitor.getMaxConcreteIndex());
            }
            currentPathNode = currentPathNode.getNext();
        }

        // Nếu không tìm được index concrete nào thì engine vẫn trả mảng độ dài 1 để giữ
        // tương thích với luồng cũ. Độ dài thật sẽ được cập nhật lại sau khi evaluate index symbolic.
        return Math.max(maxIndex + 1, 1);
    }

    // Model tạm của pass suy index có thể trả về BitVec, Int hoặc FP tùy kiểu biểu thức.
    // Hàm này chuẩn hóa tất cả về int để lấy maxIndex thống nhất.
    private int evaluateIndexFromModel(Model inferenceModel, Expr symbolicIndexExpr) {
        Expr evaluatedExpr = inferenceModel.evaluate(symbolicIndexExpr, true);
        if (evaluatedExpr instanceof BitVecNum) {
            return ((BitVecNum) evaluatedExpr).getBigInteger().intValue();
        }
        if (evaluatedExpr instanceof IntNum) {
            return ((IntNum) evaluatedExpr).getInt();
        }
        if (evaluatedExpr instanceof RatNum) {
            RatNum ratNum = (RatNum) evaluatedExpr;
            return ratNum.getBigIntNumerator().divide(ratNum.getBigIntDenominator()).intValue();
        }
        if (evaluatedExpr instanceof FPNum) {
            return (int) Double.parseDouble(evaluatedExpr.toString());
        }
        throw new RuntimeException("Unsupported solved index type: " + evaluatedExpr);
    }

    // Thu các index symbolic của truy cập mảng ngay trong luồng solve chính.
    // Hàm này không solve gì cả; nó chỉ biến index thành Expr của cùng Context với solver chính.
    // Ví dụ:
    // - arr[i]     -> lưu Expr của i
    // - arr[a + b] -> lưu Expr của a + b
    // - arr[3]     -> bỏ qua ở đây vì index concrete đã được xử lý ở inferArrayParameterLength()
    private void collectSymbolicArrayIndexesFromAst(ASTNode astNode, Context ctx) {
        astNode.accept(new ASTVisitor() {
            @Override
            public boolean visit(ArrayAccess node) {
                if (!(node.getArray() instanceof SimpleName) || node.getIndex() instanceof NumberLiteral) {
                    return super.visit(node);
                }

                String arrayName = ((SimpleName) node.getArray()).getIdentifier();
                AstNode executedIndexNode = AstNode.executeASTNode(node.getIndex(), symbolicMap);
                if (!(executedIndexNode instanceof ExpressionNode)) {
                    return super.visit(node);
                }

                try {
                    Expr symbolicIndexExpr = OperationExpressionNode.createZ3Expression(
                            (ExpressionNode) executedIndexNode, ctx, Z3Vars, symbolicMap);
                    symbolicArrayIndexExpressions
                            .computeIfAbsent(arrayName, ignored -> new ArrayList<>())
                            .add(symbolicIndexExpr);
                } catch (RuntimeException ex) {
                    // Một số index có thể chưa convert được ở thời điểm quét hiện tại.
                    // Ta bỏ qua chúng để không làm hỏng luồng solve chính; mảng sẽ fallback
                    // về độ dài đã suy được từ index concrete hoặc giá trị mặc định.
                    System.out.println("Khong the thu thap index symbolic cho mang " + arrayName
                            + " tai node hien tai: " + ex.getMessage());
                }

                return super.visit(node);
            }
        });
    }

    // Sau khi model chính đã solve xong, dùng chính model đó để evaluate mọi index symbolic đã thu.
    // Đây là phần tách biệt luồng giải chính với luồng suy kích thước mảng:
    // - solver chính chịu trách nhiệm tìm nghiệm cho path
    // - bước này chỉ là hậu xử lý trên model để cập nhật parameterArrayLengths
    private void updateArrayParameterLengthsFromModel() {
        if (model == null) {
            return;
        }

        for (Map.Entry<String, List<Expr>> entry : symbolicArrayIndexExpressions.entrySet()) {
            String arrayName = entry.getKey();
            int maxIndex = parameterArrayLengths.getOrDefault(arrayName, 1) - 1;

            for (Expr symbolicIndexExpr : entry.getValue()) {
                try {
                    int solvedIndex = evaluateIndexFromModel(model, symbolicIndexExpr);
                    if (solvedIndex >= 0) {
                        maxIndex = Math.max(maxIndex, solvedIndex);
                    }
                } catch (RuntimeException ex) {
                    // Nếu một index cụ thể không evaluate được từ model chính thì chỉ bỏ qua index đó.
                    // Không được để lỗi hậu xử lý này làm hỏng toàn bộ quá trình tạo test data.
                    System.out.println("Khong the evaluate index symbolic cua mang " + arrayName
                            + " tu model chinh: " + ex.getMessage());
                }
            }

            parameterArrayLengths.put(arrayName, Math.max(maxIndex + 1, 1));
        }
    }

    private static final class ArrayParameterAccessVisitor extends ASTVisitor {
        private final String arrayName;
        private int maxIndex = -1;

        private ArrayParameterAccessVisitor(String arrayName) {
            this.arrayName = arrayName;
        }

        @Override
        public boolean visit(ArrayAccess node) {

            if (node.getArray() instanceof SimpleName
                    && arrayName.equals(((SimpleName) node.getArray()).getIdentifier())
                    && node.getIndex() instanceof NumberLiteral) {
                try {
                    // lấy index của array node
                    int index = Integer.parseInt(((NumberLiteral) node.getIndex()).getToken());
                    
                    if (index >= 0) {
                        //
                        maxIndex = Math.max(maxIndex, index);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return super.visit(node);
        }

        private int getMaxConcreteIndex() {
            return maxIndex;
        }
    }

    private Object scanValue(Scanner scanner, String type) {
        if ("int".equals(type)) {
            return scanner.nextInt();
        } else if ("boolean".equals(type)) {
            return scanner.nextBoolean();
        } else if ("byte".equals(type)) {
            return scanner.nextByte();
        } else if ("short".equals(type)) {
            return scanner.nextShort();
        } else if ("char".equals(type)) {
            return (char) scanner.nextInt();
        } else if ("long".equals(type)) {
            return scanner.nextLong();
        } else if ("float".equals(type)) {
            return scanner.nextFloat();
        } else if ("double".equals(type)) {
            return scanner.nextDouble();
        } else if ("void".equals(type)) {
            return null;
        } else {
            throw new RuntimeException("Unsupported type: " + type);
        }
    }

    public static Object[] createRandomTestData(Class<?>[] parameterClasses) {
        Object[] result = new Object[parameterClasses.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = createRandomVariableData(parameterClasses[i]);
        }

        return result;
    }

    /**
     * Hàm hỗ trợ chuyển đổi String sang PrimitiveType.Code của AST
     */
    private PrimitiveType.Code getPrimitiveTypeCode(String typeName) {
        switch (typeName) {
            case "int":
                return PrimitiveType.INT;
            case "boolean":
                return PrimitiveType.BOOLEAN;
            case "byte":
                return PrimitiveType.BYTE;
            case "short":
                return PrimitiveType.SHORT;
            case "char":
                return PrimitiveType.CHAR;
            case "long":
                return PrimitiveType.LONG;
            case "float":
                return PrimitiveType.FLOAT;
            case "double":
                return PrimitiveType.DOUBLE;
            case "void":
                return PrimitiveType.VOID;
            default:
                return PrimitiveType.INT;
        }
    }

    public static class MockInfo {
        public String className;
        public String methodName;
        public String mockVarName;
        public String solveValue;

        public MockInfo(String className, String methodName, String mockVarName) {
            this.className = className;
            this.methodName = methodName;
            this.mockVarName = mockVarName;
        }
    }

    private static Object createRandomVariableData(Class<?> parameterClass) {
        if (parameterClass.isPrimitive()) {
            return createRandomPrimitiveVariableData(parameterClass);
        } else if (parameterClass.isArray()) {
            return createRandomArrayVariableData(parameterClass);
        }
        throw new RuntimeException("Unsupported type: " + parameterClass.getName());
    }

    private static Object createRandomArrayVariableData(Class<?> parameterClass) {
        int totalDimentsions = 1;
        for (Class<?> componentType = parameterClass.getComponentType(); ; ) {
            if (componentType.isArray()) {
                totalDimentsions++;
                componentType = componentType.getComponentType();
            } else {
                int[] dimensions = new int[totalDimentsions];
                Arrays.fill(dimensions, 10);
                return Array.newInstance(componentType, dimensions);
            }
        }
    }

    private static Object createRandomPrimitiveVariableData(Class<?> parameterClass) {
        String className = parameterClass.getName();
        Random random = new Random();

        if ("int".equals(className)) {
//            return random.nextInt();
            return 8;
        } else if ("boolean".equals(className)) {
            return random.nextInt() % 2 == 0;
        } else if ("byte".equals(className)) {
            return (byte) ((Math.random() * (127 - (-128)) + (-128)));
        } else if ("short".equals(className)) {
            return (short) ((Math.random() * (32767 - (-32768)) + (-32768)));
        } else if ("char".equals(className)) {
//            return (char) random.nextInt();
            return 'X';
        } else if ("long".equals(className)) {
//            return random.nextLong();
            return 16;
        } else if ("float".equals(className)) {
//            return random.nextFloat();
            return 8.0f;
        } else if ("double".equals(className)) {
//            return random.nextDouble();
            return 8.0;
        } else if ("void".equals(className)) {
            return null;
        }
        throw new RuntimeException("Unsupported type: " + className);
    }

    private void writeDataToFile(String data) {
        try {
            FileWriter writer = new FileWriter(FilePath.generatedTestDataPath);
            writer.write(data + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Model getModel() {
        return model;
    }

    public static CfgNode getCurrentCfgNode() {
        return currentCfgNode;
    }


}
