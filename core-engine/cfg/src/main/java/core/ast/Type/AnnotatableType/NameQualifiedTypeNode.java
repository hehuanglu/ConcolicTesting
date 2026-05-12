package core.ast.Type.AnnotatableType;

import core.ast.Expression.Name.NameNode;
import core.ast.Expression.Name.SimpleNameNode;

public class NameQualifiedTypeNode extends AnnotatableTypeNode {
    private NameNode qualifier = null;
    private SimpleNameNode name = null;
}
