package core.ast.Expression.Literal;

import org.eclipse.jdt.core.dom.StringLiteral;

public class StringLiteralNode extends LiteralNode {
    private String stringValue = "";

    public static StringLiteralNode executeStringLiteral(StringLiteral stringLiteral) {
        StringLiteralNode stringLiteralNode = new StringLiteralNode();
        stringLiteralNode.setStringValue(stringLiteral.getLiteralValue());
        return stringLiteralNode;
    }

    public String getEscapedValue() {
        return "\""+ this.stringValue + "\"";
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public String toString() {
        return getEscapedValue();
    }

    @Override
    public LiteralNode copy() {
        StringLiteralNode copy = new StringLiteralNode();
        copy.setStringValue(this.getStringValue());
        return copy;
    }
}
