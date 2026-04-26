package core.ast.Type.AnnotatableType;

import core.ast.AstNode;
import core.ast.Expression.Literal.StringLiteralNode;
import core.ast.Expression.Name.NameNode;

public class SimpleTypeNode extends AnnotatableTypeNode {
    private NameNode typeName = null;
    public static AstNode initializeString(){
        StringLiteralNode emptyString = new StringLiteralNode();
        emptyString.setStringValue("");
        return emptyString;
    }
}
