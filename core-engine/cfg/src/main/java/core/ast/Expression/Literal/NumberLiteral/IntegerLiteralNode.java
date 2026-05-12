package core.ast.Expression.Literal.NumberLiteral;

import core.ast.Expression.ExpressionNode;

public class IntegerLiteralNode extends NumberLiteralNode {

    public IntegerLiteralNode() {
    }

    public IntegerLiteralNode(int value) {
        super.setTokenValue(String.valueOf(value));
    }

    public static IntegerLiteralNode executeIntegerLiteral(int value) {
        IntegerLiteralNode integerLiteralNode = new IntegerLiteralNode();
        integerLiteralNode.setTokenValue(value);
        return integerLiteralNode;
    }

    public static IntegerLiteralNode[] createIntegerLiteralInitializationArray(int capacity) {
        IntegerLiteralNode[] array = new IntegerLiteralNode[capacity];
        for (int i = 0; i < capacity; i++) {
            array[i] = new IntegerLiteralNode();
        }
        return array;
    }

    public int getIntegerValue() {
        String token = super.getTokenValue();
        if (isIntegerValue(token)) {
            return Integer.parseInt(token);
        } else {
            return 0;
        }
    }

    public void setValue(String name) {
        this.setTokenValue(name);
    }

    public void setTokenValue(double doubleValue) {
        super.setTokenValue(String.valueOf((int) doubleValue));
    }

    public void setTokenValue(int integerValue) {
        super.setTokenValue(String.valueOf(integerValue));
    }

    @Override
    public String toString() {
        return super.getTokenValue();
    }

    @Override
    public ExpressionNode copy() {
        return new IntegerLiteralNode(this.getIntegerValue());
    }
}
