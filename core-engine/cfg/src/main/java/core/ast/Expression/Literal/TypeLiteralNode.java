package core.ast.Expression.Literal;

import core.ast.Type.TypeNode;

public class TypeLiteralNode extends LiteralNode {
    private TypeNode type = null;
    @Override
    public LiteralNode copy() {
        TypeLiteralNode copy = new TypeLiteralNode();
        return copy;
    }
}
