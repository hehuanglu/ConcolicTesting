package core.testGeneration.ConcolicTestGeneration.ConcolicTestingWithStub;

import core.ast.Expression.Method.MethodInvocationNode;
import core.cfg.CfgBoolExprNode;
import core.testDriver.TestDriverUtils;
import core.testGeneration.ConcolicTestGeneration.ConcolicTestGeneration;
import core.testGeneration.LoopTestGeneration.LoopAnalyzer;
import core.testGeneration.LoopTestGeneration.LoopInfo;
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
import core.utils.Utils;
import org.eclipse.jdt.core.dom.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class ConcolicTestingWithLoopAnalyzer extends ConcolicTestGeneration {
    private static String simpleClassName;
    private static String fullyClonedClassName;
    private static List<ASTNode> originalParameters; // parameters before adding stub vars
    private static List<CfgNode> detectedLoops;

    private ConcolicTestingWithLoopAnalyzer() {
    }

    public static TestResult runFullConcolic(int id, String path, String methodName, String className, TestGeneration.Coverage coverage) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException, InterruptedException {
        setup(path, className, methodName);
        setupCfgTree(coverage);
        //Them moi
        detectedLoops = new ArrayList<>();
        setupParameters(methodName);
        TestGeneration.isSetup = true;

        TestResult result = startGenerating(id, coverage);

        TestGeneration.isSetup = false;

        return result;
    }

    private static TestResult startGenerating(int id, TestGeneration.Coverage coverage) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException, IOException, InterruptedException {
        TestResult testResult = new TestResult();
        testResult.setId(id);

        Object[] evaluatedValues = SymbolicExecution.createRandomTestData(TestGeneration.parameterClasses); // Tao du lieu thuc nghiem ngau nhien tu para

        TestGeneration.writeDataToFile("", FilePath.concreteExecuteResultPath, false); // Sua file thanh rong de xoa du lieu cu

        String testDriver = TestDriverGenerator.generateTestDriver((MethodDeclaration) TestGeneration.testFunc, evaluatedValues, TestGeneration.getCoverageType(coverage));
        List<MarkedStatement> markedStatements = TestDriverRunner.runTestDriver(testDriver);

        MarkedPath.markPathToCFGV2(TestGeneration.cfgBeginNode, markedStatements);

        List<CoveredStatement> coveredStatements = CoveredStatement.switchToCoveredStatementList(markedStatements);

        testResult.addToFullTestData(new TestData(TestGeneration.parameterNames, TestGeneration.parameterClasses, evaluatedValues, coveredStatements,
                TestDriverRunner.getOutput(), TestDriverRunner.getRuntime(), calculateRequiredCoverage(coverage), calculateFunctionCoverage(), calculateSourceCodeCoverage()));

        boolean isTestedSuccessfully = true;

        for (CfgNode uncoveredNode = TestGeneration.findUncoverNode(TestGeneration.cfgBeginNode, coverage); uncoveredNode != null; uncoveredNode = TestGeneration.findUncoverNode(TestGeneration.cfgBeginNode, coverage)) {
            System.out.println("Uncovered Node: " + uncoveredNode);

            Path newPath = (new FindPath(TestGeneration.cfgBeginNode, uncoveredNode, TestGeneration.cfgEndNode)).getPath();

            SymbolicExecution solution = new SymbolicExecution(newPath, TestGeneration.parameters);

            try {
                solution.execute();
            } catch (RuntimeException e) {
                e.printStackTrace();
                uncoveredNode.setFakeMarked(true); // for STATEMENT coverage
                if (coverage == Coverage.MCDC || coverage == Coverage.BRANCH) {
                    CfgNode parent = uncoveredNode.getParent();
                    if (parent instanceof CfgBoolExprNode) {
                        CfgBoolExprNode cfgBoolExprNode = (CfgBoolExprNode) parent;
                        if (cfgBoolExprNode.getTrueNode() == uncoveredNode) {
                            cfgBoolExprNode.setFakeTrueMarked(true);
                        } else if (cfgBoolExprNode.getFalseNode() == uncoveredNode) {
                            cfgBoolExprNode.setFakeFalseMarked(true);
                        }
                    }
                }
                continue;
            }

            // update parameter name with stub variable
            parameterNames = TestDriverUtils.getParameterNames(parameters);
            parameterClasses = TestDriverUtils.getParameterClasses(parameters);

            evaluatedValues = solution.getEvaluatedTestData(TestGeneration.parameterClasses);
//            List<String> stubVariableDeclarations = solution.getStubVariablesDeclarations();

            TestGeneration.writeDataToFile("", FilePath.concreteExecuteResultPath, false);

            testDriver = TestDriverGenerator.generateTestDriver((MethodDeclaration) TestGeneration.testFunc, evaluatedValues, TestGeneration.getCoverageType(coverage));
            markedStatements = TestDriverRunner.runTestDriver(testDriver);

            MarkedPath.markPathToCFGV2(TestGeneration.cfgBeginNode, markedStatements);
            coveredStatements = CoveredStatement.switchToCoveredStatementList(markedStatements);

//            List<String> testDataNames = new ArrayList<>();
//            testDataNames.addAll(TestGeneration.parameterNames);
//            testDataNames.addAll(SymbolicExecution.getStubVariableNames());

            testResult.addToFullTestData(new TestData(parameterNames, parameterClasses, evaluatedValues, coveredStatements, TestDriverRunner.getOutput(), TestDriverRunner.getRuntime(), calculateRequiredCoverage(coverage), calculateFunctionCoverage(), calculateSourceCodeCoverage()));

        }

        if (isTestedSuccessfully) System.out.println("Tested successfully with 100% coverage");
        else System.out.println("Test fail due to UNSATISFIABLE constraint");

        testResult.setFullCoverage(calculateFullTestSuiteCoverage(coverage));
        List<Map<String, Object[]>> loopTestcases = new ArrayList<>();
        if (hasLoop((MethodDeclaration) TestGeneration.testFunc)) {
            loopTestcases = analyzeLoop((MethodDeclaration) TestGeneration.testFunc);
        }
        for (Map<String, Object[]> map : loopTestcases) {
            for (Map.Entry<String, Object[]> entry : map.entrySet()) {
                String key = entry.getKey();
                Object[] values = entry.getValue();

                testDriver = TestDriverGenerator.generateTestDriver((MethodDeclaration) TestGeneration.testFunc, values, TestGeneration.getCoverageType(coverage));
                TestDriverRunner.runTestDriver(testDriver);
            }
        }

        return testResult;
    }

    private static void setup(String path, String className, String methodName) throws IOException {
        TestGeneration.funcAstNodeList = ProjectParser.parseFile(path);
        TestGeneration.compilationUnit = ProjectParser.parseFileToCompilationUnit(path);
        classKey = (TestGeneration.compilationUnit.getPackage() != null ? TestGeneration.compilationUnit.getPackage().getName().toString() : "") + className.replace(".java", "") + "totalStatement";
        setupFullyClonedClassName(className);
        setUpTestFunc(methodName);
        MarkedPath.resetFullTestSuiteCoveredStatements();

        MethodInvocationNode.resetNumberOfFunctionsCall();
    }

    private static void setupFullyClonedClassName(String className) {
        className = className.replace(".java", "");
        simpleClassName = className;
        String packetName = "";
        if (TestGeneration.compilationUnit.getPackage() != null) {
            packetName = TestGeneration.compilationUnit.getPackage().getName() + ".";
        }
        fullyClonedClassName = "data.clonedProject." + packetName + className;
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

    private static double calculateRequiredCoverage(TestGeneration.Coverage coverage) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        int totalFunctionCoverage = (int) Class.forName(fullyClonedClassName).getField(getTotalFunctionCoverageVariableName((MethodDeclaration) TestGeneration.testFunc, coverage)).get(null);
        int totalCovered = 0;
        if (coverage == TestGeneration.Coverage.STATEMENT) {
            totalCovered = MarkedPath.getTotalCoveredStatement();
        } else if (coverage == TestGeneration.Coverage.BRANCH || coverage == TestGeneration.Coverage.MCDC) {
            totalCovered = MarkedPath.getTotalCoveredBranch();
            System.out.println(totalCovered);
        }
        return (totalCovered * 100.0) / totalFunctionCoverage;
    }

    private static double calculateFunctionCoverage() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        int totalFunctionStatement = (int) Class.forName(fullyClonedClassName).getField(getTotalFunctionCoverageVariableName((MethodDeclaration) TestGeneration.testFunc, TestGeneration.Coverage.STATEMENT)).get(null);
        int totalCoveredStatement = MarkedPath.getTotalCoveredStatement();
        return (totalCoveredStatement * 100.0) / (totalFunctionStatement * 1.0);
    }

    private static double calculateSourceCodeCoverage() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        int totalClassStatement = (int) Class.forName(fullyClonedClassName).getField(getTotalClassCoverageVariableName()).get(null);
        int totalCoveredStatement = MarkedPath.getTotalCoveredStatement();
        return (totalCoveredStatement * 100.0) / (totalClassStatement * 1.0);
    }

    private static void setUpTestFunc(String methodName) {
        for (ASTNode func : TestGeneration.funcAstNodeList) {
            if (((MethodDeclaration) func).getName().getIdentifier().equals(methodName)) {
                TestGeneration.testFunc = func;
            }
        }
        originalParameters = new ArrayList<>(((MethodDeclaration) testFunc).parameters());
    }

    private static void setupParameters(String methodName) {
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

    private static String getTotalClassCoverageVariableName() {
        StringBuilder result = new StringBuilder();
        result.append(simpleClassName).append("TotalStatement");
        return result.toString().replace(" ", "").replace(".", "");
    }

    protected static String getTotalFunctionCoverageVariableName(MethodDeclaration methodDeclaration, TestGeneration.Coverage coverage) {
        StringBuilder result = new StringBuilder();
        result.append(methodDeclaration.getReturnType2());
        result.append(methodDeclaration.getName());
        for (int i = 0; i < originalParameters.size(); i++) {
            result.append(originalParameters.get(i));
        }
        if (coverage == TestGeneration.Coverage.STATEMENT) {
            result.append("TotalStatement");
        } else if (coverage == TestGeneration.Coverage.BRANCH || coverage == TestGeneration.Coverage.MCDC) {
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

    private static boolean hasLoop(MethodDeclaration testFunc) {
        if (testFunc == null || testFunc.getBody() == null) {
            return false;
        }

        Block functionBlock = Utils.getFunctionBlock(TestGeneration.testFunc);

        List<ASTNode> statements = functionBlock.statements();

        for (int i = 0; i < statements.size(); i++) {
            ASTNode statement = statements.get(i);
            if (statement instanceof ForStatement || statement instanceof WhileStatement || statement instanceof DoStatement) {
                return true;
            }
        }
        return false;
    }

    public static List<Map<String, Object[]>> analyzeLoop(MethodDeclaration method) throws IOException {
        System.out.println("\n===== ANALYZING LOOPS USING SYMBOLIC EXECUTION =====");
        LoopAnalyzer loopAnalyzer = new LoopAnalyzer(method);
        List<LoopInfo> loops = loopAnalyzer.detectLoops();
        Map<String, List<Object[]>> categorizedTestcases =new HashMap<>();
        categorizedTestcases.put("0_iterations", new ArrayList<>());  // 0 lần lặp
        categorizedTestcases.put("1_iteration", new ArrayList<>());   // 1 lần lặp
        categorizedTestcases.put("m_iterations", new ArrayList<>());  // m lần lặp (m < N)
        categorizedTestcases.put("n-1_iterations", new ArrayList<>()); // N-1 lần lặp
        categorizedTestcases.put("n_iterations", new ArrayList<>());   // N lần lặp (tối đa)
        categorizedTestcases.put("n+1_iterations", new ArrayList<>());  // N+1 lần lặp

        List<Map<String, Object[]>> loopTestcases = loopAnalyzer.generateSymbolicTestCases(parameterClasses);
        if (loopTestcases.isEmpty()) {
            System.out.println("No symbolic testcases could be generated. Using boundary value analysis instead.");
            loopTestcases = loopAnalyzer.generateLoopBoundaryTestCases();
        }

        return loopTestcases;
    }
}

