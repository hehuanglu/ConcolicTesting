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
import core.ast.Expression.Literal.CharacterLiteralNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NumberLiteral.DoubleLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Name.NameNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.ast.Expression.OperationExpression.PrefixExpressionNode;
import core.ast.Type.AnnotatableType.PrimitiveTypeNode;
import core.ast.additionalNodes.Node;
import core.cfg.CfgBoolExprNode;
import core.cfg.CfgNode;
import core.path.Path;
import core.testGeneration.TestGeneration;
import core.variable.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.*;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.*;

@Slf4j
public class SymbolicExecutionRewrite {
    private MemoryModel symbolicMap;
    private List<Z3VariableWrapper> Z3Vars;
    private Model model;
    private Path testPath;
    private List<ASTNode> parameters;
    private static CfgNode currentCfgNode;
    public static Map<String, String> variableTypeMap = new HashMap<>();
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
    //  Sổ tay lưu trạng thái hiện tại của các Mảng Z3
    public static ThreadLocal<Map<String, Expr>> z3ArrayStateMap = ThreadLocal.withInitial(java.util.HashMap::new);
    public static ThreadLocal<Context> globalCtx = new ThreadLocal<>();
    public static ThreadLocal<List<Z3VariableWrapper>> globalZ3Vars = new ThreadLocal<>();
    public static Map<String, Class<?>> variableGenericTypeMap = new HashMap<>();

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

