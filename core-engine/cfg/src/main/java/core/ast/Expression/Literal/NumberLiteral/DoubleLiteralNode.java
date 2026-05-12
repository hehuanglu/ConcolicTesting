package core.ast.Expression.Literal.NumberLiteral;

import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;

public class DoubleLiteralNode extends NumberLiteralNode {

    public DoubleLiteralNode() {
        super.setTokenValue("0.0");
    }

    public static DoubleLiteralNode executeDoubleLiteral(double value) {
        DoubleLiteralNode doubleLiteralNode = new DoubleLiteralNode();
        doubleLiteralNode.setTokenValue(value);
        return doubleLiteralNode;
    }

    public static DoubleLiteralNode[] createDoubleLiteralInitializationArray(int capacity) {
        DoubleLiteralNode[] array = new DoubleLiteralNode[capacity];
        for(int i = 0; i < capacity; i++) {
            array[i] = new DoubleLiteralNode();
        }
        return array;
    }

    public double getDoubleValue() {
        String token = super.getTokenValue();
        if(isDoubleValue(token)) {
            return Double.parseDouble(token);
        } else {
            return 0.0;
        }
    }

    public void setTokenValue(double doubleValue) {
        super.setTokenValue(String.valueOf(doubleValue));
    }

    public void setTokenValue(int integerValue) {
        super.setTokenValue(String.valueOf((double) integerValue));
    }

    @Override
    public String toString() {
        return super.getTokenValue();
    }

    @Override
    public ExpressionNode copy() {
        return executeDoubleLiteral(getDoubleValue());
    }
}
