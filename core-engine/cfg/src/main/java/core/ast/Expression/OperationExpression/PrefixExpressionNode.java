package core.ast.Expression.OperationExpression;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Name.NameNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class PrefixExpressionNode extends OperationExpressionNode {
    private ExpressionNode operand;
    private PrefixExpression.Operator operator;
    private Expression originalOperand;

    public static void replaceMethodInvocationWithStub(PrefixExpression originPrefixExpression, MethodInvocation originMethodInvocation, ASTNode replacement) {
        Expression operand = originPrefixExpression.getOperand();
        if (operand == originMethodInvocation)
            originPrefixExpression.setOperand((Expression) replacement);
    }

    public static Expr createZ3Expression(PrefixExpressionNode prefixExpressionNode, Context ctx, List<Z3VariableWrapper> vars, MemoryModel memoryModel) {
        ExpressionNode operand = prefixExpressionNode.operand;
        PrefixExpression.Operator operator = prefixExpressionNode.operator;

        Expr Z3Operand = OperationExpressionNode.createZ3Expression(operand, ctx, vars, memoryModel);

        boolean isFP = Z3Operand instanceof FPExpr;
        Expr result = null;
        if(operator.equals(PrefixExpression.Operator.INCREMENT) || operator.equals(PrefixExpression.Operator.DECREMENT)) {
            if(isFP) {
                FPExpr one = ctx.mkFP(1.0, (FPSort) Z3Operand.getSort());
                FPRMExpr rm = ctx.mkFPRoundNearestTiesToEven();
                if (operator.equals(PrefixExpression.Operator.INCREMENT)) {
                    return ctx.mkFPAdd(rm, (FPExpr) Z3Operand, one);
                } else {
                    return ctx.mkFPSub(rm, (FPExpr) Z3Operand, one);
                }
            } else {
                BitVecExpr bvVal = (BitVecExpr) Z3Operand;
                BitVecExpr one = ctx.mkBV(1, bvVal.getSortSize());
                if (operator.equals(PrefixExpression.Operator.INCREMENT)) {
                    return ctx.mkBVAdd(bvVal, one);
                } else {
                    return ctx.mkBVSub(bvVal, one);
                }
            }
        } else if (operator.equals(PrefixExpression.Operator.PLUS)) {
            return Z3Operand;
        } else if (operator.equals(PrefixExpression.Operator.MINUS)) {
            if(isFP) {
                FPExpr bvVal = (FPExpr) Z3Operand;
                return ctx.mkFPNeg(bvVal);
            } else {
                BitVecExpr bvVal = (BitVecExpr) Z3Operand;
                return ctx.mkBVNeg(bvVal);
            }
        } else if (operator.equals(PrefixExpression.Operator.NOT)) {
            return ctx.mkNot(Z3Operand);
        } else if (operator.equals(PrefixExpression.Operator.COMPLEMENT)) {
            return ctx.mkBVNot(Z3Operand);
        } else {
            throw new RuntimeException("Unknown Prefix Op: " + operator);
        }
    }

    public static ExpressionNode executePrefixExpression(PrefixExpression prefixExpression, MemoryModel memoryModel) {
        PrefixExpressionNode prefixExpressionNode = new PrefixExpressionNode();
        prefixExpressionNode.originalOperand = prefixExpression.getOperand();
        prefixExpressionNode.operand = (ExpressionNode) ExpressionNode.executeExpression(prefixExpression.getOperand(), memoryModel);
        prefixExpressionNode.operator = prefixExpression.getOperator();

        ExpressionNode expressionNode = executePrefixExpressionNode(prefixExpressionNode, memoryModel);
        return expressionNode;
    }

    public static ExpressionNode executePrefixExpressionNode(PrefixExpressionNode prefixExpressionNode,
                                                             MemoryModel memoryModel) {
        ExpressionNode operand = prefixExpressionNode.operand;
        PrefixExpression.Operator operator = prefixExpressionNode.operator;
        if(operand.isLiteralNode()) {
            LiteralNode literalResult = LiteralNode.analyzeOnePrefixLiteral(operator, (LiteralNode) operand);
            return literalResult;
        } else {
//            ExpressionNode oldOperand = prefixExpressionNode.operand;
//
//            prefixExpressionNode.operand = OperationExpressionNode.executeOperandNode(operand, memoryModel);
//            // PAUSE executing
//
//            // RE_ASSIGN
            if((operator.equals(PrefixExpression.Operator.INCREMENT)
                    || operator.equals(PrefixExpression.Operator.DECREMENT))) {
//                String key = NameNode.getStringNameNode((NameNode) operand);
                String key = prefixExpressionNode.originalOperand.toString();
                AstNode value = memoryModel.getValue(key);

                if(value instanceof LiteralNode) {
                    memoryModel.assignVariable(key, LiteralNode.analyzeOnePrefixLiteral(operator, (LiteralNode) value));
                } else if (value instanceof OperationExpressionNode) {
                    PrefixExpressionNode newValue = new PrefixExpressionNode();
                    newValue.operator = operator;
                    newValue.operand = (OperationExpressionNode) value;
                    memoryModel.assignVariable(key, newValue);
                }
            }
            return prefixExpressionNode;
        }
    }

    public ExpressionNode getOperand() {
        return operand;
    }

    public void setOperand(ExpressionNode operand) {
        this.operand = operand;
    }

    public PrefixExpression.Operator getOperator() {
        return operator;
    }

    public void setOperator(PrefixExpression.Operator operator) {
        this.operator = operator;
    }

    public static boolean isBitwiseOperator(PrefixExpression.Operator operator) {
        if(operator.equals(PrefixExpression.Operator.COMPLEMENT)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("");
        result.append(operator.toString());
        result.append(operand.toString());

        return result.toString();
    }
}