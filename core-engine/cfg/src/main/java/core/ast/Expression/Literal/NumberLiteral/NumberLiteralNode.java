package core.ast.Expression.Literal.NumberLiteral;

import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.NumberLiteral;

public abstract class NumberLiteralNode extends LiteralNode {
    private String tokenValue = "0";

    public static NumberLiteralNode executeNumberLiteral(NumberLiteral numberLiteral) {
        String tokenValue = numberLiteral.getToken();

        if(isIntegerValue(tokenValue)) {
            IntegerLiteralNode integerLiteralNode = new IntegerLiteralNode();
            integerLiteralNode.setTokenValue(tokenValue);
            return integerLiteralNode;
        } else { /*isDoubleValue(tokenValue)*/
            DoubleLiteralNode doubleLiteralNode = new DoubleLiteralNode();
            doubleLiteralNode.setTokenValue(tokenValue);
            return doubleLiteralNode;
        }
    }

    public String getTokenValue() {
        return this.tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public static boolean isDoubleValue(String tokenValue) {
        try {
            Double.parseDouble(tokenValue);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isIntegerValue(String tokenValue) {
        try {
            Integer.parseInt(tokenValue);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public final boolean isIntegerLiteralNode() {
        return this instanceof IntegerLiteralNode;
    }

    public final boolean isDoubleLiteralNode() {
        return this instanceof DoubleLiteralNode;
    }

    @Override
    public String toString() {
        return this.tokenValue;
    }

    abstract public ExpressionNode copy();


}
