package core.ast.VariableDeclaration;

import core.ast.AstNode;
import core.ast.Expression.ArrayNode;
import core.ast.Expression.Name.SimpleNameNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class SingleVariableDeclarationNode extends VariableDeclarationNode {

    private List<AstNode> modifiers = null;
    private Type type = null;
    private List<AstNode> varargsAnnotations = null;
    private boolean variableArity = false;

    public static void executeSingleVariableDeclaration(SingleVariableDeclaration singleVariableDeclaration, MemoryModel memoryModel) {
        // For parameters!!!
        Type type = singleVariableDeclaration.getType();
        SimpleName variableName = singleVariableDeclaration.getName();
        SimpleNameNode simpleNameNode = new SimpleNameNode();
        simpleNameNode.setIdentifier(variableName.getIdentifier());

        String key = simpleNameNode.getIdentifier();

        if(type instanceof PrimitiveType) {
            memoryModel.declarePrimitiveTypeVariable(((PrimitiveType) type), key, simpleNameNode);
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            memoryModel.declareArrayTypeVariable(arrayType, key, arrayType.getDimensions(), createMultiDimensionsInitializationArray(key, 0, arrayType.getDimensions(), arrayType.getElementType(), memoryModel));
        } else { // OTHER TYPES
            throw new RuntimeException("Invalid type");
        }


//        if(type instanceof PrimitiveType) {
//            memoryModel.put(key, PrimitiveTypeNode.changePrimitiveTypeToLiteralInitialization((PrimitiveType) type));
//        } else {
//            /*????*/
//            throw new RuntimeException("Did not handle other type yet");
//        }
    }

    public static AstNode createMultiDimensionsInitializationArray(String identifier,
                                                                    int iterateDimension, int numberOfDimensions, Type type, MemoryModel memoryModel) {
        int capacityOfDimension = 10;  // SYMBOLIC CAPACITY

        if (iterateDimension >= 0 && iterateDimension < numberOfDimensions - 1) {
            ArrayNode tmpArray = new ArrayNode(capacityOfDimension);
            for (int i = 0; i < capacityOfDimension; i++) {
                String tmpIdentifier = identifier + "[" + i + "]";
                tmpArray.set(createMultiDimensionsInitializationArray(tmpIdentifier, iterateDimension + 1,
                        numberOfDimensions, type, memoryModel), i);
            }
            return tmpArray;
        } else if (iterateDimension == numberOfDimensions - 1) {
            SimpleNameNode[] array = new SimpleNameNode[capacityOfDimension];
            for(int i = 0; i < capacityOfDimension; i++) {
                String tmpIdentifier = identifier + "[" + i + "]";
                SimpleNameNode element = new SimpleNameNode(tmpIdentifier);
                array[i] = element;
                // Tạo từng phần từ của mảng trong parameter ra bên ngoài ArrayNode trong MemoryModel
                if(type instanceof PrimitiveType) {
                    memoryModel.declarePrimitiveTypeVariable(((PrimitiveType) type), tmpIdentifier, element);
                } else {
                    throw new RuntimeException("Invalid type");
                }
                //============================
            }
            return new ArrayNode(array);
        } else {
            throw new RuntimeException("Iterate dimension out of bound!");
        }
    }

    public static void replaceMethodInvocationWithStub(SingleVariableDeclaration originSingleVariableDeclaration, MethodInvocation originMethodInvocation, ASTNode replacement) {

    }
}
