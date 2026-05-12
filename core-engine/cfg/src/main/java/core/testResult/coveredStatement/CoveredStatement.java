package core.testResult.coveredStatement;

import core.cfg.CfgNode;
import core.path.MarkedStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CoveredStatement {
    private String statementContent = "";
    private int lineNumber = 0;

    private String conditionStatus = "";

    public CoveredStatement(String statementContent, int lineNumber) {
        this.statementContent = statementContent;
        this.lineNumber = lineNumber;
    }

    public CoveredStatement(String statementContent, int lineNumber, String conditionStatus) {
        this.statementContent = statementContent;
        this.lineNumber = lineNumber;
        this.conditionStatus = conditionStatus;
    }

    public static List<CoveredStatement> switchToCoveredStatementList(List<MarkedStatement> markedStatements) {
        List<CoveredStatement> coveredStatements = new ArrayList<>();

        for (MarkedStatement markedStatement : markedStatements) {
            CfgNode cfgNode = markedStatement.getCfgNode();
            CoveredStatement coveredStatement = new CoveredStatement(cfgNode.getContent(), cfgNode.getLineNumber());

            if (markedStatement.isTrueConditionalStatement()) {
                coveredStatement.conditionStatus = "true";
            } else if (markedStatement.isFalseConditionalStatement()) {
                coveredStatement.conditionStatus = "false";
            }

            coveredStatements.add(coveredStatement);
        }

        return coveredStatements;
    }

    public String getStatementContent() {
        return statementContent;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getConditionStatus() {
        return conditionStatus;
    }

    @Override
    public String toString() {
        return lineNumber + " " + statementContent + " " + conditionStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(statementContent, lineNumber, conditionStatus);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CoveredStatement) {
            CoveredStatement coverStatement = (CoveredStatement) o;
            return this.statementContent.equals(coverStatement.statementContent)
                    && this.lineNumber == coverStatement.lineNumber
                    && this.conditionStatus.equals(coverStatement.conditionStatus);
        } else {
            return false;
        }
    }
}