package core.ast.Type;

import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Type.AnnotatableType.*;
import core.ast.Type.AnnotatableType.WildcardTypeNode;

public class TypeNode extends AstNode {

//    public static TypeNode executeType(Type type) {
//        if(type instanceof PrimitiveType) {
//            return PrimitiveTypeNode.executePrimitiveType((PrimitiveType) type);
//        } else if (type instanceof ArrayType) {
//            return ArrayTypeNode.executeArrayType((ArrayType) type);
//        }
//    }

    public final boolean isPrimitiveTypeNode() {
        return this instanceof PrimitiveTypeNode;
    }


    public final boolean isArrayTypeNode() {
        return this instanceof ArrayTypeNode;
    }

    public final boolean isNameQualifiedTypeNode() {
        return this instanceof NameQualifiedTypeNode;
    }

    public final boolean isParameterizedTypeNode() {
        return this instanceof ParameterizedTypeNode;
    }

    public final boolean isQualifiedTypeNode() {
        return this instanceof QualifiedTypeNode;
    }

    public final boolean isUnionTypeNode() {
        return this instanceof UnionTypeNode;
    }

    public final boolean isIntersectionTypeNode() {
        return this instanceof IntersectionTypeNode;
    }

    public final boolean isWildcardTypeNode() {
        return this instanceof WildcardTypeNode;
    }

    public final boolean isAnnotatableNode() {
        return this instanceof AnnotatableTypeNode;
    }

}
