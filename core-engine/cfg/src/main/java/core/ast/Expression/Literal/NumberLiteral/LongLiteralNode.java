package core.ast.Expression.Literal.NumberLiteral;

import core.ast.Expression.ExpressionNode;

public class LongLiteralNode extends NumberLiteralNode{
    public LongLiteralNode() {
    }

    public LongLiteralNode(int value) {
        super.setTokenValue(String.valueOf(value));
    }

    public long getLongValue() {
        String token = super.getTokenValue();
        if (isLongValue(token)) {
            return Long.parseLong(token);
        } else {
            return 0;
        }
    }
    @Override
    public ExpressionNode copy() {
        return null;
    }
}
