package core.ast.Expression.OperationExpression;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class ParenthesizedExpressionNode extends OperationExpressionNode {
    private ExpressionNode expression;

    public static void replaceMethodInvocationWithStub(ParenthesizedExpression originParenthesizedExpression, MethodInvocation originMethodInvocation, ASTNode replacement) {
        Expression expression = originParenthesizedExpression.getExpression();
        if (expression == originMethodInvocation)
            originParenthesizedExpression.setExpression((Expression) replacement);
    }

    public static Expr createZ3Expression(ParenthesizedExpressionNode parenthesizedExpressionNode, Context ctx, List<Z3VariableWrapper> vars, MemoryModel memoryModel) {
        ExpressionNode operand = parenthesizedExpressionNode.expression;

        Expr Z3Operand = OperationExpressionNode.createZ3Expression(operand, ctx, vars, memoryModel);
        return Z3Operand;
    }

    public static ExpressionNode executeParenthesizedExpression(ParenthesizedExpression parenthesizedExpression, MemoryModel memoryModel) {
        ParenthesizedExpressionNode parenthesizedExpressionNode = new ParenthesizedExpressionNode();
        parenthesizedExpressionNode.expression = (ExpressionNode) ExpressionNode.executeExpression(parenthesizedExpression.getExpression(), memoryModel);

        ExpressionNode expressionNode = executeParenthesizedExpressionNode(parenthesizedExpressionNode, memoryModel);
        return expressionNode;
    }

    public static ExpressionNode executeParenthesizedExpressionNode(ParenthesizedExpressionNode parenthesizedExpressionNode, MemoryModel memoryModel) {
        ExpressionNode expression = parenthesizedExpressionNode.expression;

        if(expression instanceof LiteralNode) {
            return expression;
        } else {
            ExpressionNode oldExpression = parenthesizedExpressionNode.expression;

            parenthesizedExpressionNode.expression = OperationExpressionNode.executeOperandNode(expression, memoryModel);

//            if(oldExpression != parenthesizedExpressionNode.expression) {
//                return ParenthesizedExpressionNode.executeParenthesizedExpressionNode(parenthesizedExpressionNode, memoryModel);
//            } else {
//                return parenthesizedExpressionNode;
//            }
            return parenthesizedExpressionNode;
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("");
        result.append("(");
        result.append(expression.toString());
        result.append(")");

        return result.toString();
    }
}
