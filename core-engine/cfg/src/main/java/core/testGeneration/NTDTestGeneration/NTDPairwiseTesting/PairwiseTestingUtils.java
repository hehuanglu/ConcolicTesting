package core.testGeneration.NTDTestGeneration.NTDPairwiseTesting;

import core.testResult.result.autoTestResult.ParameterData;
import core.testResult.result.autoTestResult.TestData;
import core.testResult.result.autoTestResult.TestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PairwiseTestingUtils {

    private static final String UNSPECIFIED_VALUE = "_";

    public static List<Parameter> convertTestResultToParameterValues(TestResult testResult) {
        List<Parameter> result = new ArrayList<>();

        List<TestData> testDataList = testResult.getFullTestData();
        for (TestData testData : testDataList) {
            List<ParameterData> parameterDataList = testData.getParameterDataList();
            for (ParameterData parameterData : parameterDataList) {
                if (!findParameterAndAddValue(result, parameterData)) {
                    Parameter newPara = new Parameter(parameterData.getName());
                    newPara.addValue(parameterData.getValue());
                    result.add(newPara);
                }
            }
        }

        return result;
    }

    private static boolean findParameterAndAddValue(List<Parameter> parameters, ParameterData parameterData) {
        String name = parameterData.getName();
        Object parameterDataValue = parameterData.getValue();
        for (Parameter parameter : parameters) {
            if (name.equals(parameter.getName())) {
                parameter.addDistinctValue(parameterDataValue);
                return true;
            }
        }
        return false;
    }

    public static void generateFirstTwoPairWiseTestData(ValueDictionary valueDictionary, List<Parameter> parameterList) {
        List<Object> parameter1Values = parameterList.get(0).getValues();
        List<Object> parameter2Values = parameterList.get(1).getValues();

        for (Object p1Value : parameter1Values) {
            for (Object p2Value : parameter2Values) {
                valueDictionary.addToTestDataSet(p1Value, p2Value);
            }
        }
    }

    public static List<Parameter> merge2ParameterList(List<Parameter> parameterList1, List<Parameter> parameterList2) {
        if (parameterList1.size() != parameterList2.size()) {
            throw new RuntimeException("Invalid parameter lists");
        }

        List<Parameter> result = new ArrayList<>(parameterList1);

        for (int i = 0; i < result.size(); i++) {
            result.get(i).addDistinctValues(parameterList2.get(i).getValues());
        }

        return result;
    }

    public static void IPO_H(ValueDictionary valueDictionary, int i, List<Parameter> parameterList, List<Pair> pairs) {
        Parameter visitingParameter = parameterList.get(i);
        List<Object> visitingParameterValues = visitingParameter.getValues();

        List<List<Object>> testDataSet = valueDictionary.getTestDataSet();

        if (valueDictionary.getTestDataSetSize() <= visitingParameterValues.size()) {
            for (int j = 0; j < valueDictionary.getTestDataSetSize(); j++) {
                Object newValue = visitingParameterValues.get(j);

                List<Object> testData = testDataSet.get(j);
//                testData.add(newValue);
                addNewValueToTestData(testData, newValue, pairs, parameterList);

                // remove from π the pairs covered by the updated testdata
                for (int k = 0; k < testData.size() - 1; k++) {
                    Pair coveredPair = new Pair(testData.get(k), k, newValue, i);
                    removeCoveredPairs(coveredPair, pairs);
                }
            }
        } else {
            for (int j = 0; j < visitingParameterValues.size(); j++) {
                Object newValue = visitingParameterValues.get(j);

                List<Object> testData = testDataSet.get(j);
//                testData.add(newValue);
                addNewValueToTestData(testData, newValue, pairs, parameterList);

                // remove from π the pairs covered by the updated testdata
                for (int k = 0; k < testData.size() - 1; k++) {
                    Pair coveredPair = new Pair(testData.get(k), k, newValue, i);
                    removeCoveredPairs(coveredPair, pairs);
                }
            }
            for (int j = visitingParameterValues.size(); j < valueDictionary.getTestDataSetSize(); j++) {
                List<Object> testData = testDataSet.get(j);

                // find value cover the most pair
                int maxPairsCovered = 0;
                Object valueCoveredMostPairs = getRandomValue(visitingParameter);
                for (Object visitingParameterValue : visitingParameterValues) {
                    int numberPairsCovered = 0;
                    for (int k = 0; k < testData.size() - 1; k++) {
                        Pair coveredPair = new Pair(testData.get(k), k, visitingParameterValue, i);
                        if (containsInPairs(coveredPair, pairs)) numberPairsCovered++;
                    }
                    if (numberPairsCovered > maxPairsCovered) {
                        maxPairsCovered = numberPairsCovered;
                        valueCoveredMostPairs = visitingParameterValue;
                    }
                }

//                testData.add(valueCoveredMostPairs);
                addNewValueToTestData(testData, valueCoveredMostPairs, pairs, parameterList);

                // remove from π the pairs covered by the updated testdata
                for (int k = 0; k < testData.size() - 1; k++) {
                    Pair coveredPair = new Pair(testData.get(k), k, valueCoveredMostPairs, i);
                    removeCoveredPairs(coveredPair, pairs);
                }
            }
        }
    }

    public static List<Pair> createPairsBetween2Paras(List<Object> visitingParameterValues, int i, List<Parameter> parameterList) {
        List<Pair> pairs = new ArrayList<>();

        for (Object visitingParameterValue : visitingParameterValues) {
            for (int j = 0; j < i; j++) {
                List<Object> parameterValues = parameterList.get(j).getValues();
                for (Object parameterValue : parameterValues) {
                    Pair pair = new Pair(parameterValue, j, visitingParameterValue, i);  // thứ tự từ bé đến lớn
                    pairs.add(pair);
                }
            }
        }
        return pairs;
    }

    private static void removeCoveredPairs(Pair coveredPair, List<Pair> pairs) {
        for (Pair pair : pairs) {
            if (coveredPair.equals(pair)) {
                pairs.remove(pair);
                break;
            }
        }
    }

    private static boolean containsInPairs(Pair examinePair, List<Pair> pairs) {
        for (Pair pair : pairs) {
            if (examinePair.equals(pair)) {
                return true;
            }
        }
        return false;
    }

    private static void addNewValueToTestData(List<Object> testData, Object newValue, List<Pair> pairs, List<Parameter> parameterList) {
        testData.add(newValue);
        for (int j = 0; j < testData.size() - 1; j++) {
            if (testData.get(j).equals(UNSPECIFIED_VALUE)) {
                Pair tmpPair = new Pair(UNSPECIFIED_VALUE, j, newValue, testData.size() - 1);
                boolean containsUncoveredPair = false;
                for (Pair pair : pairs) {
                    if (tmpPair.equalsPlace1AndPlace2AndValue2(pair)) {
                        testData.set(j, pair.getValue1());
                        pairs.remove(pair);
                        containsUncoveredPair = true;
                        break;
                    }
                }
                if (!containsUncoveredPair) {
                    testData.set(j, getRandomValue(parameterList.get(j)));
                }
            }
        }
    }

    public static Object getRandomValue(Parameter parameter) {
        List<Object> values = parameter.getValues();
        Random random = new Random();
        int left = 0;
        int right = values.size() - 1;
        int range = right - left + 1;
        int randomIndex = random.nextInt(range);
        return values.get(randomIndex);
    }

    public static void IPO_V(ValueDictionary valueDictionary, int i, List<Parameter> parameterList, List<Pair> pairs) {
        List<List<Object>> newTestDataSet = new ArrayList<>();

        for (Pair pair : pairs) {
            List<Object> modifiableTestData = getModifiableTestData(pair, newTestDataSet);
            if (modifiableTestData != null) {
                modifiableTestData.set(pair.getPlaceOfParameter1(), pair.getValue1());
            } else {
                List<Object> newTestData = new ArrayList<>();
                for (int j = 0; j <= i; j++) {
                    newTestData.add(null);
                }
                for (int j = 0; j <= i; j++) {
                    if (j == pair.getPlaceOfParameter1()) {
                        newTestData.set(j, pair.getValue1());
                    } else if (j == pair.getPlaceOfParameter2()) {
                        newTestData.set(j, pair.getValue2());
                    } else {
                        newTestData.set(j, UNSPECIFIED_VALUE);
                    }
                }
                newTestDataSet.add(newTestData);
                valueDictionary.addToTestDataSet(newTestData);
            }
        }

        postIPO_V(valueDictionary, newTestDataSet, i, parameterList, pairs);
    }

    private static List<Object> getModifiableTestData(Pair pair, List<List<Object>> newTestDataSet) {
        for (List<Object> testData : newTestDataSet) {
            if (isModifiableTestData(pair, testData)) return testData;
        }
        return null;
    }

    private static boolean isModifiableTestData(Pair pair, List<Object> testData) {
        return testData.get(pair.getPlaceOfParameter1()).equals(UNSPECIFIED_VALUE)
                && testData.get(pair.getPlaceOfParameter2()).equals(pair.getValue2());
    }

    private static void postIPO_V(ValueDictionary valueDictionary, List<List<Object>> newTestDataSet, int i, List<Parameter> parameterList, List<Pair> pairs) {
        if (i == valueDictionary.getNumberOfParameters() - 1) {
            for (List<Object> testData : newTestDataSet) {
                for (int j = 0; j < testData.size(); j++) {
                    if (testData.get(j).equals(UNSPECIFIED_VALUE)) {
                        testData.set(j, getRandomValue(parameterList.get(j))); // random
                    }
                }
            }
        }
    }


}
