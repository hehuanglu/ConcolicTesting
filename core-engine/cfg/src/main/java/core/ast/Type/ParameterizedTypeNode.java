package core.ast.Type;

import core.ast.AstNode;

import java.util.List;

public class ParameterizedTypeNode extends TypeNode {
    private TypeNode type = null;
    private List<AstNode> typeArguments;
}
