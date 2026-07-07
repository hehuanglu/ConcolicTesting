package core.ast.Expression.Method;

import core.ast.AstNode;

import java.util.List;

public class StringBuilderMethodNode extends MethodInvocationNode {

    private AstNode target;
    private String methodName;
    private List<AstNode> arguments;

    public AstNode getTarget() {
        return target;
    }

    public void setTarget(AstNode target) {
        this.target = target;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<AstNode> getArguments() {
        return arguments;
    }

    public void setArguments(List<AstNode> arguments) {
        this.arguments = arguments;
    }
}