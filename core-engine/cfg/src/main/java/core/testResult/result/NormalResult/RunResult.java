package core.testResult.result.NormalResult;

import java.util.ArrayList;
import java.util.List;

public class RunResult {
//    private String coverage = "0%";
    private List<String> coveredStatements = new ArrayList<>();
    private boolean isMatchExpectedOutput = false;

    public RunResult() {
    }

//    public void setCoverage(String coverage) {
//        this.coverage = coverage;
//    }

    public void setMatchExpectedOutput(boolean matchExpectedOutput) {
        isMatchExpectedOutput = matchExpectedOutput;
    }

    public void addCoveredStatement(String coveredStatement) {
        coveredStatements.add(coveredStatement);
    }

    public void setCoveredStatements(List<String> coveredStatements) {
        this.coveredStatements = coveredStatements;
    }

    public List<String> getCoveredStatements() {
        return coveredStatements;
    }

    public boolean isMatchExpectedOutput() {
        return isMatchExpectedOutput;
    }
}
