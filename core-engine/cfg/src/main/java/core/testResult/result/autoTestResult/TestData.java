package core.testResult.result.autoTestResult;

import core.testResult.coveredStatement.CoveredStatement;

import java.util.ArrayList;
import java.util.List;

public class TestData {
    private List<CoveredStatement> coveredStatements = new ArrayList<>();
    private List<ParameterData> parameterDataList = new ArrayList<>();
    private Object output;
    private double executeTime;
    private double sourceCodeCoverage;
    private double functionCoverage;
    private double requiredCoverage;
    private String status;

    public TestData(List<String> names, Class<?>[] types, Object[] values, List<CoveredStatement> coveredStatements,
                    Object output, double executeTime, double requiredCoverage, double functionCoverage, double sourceCodeCoverage) {
        if(names.size() != types.length || types.length != values.length) {
            throw new RuntimeException("Invalid");
        }

        for(int i = 0; i < names.size(); i++) {
            this.addToParameterDataList(new ParameterData(names.get(i), types[i].toString(), values[i]));
        }

        this.coveredStatements = coveredStatements;
        this.output = output;
        this.executeTime = round(executeTime);
        this.sourceCodeCoverage = round(sourceCodeCoverage);
        this.functionCoverage = round(functionCoverage);
        this.requiredCoverage = round(requiredCoverage);
        this.status = "PASS";
    }

    private double round(double number) {
        return (double) Math.round(number * 100) / 100;
    }

    public void addToCoveredStatements(CoveredStatement statement) {
        coveredStatements.add(statement);
    }

    public void addToParameterDataList(ParameterData parameterData) {
        parameterDataList.add(parameterData);
    }

    public List<CoveredStatement> getCoveredStatements() {
        return coveredStatements;
    }

    public List<ParameterData> getParameterDataList() {
        return parameterDataList;
    }

    public Object getOutput() {
        return output;
    }

    public double getExecuteTime() {
        return executeTime;
    }

    public double getSourceCodeCoverage() {
        return sourceCodeCoverage;
    }

    public double getFunctionCoverage() {
        return functionCoverage;
    }

    public double getRequiredCoverage() {
        return requiredCoverage;
    }

    public String getStatus() {
        return status;
    }

    public void setCoveredStatements(List<CoveredStatement> coveredStatements) {
        this.coveredStatements = coveredStatements;
    }

    public List<Object> getTestDataSet() {
        List<Object> result = new ArrayList<>();
        for (ParameterData parameterData : parameterDataList) {
            result.add(parameterData.getValue());
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

//        result.append(coveredStatements);

//        for (int i = 0; i < parameterDataList.size(); i++) {
//            result.append("Parameter no " + i + ": " + parameterDataList.get(i));
//        }

        result.append(parameterDataList);

        return result.toString();
    }
}
