package core.ast.Expression.OperationExpression;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Type;

import java.util.List;

public class CastExpressionNode extends ExpressionNode {
    // Lưu kiểu đích như là  long, int là kiểu cần ép
    private Type targetNode;
    // Lưu biểu thức bị ép kiểu
    private ExpressionNode innerExpression;

    public CastExpressionNode(Type targetNode, ExpressionNode innerExpression) {
        this.targetNode = targetNode;
        this.innerExpression = innerExpression;
    }

    public CastExpressionNode() {
    }

    public Type getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(Type targetNode) {
        this.targetNode = targetNode;
    }

    public ExpressionNode getInnerExpression() {
        return innerExpression;
    }

    public void setInnerExpression(ExpressionNode innerExpression) {
        this.innerExpression = innerExpression;
    }

    // Hàm thực thi: Bóc tách AST của Eclipse JDT
    public static AstNode executeCastExpression(CastExpression castExpression, MemoryModel memoryModel) {
        Type type = castExpression.getType();
        Expression expression = castExpression.getExpression();

        // Gọi đệ quy để xử lý cái ruột bên trong (ví dụ nó sẽ trả về NameNode của biến y)
        ExpressionNode innerNode = (ExpressionNode) ExpressionNode.executeExpression(expression, memoryModel);

        return new CastExpressionNode(type, innerNode);
    }

    public static Expr createZ3Expression(CastExpressionNode castNode, MemoryModel memoryModel,
                                          Context ctx, List<Z3VariableWrapper> vars) {

        String targetType = castNode.getTargetNode().toString();

        ExpressionNode innerExpr = castNode.getInnerExpression();

        Expr z3Inner = OperationExpressionNode.createZ3Expression(innerExpr, ctx, vars, memoryModel);

        if (z3Inner instanceof BitVecExpr) {
            BitVecExpr arg = (BitVecExpr) z3Inner;

            int currentSize = arg.getSortSize();

            if ("long".equals(targetType) && currentSize == 32) {
                System.out.println("Đã ép kiểu int thành long cho Z3");
                return ctx.mkSignExt(32, arg);
            } else if ("int".equals(targetType) && currentSize == 64) {
                System.out.println(" Đã ép kiểu long thành int cho Z3");
                return ctx.mkExtract(31, 0, arg);
            } else if ("short".equals(targetType) && currentSize == 32) {
                System.out.println(" Đã ép kiểu int thành short cho Z3");
                return ctx.mkExtract(15, 0, arg);
            } else if ("int".equals(targetType) && currentSize == 8) {
                return ctx.mkSignExt(24, arg);
            }
        }

        return z3Inner;
    }
}


