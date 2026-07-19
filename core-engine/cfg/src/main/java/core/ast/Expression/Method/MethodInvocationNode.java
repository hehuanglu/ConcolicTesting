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

    public MethodInvocationNode(String className, String methodName, List<AstNode> arguments) {
        this.className = className;
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

    private static CompilationUnit getCompilationUnit(ASTNode node) {
        while (node != null) {
            if (node instanceof CompilationUnit) {
                return (CompilationUnit) node;
            }
            node = node.getParent();
        }
        return null;
    }

    public static AstNode executeMethodInvocation(MethodInvocation methodInvocation, MemoryModel memoryModel) {
        ast = methodInvocation.getAST();

        CompilationUnit cu = getCompilationUnit(methodInvocation);

        String methodName = methodInvocation.getName().toString();

        if (methodInvocation.getExpression() != null) { // method invocation in the same class
            String className = methodInvocation.getExpression().toString();

            IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
            if (methodBinding != null) {
                ITypeBinding declaringClass = methodBinding.getDeclaringClass();
                if (declaringClass != null) {
                    className = declaringClass.getQualifiedName(); // className là StringBuilder
                    System.out.println(className);
                }
            }

            if (className.equals("String") || className.equals("java.lang.String")) {
                return StringMethodNode.executeStringMethod(methodInvocation, memoryModel);
            } else if (className.equals("Character") || className.equals("java.lang.Character")) {
                return CharacterMethodNode.executeCharacterMethod(methodInvocation, memoryModel);
            } else if (className.equals("Integer") || className.equals("java.lang.Integer")) {
                return IntegerMethodNode.executeIntegerMethod(methodInvocation, memoryModel);
            } else if (className.equals("Math") || className.equals("java.lang.Math")) {
                return MathMethodNode.executeMathMethod(methodInvocation, memoryModel);
            } else if (className.equals("StringBuilder") || className.equals("java.lang.StringBuilder")) {
                return StringBuilderMethodAdapter.executeStringBuilderMethodNode(methodInvocation, memoryModel);
            }

            if (methodName.equals("get")) {
                List<AstNode> arguments = new ArrayList<>();
                for (Object arg : methodInvocation.arguments()) {
                    arguments.add(ExpressionNode.executeExpression((Expression) arg, memoryModel));
                }
                // Trả về MethodInvocationNode chứa tên List (expressionStr) và index (arguments)
                return new MethodInvocationNode(className, methodName, arguments);
            }
        } else { // method invocation outside the class or in libs
            Class<?> invokedMethodReturnClass = getInvokedMethodReturnClass(methodInvocation, memoryModel);
            return declareStubVariable(methodName, invokedMethodReturnClass, memoryModel, methodInvocation);
        }
        throw new RuntimeException("Method invocation has no method name" + methodInvocation.getName());
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

    /*
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

     */

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

        if (operand instanceof StringMethodNode) {
            return StringMethodNode.createZ3Expression((StringMethodNode) operand, memoryModel, ctx, vars);
        } else if (operand instanceof CharacterMethodNode) {
            return CharacterMethodNode.createZ3Expression((CharacterMethodNode) operand, memoryModel, ctx, vars);
        } else if (operand instanceof IntegerMethodNode) {
            return IntegerMethodNode.createZ3Expression((IntegerMethodNode) operand, memoryModel, ctx, vars);
        } else if (operand instanceof MathMethodNode) {
            return MathMethodNode.createZ3Expression((MathMethodNode) operand, memoryModel, ctx, vars);
        } else if (operand instanceof StringBuilderMethodNode) {
            return StringBuilderMethodAdapter.createZ3Expression((StringBuilderMethodNode) operand, memoryModel, ctx, vars);
        }

        if ("get".equals(methodName)) {
            // Lấy trạng thái mảng mới nhất từ map (linh hồn logic trong Z3)
            // Lưu ý: Đảm bảo đường dẫn core.symbolicExecution.SymbolicExecutionRewrite là đúng với project của bạn
            Expr z3ListBase = core.symbolicExecution.SymbolicExecutionRewrite.z3ArrayStateMap.get().get(className);

            if (z3ListBase == null) {
                throw new RuntimeException("Không tìm thấy trạng thái Z3 cho List: " + className);
            }

            // Dịch Index (biến hoặc số) sang Z3 Expression thông qua Dispatcher trung tâm
            ExpressionNode indexNode = (ExpressionNode) args.get(0);
            Expr z3IndexExpr = OperationExpressionNode.createZ3Expression(indexNode, ctx, vars, memoryModel);

            System.out.println("Đã dịch phép truy cập List: " + className + ".get(" + z3IndexExpr + ")");

            // Trả về phép toán mkSelect (tương đương Array[index])
            return ctx.mkSelect((ArrayExpr) z3ListBase, z3IndexExpr);
        } else if ("size".equals(methodName)) {
            Expr sizeVar = ctx.mkIntConst(className + ".size");
            Z3VariableWrapper wrapper = new Z3VariableWrapper(sizeVar);
            if (!vars.contains(wrapper)) vars.add(wrapper);
            return sizeVar;
        } else if ("random".equals(methodName)) {
            String mockRandomName = "mock_math_random_" + numberOfFunctionsCall;
            Sort fpSort = ctx.mkFPSortDouble();
            Expr randomVar = ctx.mkConst(mockRandomName, fpSort);

            Z3VariableWrapper wrapper = new Z3VariableWrapper(randomVar);
            if (!vars.contains(wrapper)) vars.add(wrapper);

            BoolExpr bound = ctx.mkAnd(
                    ctx.mkFPGt((FPExpr) randomVar, ctx.mkFP(0.0F, (FPSort) fpSort)),
                    ctx.mkFPLt((FPExpr) randomVar, ctx.mkFP(1.0F, (FPSort) fpSort))
            );
            return randomVar;
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


    public static void resetNumberOfFunctionsCall() {
        MethodInvocationNode.numberOfFunctionsCall = 1;
    }
}
