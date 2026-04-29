package core.ast.Expression.Array;

import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NullLiteralNode;
import core.ast.Expression.Name.NameNode;
import core.ast.Type.AnnotatableType.PrimitiveTypeNode;
import core.ast.Type.ArrayTypeNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class ArrayCreationWithNewKeyWord implements ArrayCreationStrategy {

    @Override
    public ArrayNode execute(ArrayCreation arrayCreation, MemoryModel memoryModel) {
        List<ASTNode> dimensions = arrayCreation.dimensions();
        int numberOfDimensions = dimensions.size();
        ArrayType type = arrayCreation.getType();
        Type elementType = type.getElementType();

        ArrayNode arrayNode = createMultiDimensionsInitializationArray(dimensions,
                numberOfDimensions, numberOfDimensions, 0, elementType, memoryModel);
        return arrayNode;
    }

    private ArrayNode createMultiDimensionsInitializationArray(List<ASTNode> dimensions,
                                                               int numberOfDimensions,
                                                               int demensionIndex,
                                                               int nextDimension,
                                                               Type elementType,
                                                               MemoryModel memoryModel) {
        int capacityOfDimension = 0;
        ExpressionNode lengthOfDimension = null;
        AstNode dimension = AstNode.executeASTNode(dimensions.get(nextDimension), memoryModel);
        if(dimension instanceof NameNode) {
            dimension = NameNode.executeNameNode((NameNode) dimension, memoryModel);
        }
        if(dimension instanceof LiteralNode) {
            capacityOfDimension = LiteralNode.changeLiteralNodeToInteger((LiteralNode) dimension);
            lengthOfDimension = (LiteralNode) dimension;
        } else if (dimension instanceof NameNode) {
            lengthOfDimension = (NameNode) dimension;
            System.out.println("SYMBOLIC CAPACITY");
        } else {
            throw new RuntimeException("Can't execute Dimension");
        }
        ArrayNode result = new ArrayNode();
        result.setLengthOfDimensions(lengthOfDimension);
        result.setNumberOfDimensions(demensionIndex);
        if (nextDimension >= 0 && nextDimension < numberOfDimensions - 1) {
            ArrayNode element = createMultiDimensionsInitializationArray(dimensions,
                    numberOfDimensions, demensionIndex - 1, nextDimension + 1,
                    elementType, memoryModel);
            result.setType(new ArrayTypeNode());
            for (int i = 0; i < capacityOfDimension; i++) {
                    result.setElements(i, element.copy());
            }
        } else if (nextDimension == numberOfDimensions - 1) {
            if (elementType.isPrimitiveType()) {
                PrimitiveTypeNode primitiveTypeNode = new PrimitiveTypeNode();
                primitiveTypeNode.setTypeCode(((PrimitiveType) elementType).getPrimitiveTypeCode());
                result.setType(primitiveTypeNode);
                LiteralNode[] elements = PrimitiveTypeNode.changePrimitiveTypeToLiteralInitializationArray(
                        (PrimitiveType) elementType, capacityOfDimension);
                result.setElements(0, elements);
            } else if (elementType.isSimpleType()) {
                throw new RuntimeException("Invalid type");
            } else {
                throw new RuntimeException("Invalid type");
            }
        } else {
            throw new RuntimeException("Iterate dimension out of bound!");
        }
        return result;
    }
}
