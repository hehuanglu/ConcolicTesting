package core.ast.Expression.Array;

import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.BooleanLiteralNode;
import core.ast.Expression.Literal.CharacterLiteralNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NullLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.DoubleLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Type.AnnotatableType.PrimitiveTypeNode;
import core.ast.Type.TypeNode;
import org.eclipse.jdt.core.dom.PrimitiveType;

import java.util.ArrayList;
import java.util.List;

public class ArrayNode extends ExpressionNode {
    // --- Các thuộc tính phục vụ Phân tích Ký tự / Z3 Parameter ---
    private int numberOfDimensions = 1;
    private ExpressionNode lengthOfDimensions;
    private List<ExpressionNode> elements;
    private TypeNode type;

    // --- Thuộc tính tương thích ngược với hệ thống hạ tầng cũ (AstNode[]) ---
    private AstNode[] rawArray = null;

    // Khởi tạo mặc định
    public ArrayNode() {
        this.elements = new ArrayList<>();
    }

    // Khởi tạo tương thích hạ tầng cũ theo dung lượng cố định
    public ArrayNode(int capacity) {
        this.elements = new ArrayList<>();
        this.rawArray = new AstNode[capacity];
    }

    // Khởi tạo tương thích hạ tầng cũ bằng mảng tĩnh có sẵn
    public ArrayNode(AstNode[] array) {
        this.elements = new ArrayList<>();
        this.lengthOfDimensions = new IntegerLiteralNode(array.length);
        this.rawArray = array;
        // Đồng bộ sang danh sách elements để cả 2 luồng đều đọc được
        if (array != null) {
            for (AstNode node : array) {
                if (node instanceof ExpressionNode) {
                    this.elements.add((ExpressionNode) node);
                } else {
                    this.elements.add(new NullLiteralNode());
                }
            }
        }
    }

    // --- Hệ thống hàm Getter/Setter hợp nhất ---

    public AstNode[] getArray() {
        if (this.rawArray != null) {
            return this.rawArray;
        }
        // Nếu rawArray chưa tạo, convert tự động từ elements để không bị NullPointerException ở lõi cũ
        AstNode[] res = new AstNode[elements.size()];
        return elements.toArray(res);
    }

    public void setArray(AstNode[] array) {
        this.rawArray = array;
        this.elements.clear();
        if (array != null) {
            for (AstNode node : array) {
                if (node instanceof ExpressionNode) {
                    this.elements.add((ExpressionNode) node);
                } else {
                    this.elements.add(new NullLiteralNode());
                }
            }
        }
    }

    public AstNode get(int index) {
        // Ưu tiên đọc từ hạ tầng cũ nếu có
        if (this.rawArray != null) {
            if (index < 0 || index >= this.rawArray.length) {
                throw new RuntimeException(index + ": Index out of bound trong rawArray!!!");
            }
            return this.rawArray[index];
        }
        // Fallback đọc từ danh sách elements linh hoạt
        if (index < 0 || index >= this.elements.size()) {
            throw new RuntimeException(index + ": Index out of bound trong elements!!!");
        }
        return this.elements.get(index);
    }

    public void set(AstNode astNode, int index) {
        // Đồng bộ ghi vào hạ tầng cũ
        if (this.rawArray != null) {
            if (index < 0 || index >= this.rawArray.length) {
                throw new RuntimeException("Index out of bound trong rawArray!!!");
            }
            this.rawArray[index] = astNode;
        }
        // Đồng bộ ghi vào danh sách elements linh hoạt
        assignElements(index, (ExpressionNode) astNode);
    }

    public int length() {
        if (this.rawArray != null) {
            return this.rawArray.length;
        }
        return this.elements.size();
    }

    public TypeNode getType() {
        return type;
    }

    public void setType(TypeNode type) {
        this.type = type;
    }

    public ExpressionNode getElements(String name, ExpressionNode indexNode) {
        if (indexNode instanceof IntegerLiteralNode) {
            int id = ((IntegerLiteralNode) indexNode).getIntegerValue();
            if (id >= elements.size()) {
                throw new RuntimeException(id + ": Index out of bound trong elements!!!");
            }
            return elements.get(id);
        }
        return new ArrayAccessNode(name, indexNode);
    }

    public void setElements(int index, Object element) {
        if (numberOfDimensions > 1) {
            setArrayElements(index, (ArrayNode) element);
        } else {
            setTypeElements((LiteralNode[]) element);
        }
    }

    public void assignElements(int index, ExpressionNode element) {
        while (this.elements.size() <= index) {
            this.elements.add(getDefaultValue());
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

    public void pushElement(ExpressionNode element) {
        this.elements.add(element);
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
        if (type != null && type.isPrimitiveTypeNode()) {
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
        } else if (type != null && type.isArrayTypeNode()) {
            return new ArrayNode();
        }
        return new NullLiteralNode();
    }

    // Sao chép sâu đối tượng phục vụ nhánh lưu vết Path
    public ArrayNode(ArrayNode arrayNode) {
        this.numberOfDimensions = arrayNode.numberOfDimensions;
        this.lengthOfDimensions = arrayNode.lengthOfDimensions;
        this.type = arrayNode.type;
        this.elements = new ArrayList<>();
        if (arrayNode.rawArray != null) {
            this.rawArray = arrayNode.rawArray.clone();
        }
        for (Object element : arrayNode.elements) {
            if (element instanceof ArrayNode) {
                this.elements.add(((ArrayNode) element).copy());
            } else if (element instanceof LiteralNode) {
                this.elements.add(((LiteralNode) element).copy());
            } else if (element != null) {
                this.elements.add((ExpressionNode) element);
            }
        }
    }

    public ArrayNode copy() {
        return new ArrayNode(this);
    }
}