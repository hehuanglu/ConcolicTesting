package core.ast.Expression.OperationExpression;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
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

    public static Expr createZ3Expression(PostfixExpressionNode postfixExpressionNode, Context ctx, List<Z3VariableWrapper> vars, MemoryModel memoryModel) {
        ExpressionNode operand = postfixExpressionNode.operand;
        PostfixExpression.Operator operator = postfixExpressionNode.operator;

        Expr oldValue = OperationExpressionNode.createZ3Expression(operand, ctx, vars, memoryModel);
        Expr newValue = null;
        if (oldValue instanceof BitVecExpr) {
            BitVecExpr bvOld = (BitVecExpr) oldValue;
            int size = bvOld.getSortSize();
            BitVecExpr one = ctx.mkBV(1, size);

            if (operator == PostfixExpression.Operator.INCREMENT) {
                newValue = ctx.mkBVAdd(bvOld, one);
            } else if (operator == PostfixExpression.Operator.DECREMENT) {
                newValue = ctx.mkBVSub(bvOld, one);
            }
        }
        else if (oldValue instanceof FPExpr) {
            FPExpr fpOld = (FPExpr) oldValue;
            FPSort sort = (FPSort) fpOld.getSort();
            // Tạo số 1.0 đúng định dạng FP (Float hoặc Double)
            FPExpr one = ctx.mkFP(1.0, sort);
            FPRMExpr rm = ctx.mkFPRoundNearestTiesToEven();

            if (operator == PostfixExpression.Operator.INCREMENT) {
                newValue = ctx.mkFPAdd(rm, fpOld, one);
            } else if (operator == PostfixExpression.Operator.DECREMENT) {
                newValue = ctx.mkFPSub(rm, fpOld, one);
            }
        }
        //Cần bổ sung bước cập nhật giá trị mới vào MemoryModel
        return newValue;
    }

    public static ExpressionNode executePostfixExpression(PostfixExpression postfixExpression, MemoryModel memoryModel) {
        PostfixExpressionNode postfixExpressionNode = new PostfixExpressionNode();
        postfixExpressionNode.originalOperand = postfixExpression.getOperand();
        postfixExpressionNode.operand = (ExpressionNode) ExpressionNode.executeExpression(postfixExpression.getOperand(), memoryModel);
        postfixExpressionNode.operator = postfixExpression.getOperator();

        ExpressionNode expressionNode = executePostfixExpressionNode(postfixExpressionNode, memoryModel);
        return expressionNode;
    }

    public static ExpressionNode executePostfixExpressionNode(PostfixExpressionNode postfixExpressionNode, MemoryModel memoryModel) {
        ExpressionNode operand = postfixExpressionNode.operand;
        PostfixExpression.Operator operator = postfixExpressionNode.operator;

        // PAUSE executing

        // RE-ASSIGN
            String key = postfixExpressionNode.originalOperand.toString();
            AstNode value = memoryModel.getValue(key);

            if (value instanceof NameNode) {
                InfixExpressionNode expr = new InfixExpressionNode();
                
                if (operator == PostfixExpression.Operator.INCREMENT) {
                    expr.setOperator(InfixExpression.Operator.PLUS);
                } else if (operator == PostfixExpression.Operator.DECREMENT) {
                    expr.setOperator(InfixExpression.Operator.MINUS);
                } else {
                    throw new IllegalStateException(
                            "Unsupported postfix operator (only ++/-- supported): " + operator);
                }

                expr.setLeftOperand((ExpressionNode) value);

                IntegerLiteralNode one = new IntegerLiteralNode();
                one.setTokenValue(1);
                expr.setRightOperand(one);

                memoryModel.assignVariable(key, expr);
            } else if (value instanceof InfixExpressionNode) {

                if (operator != PostfixExpression.Operator.INCREMENT
                        && operator != PostfixExpression.Operator.DECREMENT) {
                    throw new IllegalStateException(
                            "Unsupported postfix operator on infix expression: " + operator);
                }

                InfixExpressionNode inf = (InfixExpressionNode) value;

                if (!(inf.getRightOperand() instanceof IntegerLiteralNode)) {
                    throw new IllegalStateException(
                            "Right operand is not IntegerLiteralNode: " + inf.getRightOperand());
                }

                IntegerLiteralNode kNode = (IntegerLiteralNode) inf.getRightOperand();
                int k = kNode.getIntegerValue();

                if (operator == PostfixExpression.Operator.INCREMENT) {
                    k++;
                } else {
                    k--;
                }

                IntegerLiteralNode newK = new IntegerLiteralNode();
                newK.setTokenValue(k);
                inf.setRightOperand(newK);

                memoryModel.assignVariable(key, inf);
            } else if(value instanceof LiteralNode) {
                memoryModel.assignVariable(key, LiteralNode.analyzeOnePostfixLiteral((LiteralNode) value, operator));
            } else if (value instanceof OperationExpressionNode) {
                PostfixExpressionNode newValue = new PostfixExpressionNode();
                newValue.operator = operator;
                newValue.operand = (OperationExpressionNode) value;
                memoryModel.assignVariable(key, newValue);
            }

        // CONTINUE executing
//        if(oldOperand != postfixExpressionNode.operand) {
//            return executePostfixExpressionNode(postfixExpressionNode, memoryModel);
//        } else {
//            return postfixExpressionNode;
//        }
        return postfixExpressionNode;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("");
        result.append(operand.toString());
        result.append(operator.toString());

        return result.toString();
    }


}
