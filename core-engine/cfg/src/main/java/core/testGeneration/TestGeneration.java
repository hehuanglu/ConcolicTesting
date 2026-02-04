package core.testGeneration;

import core.cfg.CfgEndBlockNode;
import core.cfg.CfgNode;
import core.cfg.dataFlow.DefUsePair;
import core.cfg.utils.ASTHelper;
import core.path.MarkedPath;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class TestGeneration {
    public enum Coverage {
        STATEMENT,
        BRANCH,
        MCDC,
        PATH
    }

    protected static boolean isSetup = false;

    protected static CompilationUnit compilationUnit;
    protected static ArrayList<ASTNode> funcAstNodeList;
    protected static CfgNode cfgBeginNode;
    public static CfgEndBlockNode cfgEndNode;
    protected static List<ASTNode> parameters;
    protected static Class<?>[] parameterClasses;
    protected static List<String> parameterNames;
    protected static ASTNode testFunc;
    protected static Set<DefUsePair> targetDUAs;

    protected static CfgNode findUncoverNode(CfgNode cfgNode, Coverage coverage) {
        switch (coverage) {
            case STATEMENT:
                return MarkedPath.findUncoveredStatement(cfgNode);
            case BRANCH:
            case MCDC:
                return MarkedPath.findUncoveredBranch(cfgNode);
            default:
                throw new RuntimeException("Invalid coverage type");
        }
    }

    protected static ASTHelper.Coverage getCoverageType(Coverage coverage) {
        switch (coverage) {
            case STATEMENT:
                return ASTHelper.Coverage.STATEMENT;
            case BRANCH:
                return ASTHelper.Coverage.BRANCH;
            case MCDC:
                return ASTHelper.Coverage.MCDC;
            default:
                throw new RuntimeException("Invalid coverage");
        }
    }

    protected static void writeDataToFile(String data, String path, boolean append) {
        try {
            FileWriter writer = new FileWriter(path, append);
            writer.write(data);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static String getDataFromFile(String path) {
        StringBuilder result = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            if ((line = br.readLine()) != null) {
                result.append(line);
            }
            while ((line = br.readLine()) != null) {
                result.append("\n").append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static ArrayList<ASTNode> getFuncAstNodeList() {
//        if (isSetup) {
        return funcAstNodeList;
//        } else {
//            throw new RuntimeException("Value has not been setup");
//        }
    }

    public static CompilationUnit getCompilationUnit() {
        if (isSetup) {
            return compilationUnit;
        } else {
            throw new RuntimeException("Value has not been setup");
        }
    }

    public static MethodDeclaration getTestFunc() {
//        if (isSetup) {
        return (MethodDeclaration) testFunc;
//        } else {
//            throw new RuntimeException("Value has not been setup");
//        }
    }
}
