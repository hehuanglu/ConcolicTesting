package core.ast.Expression;

import core.ast.Expression.Name.NameNode;
import core.ast.Type.ArrayTypeNode;
import core.ast.AstNode;
import core.ast.Type.AnnotatableType.PrimitiveTypeNode;
import core.ast.Expression.Literal.LiteralNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class ArrayCreationNode extends ExpressionNode {

    private ArrayTypeNode arrayType = null;
    private List<AstNode> dimensions;
    private ArrayInitializerNode optionalInitializer;

    public static ArrayNode executeArrayCreation(ArrayCreation arrayCreation, MemoryModel memoryModel) {
        List<ASTNode> dimensions = arrayCreation.dimensions();
        int numberOfDimensions = dimensions.size();

        ArrayType type = arrayCreation.getType();
        Type elementType = type.getElementType();

        return (ArrayNode) createMultiDimensionsInitializationArray(dimensions, 0,
                numberOfDimensions, elementType, memoryModel);
    }


    private static AstNode createMultiDimensionsInitializationArray(List<ASTNode> dimensions,
                                                                    int iterateDimension, int numberOfDimensions, Type type, MemoryModel memoryModel) {
        int capacityOfDimension;
        AstNode dimension = AstNode.executeASTNode(dimensions.get(iterateDimension), memoryModel);
        if(dimension instanceof NameNode) {
            dimension = NameNode.executeNameNode((NameNode) dimension, memoryModel);
        }
        if(dimension instanceof LiteralNode) {
            capacityOfDimension = LiteralNode.changeLiteralNodeToInteger((LiteralNode) dimension);
        } else if (dimension instanceof NameNode) {
            System.out.println("SYMBOLIC CAPACITY");
            capacityOfDimension = 100; // SYMBOLIC CAPACITY
        } else {
            throw new RuntimeException("Can't execute Dimension");
        }

        if (iterateDimension >= 0 && iterateDimension < numberOfDimensions - 1) {
            ArrayNode tmpArray = new ArrayNode(capacityOfDimension);
            for (int i = 0; i < capacityOfDimension; i++) {
                ((ArrayNode) tmpArray).set(createMultiDimensionsInitializationArray(dimensions, iterateDimension + 1,
                        numberOfDimensions, type, memoryModel), i);
            }
            return tmpArray;
        } else if (iterateDimension == numberOfDimensions - 1) {
            if (type.isPrimitiveType()) {
                return new ArrayNode(PrimitiveTypeNode.changePrimitiveTypeToLiteralInitializationArray(
                        (PrimitiveType) type, capacityOfDimension));
            } else if (type.isSimpleType()) {
                // ...?
                throw new RuntimeException("Invalid type");
            } else {
                throw new RuntimeException("Invalid type");
            }
        } else {
            throw new RuntimeException("Iterate dimension out of bound!");
        }
    }


}
