package core.ast.Type.AnnotatableType;

import core.ast.Expression.Name.SimpleNameNode;
import core.ast.Type.TypeNode;

public class QualifiedTypeNode extends AnnotatableTypeNode {
    private TypeNode qualifier = null;
    private SimpleNameNode name = null;
}
