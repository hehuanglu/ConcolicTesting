package core.testGeneration.NTDTestGeneration.NTDPairwiseTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValueDictionary {
    private final int numberOfParameters;
    private List<List<Object>> testDataSet;

    public ValueDictionary(int numberOfParameters) {
        this.numberOfParameters = numberOfParameters;
        testDataSet = new ArrayList<>();
    }

    public void addToTestDataSet(Object... values) {
        List<Object> testData = new ArrayList<>(Arrays.asList(values));
        testDataSet.add(testData);
    }

    public void addToTestDataSet(List<Object> testData) {
        testDataSet.add(testData);
    }

    public void addDistinctToTestDataSet(List<List<Object>> newTestDataSet) {
        for (List<Object> newTestData : newTestDataSet) {
            if (!this.containsTestData(newTestData)) {
                testDataSet.add(newTestData);
            }
        }
    }

    private boolean containsTestData(List<Object> newTestData) {
        for (List<Object> testData : testDataSet) {
            if (isTwoTestDataEquals(testData, newTestData)) return true;
        }
        return false;
    }

    private boolean isTwoTestDataEquals(List<Object> testData1, List<Object> testData2) {
        if (testData1.size() == testData2.size()) {
            for (int i = 0; i < testData1.size(); i++) {
                if (!testData1.get(i).equals(testData2.get(i))) return false;
            }
            return true;
        }
        return false;
    }

    public int getTestDataSetSize() {
        return testDataSet.size();
    }

    public List<List<Object>> getTestDataSet() {
        return testDataSet;
    }

    public int getNumberOfParameters() {
        return numberOfParameters;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (List<Object> testData : testDataSet) {
            result.append(testData).append("\n");
        }
        return result.toString();
    }
}
