package core.path;

import com.fasterxml.jackson.annotation.JsonIgnore;
import core.cfg.CfgNode;

public class MarkedStatement {
    private String statement;
    private boolean isTrueConditionalStatement;
    private boolean isFalseConditionalStatement;
    private int lineNumber;
    @JsonIgnore
    private CfgNode cfgNode;

    public MarkedStatement(String statement, boolean isTrueConditionalStatement, boolean isFalseConditionalStatement,
                           int lineNumber) {
        this.statement = statement;
        this.isTrueConditionalStatement = isTrueConditionalStatement;
        this.isFalseConditionalStatement = isFalseConditionalStatement;
        this.lineNumber = lineNumber;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public void setTrueConditionalStatement(boolean trueConditionalStatement) {
        isTrueConditionalStatement = trueConditionalStatement;
    }

    public void setFalseConditionalStatement(boolean falseConditionalStatement) {
        isFalseConditionalStatement = falseConditionalStatement;
    }

    public String getStatement() {
        return statement;
    }

    public boolean isTrueConditionalStatement() {
        return isTrueConditionalStatement;
    }

    public boolean isFalseConditionalStatement() {
        return isFalseConditionalStatement;
    }

    @JsonIgnore
    public CfgNode getCfgNode() {
        return cfgNode;
    }

    public void setCfgNode(CfgNode cfgNode) {
        this.cfgNode = cfgNode;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarkedStatement that = (MarkedStatement) obj;
        boolean isBothTrueConditionalStatement = this.isTrueConditionalStatement == that.isTrueConditionalStatement;
        boolean isBothFalseConditionalStatement = this.isFalseConditionalStatement == that.isFalseConditionalStatement;
        boolean isSameId = this.lineNumber == that.lineNumber;
        boolean isSameStatement = this.statement.equals(that.statement);
        return isSameId && isBothTrueConditionalStatement && isBothFalseConditionalStatement && isSameStatement;
    }
}
