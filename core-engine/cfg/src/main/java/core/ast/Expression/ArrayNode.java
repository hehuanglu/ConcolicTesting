package core.ast.Expression;

import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;

public class ArrayNode extends ExpressionNode {
    private AstNode[] array = new AstNode[0];

    public ArrayNode() {
    }

    public ArrayNode(int capacity) {
        this.array = new AstNode[capacity];
    }

    public ArrayNode(AstNode[] array) {
        this.array = array;
    }

    public void setArray(AstNode[] array) {
        this.array = array;
    }

    public AstNode[] getArray() {
        return array;
    }

    public AstNode get(int index) {
        if (index < 0 || index >= array.length) {
            throw new RuntimeException(index + ": Index out of bound!!!");
        }
        return this.array[index];
    }

    public void set(AstNode astNode, int index) {
        if (index < 0 || index >= array.length) {
            throw new RuntimeException("Index out of bound!!!");
        }
        this.array[index] = astNode;
    }

    public int length() {
        return this.array.length;
    }
}
