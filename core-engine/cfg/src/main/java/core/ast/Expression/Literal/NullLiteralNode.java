package core.ast.Expression.Literal;

import core.ast.Expression.Literal.LiteralNode;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.mockito.internal.matchers.Null;

public class NullLiteralNode extends LiteralNode {
    @Override
    public String toString() {
        return "null";
    }

    @Override
    public LiteralNode copy() {
        return new NullLiteralNode();
    }

    public static NullLiteralNode executeNullLiteral() {
        return new NullLiteralNode();
    }
}
