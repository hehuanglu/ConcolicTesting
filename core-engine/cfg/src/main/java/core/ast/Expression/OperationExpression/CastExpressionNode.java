package core.ast.Expression.OperationExpression;

import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Type;

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
}


