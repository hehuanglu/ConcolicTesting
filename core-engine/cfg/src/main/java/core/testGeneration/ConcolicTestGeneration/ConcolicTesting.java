package core.testGeneration.ConcolicTestGeneration;

import core.testDriver.TestDriverUtils;
import core.testGeneration.TestGeneration;
import core.testResult.coveredStatement.CoveredStatement;
import core.testResult.result.autoTestResult.TestData;
import core.testResult.result.autoTestResult.TestResult;
import core.FilePath;
import core.path.FindPath;
import core.symbolicExecution.SymbolicExecution;
import core.cfg.CfgBlockNode;
import core.cfg.CfgEndBlockNode;
import core.cfg.CfgNode;
import core.path.MarkedPath;
import core.path.MarkedStatement;
import core.path.Path;
import core.cfg.utils.ASTHelper;
import core.cfg.utils.ProjectParser;
import core.testDriver.TestDriverGenerator;
import core.testDriver.TestDriverRunner;
import core.uploadProjectUtils.ConcolicUploadUtil;
import core.utils.Utils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ConcolicTesting extends ConcolicTestGeneration {

    private ConcolicTesting() {
    }

    public static TestResult runFullConcolic(int id, String path, String methodName, String className, TestGeneration.Coverage coverage) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException, InterruptedException {
        setup(path, className, methodName);
        setupCfgTree(coverage);
        setupParameters();
        TestGeneration.isSetup = true;

        TestResult result = startGenerating(id, coverage);

        TestGeneration.isSetup = false;

        return result;
    }

    private static TestResult startGenerating(int id, TestGeneration.Coverage coverage) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException, IOException, InterruptedException {
        TestResult testResult = new TestResult();
        testResult.setId(id);

        Object[] evaluatedValues = SymbolicExecution.createRandomTestData(TestGeneration.parameterClasses);

        TestGeneration.writeDataToFile("", FilePath.concreteExecuteResultPath, false);

        String testDriver = TestDriverGenerator.generateTestDriver((MethodDeclaration) TestGeneration.testFunc, evaluatedValues, TestGeneration.getCoverageType(coverage));
        List<MarkedStatement> markedStatements = TestDriverRunner.runTestDriver(testDriver);

        MarkedPath.markPathToCFGV2(TestGeneration.cfgBeginNode, markedStatements);

        List<CoveredStatement> coveredStatements = CoveredStatement.switchToCoveredStatementList(markedStatements);

        testResult.addToFullTestData(new TestData(TestGeneration.parameterNames, TestGeneration.parameterClasses, evaluatedValues, coveredStatements,
                TestDriverRunner.getOutput(), TestDriverRunner.getRuntime(), calculateRequiredCoverage(coverage), calculateFunctionCoverage(), calculateSourceCodeCoverage()));

        boolean isTestedSuccessfully = true;

        for (CfgNode uncoveredNode = TestGeneration.findUncoverNode(TestGeneration.cfgBeginNode, coverage); uncoveredNode != null; ) {

            Path newPath = (new FindPath(TestGeneration.cfgBeginNode, uncoveredNode, TestGeneration.cfgEndNode)).getPath();

            SymbolicExecution solution = new SymbolicExecution(newPath, TestGeneration.parameters);
            solution.execute();

            if (solution.getModel() == null) {
                isTestedSuccessfully = false;
                break;
            }

            evaluatedValues = solution.getEvaluatedTestData(TestGeneration.parameterClasses);

            TestGeneration.writeDataToFile("", FilePath.concreteExecuteResultPath, false);

            testDriver = TestDriverGenerator.generateTestDriver((MethodDeclaration) TestGeneration.testFunc, evaluatedValues, TestGeneration.getCoverageType(coverage));
            markedStatements = TestDriverRunner.runTestDriver(testDriver);

            MarkedPath.markPathToCFGV2(TestGeneration.cfgBeginNode, markedStatements);
            coveredStatements = CoveredStatement.switchToCoveredStatementList(markedStatements);

            testResult.addToFullTestData(new TestData(TestGeneration.parameterNames, TestGeneration.parameterClasses, evaluatedValues, coveredStatements, TestDriverRunner.getOutput(), TestDriverRunner.getRuntime(), calculateRequiredCoverage(coverage), calculateFunctionCoverage(), calculateSourceCodeCoverage()));

            uncoveredNode = TestGeneration.findUncoverNode(TestGeneration.cfgBeginNode, coverage);
            System.out.println("Uncovered Node: " + uncoveredNode);
        }

        if (isTestedSuccessfully) System.out.println("Tested successfully with 100% coverage");
        else System.out.println("Test fail due to UNSATISFIABLE constraint");

        testResult.setFullCoverage(calculateFullTestSuiteCoverage(coverage));

        return testResult;
    }

    private static void setup(String path, String className, String methodName) throws IOException {
        TestGeneration.funcAstNodeList = ProjectParser.parseFile(path);
        TestGeneration.compilationUnit = ProjectParser.parseFileToCompilationUnit(path);
        classKey = (TestGeneration.compilationUnit.getPackage() != null ? TestGeneration.compilationUnit.getPackage().getName().toString() : "") + className.replace(".java", "") + "totalStatement";
        setUpTestFunc(methodName);
        MarkedPath.resetFullTestSuiteCoveredStatements();
    }

    private static double calculateFullTestSuiteCoverage(Coverage coverage) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
//        String key = getTotalFunctionCoverageVariableName((MethodDeclaration) testFunc, coverage);
//        if (coverage == Coverage.STATEMENT) {
//            int totalFunctionStatement = ConcolicUploadUtil.totalStatementsInUnits.get(key);
//            int totalCovered = MarkedPath.getFullTestSuiteTotalCoveredStatements();
//            return (totalCovered * 100.0) / totalFunctionStatement;
//        } else { // branch
//            int totalFunctionBranch = ConcolicUploadUtil.totalBranchesInUnits.get(key);
//            int totalCovered = MarkedPath.getFullTestSuiteTotalCoveredBranch();
//            return (totalCovered * 100.0) / totalFunctionBranch;
//        }
        return 0;
    }

    private static double calculateRequiredCoverage(TestGeneration.Coverage coverage) {
//        String key = getTotalFunctionCoverageVariableName((MethodDeclaration) TestGeneration.testFunc, coverage);
//        int totalFunctionCoverage = 1;
//        int totalCovered = 0;
//        if (coverage == TestGeneration.Coverage.STATEMENT) {
//            totalCovered = MarkedPath.getTotalCoveredStatement();
//            totalFunctionCoverage = ConcolicUploadUtil.totalStatementsInUnits.get(key);
//        } else if (coverage == TestGeneration.Coverage.BRANCH) {
//            totalCovered = MarkedPath.getTotalCoveredBranch();
//            Map<String, Integer> hashMap = ConcolicUploadUtil.totalBranchesInUnits;
//            totalFunctionCoverage = hashMap.get(key);
//        }
//        return (totalCovered * 100.0) / totalFunctionCoverage;
        return 0;
    }

    private static double calculateFunctionCoverage() {
//        String key = getTotalFunctionCoverageVariableName((MethodDeclaration) TestGeneration.testFunc, TestGeneration.Coverage.STATEMENT);
//        int totalFunctionStatement = ConcolicUploadUtil.totalStatementsInUnits.get(key);
//        int totalCoveredStatement = MarkedPath.getTotalCoveredStatement();
//        return (totalCoveredStatement * 100.0) / (totalFunctionStatement * 1.0);
        return 0;
    }

    private static double calculateSourceCodeCoverage() {
//        int totalClassStatement = ConcolicUploadUtil.totalStatementsInJavaFile.get(classKey);
//        int totalCoveredStatement = MarkedPath.getTotalCoveredStatement();
//        return (totalCoveredStatement * 100.0) / (totalClassStatement * 1.0);
        return 0;
    }

    private static void setUpTestFunc(String methodName) {
        for (ASTNode func : TestGeneration.funcAstNodeList) {
            if (((MethodDeclaration) func).getName().getIdentifier().equals(methodName)) {
                TestGeneration.testFunc = func;
            }
        }
    }

    private static void setupParameters() {
        TestGeneration.parameters = ((MethodDeclaration) TestGeneration.testFunc).parameters();
        TestGeneration.parameterClasses = TestDriverUtils.getParameterClasses(TestGeneration.parameters);
        TestGeneration.parameterNames = TestDriverUtils.getParameterNames(TestGeneration.parameters);
    }

    private static void setupCfgTree(TestGeneration.Coverage coverage) {
        Block functionBlock = Utils.getFunctionBlock(TestGeneration.testFunc);

        TestGeneration.cfgBeginNode = new CfgNode();
        TestGeneration.cfgBeginNode.setIsBeginCfgNode(true);

        TestGeneration.cfgEndNode = new CfgEndBlockNode();
        TestGeneration.cfgEndNode.setIsEndCfgNode(true);

        CfgNode block = new CfgBlockNode();
        block.setAst(functionBlock);

        int firstLine = TestGeneration.compilationUnit.getLineNumber(functionBlock.getStartPosition());
        block.setLineNumber(1);

        block.setBeforeStatementNode(TestGeneration.cfgBeginNode);
        block.setAfterStatementNode(TestGeneration.cfgEndNode);

        ASTHelper.generateCFG(block, TestGeneration.compilationUnit, firstLine, TestGeneration.getCoverageType(coverage));
    }
}

