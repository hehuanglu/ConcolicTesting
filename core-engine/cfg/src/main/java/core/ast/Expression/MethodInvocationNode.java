package core.ast.Expression;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.Name.SimpleNameNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.ast.VariableDeclaration.SingleVariableDeclarationNode;
import core.symbolicExecution.MemoryModel;
import core.testDriver.TestDriverUtils;
import core.testGeneration.TestGeneration;
import core.variable.SimpleTypeVariable;
import core.variable.Variable;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MethodInvocationNode extends ExpressionNode {
    private static int numberOfFunctionsCall = 1;
    private static AST ast;
    private String className;
    private String methodName;
    private List<AstNode> arguments = new ArrayList<>();
    private ExpressionNode receiver;
    public MethodInvocationNode(String className, String methodName, List<AstNode> arguments) {
        this.className = className;
        this.methodName = methodName;
        this.arguments = arguments;
    }
    public MethodInvocationNode(ExpressionNode receiver, String methodName, List<AstNode> arguments) {
        this.receiver = receiver;
        this.methodName = methodName;
        this.arguments = arguments;
    }

    public MethodInvocationNode() {
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<AstNode> getArgument() {
        return arguments;
    }

    public static AstNode executeMethodInvocation(MethodInvocation methodInvocation, MemoryModel memoryModel) {
        ast = methodInvocation.getAST();

        String methodName = methodInvocation.getName().toString();

        // Logic xét kiểu nếu caller là String
        Expression expression = methodInvocation.getExpression();
        if (expression != null) {
            boolean isString = false;
            if (expression instanceof StringLiteral) {
                isString = true;
            } else {
                try {
                    // Tìm biến trong memoryModel để kiểm tra kiểu
                    Variable var = memoryModel.getVariable(expression.toString());
                    if (var instanceof SimpleTypeVariable) {
                        SimpleTypeVariable simpleVar = (SimpleTypeVariable) var;
                        // Kiểm tra nếu kiểu là String
                        if (simpleVar.getType().toString().equals("String")) {
                            isString = true;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            ///  Phát hiện phương thức gọi từ một String:
            if (isString) {
                // xử lý phần đầu (vd : input.contains("") thì phần này xử lý input)
                Expression receiver = methodInvocation.getExpression();
                AstNode executedReceiver = ExpressionNode.executeExpression(receiver,memoryModel);
                // xử lý phan bên trong phương thức
                List<AstNode> arguments = new ArrayList<>();
                for(int i=0;i<methodInvocation.arguments().size();i++){
                    AstNode argNode = ExpressionNode.executeExpression((Expression) methodInvocation.arguments().get(i), memoryModel);
                    arguments.add(argNode);
                }
                return new MethodInvocationNode((ExpressionNode)executedReceiver,methodName,arguments);
            }
        }

        if (methodInvocation.getExpression() != null) { // method invocation in the same class
            String className = methodInvocation.getExpression().toString();

            if (className.equals("Math") && (methodName.equals("abs") || methodName.equals("max") || methodName.equals("min"))) {
                List<AstNode> arguments = new ArrayList<>();
                for (int i = 0; i < methodInvocation.arguments().size(); i++) {
                    AstNode argNode = ExpressionNode.executeExpression((Expression) methodInvocation.arguments().get(i), memoryModel);
                    arguments.add(argNode);
                }
                return new MethodInvocationNode(className, methodName, arguments);
            }


            MethodDeclaration methodDeclaration = getInvokedMethodAST(methodName);
            return declareStubVariable(methodName, methodDeclaration, memoryModel, methodInvocation);
        } else { // method invocation outside the class or in libs
            Class<?> invokedMethodReturnClass = getInvokedMethodReturnClass(methodInvocation, memoryModel);
            return declareStubVariable(methodName, invokedMethodReturnClass, memoryModel, methodInvocation);
        }
    }

    private static MethodDeclaration getInvokedMethodAST(String methodName) {
        ArrayList<ASTNode> funcAstNodeList = TestGeneration.getFuncAstNodeList();
        for (ASTNode astNode : funcAstNodeList) {
            if (((MethodDeclaration) astNode).getName().getIdentifier().equals(methodName)) {
                return (MethodDeclaration) astNode;
            }
        }
        throw new RuntimeException("There is no method named: " + methodName);
    }

    private static Class<?> getInvokedMethodReturnClass(MethodInvocation methodInvocation, MemoryModel memoryModel) {
        CompilationUnit compilationUnit = TestGeneration.getCompilationUnit();
        String optionalExpression = methodInvocation.getExpression().toString();

        for (ASTNode iImport : (List<ASTNode>) compilationUnit.imports()) {
            ImportDeclaration importDeclaration = (ImportDeclaration) iImport;
            String importName = importDeclaration.getName().toString();

            if (importName.contains(optionalExpression)) {
                Class<?>[] classes = TestDriverUtils.getVariableClasses(methodInvocation.arguments(), memoryModel);
                try {
                    Method invokedMethodReflect = Class.forName(importName).getDeclaredMethod(methodInvocation.getName().toString(), classes);
                    return invokedMethodReflect.getReturnType();
                } catch (NoSuchMethodException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Class<?>[] classes = TestDriverUtils.getVariableClasses(methodInvocation.arguments(), memoryModel);
        try {
            Method invokedMethodReflect = Class.forName("java.lang." + optionalExpression).getDeclaredMethod(methodInvocation.getName().toString(), classes);
            return invokedMethodReflect.getReturnType();
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private static AstNode declareStubVariable(String methodName, MethodDeclaration methodDeclaration, MemoryModel memoryModel, MethodInvocation methodInvocation) {
        Type funcReturnType = methodDeclaration.getReturnType2();
        String stubName = methodName + "_call_" + numberOfFunctionsCall;
        numberOfFunctionsCall++;
        SimpleNameNode stubNameNode = new SimpleNameNode(stubName);

        replaceMethodInvocationWithStub(methodInvocation, stubName);

        if (funcReturnType instanceof PrimitiveType) {
            memoryModel.declarePrimitiveTypeVariable(((PrimitiveType) funcReturnType), stubName, stubNameNode);
            addStubVariableToParameterList(stubName, funcReturnType);
            return stubNameNode;
        } else if (funcReturnType instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) funcReturnType;
            AstNode arrayNode = SingleVariableDeclarationNode.createMultiDimensionsInitializationArray(stubName, 0, arrayType.getDimensions(), arrayType.getElementType(), memoryModel);
            memoryModel.declareArrayTypeVariable(arrayType, stubName, arrayType.getDimensions(), arrayNode);
            addStubVariableToParameterList(stubName, funcReturnType);
            return arrayNode;
        } else { // OTHER TYPES
            throw new RuntimeException("Invalid type");
        }
    }

    private static AstNode declareStubVariable(String methodName, Class<?> invokedMethodReturnClass, MemoryModel memoryModel, MethodInvocation methodInvocation) {
        String stubName = methodName + "_call_" + numberOfFunctionsCall;
        numberOfFunctionsCall++;
        SimpleNameNode stubNameNode = new SimpleNameNode(stubName);

        replaceMethodInvocationWithStub(methodInvocation, stubName);

        if (invokedMethodReturnClass.isPrimitive()) {
            PrimitiveType type = ast.newPrimitiveType(TestDriverUtils.getPrimitiveCode(invokedMethodReturnClass));
            memoryModel.declarePrimitiveTypeVariable(type, stubName, stubNameNode);
            addStubVariableToParameterList(stubName, type);
            return stubNameNode;
        } else if (invokedMethodReturnClass.isArray()) {

            throw new RuntimeException("Haven't handled array type");
//            ArrayType arrayType = (ArrayType) funcReturnType;
//            AstNode arrayNode = SingleVariableDeclarationNode.createMultiDimensionsInitializationArray(stubName, 0, arrayType.getDimensions(), arrayType.getElementType(), memoryModel);
//            memoryModel.declareArrayTypeVariable(arrayType, stubName, arrayType.getDimensions(), arrayNode);
//            return arrayNode;
        } else { // OTHER TYPES
            throw new RuntimeException("Invalid type");
        }
    }

    public static Expr createZ3Expression(MethodInvocationNode operand, MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        MethodInvocationNode methodInvocationNode = (MethodInvocationNode) operand;
        String methodName = methodInvocationNode.getMethodName();
        String className = methodInvocationNode.getClassName();
        List<AstNode> args = methodInvocationNode.getArgument();

        if ("Math".equals(className)) {
            if ("abs".equals(methodName)) {
                ExpressionNode argNode = (ExpressionNode) args.get(0);
                Expr argZ3 = OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);
                if (argZ3 instanceof BitVecExpr) {
                    BitVecExpr x_arg = (BitVecExpr) argZ3;
                    BoolExpr isNegative = ctx.mkBVSLT(x_arg, ctx.mkBV(0, x_arg.getSortSize()));
                    BitVecExpr negativeX = ctx.mkBVNeg(x_arg);
                    System.out.println("Đã dịch Math.abs sang Z3");
                    return ctx.mkITE(isNegative, negativeX, x_arg);
                }
            } else if ("max".equals(methodName)) {
                ExpressionNode arg1Node = (ExpressionNode) args.get(0);
                ExpressionNode arg2Node = (ExpressionNode) args.get(1);

                Expr arg1Z3 = OperationExpressionNode.createZ3Expression(arg1Node, ctx, vars, memoryModel);
                Expr arg2Z3 = OperationExpressionNode.createZ3Expression(arg2Node, ctx, vars, memoryModel);

                if (arg1Z3 instanceof BitVecExpr && arg2Z3 instanceof BitVecExpr) {
                    BitVecExpr x_arg1 = (BitVecExpr) arg1Z3;
                    BitVecExpr x_arg2 = (BitVecExpr) arg2Z3;

                    BoolExpr a_gt_b = ctx.mkBVSGT(x_arg1, x_arg2);
                    System.out.println("Đã dịch Math.max sang Z3");

                    return ctx.mkITE(a_gt_b, x_arg1, x_arg2);
                }
            } else if ("min".equals(methodName)) {
                ExpressionNode arg1Node = (ExpressionNode) args.get(0);
                ExpressionNode arg2Node = (ExpressionNode) args.get(1);

                Expr z3Arg1 = OperationExpressionNode.createZ3Expression(arg1Node, ctx, vars, memoryModel);
                Expr z3Arg2 = OperationExpressionNode.createZ3Expression(arg2Node, ctx, vars, memoryModel);

                if (z3Arg1 instanceof BitVecExpr && z3Arg2 instanceof BitVecExpr) {
                    BitVecExpr a = (BitVecExpr) z3Arg1;
                    BitVecExpr b = (BitVecExpr) z3Arg2;

                    BoolExpr a_lt_b = ctx.mkBVSLT(a, b);
                    System.out.println("Đã dịch Math.min sang Z3");

                    return ctx.mkITE(a_lt_b, a, b);
                }
            } else if ("pow".equals(methodName)) {
                ExpressionNode baseNode = (ExpressionNode) args.get(0);
                ExpressionNode powNode = (ExpressionNode) args.get(1);

                Expr z3Base = OperationExpressionNode.createZ3Expression(powNode, ctx, vars, memoryModel);

                boolean isSquare = false;

                if (powNode instanceof LiteralNode) {
                    LiteralNode literalExp = (LiteralNode) powNode;

                    // check xem nó có là số không
                    if (literalExp.isNumberLiteralNode()) {
                        // ép kiểu
                        NumberLiteralNode numNode =
                                (NumberLiteralNode) literalExp;

                        String val = numNode.getTokenValue();

                        // bắt cả số nguyên và số thực
                        if (val.equals("2") || val.equals("2.0")) {
                            isSquare = true;
                        }
                    }
                }

                if (isSquare) {
                    if (z3Base instanceof BitVecExpr) {
                        return ctx.mkBVMul((BitVecExpr) z3Base, (BitVecExpr) z3Base);
                    } else if (z3Base instanceof FPExpr) {
                        return ctx.mkFPMul(ctx.mkFPRoundNearestTiesToEven(), (FPExpr) z3Base, (FPExpr) z3Base);
                    }
                } else {
                    return null;
                }
            } else if ("sqrt".equals(methodName)) {
                ExpressionNode argNode = (ExpressionNode) args.get(0);
                Expr z3Arg = OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);

                if (z3Arg instanceof FPExpr) {
                    return ctx.mkFPSqrt(ctx.mkFPRoundNearestTiesToEven(), (FPExpr) z3Arg);
                } else if (z3Arg instanceof BitVecExpr) {
                    ;
                    return null;
                }
            }
        }

        // xử lý các phương thức của String bên dưới
        if (methodName.equals("contains")) {
            ExpressionNode argNode = (ExpressionNode) args.get(0);
            Expr arg = OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);
            if (receiver instanceof SeqExpr && arg instanceof SeqExpr) {
                return ctx.mkContains((SeqExpr) receiver, (SeqExpr) arg);
            }
            return ctx.mkEq(receiver, arg);

        } else if (methodName.equals("length")) {
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);
            // ctx.mkLength trả về IntExpr
            return ctx.mkLength(receiver);

        } else if (methodName.equals("equals")) {
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);
            Expr arg = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(0), ctx, vars, memoryModel);
            return ctx.mkEq(receiver, arg);

        } else if (methodName.equals("concat")) {
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);
            Expr arg = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(0), ctx, vars, memoryModel);
            if (receiver instanceof SeqExpr && arg instanceof SeqExpr) {
                return ctx.mkConcat((SeqExpr) receiver, (SeqExpr) arg);
            }

        } else if (methodName.equals("startsWith")) {
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);
            Expr prefix = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(0), ctx, vars, memoryModel);
            if (receiver instanceof SeqExpr && prefix instanceof SeqExpr) {
                // Lưu ý: Z3 mkPrefixOf nhận tham số (prefix, string)
                return ctx.mkPrefixOf((SeqExpr) prefix, (SeqExpr) receiver);
            }

        } else if (methodName.equals("endsWith")) {
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);
            Expr suffix = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(0), ctx, vars, memoryModel);
            if (receiver instanceof SeqExpr && suffix instanceof SeqExpr) {
                // Lưu ý: Z3 mkSuffixOf nhận tham số (suffix, string)
                return ctx.mkSuffixOf((SeqExpr) suffix, (SeqExpr) receiver);
            }

        } else if (methodName.equals("indexOf")) {
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);
            Expr target = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(0), ctx, vars, memoryModel);

            if (receiver instanceof SeqExpr && target instanceof SeqExpr) {
                Expr offset;
                // Xử lý nạp chồng: indexOf(String str) vs indexOf(String str, int fromIndex)
                if (args.size() == 2) {
                    offset = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(1), ctx, vars, memoryModel);
                } else {
                    offset = ctx.mkInt(0); // Mặc định tìm từ vị trí 0
                }

                if (offset instanceof IntExpr) {
                    return ctx.mkIndexOf((SeqExpr) receiver, (SeqExpr) target, (IntExpr) offset);
                }
            }

        } else if (methodName.equals("substring")) {
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);

            if (receiver instanceof SeqExpr) {
                Expr startExpr = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(0), ctx, vars, memoryModel);

                if (startExpr instanceof IntExpr) {
                    IntExpr start = (IntExpr) startExpr;
                    IntExpr length;

                    // Xử lý nạp chồng: substring(int beginIndex) vs substring(int beginIndex, int endIndex)
                    if (args.size() == 2) {
                        Expr endExpr = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(1), ctx, vars, memoryModel);
                        if (endExpr instanceof IntExpr) {
                            // Java dùng endIndex, Z3 dùng length. Tính length = end - start
                            length = (IntExpr) ctx.mkSub((IntExpr) endExpr, start);
                        } else {
                            throw new RuntimeException("endIndex của substring phải là số nguyên");
                        }
                    } else {
                        // Nếu chỉ có start, length = receiver.length() - start
                        length = (IntExpr) ctx.mkSub((IntExpr) ctx.mkLength(receiver), start);
                    }

                    // Z3 mkExtract nhận (string, offset, length)
                    return ctx.mkExtract((SeqExpr) receiver, start, length);
                }
            }

        } else if (methodName.equals("replace")) {
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);
            Expr target = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(0), ctx, vars, memoryModel);
            Expr replacement = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(1), ctx, vars, memoryModel);

            if (receiver instanceof SeqExpr && target instanceof SeqExpr && replacement instanceof SeqExpr) {
                // Lưu ý: Z3 mkReplace chỉ thay thế lần xuất hiện ĐẦU TIÊN (tương đương replaceFirst).
                // Thay thế toàn bộ (replaceAll) phức tạp hơn và thường yêu cầu hàm đệ quy trong Z3.
                return ctx.mkReplace((SeqExpr) receiver, (SeqExpr) target, (SeqExpr) replacement);
            }

        } else if (methodName.equals("charAt")) {
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);
            Expr index = OperationExpressionNode.createZ3Expression((ExpressionNode) args.get(0), ctx, vars, memoryModel);

            if (receiver instanceof SeqExpr && index instanceof IntExpr) {
                // Trả về một chuỗi con có độ dài 1 tại vị trí index
                return ctx.mkAt((SeqExpr) receiver, (IntExpr) index);
            }

        } else if (methodName.equals("isEmpty")) {
            Expr receiver = OperationExpressionNode.createZ3Expression(operand.getReceiver(), ctx, vars, memoryModel);
            // So sánh độ dài với 0
            return ctx.mkEq(ctx.mkLength(receiver), ctx.mkInt(0));
        }
        // Fallback nếu không khớp method nào hoặc lỗi ép kiểu (Tùy thuộc vào thiết kế hệ thống của bạn)
        throw new UnsupportedOperationException("Chưa hỗ trợ ánh xạ Z3 cho thao tác String: " + methodName);

    }

    public ExpressionNode getReceiver() {
        return receiver;
    }

    private static SimpleName replaceMethodInvocationWithStub(MethodInvocation methodInvocation, String stubName) {
        SimpleName simpleName = ast.newSimpleName(stubName);
        ASTNode methodInvocationParent = methodInvocation.getParent();
        AstNode.replaceMethodInvocationWithStub(methodInvocationParent, methodInvocation, simpleName);
        return simpleName;
    }

    private static void addStubVariableToParameterList(String stubName, Type funcReturnType) {
        MethodDeclaration methodDeclaration = TestGeneration.getTestFunc();
        SingleVariableDeclaration singleVariableDeclaration = ast.newSingleVariableDeclaration();
        singleVariableDeclaration.setName(ast.newSimpleName(stubName));
        singleVariableDeclaration.setType(TestDriverUtils.cloneTypeAST(funcReturnType, ast));
        methodDeclaration.parameters().add(singleVariableDeclaration);
    }


    public static void resetNumberOfFunctionsCall() {
        MethodInvocationNode.numberOfFunctionsCall = 1;
    }
}
