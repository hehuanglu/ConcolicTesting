package core.cfg.dataFlow;

import core.cfg.CfgNode;

public class DefUsePair {
    public String variable;
    public CfgNode defNode;
    public CfgNode useNode;

    public boolean isCovered = false;

    public DefUsePair(String variable, CfgNode defNode, CfgNode useNode) {
        this.variable = variable;
        this.defNode = defNode;
        this.useNode = useNode;
    }

    @Override
    public String toString() {
        return "DUA{" + "var='" + variable + '\'' + ", defLine=" + defNode.getLineNumber() +
                ", useLine=" + useNode.getLineNumber() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefUsePair that = (DefUsePair) o;
        if (variable != null ? !variable.equals(that.variable) : that.variable != null) return false;
        if (defNode != null ? !defNode.equals(that.defNode) : that.defNode != null) return false;
        return useNode != null ? useNode.equals(that.useNode) : that.useNode == null;
    }

    @Override
    public int hashCode() {
        int result = variable != null ? variable.hashCode() : 0;
        result = 31 * result + (defNode != null ? defNode.hashCode() : 0);
        result = 31 * result + (useNode != null ? useNode.hashCode() : 0);
        return result;
    }

}
