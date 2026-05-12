package core.ast.Type;

import core.ast.AstNode;
import core.ast.Type.TypeNode;

import java.util.List;

public class UnionTypeNode extends TypeNode {
    private List<AstNode> types;
}
