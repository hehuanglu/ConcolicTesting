package core.ast.Expression.Name;

import core.ast.Expression.ExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

public abstract class NameNode extends ExpressionNode {

    public static ExpressionNode executeName(Name name, MemoryModel memoryModel) {
        if(name instanceof SimpleName) {
            return SimpleNameNode.executeSimpleName((SimpleName) name, memoryModel);
        } else { /*name instanceof QualifiedName*/
            return QualifiedNameNode.executeQualifiedName((QualifiedName) name, memoryModel);
        }
    }

    public static ExpressionNode executeNameNode(NameNode nameNode, MemoryModel memoryModel) {
        if(nameNode instanceof SimpleNameNode) {
            return SimpleNameNode.executeSimpleNameNode((SimpleNameNode) nameNode, memoryModel);
        } else { /*nameNode instanceof QualifiedNameNode*/
            return QualifiedNameNode.executeQualifiedNameNode((QualifiedNameNode) nameNode, memoryModel);
        }
    }

    public static String getStringName(Name name) {
        if(name instanceof SimpleName) {
            return SimpleNameNode.getStringSimpleName((SimpleName) name);
        } else { /*name instanceof QualifiedName*/
            return QualifiedNameNode.getStringQualifiedName((QualifiedName) name);
        }
    }

    public static String getStringNameNode(NameNode nameNode) {
        if(nameNode instanceof SimpleNameNode) {
            return SimpleNameNode.getStringSimpleNameNode((SimpleNameNode) nameNode);
        } else { // nameNode instanceof QualifiedNameNode
            return QualifiedNameNode.getStringQualifiedNameNode((QualifiedNameNode) nameNode);
        }
    }

    public final boolean isSimpleNameNode() {
        return this instanceof SimpleNameNode;
    }

    public final boolean isQualifiedNameNode() {
        return this instanceof QualifiedNameNode;
    }

//    public final  String getFullyQualifiedName() {
//        if(this.isSimpleNameNode()) {
//            return ((SimpleNameNode) this).getIdentifier();
//        } else {
//            //...?
//        }
//    }
}
