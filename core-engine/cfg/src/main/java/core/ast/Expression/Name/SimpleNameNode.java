package core.ast.Expression.Name;

import core.ast.Expression.ExpressionNode;
import core.ast.Type.AnnotatableType.SimpleTypeNode;
import core.symbolicExecution.MemoryModel;
import core.variable.Variable;
import org.eclipse.jdt.core.dom.SimpleName;

public class SimpleNameNode extends NameNode {
    private String identifier = "MISSING";
    private SimpleTypeNode target = null;


    public SimpleNameNode() {
    }

    public void setTarget(SimpleTypeNode target) {
        this.target = target;
    }

    public SimpleTypeNode getTarget() {
        return target;
    }

    public SimpleNameNode(String identifier) {
        this.identifier = identifier;
    }

    public boolean isReference() {
        return (target != null);
    }

    public static ExpressionNode executeSimpleName(SimpleName simpleName, MemoryModel memoryModel) {
        SimpleNameNode simpleNameNode = new SimpleNameNode();
        simpleNameNode.identifier = simpleName.getIdentifier();
        Variable var = memoryModel.getVariable(simpleName.getIdentifier());
        // nếu đã lưu biểu thức expr trong cache thì không thực thi tượng trưng nữa
        if( var.getCacheExpr() != null) {
            simpleNameNode.setCacheExpr(var.getCacheExpr());
            return simpleNameNode;
        }
        return NameNode.executeNameNode(simpleNameNode, memoryModel);
        //return simpleNameNode;
    }

    public static ExpressionNode executeSimpleNameNode(SimpleNameNode simpleNameNode, MemoryModel memoryModel) {
        if (simpleNameNode.isFake() || simpleNameNode.getCacheExpr() != null) return simpleNameNode;

        return (ExpressionNode) memoryModel.getValue(simpleNameNode);
    }

    public static ExpressionNode executeSimpleName(SimpleName simpleName) {
        SimpleNameNode simpleNameNode = new SimpleNameNode();
        simpleNameNode.identifier = simpleName.getIdentifier();
        return simpleNameNode;
    }

    public static String getStringSimpleName(SimpleName simpleName) {
        return simpleName.getIdentifier();
    }

    public static String getStringSimpleNameNode(SimpleNameNode simpleNameNode) {
        return simpleNameNode.identifier;
    }
    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return identifier;
    }
}
