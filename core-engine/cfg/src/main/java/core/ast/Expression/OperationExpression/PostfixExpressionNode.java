package core.ast.Expression.OperationExpression;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.AssignmentNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.Name.NameNode;
import core.ast.Expression.Name.SimpleNameNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression;


import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class PostfixExpressionNode extends OperationExpressionNode {
    private ExpressionNode operand;
    private PostfixExpression.Operator operator;
    private Expression originalOperand;

    public static void replaceMethodInvocationWithStub(PostfixExpression originPostfixExpression, MethodInvocation originMethodInvocation, ASTNode replacement) {
        Expression operand = originPostfixExpression.getOperand();
        if (operand == originMethodInvocation)
            originPostfixExpression.setOperand((Expression) replacement);
    }

    public static Expr createZ3Expression(PostfixExpressionNode postfixExpressionNode, Context ctx,
                                          List<Z3VariableWrapper> vars, MemoryModel memoryModel) {
        ExpressionNode operand = postfixExpressionNode.operand;
        PostfixExpression.Operator operator = postfixExpressionNode.operator;

        // Live-lookup này đã phản ánh giá trị SAU khi execute() chạy (side-effect đã xảy ra trước đó)
        Expr newValue = OperationExpressionNode.createZ3Expression(operand, ctx, vars, memoryModel);

        boolean isIncrement = operator == PostfixExpression.Operator.INCREMENT;
        if (!isIncrement && operator != PostfixExpression.Operator.DECREMENT) {
            throw new IllegalStateException("Unsupported postfix operator: " + operator);
        }

        // Giá trị CỦA BIỂU THỨC postfix là giá trị TRƯỚC khi tăng/giảm
        // => đảo ngược đúng phép toán mà execute() đã áp dụng
        Expr oldValue;
        if (newValue instanceof IntExpr) {
            IntExpr intNew = (IntExpr) newValue;
            IntExpr one = (IntExpr) ctx.mkInt(1);
            oldValue = isIncrement ? ctx.mkSub(intNew, one) : ctx.mkAdd(intNew, one);

        } else if (newValue instanceof BitVecExpr) {
            BitVecExpr bvNew = (BitVecExpr) newValue;
            BitVecExpr one = ctx.mkBV(1, bvNew.getSortSize());
            oldValue = isIncrement ? ctx.mkBVSub(bvNew, one) : ctx.mkBVAdd(bvNew, one);

        } else if (newValue instanceof FPExpr) {
            FPExpr fpNew = (FPExpr) newValue;
            FPSort sort = (FPSort) fpNew.getSort();
            FPExpr one = ctx.mkFP(1.0, sort);
            FPRMExpr rm = ctx.mkFPRoundNearestTiesToEven();
            oldValue = isIncrement ? ctx.mkFPSub(rm, fpNew, one) : ctx.mkFPAdd(rm, fpNew, one);

        } else {
            throw new IllegalStateException(
                    "Unsupported Z3 sort for postfix: " + (newValue == null ? "null" : newValue.getSort()));
        }

        return oldValue;
    }

    public static ExpressionNode executePostfixExpression(PostfixExpression postfixExpression, MemoryModel memoryModel) {
        PostfixExpressionNode postfixExpressionNode = new PostfixExpressionNode();
        postfixExpressionNode.originalOperand = postfixExpression.getOperand();
        postfixExpressionNode.operand = (ExpressionNode) ExpressionNode.executeExpression(postfixExpression.getOperand(), memoryModel);
        postfixExpressionNode.operator = postfixExpression.getOperator();

        ExpressionNode expressionNode = executePostfixExpressionNode(postfixExpressionNode, memoryModel);
        return expressionNode;
    }

    public static ExpressionNode executePostfixExpressionNode(PostfixExpressionNode postfixExpressionNode,
                                                              MemoryModel memoryModel) {
        ExpressionNode operand = postfixExpressionNode.operand;
        PostfixExpression.Operator operator = postfixExpressionNode.operator;

        if (operator != PostfixExpression.Operator.INCREMENT
                && operator != PostfixExpression.Operator.DECREMENT) {
            throw new IllegalStateException("Unsupported postfix operator: " + operator);
        }

        ExpressionNode oldValue = operand; // chụp lại TRƯỚC khi mutate

        boolean isIncrement = operator == PostfixExpression.Operator.INCREMENT;
        AssignmentNode.executeIncrementDecrement(
                postfixExpressionNode.originalOperand, operand, isIncrement, memoryModel);

        return oldValue;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("");
        result.append(operand.toString());
        result.append(operator.toString());

        return result.toString();
    }


}
