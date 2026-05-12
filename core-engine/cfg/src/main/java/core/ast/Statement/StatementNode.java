package core.ast.Statement;

import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

public abstract class StatementNode extends AstNode {
    private String optionalLeadingComment = null; // ???

    public static AstNode executeStatement(Statement statement, MemoryModel memoryModel) {
        if(statement instanceof VariableDeclarationStatement) {
            VariableDeclarationStatementNode.executeVariableDeclarationStatement((VariableDeclarationStatement) statement, memoryModel);
            return null; /*???*/
        } else if (statement instanceof ExpressionStatement) {
            return ExpressionStatementNode.executeExpressionStatement((ExpressionStatement) statement, memoryModel);
        } else if (statement instanceof ReturnStatement) {
//            ReturnStatement returnStatement = (ReturnStatement) statement;
//            ExpressionNode.executeExpression(returnStatement.getExpression(), memoryModel);
            return null;
        } else {
//            throw new RuntimeException(statement.getClass() + " is not a Statement");
            return null;
        }
    }

    public static void replaceMethodInvocationWithStub(Statement originStatement, MethodInvocation originMethodInvocation, ASTNode replacement) {
        if(originStatement instanceof VariableDeclarationStatement) {
            VariableDeclarationStatementNode.replaceMethodInvocationWithStub((VariableDeclarationStatement) originStatement, originMethodInvocation, replacement);
        } else if (originStatement instanceof ExpressionStatement) {
            ExpressionStatementNode.replaceMethodInvocationWithStub((ExpressionStatement) originStatement, originMethodInvocation, replacement);
        } else if (originStatement instanceof IfStatement) {
            IfStatementNode.replaceMethodInvocationWithStub((IfStatement) originStatement, originMethodInvocation, replacement);
        } else if (originStatement instanceof ForStatement) {
            ForStatementNode.replaceMethodInvocationWithStub((ForStatement) originStatement, originMethodInvocation, replacement);
        } else if (originStatement instanceof WhileStatement) {
            WhileStatementNode.replaceMethodInvocationWithStub((WhileStatement) originStatement, originMethodInvocation, replacement);
        } else if (originStatement instanceof DoStatement) {
            DoStatementNode.replaceMethodInvocationWithStub((DoStatement) originStatement, originMethodInvocation, replacement);
        } else if (originStatement instanceof Block) {
            BlockNode.replaceMethodInvocationWithStub((Block) originStatement, originMethodInvocation, replacement);
        }
    }
}
