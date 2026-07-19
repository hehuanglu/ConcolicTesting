package core.ast.Expression.OperationExpression;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.AssignmentNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Name.NameNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;

import java.util.List;

import static org.eclipse.jdt.core.dom.Assignment.Operator.MINUS_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.PLUS_ASSIGN;

public class PrefixExpressionNode extends OperationExpressionNode {
    private ExpressionNode operand;
    private PrefixExpression.Operator operator;
    private Expression originalOperand;

    public static void replaceMethodInvocationWithStub(PrefixExpression originPrefixExpression, MethodInvocation originMethodInvocation, ASTNode replacement) {
        Expression operand = originPrefixExpression.getOperand();
        if (operand == originMethodInvocation)
            originPrefixExpression.setOperand((Expression) replacement);
    }

    public static Expr createZ3Expression(PrefixExpressionNode prefixExpressionNode, Context ctx,
                                          List<Z3VariableWrapper> vars, MemoryModel memoryModel) {
        ExpressionNode operand = prefixExpressionNode.operand;
        PrefixExpression.Operator operator = prefixExpressionNode.operator;

        Expr z3Operand = OperationExpressionNode.createZ3Expression(operand, ctx, vars, memoryModel);

        if (operator.equals(PrefixExpression.Operator.INCREMENT)
                || operator.equals(PrefixExpression.Operator.DECREMENT)) {
            // MemoryModel đã được execute() cập nhật TRƯỚC khi hàm này chạy,
            // và lookup ở trên là live-lookup → z3Operand đã phản ánh giá trị MỚI rồi.
            // KHÔNG cộng/trừ thêm 1 lần nữa ở đây, nếu không sẽ bị tăng/giảm 2 lần.
            return z3Operand;
        }

        if (operator.equals(PrefixExpression.Operator.PLUS)) {
            return z3Operand;
        }

        if (operator.equals(PrefixExpression.Operator.MINUS)) {
            if (z3Operand instanceof FPExpr) {
                return ctx.mkFPNeg((FPExpr) z3Operand);
            } else if (z3Operand instanceof BitVecExpr) {
                return ctx.mkBVNeg((BitVecExpr) z3Operand);
            } else if (z3Operand instanceof ArithExpr) { // bao gồm IntExpr
                return ctx.mkUnaryMinus((ArithExpr) z3Operand);
            } else {
                throw new IllegalStateException("Unsupported sort for unary MINUS: " + z3Operand.getSort());
            }
        }

        if (operator.equals(PrefixExpression.Operator.NOT)) {
            if (!(z3Operand instanceof BoolExpr)) {
                throw new IllegalStateException("NOT operator requires BoolExpr, got: " + z3Operand.getSort());
            }
            return ctx.mkNot((BoolExpr) z3Operand);
        }

        if (operator.equals(PrefixExpression.Operator.COMPLEMENT)) {
            if (z3Operand instanceof BitVecExpr) {
                return ctx.mkBVNot((BitVecExpr) z3Operand);
            } else if (z3Operand instanceof IntExpr) {
                // ~x == -x - 1 (định nghĩa toán học của bitwise complement trên số nguyên)
                IntExpr intVal = (IntExpr) z3Operand;
                return ctx.mkSub(ctx.mkUnaryMinus(intVal), ctx.mkInt(1));
            } else {
                throw new IllegalStateException("Unsupported sort for COMPLEMENT: " + z3Operand.getSort());
            }
        }

        throw new IllegalStateException("Unknown Prefix Op: " + operator);
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

        if (operand.isLiteralNode()) {
            return LiteralNode.analyzeOnePrefixLiteral(operator, (LiteralNode) operand);
        }

        if (operator == PrefixExpression.Operator.INCREMENT
                || operator == PrefixExpression.Operator.DECREMENT) {
            boolean isIncrement = operator == PrefixExpression.Operator.INCREMENT;
            return AssignmentNode.executeIncrementDecrement(
                    prefixExpressionNode.originalOperand, operand, isIncrement, memoryModel);
        }

        if (operator == PrefixExpression.Operator.NOT
                || operator == PrefixExpression.Operator.MINUS
                || operator == PrefixExpression.Operator.COMPLEMENT
                || operator == PrefixExpression.Operator.PLUS) {
            return prefixExpressionNode; // symbolic, để tầng Z3 conversion xử lý
        }

        throw new IllegalStateException("Unsupported prefix operator: " + operator);
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