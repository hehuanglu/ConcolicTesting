package core.ast.Expression.Name;


import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.QualifiedName;

public class QualifiedNameNode extends NameNode {
    private NameNode qualifier = null;
    private SimpleNameNode name = null;

    public static ExpressionNode executeQualifiedName(QualifiedName qualifiedName, MemoryModel memoryModel) {
        String fullName = qualifiedName.getFullyQualifiedName();

        if ("Integer.MAX_VALUE".equals(fullName)) {
            IntegerLiteralNode maxNode = new IntegerLiteralNode();
            maxNode.setValue("2147483647");
            return maxNode;
        }

        if ("Integer.MIN_VALUE".equals(fullName)) {
            IntegerLiteralNode minNode = new IntegerLiteralNode();
            minNode.setValue("-2147483648");
            return minNode;
        }

        QualifiedNameNode qualifiedNameNode = new QualifiedNameNode();
        qualifiedNameNode.qualifier = (NameNode) NameNode.executeName(qualifiedName.getQualifier(), memoryModel);
        qualifiedNameNode.name = (SimpleNameNode) SimpleNameNode.executeSimpleName(qualifiedName.getName(), memoryModel);
        return qualifiedNameNode;

        /*????*/
//        return null;
    }

    public static ExpressionNode executeQualifiedNameNode(QualifiedNameNode qualifiedNameNode, MemoryModel memoryModel) {
        /*????*/
        return null;
    }

    public static String getStringQualifiedName(QualifiedName qualifiedName) {
        /*????*/
        return null;
    }

    public static String getStringQualifiedNameNode(QualifiedNameNode qualifiedNameNode) {
        return null;
    }
}
