package core.ast.Expression.Method;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.Name.SimpleNameNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.ast.VariableDeclaration.SingleVariableDeclarationNode;
import core.symbolicExecution.MemoryModel;
import core.symbolicExecution.SymbolicExecutionRewrite;
import core.testDriver.TestDriverUtils;
import core.testGeneration.TestGeneration;
import core.variable.ParameterizedTypeVariable;
import core.variable.Variable;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static core.symbolicExecution.SymbolicExecutionRewrite.CollectionKeys;

public class MethodInvocationNode extends ExpressionNode {
    private static int numberOfFunctionsCall = 1;
    private static AST ast;
    private String className;
    private String methodName;
    private String targetName;
    private List<AstNode> arguments = new ArrayList<>();

    public MethodInvocationNode(String className, String methodName, List<AstNode> arguments) {
        this.className = className;
        this.methodName = methodName;
        this.arguments = arguments;
    }

    public MethodInvocationNode(String className, String methodName, List<AstNode> arguments,String targetName){
        this.className = className;
        this.methodName = methodName;
        this.arguments = arguments;
        this.targetName = targetName;
    }
    public MethodInvocationNode() {
    }

    public String getTargetName() {
        return targetName;
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
        String targetName = methodInvocation.getExpression().toString();
        if (methodInvocation.getExpression() != null) { // method invocation in the same class
            String className = methodInvocation.getExpression().toString();
            IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
            boolean isStatic = (methodBinding != null) && Modifier.isStatic(methodBinding.getModifiers());
            if (methodBinding != null) {
                ITypeBinding declaringClass = methodBinding.getDeclaringClass();
                if (declaringClass != null) {
                    className = declaringClass.getQualifiedName();
                }
            }

            if (className.equals("String") || className.equals("java.lang.String")) {
                List<AstNode> arguments = new ArrayList<>();
                // thực thi tượng trưng từng arguments của String
                for (int i = 0; i < methodInvocation.arguments().size(); i++) {
                    AstNode argNode = ExpressionNode.executeExpression((Expression) methodInvocation.arguments().get(i), memoryModel);
                    arguments.add(argNode);
                }
                // thực thi tượng trưng target
                AstNode target = null;
                if (!isStatic) {
                    target = ExpressionNode.executeExpression(methodInvocation.getExpression(), memoryModel);
                }
                return new StringMethodNode(target, methodName, arguments);
            }
            else if(className.equals("Long") || className.equals("java.lang.Long")){
                List<AstNode> arguments = new ArrayList<>();
                for (int i = 0; i < methodInvocation.arguments().size(); i++) {
                    arguments.add(ExpressionNode.executeExpression((Expression) methodInvocation.arguments().get(i), memoryModel));
                }
                AstNode target = ExpressionNode.executeExpression(methodInvocation.getExpression(), memoryModel);
                return new LongMethodNode(target,methodName,arguments);
            }


            if (className.equals("Math") && (methodName.equals("abs") || methodName.equals("max") || methodName.equals("min"))) {
                List<AstNode> arguments = new ArrayList<>();
                for (int i = 0; i < methodInvocation.arguments().size(); i++) {
                    AstNode argNode = ExpressionNode.executeExpression((Expression) methodInvocation.arguments().get(i), memoryModel);
                    arguments.add(argNode);
                }
                return new MethodInvocationNode(className, methodName, arguments);
            }

            if (className.contains("java.util.List") || className.contains("java.util.Map")) {
                String expressionStr = methodInvocation.getExpression().toString();

                // Đặt cờ kiểm tra xem đối tượng hiện tại có phải là Map hay không
                boolean isMap = className.contains("java.util.Map");

                // Xử lý riêng cho phương thức không có tham số (ví dụ: size)
                if (methodName.equals("size")) {
                    if (isMap) {
                        return new MapMethodNode(expressionStr, methodName, new ArrayList<>(), targetName);
                    } else {
                        return new MethodInvocationNode(expressionStr, methodName, new ArrayList<>(), targetName);
                    }
                }

                // Xử lý cho các phương thức có tham số (get, put, add, set, v.v.)
                List<AstNode> arguments = new ArrayList<>();
                for (Object arg : methodInvocation.arguments()) {
                    arguments.add(ExpressionNode.executeExpression((Expression) arg, memoryModel));
                }

                // Rẽ nhánh trả về loại Node tương ứng
                if (isMap) {
                    // Trả về MapMethodNode nếu className chứa java.util.Map
                    return new MapMethodNode(expressionStr, methodName, arguments, targetName);
                } else {
                    if (methodName.equals("add")) {
                        arguments = new ArrayList<>();
                        for (Object arg : methodInvocation.arguments()) {
                            arguments.add(ExpressionNode.executeExpression((Expression) arg, memoryModel));
                        }

                        MethodInvocationNode methodInvocationNode = new MethodInvocationNode(expressionStr, methodName, arguments);
                        createZ3Expression(methodInvocationNode, memoryModel, SymbolicExecutionRewrite.globalCtx.get(), SymbolicExecutionRewrite.globalZ3Vars.get());

                        return methodInvocationNode;
                    }
                    // Trả về MethodInvocationNode cho phần còn lại (java.util.List)
                    return new MethodInvocationNode(expressionStr, methodName, arguments, targetName);
                }


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

    public static Expr  createZ3Expression(MethodInvocationNode operand, MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        MethodInvocationNode methodInvocationNode = (MethodInvocationNode) operand;
        String methodName = methodInvocationNode.getMethodName();
        String className = methodInvocationNode.getClassName();
        List<AstNode> args = methodInvocationNode.getArgument();
        String targetName = className;
        if (methodInvocationNode.getTargetName() != null){
            targetName = methodInvocationNode.getTargetName();
        }
        if (isSimpleTypeNode(operand)) {
            if (operand instanceof StringMethodNode)
                return StringMethodNode.createZ3Expression((StringMethodNode) operand, memoryModel, ctx, vars);
            else if (operand instanceof  LongMethodNode)
                return LongMethodNode.createZ3Expression((LongMethodNode) operand, memoryModel, ctx, vars);
        }

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
        } else if (SymbolicExecutionRewrite.variableGenericTypeMap.containsKey(targetName)) {
            if(operand instanceof MapMethodNode){
                return MapMethodNode.createZ3Expression(operand,memoryModel,ctx,vars);
            }else{
                return ParameterizedNode.createZ3Expression(operand,memoryModel,ctx,vars);
            }
        }

        throw new RuntimeException("Invalid type");
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

    private static boolean isSimpleTypeNode(MethodInvocationNode operand){
        if (operand instanceof StringMethodNode || operand instanceof LongMethodNode) {
            return true;
        }
        return false;
    }

    public static void resetNumberOfFunctionsCall() {
        MethodInvocationNode.numberOfFunctionsCall = 1;
    }
}