        globalCtx.set(ctx);
        globalZ3Vars.set(Z3Vars);
        z3ArrayStateMap.get().clear();

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
                        log.info("Phát hiện Parameter là Mảng (Array): {}", name);
                        int inferredLength = inferArrayParameterLength(name);
                        parameterArrayLengths.put(name, inferredLength);
                        ArrayNode virtualArray = createVirtualArray(arrayType, inferredLength);
                        symbolicMap.declareArrayTypeVariable(arrayType, name, arrayType.getDimensions(), virtualArray);
                    } else if (decl.getType().isParameterizedType()) {
                        ParameterizedType pType = (ParameterizedType) decl.getType();
                        System.out.println(" Phát hiện Parameter là Collection: " + name);
                        int inferredLength = inferArrayParameterLength(name);
                        parameterArrayLengths.put(name, inferredLength);
                        ArrayNode virtualList = createVirtualArrayForParameterized(pType, inferredLength, name);
                        symbolicMap.declareParameterizedTypeVariable(pType, name, virtualList);
                    }
                }
            }
        }

        int limit = 0;
        while (currentNode != null) {
            if (++limit > 400) break;
            currentCfgNode = currentNode.getData();
            //log.debug("Phân tích Node [Line {}]: {}", currentCfgNode.getLineNumber(), currentCfgNode.getContentReport());
            ASTNode astNode = currentCfgNode.getAst();

            if (astNode != null) {
                // Thu các index symbolic ngay tại thời điểm node sắp được symbolic execution.
                // Ở đây ta dùng chính symbolicMap/Z3Vars/ctx của luồng solve chính để chuyển
                // index như i, a + b, i + 1... thành Expr của cùng một Context.
                // Các Expr này sẽ được evaluate lại sau khi model chính được solve xong.
                /*
                collectSymbolicArrayIndexesFromAst(astNode, ctx);

                // BẮT, NỘI SOI KIỂU VÀ TẠO BIẾN GIẢ Z3
                astNode.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation methodInvocation) {
                        String methodName = methodInvocation.getName().getIdentifier();
                        List<String> listMethods = Arrays.asList("get", "size", "set", "add");
                        if (listMethods.contains(methodName)) {
                            System.out.println("   ---> Bỏ qua Mock cho hàm của List: " + methodName + ". Để reStm xử lý.");
                            return true;
                        }
                        String className = "";
                        if (methodInvocation.getExpression() != null) {
                            className = methodInvocation.getExpression().toString();
                        }

                        ASTNode parentNode = methodInvocation.getParent();
                        String currentTestingMethodName = "";

                        // đi lùi lên trên cho đến khi gặp Node khai báo hàm
                        while (parentNode != null && !(parentNode instanceof MethodDeclaration)) {
                            parentNode = parentNode.getParent();
                        }

                        // Khi đã tìm thấy khung hàm, lấy ra
                        if (parentNode instanceof MethodDeclaration) {
                            currentTestingMethodName = ((MethodDeclaration) parentNode).getName().getIdentifier();
                        }

                        // Nếu expression là một biến đã tồn tại trong bộ nhớ (ví dụ: String input)
                        // thì đây là gọi hàm từ object, không phải gọi hàm static từ Class -> KHÔNG MOCK
                        if (methodInvocation.getExpression() != null) {
                            String expressionStr = methodInvocation.getExpression().toString();
                            try {
                                if (symbolicMap.getVariable(expressionStr) != null) {
                                    System.out.println("Bỏ qua mock vì " + expressionStr + " là một biến đối tượng.");
                                    return super.visit(methodInvocation);
                                }
                            } catch (Exception ignored) {
                                // Nếu không tìm thấy trong symbolicMap thì có thể là tên Class thật
                            }
                        }

                        // Ta đã xử lý các hàm thư viện ở phía trước rồi nên bỏ qua
                        List<String> blackList = Arrays.asList("Math", "String",
                                "System", "Integer", "Double", "Thread");
                        if (blackList.contains(className)) {
                            log.debug("Bỏ qua class thư viện chuẩn: {}", className);
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
                            log.warn("LỚP LẠ ({}): Không thể tìm được kiểu trả về cho hàm {}. Tự động ép về int.", className, methodName);
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
                                variableTypeMap.put(mockVariableName, typeCode.toString());

                                try {
                                    SingleVariableDeclaration fakeParam = ast.newSingleVariableDeclaration();
                                    fakeParam.setName(ast.newSimpleName(mockVariableName));
                                    fakeParam.setType(ast.newPrimitiveType(typeCode));

                                    // Đưa vào memory map dưới dạng Parameter để Tool nhận nó là biến Symbolic
                                    AstNode.executeASTNode(fakeParam, symbolicMap);
                                } catch (Exception e) {
                                    log.error("Lỗi tạo fake parameter cho hàm mock [{}]: {}", mockVariableName, e.getMessage(), e);
                                }
                            }

                            // THAY THẾ AST NODE: Cắt bỏ MethodInvocation, thế bằng SimpleName
                            try {
                                SimpleName mockNameNode = ast.newSimpleName(mockVariableName);
                                org.eclipse.jdt.core.dom.StructuralPropertyDescriptor location = methodInvocation.getLocationInParent();
                                if (location != null) {
                                    methodInvocation.getParent().setStructuralProperty(location, mockNameNode);
                                    log.debug("Đã thay thế thành công lời gọi hàm thành biến Mock: {}", mockVariableName);
                                }
                            } catch (Exception e) {
                                log.error("Lỗi thay thế cây AST tại hàm [{}]: {}", methodName, e.getMessage(), e);
                            }
                        }
                        return super.visit(methodInvocation);
                    }
                });
                */

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
                            } else if (stm.getType() instanceof SimpleType) {
                                SimpleType simpleType = (SimpleType) stm.getType();

                                symbolicMap.declareSimpleTypeVariable(simpleType, name,
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

        for (Map.Entry<String, List<Expr>> entry : symbolicArrayIndexExpressions.entrySet()) {
            int size = 1000; //
            for (Expr indexExpr : entry.getValue()) {
                if (indexExpr instanceof BitVecExpr) {
                    BitVecExpr bvIdx = (BitVecExpr) indexExpr;
                    BoolExpr bound = ctx.mkAnd(
                            ctx.mkBVSGE(bvIdx, ctx.mkBV(0, 32)),
                            ctx.mkBVSLT(bvIdx, ctx.mkBV(size, 32))
                    );
                    finalZ3Expression = ctx.mkAnd(finalZ3Expression, bound);
                }
            }
        }

        currentCfgNode = null;
        if (finalZ3Expression != null) {
            log.info("=== XÂY DỰNG XONG PHƯƠNG TRÌNH Z3 CHÍNH ===");
            log.debug(" - Raw Constraint: \n{}", finalZ3Expression.toString());
            log.info(" - Simplified Constraint: \n{}", finalZ3Expression.simplify());
        } else {
            log.warn("Không thu thập được bất kỳ Z3 Constraint nào trong hàm này!");
        }

        for (Z3VariableWrapper var : Z3Vars) {
            String varName = var.getPrimitiveVar().toString();
            if (varName.endsWith(".length")) {
                BoolExpr positiveLengthConstraint = ctx.mkGe((ArithExpr) var.getPrimitiveVar(), ctx.mkInt(0));
                if (finalZ3Expression == null) {
                    finalZ3Expression = positiveLengthConstraint;
                } else {
                    finalZ3Expression = ctx.mkAnd(finalZ3Expression, positiveLengthConstraint);
                }
            }
        }

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
            } else if (variable instanceof SimpleTypeVariable) {
                Expr z3Variable = Variable.createZ3Variable(variable, ctx);
                if (z3Variable != null) {
                    Z3VariableWrapper z3VariableWrapper = new Z3VariableWrapper(z3Variable);
                    if (!haveDuplicateVariable(z3VariableWrapper, z3Vars)) {
                        z3Vars.add(z3VariableWrapper);
                        System.out.println(z3VariableWrapper.getPrimitiveVar().getSort().toString());
                    }
                }
            } else if (variable instanceof ArrayTypeVariable) {
                // Khai báo mảng mới
                // Chỉ số mảng luôn là int 32-bit
                Sort domain = ctx.mkIntSort();

                // lấy đúng kích thước sort
                Sort range = ctx.mkIntSort();
                if (declaration.getType().isArrayType()) {
                    ArrayType arrType = (ArrayType) declaration.getType();
                    String elementTypeName = arrType.getElementType().toString();

                    if (elementTypeName.equals("long")) {
                        range = ctx.mkIntSort();
                    } else if (elementTypeName.equals("double")) {
                        range = ctx.mkFPSortDouble();
                    } else if (elementTypeName.equals("float")) {
                        range = ctx.mkFPSortSingle();
                    }
                }

                // Tạo kiểu mảng z3
                ArraySort z3ArraySort = ctx.mkArraySort(domain, range);

                // Khai báo mảng gốc với z3
                Expr z3ArrayBase = ctx.mkConst(name, z3ArraySort);

                // Bọc xong đưa vào danh sách Z3Vars để đi luồng chính
                Z3VariableWrapper z3VariableWrapper = new Z3VariableWrapper(z3ArrayBase);
                if (!haveDuplicateVariable(z3VariableWrapper, z3Vars)) {
                    z3Vars.add(z3VariableWrapper);
                }

                SymbolicExecutionRewrite.z3ArrayStateMap.get().put(name, z3ArrayBase);
            } else if (variable instanceof ParameterizedTypeVariable) {
                Sort domain = ctx.mkBitVecSort(32);
                Sort range;

                Class<?> genericClass = variableGenericTypeMap.get(name);
                if (genericClass.equals(Long.class)) {
                    range = ctx.mkBitVecSort(64);
                } else if (genericClass.equals(Double.class)) {
                    range = ctx.mkFPSortDouble();
                } else if (genericClass.equals(Float.class)) {
                    range = ctx.mkFPSortSingle();
                } else if (genericClass.equals(Integer.class)) {
                    range = ctx.mkBitVecSort(32);
                } else {
                    throw new RuntimeException("Chua ho tro kieu Generic cho" + genericClass);
                }

                ArraySort z3ArraySort = ctx.mkArraySort(domain, range);
                Expr z3ParameterizedBase = ctx.mkConst(name, z3ArraySort);
                Z3VariableWrapper z3VariableWrapper = new Z3VariableWrapper(z3ParameterizedBase);
                if (!haveDuplicateVariable(z3VariableWrapper, z3Vars)) {
                    z3Vars.add(z3VariableWrapper);
                }
                SymbolicExecutionRewrite.z3ArrayStateMap.get().put(name, z3ParameterizedBase);
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

        log.debug("Trạng thái Solver Z3 trước khi Check: \n{}", s.toString());

        Status satisfaction = s.check();
        if (satisfaction != Status.SATISFIABLE) {
            log.warn("Biểu thức hiện tại là UNSATISFIABLE. Không thể tìm ra nghiệm Z3.");
            throw new RuntimeException("Expression is UNSATISFIABLE");
        } else {
            log.info("Z3 đã giải thành công (SATISFIABLE)!");
            return s.getModel();
        }
    }

    private void evaluateAndSaveTestDataCreated(Context ctx) {
        if (model != null) {
            StringBuilder result = new StringBuilder();
            Map<String, String> evaluatedValues = new HashMap<>();

            // quét tất cả các biến Z3 cơ bản đã giải được và lưu vào Map tạm
            for (Z3VariableWrapper z3VariableWrapper : Z3Vars) {
                if (z3VariableWrapper.getPrimitiveVar() != null) {
                    Expr primitiveVar = z3VariableWrapper.getPrimitiveVar();
                    Expr evaluateResult = model.evaluate(primitiveVar, true);
                    String name = primitiveVar.toString();
                    String stringValue = "0";

                    Object originalTypeCode = variableTypeMap.get(name);
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
                                stringValue = String.valueOf(val.longValue()) + "L";
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
                        if (stringValue.startsWith("\"") && stringValue.endsWith("\"")) {
                            stringValue = stringValue.substring(1, stringValue.length() - 1);
                        }
                    }

                    evaluatedValues.put(name, stringValue);

                    for (MockInfo info : currentMockInfos) {
                        if (info.mockVarName.equals(name)) {
                            info.solveValue = stringValue;
                        }
                    }
                }
            }

            // lắp ráp lại dữ liệu theo đúng định dạng Parameter đầu vào
            if (this.parameters != null) {
                for (int i = 0; i < parameters.size(); i++) {
                    ASTNode param = parameters.get(i);
                    if (param instanceof SingleVariableDeclaration) {
                        SingleVariableDeclaration decl = (SingleVariableDeclaration) param;
                        String paramName = decl.getName().getIdentifier();

                        if (decl.getType().isArrayType() || decl.getType().isParameterizedType()) {
                            int arrayLength = parameterArrayLengths.getOrDefault(paramName, 1);

                            try {
                                Expr lengthVar = ctx.mkIntConst(paramName + ".length");
                                Expr evaluatedLength = model.evaluate(lengthVar, true);

                                if (evaluatedLength instanceof IntNum) {
                                    int z3SolvedLength = ((IntNum) evaluatedLength).getInt();
                                    if (z3SolvedLength > 0 && z3SolvedLength <= 1000) {
                                        arrayLength = z3SolvedLength;
                                        log.info("Mảng '{}': Z3 giải ra độ dài = {}", paramName, arrayLength);
                                    }
                                }
                            } catch (Exception e) {
                                log.error("Lỗi khi đọc độ dài mảng [{}] từ Z3: {}", paramName, e.getMessage(), e);
                                e.printStackTrace();
                            }

                            StringBuilder arrStr = new StringBuilder();

                            // lấy kiểu dữ liệu của mảng từ AST
//                            ArrayType arrayType = (ArrayType) decl.getType();
//                            String elementTypeName = arrayType.getElementType().toString();
                            String elementTypeName = "";
                            if (decl.getType().isArrayType()) {
                                // Nếu là mảng (int[], String[]...)
                                ArrayType arrayType = (ArrayType) decl.getType();
                                elementTypeName = arrayType.getElementType().toString();
                            } else {
                                // Nếu là List<Integer>, List<String>...
                                Class<?> genericClass = variableGenericTypeMap.get(paramName);
                                elementTypeName = genericClass.getSimpleName();
                            }

                            try {
                                // Chọn kích thước sort theo kiểu dữ liệu
                                Sort domainSort = ctx.mkIntSort();
                                Sort rangeSort;

                                if (elementTypeName.equals("long")) {
                                    rangeSort = ctx.mkIntSort();
                                } else if (elementTypeName.equals("double")) {
                                    rangeSort = ctx.mkFPSortDouble();
                                } else if (elementTypeName.equals("float")) {
                                    rangeSort = ctx.mkFPSortSingle();
                                } else {
                                    rangeSort = ctx.mkIntSort();
                                }

                                // dựng lại tham chiếu đến mảng gốc ban đầu với đúng sort
                                Expr z3ArrayBase = SymbolicExecutionRewrite.z3ArrayStateMap.get().get(paramName);
                                if (z3ArrayBase == null) {
                                    z3ArrayBase = ctx.mkConst(paramName, ctx.mkArraySort(domainSort, rangeSort));
                                }
                                for (int k = 0; k < arrayLength; k++) {
                                    Expr kExpr = ctx.mkInt(k);
                                    Expr selectExpr = ctx.mkSelect((ArrayExpr) z3ArrayBase, kExpr);

                                    Expr evaluatedElement = model.evaluate(selectExpr, true).simplify();

                                    String valStr = "0";

                                    if (evaluatedElement instanceof IntNum) {
                                        IntNum intNum = (IntNum) evaluatedElement;
                                        if (elementTypeName.equals("long") || elementTypeName.equals("Long")) {
                                            valStr = String.valueOf(intNum.getBigInteger().longValue()) + "L";
                                        } else {
                                            valStr = String.valueOf(intNum.getInt());
                                        }
                                    } else if (evaluatedElement instanceof FPNum) {
                                        FPNum fpNum = (FPNum) evaluatedElement;
                                        if (fpNum.isNaN()) {
                                            valStr = "Double.NaN";
                                        } else if (fpNum.isInf()) {
                                            valStr = fpNum.isNegative() ? "Double.NEGATIVE_INFINITY" : "Double.POSITIVE_INFINITY";
                                        } else {
                                            Expr bvExpr = ctx.mkFPToIEEEBV(fpNum).simplify();
                                            if (bvExpr instanceof BitVecNum) {
                                                BigInteger bits = ((BitVecNum) bvExpr).getBigInteger();
                                                if (elementTypeName.equals("float")) {
                                                    valStr = String.valueOf(Float.intBitsToFloat(bits.intValue()));
                                                } else {
                                                    valStr = String.valueOf(Double.longBitsToDouble(bits.longValue()));
                                                }
                                            } else {
                                                valStr = fpNum.toString();
                                            }
                                        }
                                    } else {
                                        valStr = evaluatedElement.toString();
                                    }

                                    arrStr.append(valStr);
                                    if (k < arrayLength - 1) arrStr.append(",");
                                }
                                log.debug("Đã dịch xong Mảng [{}]: [{}]", paramName, arrStr.toString());
                            } catch (Exception e) {
                                System.out.println("   ---> Lỗi lấy mảng Z3: " + e.getMessage());
                            }

                            result.append(arrStr.toString());

                        } else {
                            // Biến bình thường thì lấy từ map ta đã quét ở trên
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
            log.info("Đã ghi file kết quả Input thành công!");
        }
    }

    public Object[] getEvaluatedTestData(Class<?>[] parameterClasses) {
        List<Object> result = new ArrayList<>();

        String[] lines = this.globalZ3Result.split("\\r?\\n");
        log.debug("Bắt đầu parse {} dòng dữ liệu Z3 trả về thành Object Java...", lines.length);

        for (int i = 0; i < parameterClasses.length; i++) {
            // nếu z3 ko giải được, bỏ qua
            if (i >= lines.length) {
                log.warn("Dữ liệu Z3 bị thiếu hoặc rỗng ở tham số thứ {}. Gán giá trị mặc định (null).", i);
                result.add(null);
                continue;
            }

            Class<?> parameterClass = parameterClasses[i];
            String lineData = lines[i].trim();

            // tham số là biến đơn
            try {
                if (parameterClass.isPrimitive()) {
                    result.add(parsePrimitiveString(lineData, parameterClass.getName()));
                } else if (parameterClass == String.class) {
                    result.add(lineData);
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
                } else if (java.util.List.class.isAssignableFrom(parameterClass)) {
                    // Cắt chuỗi theo dấu phẩy
                    String[] strElements = lineData.split(",");
                    List<Object> listInstance = new ArrayList<>();

                    for (String str : strElements) {
                        listInstance.add(parsePrimitiveString(str.trim(), "int"));
                    }
                    result.add(listInstance);
                } else {
                    log.warn("Chưa hỗ trợ ép kiểu Object phức tạp: {}. Tự động gán null.", parameterClass.getName());
                    result.add(null);
                }
            } catch (Exception e) {
                log.error("Lỗi ép kiểu dữ liệu từ Z3 [{}] sang Java [{}]: {}", lineData, parameterClass.getName(), e.getMessage(), e);
                result.add(null);
            }
        }

        return result.toArray();
    }

    private Object parsePrimitiveString(String valStr, String type) {
        valStr = valStr.trim();

        if ("int".equals(type)) return Integer.parseInt(valStr);
        if ("boolean".equals(type)) return Boolean.parseBoolean(valStr);
        if ("byte".equals(type)) return Byte.parseByte(valStr);
        if ("short".equals(type)) return Short.parseShort(valStr);
        if ("char".equals(type)) return (char) Integer.parseInt(valStr);

        if ("long".equals(type)) {
            if (valStr.toUpperCase().endsWith("L")) {
                valStr = valStr.substring(0, valStr.length() - 1);
            }
            return Long.parseLong(valStr);
        }

        if ("float".equals(type)) {
            if (valStr.toUpperCase().endsWith("F")) {
                valStr = valStr.substring(0, valStr.length() - 1);
            }
            return Float.parseFloat(valStr);
        }

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
        int inferredLength = Math.max(maxIndex + 1, 1);

        log.debug("Suy luận kích thước tối thiểu cho mảng [{}] dựa trên Concrete Index là: {}", arrayName, inferredLength);

        return inferredLength;
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

                    log.debug("Đã thu thập Symbolic Index cho mảng [{}]: {}", arrayName, symbolicIndexExpr);
                } catch (RuntimeException ex) {
                    // Một số index có thể chưa convert được ở thời điểm quét hiện tại.
                    // Ta bỏ qua chúng để không làm hỏng luồng solve chính; mảng sẽ fallback
                    // về độ dài đã suy được từ index concrete hoặc giá trị mặc định.
                    log.warn("Không thể thu thập index symbolic cho mảng [{}] tại node hiện tại: {}. Fallback về độ dài an toàn.", arrayName, ex.getMessage());
                }

                return super.visit(node);
            }

            @Override
            public boolean visit(MethodInvocation node) {
                String methodName = node.getName().getIdentifier();
                if (!methodName.equals("get") || !(node.getExpression() instanceof SimpleName)) {
                    return super.visit(node);
                }

                if (node.arguments().isEmpty() || node.arguments().get(0) instanceof NumberLiteral) {
                    return super.visit(node);
                }

                String listName = ((SimpleName) node.getExpression()).getIdentifier();
                ASTNode indexNode = (ASTNode) node.arguments().get(0);
                AstNode executedIndexNode = AstNode.executeASTNode(indexNode, symbolicMap);

                if (!(executedIndexNode instanceof ExpressionNode)) {
                    return super.visit(node);
                }

                try {
                    Expr symbolicIndexExpr = OperationExpressionNode.createZ3Expression(
                            (ExpressionNode) executedIndexNode, ctx, Z3Vars, symbolicMap);

                    symbolicArrayIndexExpressions
                            .computeIfAbsent(listName, ignored -> new ArrayList<>())
                            .add(symbolicIndexExpr);
                } catch (RuntimeException ex) {
                    System.out.println("Khong the thu thap index symbolic cho List " + listName + ": " + ex.getMessage());
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
                    log.warn("Không thể evaluate index symbolic của mảng [{}] từ model chính: {}. Bỏ qua index này.", arrayName, ex.getMessage());
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

    private ArrayNode createVirtualArrayForParameterized(ParameterizedType pType, int length, String variableName) {
        // 1. Khởi tạo khay chứa ảo (ArrayNode)
        ArrayNode virtualArray = new ArrayNode();

        // Với Collection (List/Stack/Queue), ta luôn mặc định là mảng 1 chiều trong Symbolic Execution
        virtualArray.setNumberOfDimensions(1);
        virtualArray.setLengthOfDimensions(IntegerLiteralNode.executeIntegerLiteral(length));

        // 2. Xác định kiểu phần tử từ Map Generic (thay vì dùng getElementType như mảng)
        Class<?> genericClass = variableGenericTypeMap.get(variableName);

        if (genericClass != null) {
            // 3. Chuyển đổi Class sang PrimitiveTypeNode (để Tool hiểu loại dữ liệu)
            PrimitiveType.Code primitiveCode = mapClassToJDTPrimitiveCode(genericClass);
            PrimitiveTypeNode primitiveTypeNode = new PrimitiveTypeNode();
            primitiveTypeNode.setTypeCode(primitiveCode);
            virtualArray.setType(primitiveTypeNode);

            // 4. Đổ các "ô nhớ giả" (default elements) vào khay chứa
            // Ta sử dụng lại chính logic changePrimitiveTypeToLiteralInitializationArray của Tool
            // để tạo ra danh sách các LiteralNode mặc định (như 0, 0.0, false...)
            LiteralNode[] defaultElements = changeClassToLiteralInitializationArray(genericClass, length);
            virtualArray.setElements(0, defaultElements);
        }

        return virtualArray;
    }

    // Dịch từ Class sang Code của JDT PrimitiveType
    private PrimitiveType.Code mapClassToJDTPrimitiveCode(Class<?> clazz) {
        if (clazz.equals(Long.class)) return PrimitiveType.LONG;
        if (clazz.equals(Double.class)) return PrimitiveType.DOUBLE;
        if (clazz.equals(Float.class)) return PrimitiveType.FLOAT;
        if (clazz.equals(Boolean.class)) return PrimitiveType.BOOLEAN;
        if (clazz.equals(Character.class)) return PrimitiveType.CHAR;
        return PrimitiveType.INT; // Mặc định là Integer
    }

    // Tạo mảng các Literal mặc định giống hệt cách mảng đang làm
    private LiteralNode[] changeClassToLiteralInitializationArray(Class<?> clazz, int length) {
        // Ta ánh xạ từ Class sang đúng các hàm sinh Array của Tool
        if (clazz.equals(Integer.class)) {

            return IntegerLiteralNode.createIntegerLiteralInitializationArray(length);

        } else if (clazz.equals(Double.class)) {

            return DoubleLiteralNode.createDoubleLiteralInitializationArray(length);

        } else if (clazz.equals(Character.class)) {

            return CharacterLiteralNode.createCharacterLiteralInitializationArray(length);

        } else if (clazz.equals(Boolean.class)) {

            return BooleanLiteralNode.createBooleanLiteralInitializationArray(length);

        } else {
            throw new RuntimeException("Chưa hỗ trợ khởi tạo mảng cho kiểu: " + clazz.getName());
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
        List<String> parameterNames = TestGeneration.getParameterNames();
        for (int i = 0; i < result.length; i++) {
            String parameterName = "";
            if (parameterNames != null && i < parameterNames.size()) {
                parameterName = parameterNames.get(i);
            }
            result[i] = createRandomVariableData(parameterClasses[i], parameterName);
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

    private static Object createRandomVariableData(Class<?> parameterClass, String parameterName) {
        if (parameterClass.isPrimitive()) {
            return createRandomPrimitiveVariableData(parameterClass);
        } else if (parameterClass.isArray()) {
            return createRandomArrayVariableData(parameterClass);
        } else if (java.util.List.class.isAssignableFrom(parameterClass)) {
            return createRandomCollectionVariableData(parameterClass, parameterName);
        } else {
            try {
                return createRandomSimpleTypeData(parameterClass);
            } catch (RuntimeException ex) {
                throw new RuntimeException("Unsupported type: " + parameterClass.getName());
            }
        }
    }

    private static Object createRandomArrayVariableData(Class<?> parameterClass) {
        // đếm số chiều và tìm kiểu dữ liệu gốc
        int totalDimensions = 1;
        Class<?> componentType = parameterClass.getComponentType();
        while (componentType.isArray()) {
            totalDimensions++;
            componentType = componentType.getComponentType();
        }

        // tạo mảng với kích thước mặc định
        int[] dimensions = new int[totalDimensions];
        Arrays.fill(dimensions, 10);
        Object arrayInstance = Array.newInstance(componentType, dimensions);

        // thêm dữ liệu random vào từng ô của mảng
        fillArrayWithRandomData(arrayInstance, componentType, totalDimensions);

        return arrayInstance;
    }

    // hàm đệ quy bơm dữ liệu random vào mảng
    private static void fillArrayWithRandomData(Object arrayObj, Class<?> baseComponentType, int currentDimension) {
        int length = Array.getLength(arrayObj);

        if (currentDimension == 1) {
            // mảng 1 chiều
            for (int i = 0; i < length; i++) {
                Object randomVal = createRandomPrimitiveVariableData(baseComponentType);
                Array.set(arrayObj, i, randomVal);
            }
        } else {
            // mảng nhiều chiều ta bóc từng lớp mảng con ra đệ quy tiếp
            for (int i = 0; i < length; i++) {
                Object subArray = Array.get(arrayObj, i);
                fillArrayWithRandomData(subArray, baseComponentType, currentDimension - 1);
            }
        }
    }

    private static Object createRandomPrimitiveVariableData(Class<?> parameterClass) {
        String className = parameterClass.getName();
        Random random = new Random();

        if ("int".equals(className) || parameterClass == Integer.class) {
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

    private static Object createRandomCollectionVariableData(Class<?> parameterClass, String parameterName) {
        List<Object> listInstance = new ArrayList<>();
        Class<?> targetType = variableGenericTypeMap.get(parameterName);
        for (int i = 0; i < 10; i++) {
            Object randomData = createRandomPrimitiveVariableData(targetType);
            listInstance.add(randomData);
        }
        return listInstance;
    }

    private static Object createRandomSimpleTypeData(Class<?> clazz) {
        // String
        if (clazz == String.class) {
            return "dummy String";
        }
        if (clazz == Integer.class) {
            return 42;
        }
        if (clazz == Long.class) {
            return 42L;
        }
        if (clazz == Boolean.class) {
            return false;
        }
        if (clazz == Double.class) {
            return 3.14;
        }
        if (clazz == Float.class) {
            return 3.14f;
        }
        if (clazz == Byte.class) {
            return (byte) 1;
        }
        if (clazz == Short.class) {
            return (short) 1;
        }
        if (clazz == Character.class) {
            return 'A';
        }
        if (clazz.isEnum()) {
            Object[] constants = clazz.getEnumConstants();
            return (constants != null && constants.length > 0) ? constants[0] : null;
        }
        return null;
    }

    private void writeDataToFile(String data) {
        try {
            FileWriter writer = new FileWriter(FilePath.generatedTestDataPath);
            writer.write(data + "\n");
            writer.close();
        } catch (IOException e) {
            log.error("Lỗi khi ghi dữ liệu Test Data ra file: {}", e.getMessage(), e);
        }
    }

    public Model getModel() {
        return model;
    }

    public static CfgNode getCurrentCfgNode() {
        return currentCfgNode;
    }


}


