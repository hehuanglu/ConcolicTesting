package core.cfg.utils;

import core.ast.AstNode;
import core.ast.additionalNodes.Node;
import core.cfg.CfgNode;
import core.path.FindAllPath;
import core.path.Path;
import core.symbolicExecution.SymbolicExecution;
import core.testDriver.TestDriverUtils;
import core.testGeneration.TestGeneration;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public final class CfgUtils {
    public static void modifyCfgWithStubVars(CfgNode cfgBeginNode) {
        numberOfFunctionsCall = 1;
        List<Path> testPaths = (new FindAllPath(cfgBeginNode)).getPaths();

        for (Path path : testPaths) {
            modifyTestPathWithStubVars(path);
        }
    }

    private static CfgNode currentCfgNode = null;

    public static CfgNode getCurrentCfgNode() {
        return currentCfgNode;
    }

    private static void modifyTestPathWithStubVars(Path path) {
        currentCfgNode = null;
        Node currentNode = path.getCurrentFirst();
        while (currentNode != null) {
            currentCfgNode = currentNode.getData();
            ASTNode astNode = currentCfgNode.getAst();
            if (astNode != null) {
                modifyAstWithStubVars(astNode, false);
            }
            currentNode = currentNode.getNext();
        }
        currentCfgNode = null;
    }

    private static ASTNode modifyAstWithStubVars(ASTNode astNode, boolean isChildNode) {
        if (astNode instanceof InfixExpression) {
            return modifyInfixExpressionWithStubVars((InfixExpression) astNode);
        } else if (astNode instanceof PostfixExpression) {
            return modifyPostfixExpressionWithStubVars((PostfixExpression) astNode);
        } else if (astNode instanceof PrefixExpression) {
            return modifyPrefixExpressionWithStubVars((PrefixExpression) astNode);
        } else if (astNode instanceof ParenthesizedExpression) {
            return modifyParenthesizedExpressionWithStubVars((ParenthesizedExpression) astNode);
        } else if (astNode instanceof Assignment) {
            return modifyAssignmentWithStubVars((Assignment) astNode);
        } else if (astNode instanceof VariableDeclarationExpression) {
            return modifyVariableDeclarationExpressionWithStubVars((VariableDeclarationExpression) astNode);
        } else if (astNode instanceof ExpressionStatement) {
            return modifyExpressionStatementWithStubVars((ExpressionStatement) astNode);
        } else if (astNode instanceof VariableDeclarationStatement) {
            return modifyVariableDeclarationStatementWithStubVars((VariableDeclarationStatement) astNode);
        } else if (astNode instanceof VariableDeclarationFragment) {
            return modifyVariableDeclarationFragmentWithStubVars((VariableDeclarationFragment) astNode);
        } else if (astNode instanceof MethodInvocation) {
            return modifyMethodInvocationWithStubVars((MethodInvocation) astNode, isChildNode);
        } else {
            return null;
        }
    }

    private static ASTNode modifyInfixExpressionWithStubVars(InfixExpression infixExpression) {
        ASTNode newRightOperand = modifyAstWithStubVars(infixExpression.getRightOperand(), true);
        if (newRightOperand != null) {
            infixExpression.setRightOperand((Expression) newRightOperand);
        }

        ASTNode newLeftOperand = modifyAstWithStubVars(infixExpression.getLeftOperand(), true);
        if (newLeftOperand != null) {
            infixExpression.setLeftOperand((Expression) newLeftOperand);
        }

        List<ASTNode> extendedOperands = infixExpression.extendedOperands();
        for (int i = 0; i < extendedOperands.size(); i++) {
            ASTNode newExtendedOperand = modifyAstWithStubVars(extendedOperands.get(i), true);
            if (newExtendedOperand != null) {
                extendedOperands.set(i, newExtendedOperand);
            }
        }

        return null;
    }

    private static ASTNode modifyPostfixExpressionWithStubVars(PostfixExpression postfixExpression) {
        ASTNode newOperand = modifyAstWithStubVars(postfixExpression.getOperand(), true);
        if (newOperand != null) {
            postfixExpression.setOperand((Expression) newOperand);
        }

        return null;
    }

    private static ASTNode modifyPrefixExpressionWithStubVars(PrefixExpression prefixExpression) {
        ASTNode newOperand = modifyAstWithStubVars(prefixExpression.getOperand(), true);
        if (newOperand != null) {
            prefixExpression.setOperand((Expression) newOperand);
        }

        return null;
    }

    private static ASTNode modifyParenthesizedExpressionWithStubVars(ParenthesizedExpression parenthesizedExpression) {
        ASTNode newExpression = modifyAstWithStubVars(parenthesizedExpression.getExpression(), true);
        if (newExpression != null) {
            parenthesizedExpression.setExpression((Expression) newExpression);
        }

        return null;
    }

    private static ASTNode modifyAssignmentWithStubVars(Assignment assignment) {
        ASTNode newRightHandSide = modifyAstWithStubVars(assignment.getRightHandSide(), true);
        if (newRightHandSide != null) {
            assignment.setRightHandSide((Expression) newRightHandSide);
        }

        ASTNode newLeftHandSide = modifyAstWithStubVars(assignment.getLeftHandSide(), true);
        if (newLeftHandSide != null) {
            assignment.setLeftHandSide((Expression) newLeftHandSide);
        }

        return null;
    }

    private static ASTNode modifyVariableDeclarationExpressionWithStubVars(VariableDeclarationExpression variableDeclarationExpression) {
        List<ASTNode> fragments = variableDeclarationExpression.fragments();
        for (int i = 0; i < fragments.size(); i++) {
            ASTNode newFragment = modifyAstWithStubVars(fragments.get(i), true);
            if (newFragment != null) {
                fragments.set(i, newFragment);
            }
        }

        return null;
    }

    private static ASTNode modifyExpressionStatementWithStubVars(ExpressionStatement expressionStatement) {
        ASTNode newExpression = modifyAstWithStubVars(expressionStatement.getExpression(), true);
        if (newExpression != null) {
            expressionStatement.setExpression((Expression) newExpression);
        }

        return null;
    }

    private static ASTNode modifyVariableDeclarationStatementWithStubVars(VariableDeclarationStatement variableDeclarationStatement) {
        List<ASTNode> fragments = variableDeclarationStatement.fragments();
        for (int i = 0; i < fragments.size(); i++) {
            ASTNode newFragment = modifyAstWithStubVars(fragments.get(i), true);
            if (newFragment != null) {
                fragments.set(i, newFragment);
            }
        }

        return null;
    }

    private static ASTNode modifyVariableDeclarationFragmentWithStubVars(VariableDeclarationFragment variableDeclarationFragment) {
        ASTNode newInitializer = modifyAstWithStubVars(variableDeclarationFragment.getInitializer(), true);
        if (newInitializer != null) {
            variableDeclarationFragment.setInitializer((Expression) newInitializer);
        }

        return null;
    }

    private static int numberOfFunctionsCall = 1;

    private static ASTNode modifyMethodInvocationWithStubVars(MethodInvocation methodInvocation, boolean isChildNode) {
        String methodName = methodInvocation.getName().toString();

        if (methodInvocation.getExpression() == null) { // method invocation in the same class
            MethodDeclaration methodDeclaration = getInvokedMethodAST(methodName);
            return declareStubVariable(methodName, methodDeclaration, methodInvocation, isChildNode);
        } else {
            return null;
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

    private static ASTNode declareStubVariable(String methodName, MethodDeclaration methodDeclaration, MethodInvocation methodInvocation, boolean isChildNode) {
        Type funcReturnType = methodDeclaration.getReturnType2();
        String stubName = methodName + "_call_" + numberOfFunctionsCall;
        numberOfFunctionsCall++;

        AST ast = methodInvocation.getAST();

        addStubVariableToParameterList(stubName, funcReturnType, ast);
        return replaceMethodInvocationWithStub(methodInvocation, stubName, ast, isChildNode);
    }

    private static SimpleName replaceMethodInvocationWithStub(MethodInvocation methodInvocation, String stubName, AST ast, boolean isChildNode) {
        SimpleName simpleName = ast.newSimpleName(stubName);
        if (!isChildNode) {
            ASTNode methodInvocationParent = methodInvocation.getParent();
            AstNode.replaceMethodInvocationWithStub(methodInvocationParent, methodInvocation, simpleName);
            return null;
        } else return simpleName;
    }

    private static void addStubVariableToParameterList(String stubName, Type funcReturnType, AST ast) {
        MethodDeclaration methodDeclaration = TestGeneration.getTestFunc();
        SingleVariableDeclaration singleVariableDeclaration = ast.newSingleVariableDeclaration();
        singleVariableDeclaration.setName(ast.newSimpleName(stubName));
        singleVariableDeclaration.setType(TestDriverUtils.cloneTypeAST(funcReturnType, ast));
        methodDeclaration.parameters().add(singleVariableDeclaration);
    }
}
