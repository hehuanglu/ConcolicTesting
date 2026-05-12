package core.cfg;

import org.eclipse.jdt.core.dom.ASTNode;

public class CfgForEachExpressionNode extends CfgNode {
    public CfgForEachExpressionNode() {
    }

    private CfgNode hasElementAfterNode = null;
    private CfgNode noMoreElementAfterNode = null;
    private CfgNode parameterNode = null;

    public CfgNode getHasElementAfterNode() {
        return hasElementAfterNode;
    }

    public void setHasElementAfterNode(CfgNode hasElementAfterNode) {
        this.hasElementAfterNode = hasElementAfterNode;
    }

    public CfgNode getNoMoreElementAfterNode() {
        return noMoreElementAfterNode;
    }

    public void setNoMoreElementAfterNode(CfgNode noMoreElementAfterNode) {
        this.noMoreElementAfterNode = noMoreElementAfterNode;
    }

    public CfgNode getParameterNode() {
        return parameterNode;
    }

    public void setParameterNode(CfgNode parameterNode) {
        this.parameterNode = parameterNode;
    }

    private boolean isTrueMarked = false;
    private boolean isFalseMarked = false;

    private boolean isFakeTrueMarked = false;
    private boolean isFakeFalseMarked = false;

    public boolean isTrueMarked() {
        return isTrueMarked;
    }

    public void setTrueMarked(boolean trueMarked) {
        isTrueMarked = trueMarked;
    }

    public boolean isFalseMarked() {
        return isFalseMarked;
    }

    public void setFalseMarked(boolean falseMarked) {
        isFalseMarked = falseMarked;
    }

    public boolean isFakeTrueMarked() {
        return isFakeTrueMarked;
    }

    public void setFakeTrueMarked(boolean fakeTrueMarked) {
        isFakeTrueMarked = fakeTrueMarked;
    }

    public boolean isFakeFalseMarked() {
        return isFakeFalseMarked;
    }

    public void setFakeFalseMarked(boolean fakeFalseMarked) {
        isFakeFalseMarked = fakeFalseMarked;
    }
}
