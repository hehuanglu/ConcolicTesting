package core.testResult.result.autoTestResult;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TestResult {
    private int id = 0;
    private List<TestData> fullTestData = new ArrayList<>();
    private double fullCoverage = 0;

    // Ha Hoang Loc

    private double executionTime = 0;
    private double memoryUsage = 0;
    private String methodName;
    private static final DecimalFormat df = new DecimalFormat("#.##");

    public double getExecutionTime() {
        return Double.parseDouble(df.format(executionTime));
    }

    public void setExecutionTime(double executionTime) {
        this.executionTime = Double.parseDouble(df.format(executionTime));
    }

    public double getMemoryUsage() {
        return Double.parseDouble(df.format(memoryUsage));
    }

    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = Double.parseDouble(df.format(memoryUsage));
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    // Ha Hoang Loc

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addToFullTestData(TestData testData) {
        fullTestData.add(testData);
    }

    public List<TestData> getFullTestData() {
        return fullTestData;
    }

    public double getFullCoverage() {
        return fullCoverage;
    }

    public void setFullCoverage(double fullCoverage) {
        this.fullCoverage = (double) Math.round(fullCoverage * 100) / 100;
    }

    public List<List<Object>> getFullTestDataSet() {
        List<List<Object>> result = new ArrayList<>();
        for (TestData testData : fullTestData) {
            result.add(testData.getTestDataSet());
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
//        for (int i = 0; i < fullTestData.size(); i++) {
//            result.append("Test no " + i + ": " + fullTestData.get(i));
//        }
        result.append(fullTestData);
        return result.toString();
    }
}
