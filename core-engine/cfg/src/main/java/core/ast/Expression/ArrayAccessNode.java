package core.ast.Expression;

import core.ast.AstNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Name.NameNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;

public class ArrayAccessNode extends ExpressionNode {
    private ExpressionNode arrayExpression = null;
    private ExpressionNode indexExpression = null;

    public static ExpressionNode executeArrayAccessNode(ArrayAccess arrayAccess, MemoryModel memoryModel) {
        int index;
        ExpressionNode arrayIndex = (ExpressionNode) AstNode.executeASTNode(arrayAccess.getIndex(), memoryModel);
        if(arrayIndex instanceof NameNode) {
            arrayIndex = NameNode.executeNameNode((NameNode) arrayIndex, memoryModel);
        }
        if(arrayIndex instanceof LiteralNode) {
            index = LiteralNode.changeLiteralNodeToInteger((LiteralNode) arrayIndex);
        } else {
            throw new RuntimeException("Can't execute Index");
        }

        Expression arrayExpression = arrayAccess.getArray();
        if(arrayExpression instanceof ArrayAccess) {
            ArrayNode arrayNode = (ArrayNode) executeArrayAccessNode((ArrayAccess) arrayExpression, memoryModel);
            return (ExpressionNode) arrayNode.get(index);
        } else if(arrayExpression instanceof Name){
            String name = NameNode.getStringName((Name) arrayExpression);
            return (ExpressionNode) ((ArrayNode) memoryModel.getValue(name)).get(index);
        } else {
            throw new RuntimeException("Can't execute ArrayAccess");
        }
    }
}
