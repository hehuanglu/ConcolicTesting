package core.testGeneration.NTDTestGeneration;

import core.FilePath;
import core.path.MarkedStatement;
import core.testGeneration.TestGeneration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class NTDTestGeneration extends TestGeneration {
    protected static String simpleClassName;
    protected static String fullyClonedClassName;
    protected static Method method;

    protected static List<MarkedStatement> getMarkedStatement() {
        List<MarkedStatement> result = new ArrayList<>();

        String markedData = getDataFromFile(FilePath.concreteExecuteResultPath);
        String[] markedStatements = markedData.split("---end---");
        for (int i = 0; i < markedStatements.length; i++) {
            String[] markedStatementData = markedStatements[i].split("===");
            String statement = markedStatementData[0];
            boolean isTrueConditionalStatement = Boolean.parseBoolean(markedStatementData[1]);
            boolean isFalseConditionalStatement = Boolean.parseBoolean(markedStatementData[2]);
            result.add(new MarkedStatement(statement, isTrueConditionalStatement, isFalseConditionalStatement, 0));
        }
        return result;
    }
}
