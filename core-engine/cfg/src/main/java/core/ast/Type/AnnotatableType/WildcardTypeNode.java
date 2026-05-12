package core.ast.Type.AnnotatableType;

import core.ast.Type.AnnotatableType.AnnotatableTypeNode;
import core.ast.Type.TypeNode;

public class WildcardTypeNode extends AnnotatableTypeNode {
    private TypeNode optionalBound = null;
    private boolean isUpperBound = true;
}
