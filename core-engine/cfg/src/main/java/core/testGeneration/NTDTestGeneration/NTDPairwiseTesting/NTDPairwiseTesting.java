package core.testGeneration.NTDTestGeneration.NTDPairwiseTesting;

import core.testGeneration.NTDTestGeneration.NTDTestGeneration;
import core.path.*;
import core.testDriver.TestDriverUtils;
import core.testGeneration.TestGeneration;
import core.testResult.coveredStatement.CoveredStatement;
import core.testResult.result.autoTestResult.TestData;
import core.testResult.result.autoTestResult.TestResult;
import core.FilePath;
import core.symbolicExecution.SymbolicExecution;
import core.cfg.CfgBlockNode;
import core.cfg.CfgEndBlockNode;
import core.cfg.CfgNode;
import core.cfg.utils.ASTHelper;
import core.cfg.utils.ProjectParser;
import core.utils.Utils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class NTDPairwiseTesting extends NTDTestGeneration {
    private static long totalUsedMem = 0;
    private static long tickCount = 0;

    private NTDPairwiseTesting() {
    }

    public static TestResult runFullPairwise(int id, String path, String methodName, String className, Coverage coverage) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException, InterruptedException {
        setup(path, className, methodName);
        setupCfgTree(coverage);
        setupParameters(methodName);
        isSetup = true;

        totalUsedMem = 0;
        tickCount = 0;
        Timer T = new Timer(true);

        TimerTask memoryTask = new TimerTask() {
            @Override
            public void run() {
                totalUsedMem += (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                tickCount += 1;
            }
        };
        T.scheduleAtFixedRate(memoryTask, 0, 1); //0 delay and 5 ms tick

        ValueDictionary valueDictionary1 = new ValueDictionary(parameterNames.size());
        ValueDictionary valueDictionary2 = new ValueDictionary(parameterNames.size());
        ValueDictionary valueDictionary3 = new ValueDictionary(parameterNames.size());

        long startRunTestTime = System.nanoTime();

        TestResult result = startGenerating(id, coverage);

        isSetup = false;

        long endRunTestTime = System.nanoTime();
        double runTestDuration = (endRunTestTime - startRunTestTime) / 1000000.0;
        float usedMem = ((float) totalUsedMem) / tickCount / 1024 / 1024;
        System.out.println("DS1: " + runTestDuration + " ms, " + usedMem + " MB");
//        System.out.println(result);
        System.out.println(result.getFullTestData().size());

        List<Parameter> parameterList1 = PairwiseTestingUtils.convertTestResultToParameterValues(result);
        generatePairWiseTestdata(valueDictionary1, parameterList1);
        valueDictionary1.addDistinctToTestDataSet(result.getFullTestDataSet());

        endRunTestTime = System.nanoTime();
        runTestDuration = (endRunTestTime - startRunTestTime) / 1000000.0;
        usedMem = ((float) totalUsedMem) / tickCount / 1024 / 1024;
        System.out.println("DS2: " + runTestDuration + " ms, " + usedMem + " MB");
//        System.out.println(valueDictionary1);
        System.out.println(valueDictionary1.getTestDataSetSize());

        List<Parameter> parameterList2 = GetParameterList.getFromAnalyzeSimpleConditions(cfgBeginNode, parameterClasses, parameterNames);
        parameterList2 = PairwiseTestingUtils.merge2ParameterList(parameterList1, parameterList2);
        generatePairWiseTestdata(valueDictionary2, parameterList2);
        valueDictionary2.addDistinctToTestDataSet(result.getFullTestDataSet());

        endRunTestTime = System.nanoTime();
        runTestDuration = (endRunTestTime - startRunTestTime) / 1000000.0;
        usedMem = ((float) totalUsedMem) / tickCount / 1024 / 1024;
        System.out.println("DS3: " + runTestDuration + " ms, " + usedMem + " MB");
//        System.out.println(valueDictionary2);
        System.out.println(valueDictionary2.getTestDataSetSize());

        List<Parameter> parameterList3 = GetParameterList.getFromSymbolicExecuteAllPaths(cfgBeginNode, parameters, parameterClasses, parameterNames);
        parameterList3 = PairwiseTestingUtils.merge2ParameterList(parameterList2, parameterList3);
        generatePairWiseTestdata(valueDictionary3, parameterList3);
        valueDictionary3.addDistinctToTestDataSet(result.getFullTestDataSet());

        endRunTestTime = System.nanoTime();
        runTestDuration = (endRunTestTime - startRunTestTime) / 1000000.0;
        usedMem = ((float) totalUsedMem) / tickCount / 1024 / 1024;
        System.out.println("DS4: " + runTestDuration + " ms, " + usedMem + " MB");
//        System.out.println(valueDictionary3);
        System.out.println(valueDictionary3.getTestDataSetSize());

        return result;
    }

    private static void generatePairWiseTestdata(ValueDictionary valueDictionary, List<Parameter> parameterList) {
        PairwiseTestingUtils.generateFirstTwoPairWiseTestData(valueDictionary, parameterList);

        for (int i = 2; i < parameterList.size(); i++) {
            List<Object> visitingParameterValues = parameterList.get(i).getValues();

            List<Pair> pairs = PairwiseTestingUtils.createPairsBetween2Paras(visitingParameterValues, i, parameterList);

            PairwiseTestingUtils.IPO_H(valueDictionary, i, parameterList, pairs);
            PairwiseTestingUtils.IPO_V(valueDictionary, i, parameterList, pairs);
        }
    }

    private static TestResult startGenerating(int id, Coverage coverage) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        TestResult testResult = new TestResult();
        testResult.setId(id);

        Object[] evaluatedValues = SymbolicExecution.createRandomTestData(parameterClasses);

        writeDataToFile("", FilePath.concreteExecuteResultPath, false);

        long startRunTestTime = System.nanoTime();
        Object output = method.invoke(parameterClasses, evaluatedValues);
        long endRunTestTime = System.nanoTime();
        double runTestDuration = (endRunTestTime - startRunTestTime) / 1000000.0;

        List<MarkedStatement> markedStatements = getMarkedStatement();
        MarkedPath.markPathToCFGV2(cfgBeginNode, markedStatements);

        List<CoveredStatement> coveredStatements = CoveredStatement.switchToCoveredStatementList(markedStatements);

        testResult.addToFullTestData(new TestData(parameterNames, parameterClasses, evaluatedValues, coveredStatements,
                output, runTestDuration, calculateRequiredCoverage(coverage), calculateFunctionCoverage(), calculateSourceCodeCoverage()));

        boolean isTestedSuccessfully = true;

        for (CfgNode uncoveredNode = findUncoverNode(cfgBeginNode, coverage); uncoveredNode != null; ) {

            Path newPath = (new FindPath(cfgBeginNode, uncoveredNode, cfgEndNode)).getPath();

            SymbolicExecution solution = new SymbolicExecution(newPath, parameters);
            solution.execute();

            if (solution.getModel() == null) {
                isTestedSuccessfully = false;
                break;
            }

            evaluatedValues = solution.getEvaluatedTestData(parameterClasses);

            writeDataToFile("", FilePath.concreteExecuteResultPath, false);

            startRunTestTime = System.nanoTime();
            output = method.invoke(parameterClasses, evaluatedValues);
            endRunTestTime = System.nanoTime();
            runTestDuration = (endRunTestTime - startRunTestTime) / 1000000.0;

            markedStatements = getMarkedStatement();
            MarkedPath.markPathToCFGV2(cfgBeginNode, markedStatements);

            coveredStatements = CoveredStatement.switchToCoveredStatementList(markedStatements);

            testResult.addToFullTestData(new TestData(parameterNames, parameterClasses, evaluatedValues, coveredStatements, output, runTestDuration, calculateRequiredCoverage(coverage), calculateFunctionCoverage(), calculateSourceCodeCoverage()));

            uncoveredNode = findUncoverNode(cfgBeginNode, coverage);
//            System.out.println("Uncovered Node: " + uncoveredNode);
        }

        if (isTestedSuccessfully) System.out.println("Tested successfully with 100% coverage");
        else System.out.println("Test fail due to UNSATISFIABLE constraint");

        testResult.setFullCoverage(calculateFullTestSuiteCoverage(coverage));

        return testResult;
    }

    private static void setup(String path, String className, String methodName) throws IOException {
        funcAstNodeList = ProjectParser.parseFile(path);
        compilationUnit = ProjectParser.parseFileToCompilationUnit(path);
        setupFullyClonedClassName(className);
        setUpTestFunc(methodName);
        MarkedPath.resetFullTestSuiteCoveredStatements();
    }

    private static double calculateFullTestSuiteCoverage(Coverage coverage) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        int totalFunctionStatement = (int) Class.forName(fullyClonedClassName).getField(getTotalFunctionCoverageVariableName((MethodDeclaration) TestGeneration.testFunc, coverage)).get(null);
        int totalCovered = 0;
        if (coverage == Coverage.STATEMENT) {
            totalCovered = MarkedPath.getFullTestSuiteTotalCoveredStatements();
        } else { // branch
            totalCovered = MarkedPath.getFullTestSuiteTotalCoveredBranch();
        }
        return (totalCovered * 100.0) / totalFunctionStatement;
    }

    private static double calculateRequiredCoverage(Coverage coverage) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        int totalFunctionCoverage = (int) Class.forName(fullyClonedClassName).getField(getTotalFunctionCoverageVariableName((MethodDeclaration) testFunc, coverage)).get(null);
        int totalCovered = 0;
        if (coverage == Coverage.STATEMENT) {
            totalCovered = MarkedPath.getTotalCoveredStatement();
        } else if (coverage == Coverage.BRANCH) {
            totalCovered = MarkedPath.getTotalCoveredBranch();
            System.out.println(totalCovered);
        }
        return (totalCovered * 100.0) / totalFunctionCoverage;
    }

    private static double calculateFunctionCoverage() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        int totalFunctionStatement = (int) Class.forName(fullyClonedClassName).getField(getTotalFunctionCoverageVariableName((MethodDeclaration) testFunc, Coverage.STATEMENT)).get(null);
        int totalCoveredStatement = MarkedPath.getTotalCoveredStatement();
        return (totalCoveredStatement * 100.0) / (totalFunctionStatement * 1.0);
    }

    private static double calculateSourceCodeCoverage() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        int totalClassStatement = (int) Class.forName(fullyClonedClassName).getField(getTotalClassCoverageVariableName()).get(null);
        int totalCoveredStatement = MarkedPath.getTotalCoveredStatement();
        return (totalCoveredStatement * 100.0) / (totalClassStatement * 1.0);
    }

    private static void setUpTestFunc(String methodName) {
        for (ASTNode func : funcAstNodeList) {
            if (((MethodDeclaration) func).getName().getIdentifier().equals(methodName)) {
                testFunc = func;
            }
        }
    }

    private static void setupParameters(String methodName) throws ClassNotFoundException, NoSuchMethodException {
        parameters = ((MethodDeclaration) testFunc).parameters();
        parameterClasses = TestDriverUtils.getParameterClasses(parameters);
        parameterNames = TestDriverUtils.getParameterNames(parameters);
        String methodNameClone = methodName + "_clone";
        method = Class.forName(fullyClonedClassName).getDeclaredMethod(methodNameClone, parameterClasses);
    }

    private static void setupFullyClonedClassName(String className) {
        className = className.replace(".java", "");
        simpleClassName = className;
        String packetName = "";
        if (compilationUnit.getPackage() != null) {
            packetName = compilationUnit.getPackage().getName() + ".";
        }
        fullyClonedClassName = "data.clonedProject." + packetName + className;
    }

    private static void setupCfgTree(Coverage coverage) {
        Block functionBlock = Utils.getFunctionBlock(testFunc);

        cfgBeginNode = new CfgNode();
        cfgBeginNode.setIsBeginCfgNode(true);

        cfgEndNode = new CfgEndBlockNode();
        cfgEndNode.setIsEndCfgNode(true);

        CfgNode block = new CfgBlockNode();
        block.setAst(functionBlock);

        int firstLine = compilationUnit.getLineNumber(functionBlock.getStartPosition());
        block.setLineNumber(1);

        block.setBeforeStatementNode(cfgBeginNode);
        block.setAfterStatementNode(cfgEndNode);

        ASTHelper.generateCFG(block, compilationUnit, firstLine, getCoverageType(coverage));
    }

    private static String getTotalFunctionCoverageVariableName(MethodDeclaration methodDeclaration, Coverage coverage) {
        StringBuilder result = new StringBuilder();
        result.append(methodDeclaration.getReturnType2());
        result.append(methodDeclaration.getName());
        for (int i = 0; i < methodDeclaration.parameters().size(); i++) {
            result.append(methodDeclaration.parameters().get(i));
        }
        if (coverage == Coverage.STATEMENT) {
            result.append("TotalStatement");
        } else if (coverage == Coverage.BRANCH) {
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

    private static String getTotalClassCoverageVariableName() {
        StringBuilder result = new StringBuilder();
        result.append(simpleClassName).append("TotalStatement");
        return result.toString().replace(" ", "").replace(".", "");
    }
}

