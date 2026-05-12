package core.ast;

import core.ast.Expression.ExpressionNode;
import core.ast.Statement.StatementNode;
import core.ast.VariableDeclaration.VariableDeclarationNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

public abstract class AstNode {

    public static AstNode executeASTNode(ASTNode astNode, MemoryModel memoryModel) {
        if(astNode instanceof Expression) {
            return ExpressionNode.executeExpression((Expression) astNode, memoryModel);
        } else if (astNode instanceof Statement) {
            return StatementNode.executeStatement((Statement) astNode, memoryModel);
        } else if (astNode instanceof VariableDeclaration) {
            VariableDeclarationNode.executeVariableDeclaration((VariableDeclaration) astNode, memoryModel);
            return null;
        } else if (astNode instanceof Dimension) {
            return DimensionNode.executeDimension((Dimension) astNode);
        } else {
//            throw new RuntimeException(astNode.getClass() + " is not an ASTNode");
            return null;
        }
    }

    public static void replaceMethodInvocationWithStub(ASTNode originStatement, MethodInvocation originMethodInvocation, ASTNode replacement) {
        if(originStatement instanceof Expression) {
            ExpressionNode.replaceMethodInvocationWithStub((Expression) originStatement, originMethodInvocation, replacement);
        } else if (originStatement instanceof Statement) {
            StatementNode.replaceMethodInvocationWithStub((Statement) originStatement, originMethodInvocation, replacement);
        } else if (originStatement instanceof VariableDeclaration) {
            VariableDeclarationNode.replaceMethodInvocationWithStub((VariableDeclaration) originStatement, originMethodInvocation, replacement);
        } else if (originStatement instanceof Dimension) {
            DimensionNode.replaceMethodInvocationWithStub((Dimension) originStatement, originMethodInvocation, replacement);
        }
    }
}
