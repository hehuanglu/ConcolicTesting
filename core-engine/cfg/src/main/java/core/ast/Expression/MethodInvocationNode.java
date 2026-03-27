package core.ast.Expression;

import core.ast.AstNode;
import core.ast.Expression.Name.SimpleNameNode;
import core.ast.VariableDeclaration.SingleVariableDeclarationNode;
import core.symbolicExecution.MemoryModel;
import core.testDriver.TestDriverUtils;
import core.testGeneration.TestGeneration;
import org.eclipse.jdt.core.dom.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MethodInvocationNode extends ExpressionNode {
    private static int numberOfFunctionsCall = 1;
    private static AST ast;
    private String className;
    private String methodName;
    private AstNode argument;

    public MethodInvocationNode(String className, String methodName, AstNode argument) {
        this.className = className;
        this.methodName = methodName;
        this.argument = argument;
    }

    public MethodInvocationNode() {
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public AstNode getArgument() {
        return argument;
    }

    public static AstNode executeMethodInvocation(MethodInvocation methodInvocation, MemoryModel memoryModel) {
        ast = methodInvocation.getAST();

        String methodName = methodInvocation.getName().toString();


        if (methodInvocation.getExpression() != null) { // method invocation in the same class
            String className = methodInvocation.getExpression().toString();

            if (className.equals("Math") && methodName.equals("abs")) {
                AstNode argNode = ExpressionNode.executeExpression((Expression) methodInvocation.arguments().get(0), memoryModel);
                return new MethodInvocationNode(className, methodName, argNode);
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
