package core.ast.Type;

import core.ast.AstNode;
import org.eclipse.jdt.core.dom.ArrayType;

import java.util.List;

public class ArrayTypeNode extends TypeNode {
    private TypeNode type = null;
    private List<AstNode> dimensions = null;

    public TypeNode getType() {
        return type;
    }

    public void setType(TypeNode type) {
        this.type = type;
    }

//    public static ArrayTypeNode executeArrayType(ArrayType arrayType) {
//        ArrayTypeNode typeNode = new ArrayTypeNode();
//
//    }
}
