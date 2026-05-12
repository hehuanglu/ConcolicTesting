package core.ast.Expression.Array;

import core.ast.Expression.ExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;

import java.util.List;

public class ArrayCreationNode extends ExpressionNode {
    private List<ASTNode> dimensions;
    private ArrayCreationStrategy strategy;

    public static ArrayNode executeArrayCreation(ArrayCreation arrayCreation, MemoryModel memoryModel,
                                                 ArrayCreationStrategy strategy) {
        return strategy.execute(arrayCreation, memoryModel);

    }
}
