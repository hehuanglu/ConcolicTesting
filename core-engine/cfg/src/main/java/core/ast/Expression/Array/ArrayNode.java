package core.ast.Expression.Array;

import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.BooleanLiteralNode;
import core.ast.Expression.Literal.CharacterLiteralNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NullLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.DoubleLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Type.AnnotatableType.PrimitiveTypeNode;
import core.ast.Type.TypeNode;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.ast.NumberLiteral;

import java.util.ArrayList;
import java.util.List;

public class ArrayNode extends ExpressionNode {
    private int numberOfDimensions;
    private ExpressionNode lengthOfDimensions;
    private List<ExpressionNode> elements;
    private TypeNode type;

    public ArrayNode() {
        this.elements = new ArrayList<>();
    }

    public TypeNode getType() {
        return type;
    }

    public void setType(TypeNode type) {
        this.type = type;
    }

    public List<ExpressionNode> getElements(int leastLengthRequire) {
        if (this.elements.size() <= leastLengthRequire) {
            for (int i = this.elements.size(); i <= leastLengthRequire; i++) {
                ExpressionNode defaultValue = getDefaultValue();
                this.elements.add(defaultValue);
            }
        }
        return elements;
    }

    public void setElements(int index, Object element) {
        if (numberOfDimensions > 1) {
            setArrayElements(index, (ArrayNode) element);
        } else {
            setTypeElements((LiteralNode[]) element);
        }
    }

    public void assignElements(int index, ExpressionNode element) {
        if (this.elements.size() <= index) {
            for (int i = this.elements.size(); i <= index; i++) {
                ExpressionNode defaultValue = getDefaultValue();
                this.elements.add(defaultValue);
            }
        }
        this.elements.set(index, element);
    }

    private void setTypeElements(LiteralNode[] elements) {
        this.elements = new ArrayList<>(List.of(elements));
    }

    private void setArrayElements(int index, ArrayNode elements) {
        while (this.elements.size() <= index) {
            this.elements.add(null);
        }
        this.elements.set(index, elements);
    }

    public ExpressionNode getLengthOfDimensions() {
        return lengthOfDimensions;
    }

    public void setLengthOfDimensions(ExpressionNode lengthOfDimension) {
        this.lengthOfDimensions = lengthOfDimension;
    }

    public int getNumberOfDimensions() {
        return numberOfDimensions;
    }

    public void setNumberOfDimensions(int numberOfDimensions) {
        this.numberOfDimensions = numberOfDimensions;
    }

    public ExpressionNode getDefaultValue() {
        if (type.isPrimitiveTypeNode()) {
            if (((PrimitiveTypeNode) type).getTypeCode().equals(PrimitiveType.BOOLEAN)) {
                return BooleanLiteralNode.createBooleanLiteral(false);
            } else if (((PrimitiveTypeNode) type).getTypeCode().equals(PrimitiveType.CHAR)) {
                return CharacterLiteralNode.createCharacterLiteral('X');
            } else if (((PrimitiveTypeNode) type).getTypeCode().equals(PrimitiveType.INT) ||
                       ((PrimitiveTypeNode) type).getTypeCode().equals(PrimitiveType.BYTE) ||
                       ((PrimitiveTypeNode) type).getTypeCode().equals(PrimitiveType.SHORT) ||
                       ((PrimitiveTypeNode) type).getTypeCode().equals(PrimitiveType.LONG)) {
                return IntegerLiteralNode.executeIntegerLiteral(0);
            } else if (((PrimitiveTypeNode) type).getTypeCode().equals(PrimitiveType.FLOAT) ||
                       ((PrimitiveTypeNode) type).getTypeCode().equals(PrimitiveType.DOUBLE)) {
                return DoubleLiteralNode.executeDoubleLiteral(0.0);
            }
            return new NullLiteralNode(); // Default for non-primitive types
        } else if (type.isArrayTypeNode()) {
            return new ArrayNode();
        }
        return new NullLiteralNode(); // Default for non-primitive types
    }

    public ArrayNode(ArrayNode arrayNode) {
        this.numberOfDimensions = arrayNode.numberOfDimensions;
        this.lengthOfDimensions = arrayNode.lengthOfDimensions;
        this.type = arrayNode.type;
        this.elements = new ArrayList<>();
        for (Object element : arrayNode.elements) {
            if (element instanceof ArrayNode) {
                this.elements.add(((ArrayNode) element).copy());
            } else if (element instanceof LiteralNode) {
                this.elements.add(((LiteralNode) element).copy());
            }
        }
    }

    public ArrayNode copy() {
        return new ArrayNode(this);
    }
}
