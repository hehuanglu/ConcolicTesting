package core.ast.Expression.Name;


import core.ast.AstNode;
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
        if (fullName.endsWith(".length")) {

            SimpleNameNode qualifierNode = new SimpleNameNode();
            qualifierNode.setIdentifier(qualifiedName.getQualifier().getFullyQualifiedName());

            SimpleNameNode nameNode = new SimpleNameNode();
            nameNode.setIdentifier(qualifiedName.getName().getIdentifier());

            qualifiedNameNode.qualifier = qualifierNode;
            qualifiedNameNode.name = nameNode;

            return qualifiedNameNode;
        }

        AstNode qualifierAst = NameNode.executeName(qualifiedName.getQualifier(), memoryModel);

        if (qualifierAst instanceof NameNode) {
            qualifiedNameNode.qualifier = (NameNode) qualifierAst;
        } else {
            System.out.println("Qualifier không phải NameNode (nó là " + qualifierAst.getClass().getSimpleName() + "). Bỏ qua ép kiểu để tránh crash");
        }

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
        if (qualifiedNameNode == null) return null;

        // Lấy vế trái
        String qualifierStr = NameNode.getStringNameNode(qualifiedNameNode.qualifier);

        // Lấy vế phải
        String nameStr = SimpleNameNode.getStringSimpleNameNode(qualifiedNameNode.name);

        return qualifierStr + "." + nameStr;
    }
}
