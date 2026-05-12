package core.testGeneration.ConcolicTestGeneration;

import core.testGeneration.TestGeneration;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public abstract class ConcolicTestGeneration extends TestGeneration {
    protected static String classKey;
    protected static String getTotalFunctionCoverageVariableName(MethodDeclaration methodDeclaration, TestGeneration.Coverage coverage) {
        StringBuilder result = new StringBuilder(classKey);
        result.append(methodDeclaration.getReturnType2());
        result.append(methodDeclaration.getName());
        for (int i = 0; i < methodDeclaration.parameters().size(); i++) {
            result.append(methodDeclaration.parameters().get(i));
        }
        if (coverage == TestGeneration.Coverage.STATEMENT) {
            result.append("TotalStatement");
        } else if (coverage == TestGeneration.Coverage.BRANCH) {
            result.append("TotalBranch");
        } else {
            throw new RuntimeException("Invalid Coverage");
        }

        return reformatVariableName(result.toString());
    }

    private static String reformatVariableName(String name) {
        return name.replace(" ", "").replace(".", "")
                .replace("[", "").replace("]", "")
                .replace("<", "").replace(">", "")
                .replace(",", "");
    }
}
