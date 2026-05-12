package core.ast.Expression.Array;

import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.ArrayCreation;

public interface ArrayCreationStrategy {
    public ArrayNode execute(ArrayCreation arrayCreation, MemoryModel memoryModel);
}
