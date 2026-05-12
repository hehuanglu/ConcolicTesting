package core.testGeneration.NTDTestGeneration.NTDPairwiseTesting;

import core.cfg.CfgNode;
import core.path.FindAllPath;
import core.path.Path;
import core.symbolicExecution.SymbolicExecution;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.*;

public class GetParameterList {
    public static List<Parameter> getFromSymbolicExecuteAllPaths(CfgNode cfgBeginNode, List<ASTNode> parameters, Class<?>[] parameterClasses, List<String> parameterNames) {
        List<Parameter> result = new ArrayList<>();
        for (String parameterName : parameterNames) {
            result.add(new Parameter(parameterName));
        }

        List<Path> paths = (new FindAllPath(cfgBeginNode)).getPaths();
        System.out.println("path size: " + paths.size());
        for (Path path : paths) {
            SymbolicExecution solution = new SymbolicExecution(path, parameters);
            solution.execute();

            if (solution.getModel() == null) continue;

            Object[] evaluatedValues = solution.getEvaluatedTestData(parameterClasses);
            for (int i = 0; i < result.size(); i++) {
                result.get(i).addDistinctValue(evaluatedValues[i]);
            }
        }

        return result;
    }

    public static List<Parameter> getFromAnalyzeSimpleConditions(CfgNode cfgBeginNode, Class<?>[] parameterClasses, List<String> parameterNames) {
        List<Parameter> result = new ArrayList<>();

        FindAllSimpleConditions findAllSimpleConditions = new FindAllSimpleConditions(cfgBeginNode, parameterNames, parameterClasses);
        Map<String, Set<Object>> simpleConditions = findAllSimpleConditions.getExtractedValues();

        for (int i = 0; i < parameterNames.size(); i++) {
            String name = parameterNames.get(i);
            Set<Object> valueSet = simpleConditions.get(name);

            Parameter parameter = new Parameter(name);
            String className = parameterClasses[i].getName();

            for (Object value : valueSet) {
                addBoundaryValuesToParameter(parameter, className, value);
            }

            if (valueSet.size() >= 2) {
                List<Object> valueList = new ArrayList<>(valueSet);
                for (int j = 0; j < valueList.size() - 1; j++) {
                    addNormValueToParameter(parameter, className, valueList.get(j), valueList.get(j + 1));
                }
            }

            result.add(parameter);
        }

        return result;
    }

    private static void addBoundaryValuesToParameter(Parameter parameter, String className, Object originalValue) {
        if ("int".equals(className)) {
            int value = (int) originalValue;
            parameter.addDistinctValue(value);
            parameter.addDistinctValue(value + 1);
            parameter.addDistinctValue(value - 1);
        } else if ("boolean".equals(className)) {
            parameter.addDistinctValue(true);
            parameter.addDistinctValue(false);
        } else if ("byte".equals(className)) {
            byte value = (byte) originalValue;
            parameter.addDistinctValue(value);
            parameter.addDistinctValue(value + 1);
            parameter.addDistinctValue(value - 1);
        } else if ("short".equals(className)) {
            short value = (short) originalValue;
            parameter.addDistinctValue(value);
            parameter.addDistinctValue(value + 1);
            parameter.addDistinctValue(value - 1);
        } else if ("char".equals(className)) {
            char value = (char) originalValue;
            parameter.addDistinctValue(value);
            parameter.addDistinctValue((char) (value + 1));
            parameter.addDistinctValue((char) (value - 1));
        } else if ("long".equals(className)) {
            long value = (long) originalValue;
            parameter.addDistinctValue(value);
            parameter.addDistinctValue(value + 1);
            parameter.addDistinctValue(value - 1);
        } else if ("float".equals(className)) {
            float value = (float) originalValue;
            parameter.addDistinctValue(value);
            parameter.addDistinctValue(value + 1);
            parameter.addDistinctValue(value - 1);
        } else if ("double".equals(className)) {
            double value = (double) originalValue;
            parameter.addDistinctValue(value);
            parameter.addDistinctValue(value + 1);
            parameter.addDistinctValue(value - 1);
        } else if ("void".equals(className)) {
            //??????????//
        } else {
            throw new RuntimeException("Unsupported type: " + className);
        }
    }

    private static void addNormValueToParameter(Parameter parameter, String className, Object left, Object right) {
        if ("int".equals(className)) {
            int value1 = (int) left;
            int value2 = (int) right;
            parameter.addDistinctValue((value1 + value2) / 2);
        } else if ("boolean".equals(className)) {
            // do nothing
        } else if ("byte".equals(className)) {
            byte value1 = (byte) left;
            byte value2 = (byte) right;
            parameter.addDistinctValue((value1 + value2) / 2);
        } else if ("short".equals(className)) {
            short value1 = (short) left;
            short value2 = (short) right;
            parameter.addDistinctValue((value1 + value2) / 2);
        } else if ("char".equals(className)) {
            char value1 = (char) left;
            char value2 = (char) right;
            parameter.addDistinctValue((char) ((value1 + value2) / 2));
        } else if ("long".equals(className)) {
            long value1 = (long) left;
            long value2 = (long) right;
            parameter.addDistinctValue((value1 + value2) / 2);
        } else if ("float".equals(className)) {
            float value1 = (float) left;
            float value2 = (float) right;
            parameter.addDistinctValue((value1 + value2) / 2);
        } else if ("double".equals(className)) {
            double value1 = (double) left;
            double value2 = (double) right;
            parameter.addDistinctValue((value1 + value2) / 2);
        } else if ("void".equals(className)) {
            //??????????//
        } else {
            throw new RuntimeException("Unsupported type: " + className);
        }
    }
}
