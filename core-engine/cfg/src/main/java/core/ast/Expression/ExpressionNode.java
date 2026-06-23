package core.ast.Expression;

import core.ast.AstNode;
import core.ast.Expression.Array.ArrayAccessNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Method.MethodInvocationNode;
import core.ast.Expression.Name.NameNode;
import core.ast.Expression.OperationExpression.CastExpressionNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

public abstract class ExpressionNode extends AstNode {

    public static AstNode executeExpression(Expression expression, MemoryModel memoryModel) {
        if (isOperationExpression(expression)) {
            return OperationExpressionNode.executeOperationExpression(expression, memoryModel);
        } else if (isLiteral(expression)) {
            return LiteralNode.executeLiteral(expression);
        } else if (expression instanceof ArrayInitializer) {
            return ArrayInitializerNode.executeArrayInitializer((ArrayInitializer) expression, memoryModel);
        } else if (expression instanceof ArrayCreation) {
            return ArrayCreationNode.executeArrayCreation((ArrayCreation) expression, memoryModel);
        } else if (expression instanceof ArrayAccess) {
            return ArrayAccessNode.executeArrayAccessNode((ArrayAccess) expression, memoryModel);
        } else if (expression instanceof Name) {
            return NameNode.executeName((Name) expression, memoryModel);
        } else if (expression instanceof Assignment) {
            AssignmentNode.executeAssignment((Assignment) expression, memoryModel);
            return null;
        } else if (expression instanceof VariableDeclarationExpression) {
            VariableDeclarationExpressionNode.executeVariableDeclarationExpression((VariableDeclarationExpression) expression,
                    memoryModel);
            return null;
        } else if (expression instanceof CastExpression) {
            return CastExpressionNode.executeCastExpression((CastExpression) expression, memoryModel);
        } else if (expression instanceof MethodInvocation) {
            return MethodInvocationNode.executeMethodInvocation((MethodInvocation) expression, memoryModel);
        } else {
//            throw new RuntimeException(expression.getClass() + " is not an Expression!!!");
            return null;
        }
    }

    public final boolean isLiteralNode() {
        return this instanceof LiteralNode;
    }

    public static boolean isLiteral(Expression expression) {
        return (expression instanceof NumberLiteral) ||
                (expression instanceof CharacterLiteral) ||
                (expression instanceof TypeLiteral) ||
                (expression instanceof NullLiteral) ||
                (expression instanceof StringLiteral) ||
                (expression instanceof BooleanLiteral);

    }

    public static boolean isOperationExpression(Expression expression) {
        return (expression instanceof InfixExpression) ||
                (expression instanceof PostfixExpression) ||
                (expression instanceof PrefixExpression) ||
                (expression instanceof ParenthesizedExpression);
    }

    public static void replaceMethodInvocationWithStub(Expression originExpression, MethodInvocation originMethodInvocation, ASTNode replacement) {
        if (isOperationExpression(originExpression)) {
            OperationExpressionNode.replaceMethodInvocationWithStub(originExpression, originMethodInvocation, replacement);
        } else if (originExpression instanceof Assignment) {
            AssignmentNode.replaceMethodInvocationWithStub((Assignment) originExpression, originMethodInvocation, replacement);
        } else if (originExpression instanceof VariableDeclarationExpression) {
            VariableDeclarationExpressionNode.replaceMethodInvocationWithStub((VariableDeclarationExpression) originExpression, originMethodInvocation, replacement);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "@" + System.identityHashCode(this) +
                fieldsToString();
    }

    protected String fieldsToString() {
        return "";
    }

    private boolean isFake = false;

    public void markFake() {
        isFake = true;
    }

    public boolean isFake() {
        return isFake;
    }
}
