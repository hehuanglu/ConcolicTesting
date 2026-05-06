package core.testGeneration.ConcolicTestGeneration.ConcolicTestingWithStub;

import core.FilePath;
import core.ast.Expression.Method.MethodInvocationNode;
import core.cfg.CfgBlockNode;
import core.cfg.CfgBoolExprNode;
import core.cfg.CfgEndBlockNode;
import core.cfg.CfgNode;
import core.cfg.dataFlow.DefUsePair;
import core.cfg.utils.ASTHelper;
import core.cfg.utils.DataFlowHelper;
import core.cfg.utils.ProjectParser;
import core.cfg.utils.ProjectParserRewrite;
import core.path.FindPath;
import core.path.MarkedPath;
import core.path.MarkedStatement;
import core.path.Path;
import core.symbolicExecution.SymbolicExecutionRewrite;
import core.testDriver.TestDriverGenerator;
import core.testDriver.TestDriverRunner;
import core.testDriver.TestDriverUtils;
import core.testGeneration.ConcolicTestGeneration.ConcolicTestGeneration;
import core.testGeneration.TestGeneration;
import core.testResult.coveredStatement.CoveredStatement;
import core.testResult.result.autoTestResult.TestData;
import core.testResult.result.autoTestResult.TestResult;
import core.uploadProjectUtils.cloneProjectUtils.CloneProject;
import core.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.*;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ConcolicTestingWithStub4Libs extends ConcolicTestGeneration {
    private static String simpleClassName;
    private static String fullyClonedClassName;
    private static String originalFileLocation;
    private static List<ASTNode> originalParameters; // parameters before adding stub vars

    private ConcolicTestingWithStub4Libs() {
    }

    public static TestResult runFullConcolic(int id, String path, String methodName, String className, TestGeneration.Coverage coverage) throws Exception {
        setup(path, className, methodName, coverage);
        setupCfgTree(coverage);
        setupParameters(methodName);
        TestGeneration.isSetup = true;

        // data flow setup
        DataFlowHelper.ComputeDefUse(TestGeneration.cfgBeginNode);
        Set<DefUsePair> targetDUAs = DataFlowHelper.findAllDUAs(TestGeneration.cfgBeginNode);

        log.info("SIZE targetDUAs: " + targetDUAs.size());
        for (DefUsePair pair : targetDUAs) {
            log.info("targetDUA: " + pair);
        }
        TestGeneration.targetDUAs = targetDUAs;

        TestResult result = startGenerating(id, coverage);

        TestGeneration.isSetup = false;

        return result;
    }

    private static TestResult startGenerating(int id, TestGeneration.Coverage coverage) throws Exception {
        DecimalFormat df = new DecimalFormat("#.##");
        List<Double> memorySamples = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean isRunning = new AtomicBoolean(true);

        Thread monitorThread = new Thread(() -> {
            Runtime runtime = Runtime.getRuntime();
            while (isRunning.get()) {
                // Đo RAM hiện tại (MB)
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                double usedMB = usedMemory / (1024.0 * 1024.0);

                memorySamples.add(usedMB);

                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        monitorThread.start();
        long startTime = System.currentTimeMillis();

        log.info("BẮT ĐẦU QUÁ TRÌNH CONCOLIC TESTING CHO HÀM ID: {}", id);
        TestResult testResult = new TestResult();

        try {
            testResult.setId(id);

            Object[] evaluatedValues = SymbolicExecutionRewrite.createRandomTestData(TestGeneration.parameterClasses);

            TestGeneration.writeDataToFile("", FilePath.concreteExecuteResultPath, false);

            String testDriver = TestDriverGenerator.generateTestDriverNew((MethodDeclaration) TestGeneration.testFunc, evaluatedValues, TestGeneration.getCoverageType(coverage), originalFileLocation, simpleClassName);
            List<MarkedStatement> markedStatements = TestDriverRunner.newRunTestDriver(testDriver, originalFileLocation);

            MarkedPath.markPathToCFGV2(TestGeneration.cfgBeginNode, markedStatements);

            List<CoveredStatement> coveredStatements = CoveredStatement.switchToCoveredStatementList(markedStatements);

            testResult.addToFullTestData(new TestData(TestGeneration.parameterNames, TestGeneration.parameterClasses, evaluatedValues, coveredStatements,
                    TestDriverRunner.getOutput(), TestDriverRunner.getRuntime(), calculateRequiredCoverage(coverage), calculateFunctionCoverage(), calculateSourceCodeCoverage()));


            // ====== LICO testing method ======
//            List<Path> licoPaths = LoopPathGenerator.generateLicoPaths(TestGeneration.cfgBeginNode, TestGeneration.cfgEndNode);
//            int cnt = 1;
//            for (Path path : licoPaths) {
//                System.out.println("LICO đang chạy path: " + cnt);
//                cnt++;
//                solveAndRunTest(path, testResult, coverage);
//            }
            // ==================================

            //----- End test Lico

            // 1. Xây dựng Map tìm kiếm nhanh từ Content -> CfgNode
            Map<String, CfgNode> contentToCfgNodeMap = buildContentToNodeMap(TestGeneration.cfgBeginNode);

            log.info("Bắt đầu chạy luồng Data Flow Graph...");
            if (!TestGeneration.targetDUAs.isEmpty()) {
                for (DefUsePair pair : targetDUAs) {
                    if (pair.isCovered) continue;

                    log.debug("Đang xử lý DUA: {}", pair);
                    boolean duaSolved = false;

                    // Tìm các đường Def-Clear. Ta sẽ thử tối đa 3 đường khác nhau
                    List<List<CfgNode>> potentialCorePaths = DataFlowHelper.findAllDefClearPaths(pair.defNode, pair.useNode, pair.variable, 3);

                    if (potentialCorePaths.isEmpty()) {
                        log.warn("Không tìm thấy đường Def-Clear khả thi cho DUA: {}", pair.variable);
                        continue;
                    }

                    // Duyệt từng phương án Core Path
                    for (List<CfgNode> corePath : potentialCorePaths) {
                        // Nếu đã giải được thì thoát vòng lặp core
                        if (duaSolved) break;

                        // Tìm Prefix (Start -> Def). Ta sẽ thử tối đa 3 đường khác nhau
                        List<List<CfgNode>> potentialPrefixes = DataFlowHelper.findAllPaths(TestGeneration.cfgBeginNode, pair.defNode, 3);

                        for (List<CfgNode> prefix : potentialPrefixes) {
                            if (duaSolved) break;

                            // Tìm Suffix (Use -> End). Ta chỉ cần 1 đường.
                            List<CfgNode> suffix = DataFlowHelper.findStandardPath(pair.useNode, TestGeneration.cfgEndNode);

                            if (suffix == null) continue;

                            // ghép các đườnng lại với nhau
                            List<CfgNode> fullPath = new ArrayList<>(prefix);
                            if (corePath.size() > 1) fullPath.addAll(corePath.subList(1, corePath.size()));
                            if (suffix.size() > 1) fullPath.addAll(suffix.subList(1, suffix.size()));

                            // GỌI solve
                            try {
                                Path pathObj = convertToPath(fullPath);
                                boolean success = solveAndRunTest(pathObj, testResult, coverage);

                                if (success) {
                                    log.info("Đã giải thành công DUA [{}] với Core Path index: {}", pair.variable, potentialCorePaths.indexOf(corePath));
                                    pair.isCovered = true;
                                    duaSolved = true;

                                    // Tính Bao hàm & Tối ưu (Subsumption)
                                    TestData lastRun = testResult.getFullTestData().get(testResult.getFullTestData().size() - 1);
                                    checkCoverageForAllDUAs(lastRun, targetDUAs, contentToCfgNodeMap);
                                } else {
                                    log.debug("Core Path index {} UNSAT, thử đường khác...", potentialCorePaths.indexOf(corePath));
                                }
                            } catch (Exception e) {
                                log.error("Lỗi khi giải đường đi (Path): {}", e.getMessage(), e);
                            }
                        }
                    }

                    if (!duaSolved) {
                        log.warn("Đã thử hết nhưng UNSAT cho DUA: {}", pair.variable);
                    }
                }
            }


            boolean isTestedSuccessfully = true;

            for (CfgNode uncoveredNode = TestGeneration.findUncoverNode(TestGeneration.cfgBeginNode, coverage); uncoveredNode != null; uncoveredNode = TestGeneration.findUncoverNode(TestGeneration.cfgBeginNode, coverage)) {
                log.info("Cố gắng phủ nhánh còn thiếu tại Node: {}", uncoveredNode);

                Path newPath = (new FindPath(TestGeneration.cfgBeginNode, uncoveredNode, TestGeneration.cfgEndNode)).getPath();

                boolean success = solveAndRunTest(newPath, testResult, coverage);

                if (!success) {
                    uncoveredNode.setFakeMarked(true);
                    /*
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

                     */
                }
            }

            testResult.setFullCoverage(calculateFullTestSuiteCoverage(coverage));

            // 2. Duyệt qua từng TestData (mỗi test case sinh ra)
            for (TestData data : testResult.getFullTestData()) {
                List<CoveredStatement> originalList = data.getCoveredStatements();
                List<CoveredStatement> translatedList = new ArrayList<>();

                // Dùng Set để tránh trùng lặp (vì if/else con có thể trỏ về cùng 1 cha)
                Set<String> addedStatements = new HashSet<>();

                for (CoveredStatement stmt : originalList) {
                    String oldContent = stmt.getStatementContent();

                    // Tìm node trong CFG
                    CfgNode node = contentToCfgNodeMap.get(oldContent.trim());

                    ASTNode originalAst = null;
                    if (node != null) {
                        // Tra từ điển ánh xạ ngược (ASTHelper)
                        originalAst = ASTHelper.syntheticToOriginalMap.get(node.getAst());
                    }

                    if (originalAst != null) {
                        String newContent = originalAst.toString();

                        // Chỉ thêm nếu chưa có trong danh sách hiển thị
                        if (!addedStatements.contains(newContent)) {
                            // Tạo statement mới với nội dung gốc
                            // Giữ nguyên lineNumber vì logic setSourceRange đã làm nó đúng
                            translatedList.add(new CoveredStatement(newContent, stmt.getLineNumber(), stmt.getConditionStatus()));
                            addedStatements.add(newContent);
                        }
                    } else {
                        // Không tìm thấy cha, đây là node bình thường -> Giữ nguyên
                        if (!addedStatements.contains(oldContent)) {
                            translatedList.add(stmt);
                            addedStatements.add(oldContent);
                        }
                    }
                }
                // Cập nhật lại danh sách hiển thị cho TestData này
                data.setCoveredStatements(translatedList);
            }
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng làm dừng quá trình Test: {}", e.getMessage(), e);
        } finally {
            isRunning.set(false);
            monitorThread.interrupt();
            long endTime = System.currentTimeMillis();

            double sum = 0;
            for (Double sample : memorySamples) {
                sum += sample;
            }

            double averageMemory = 0;
            if (memorySamples.isEmpty()) {
                long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                averageMemory = used / (1024.0 * 1024.0);
                log.debug("Thu thập RAM tức thời do thời gian chạy quá nhanh.");
            } else {
                averageMemory = sum / memorySamples.size();
            }

            StringBuilder report = new StringBuilder();
            report.append("\n================ BÁO CÁO HIỆU NĂNG ================\n");
            report.append(String.format(" - Mẫu đo (RAM): %d lần\n", memorySamples.size()));
            report.append(String.format(" - Trung bình:   %s MB\n", df.format(averageMemory)));
            if (!memorySamples.isEmpty()) {
                double maxMem = Collections.max(memorySamples);
                report.append(String.format(" - Đỉnh:  %s MB\n", df.format(maxMem)));
            }
            report.append(String.format(" - Thời gian:    %s s\n", df.format((endTime - startTime) / 1000.0)));
            report.append("===================================================");

            log.info(report.toString());
        }
        double totalTime = 0;
        for (TestData data : testResult.getFullTestData()) {
            totalTime += data.getExecuteTime();
        }

        log.info("Tổng Execute Time (Các Test Cases): {} ms", totalTime);

        try {
            Set<String> uniqueInputs = new HashSet<>();
            List<Object[]> generatedInputs = new ArrayList<>();

            log.info("--Danh sách Input Unique ({})--", testResult.getFullTestData().size());

            for (TestData data : testResult.getFullTestData()) {
                Object[] input = data.getTestDataSet().toArray();

                String markInput = Arrays.toString(input);

                if (!uniqueInputs.contains(markInput)) {
                    uniqueInputs.add(markInput);
                    generatedInputs.add(input);
                    log.debug("Add Input {}", markInput);
                }
            }
            log.info("Tổng số bộ dữ liệu thu thâp được là: {}", generatedInputs.size());

            // Specify the name of the target method to be analyzed.
            // This value must match the Java class and method name in the source project.
            String classBaseName = "convertToBase7";

            String fullyClonedClassName = classBaseName + "." + classBaseName;

            List<String> mutants = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                mutants.add(classBaseName + "." + classBaseName + i);
            }

            String targetMethodName =
                    ((MethodDeclaration) TestGeneration.testFunc)
                            .getName()
                            .getIdentifier();

            // Define the absolute path to the Java source file of the target method.
            // Users should ensure that the directory structure and file name
            // correspond to the specified method name.
            String fullPath = Paths.get(
                    "D:", // Ổ đĩa
                    "projectLAB", "backend", "jcia-backend",
                    "project", "anonymous", "tmp-prj",
                    "dataTest.zip.project", "dataTest",
                    "src", "main", "java", // Kiểm tra kỹ xem có folder 'main/java' không hay chỉ là 'src'?
                    classBaseName,          // Folder package (binaryGap)
                    classBaseName + ".java" // File (binaryGap.java)
            ).toString();

            String packageName = classBaseName;

            String rootPath = getRootSourcePath(fullPath, packageName);

//            System.out.println("Path nạp vào ClassLoader: " + rootPath);

//            int killed = runMutationTest(fullyClonedClassName, mutants, generatedInputs, targetMethodName, TestGeneration.parameterClasses, rootPath);

        } catch (Exception e) {
            log.error("Lỗi ở khâu hậu kỳ: {}", e.getMessage(), e);
        }

        return testResult;
    }

    /**
     *
     *
     * @param testData         Kết quả chạy của test case vừa rồi
     * @param targetDUAs       Danh sách tất cả các DUA cần phủ
     * @param contentToNodeMap Map ánh xạ từ String code sang CfgNode để tra cứu
     */
    private static void checkCoverageForAllDUAs(TestData testData, Set<DefUsePair> targetDUAs, Map<String, CfgNode> contentToNodeMap) {
        List<CoveredStatement> trace = testData.getCoveredStatements();
        Map<String, CfgNode> activeDefinitions = new HashMap<>();

        for (CoveredStatement stmt : trace) {
            String content = stmt.getStatementContent().trim();

            CfgNode node = contentToNodeMap.get(content);
            if (node == null) {
                continue;
            }

            // KIỂM TRA USE
            // Nếu node này dùng biến X, và ta biết X được define ở đâu trước đó
            // Ta vừa hoàn thành một đường đi Def-Use
            Set<String> useVars = node.getUseVars();
            for (String useVar : useVars) {
                if (activeDefinitions.containsKey(useVar)) {
                    CfgNode lastDefNode = activeDefinitions.get(useVar);

                    for (DefUsePair pair : targetDUAs) {
                        if (!pair.isCovered && pair.variable.equals(useVar) && pair.defNode == lastDefNode && pair.useNode == node) {
                            pair.isCovered = true;
                            log.info("SKIP: {} {}", pair, " (Đã được phủ trước đó)");
                        }
                    }
                }
            }

            // KIỂM TRA DEF
            // Node này định nghĩa lại biến X => Các Use sau này sẽ dùng X mới này
            Set<String> defVars = node.getDefVars();
            for (String defVar : defVars) {
                activeDefinitions.put(defVar, node);
            }
        }
    }

    private static Path convertToPath(List<CfgNode> fullPath) {
        Path path = new Path();
        for (CfgNode node : fullPath) {
            path.addLast(node);
        }
        return path;
    }

    public static String getRootSourcePath(String fullFilePath, String packageName) {

        File file = new File(fullFilePath);
        String parentDir = file.getParent();

        int srcIndex = fullFilePath.indexOf("src" + File.separator + "main" + File.separator + "java");
        if (srcIndex != -1) {
            String rootPath = fullFilePath.substring(0, srcIndex + ("src" + File.separator + "main" + File.separator + "java").length());
            log.debug("Get Root Source Path (Dựa theo Maven src/main/java): {}", rootPath);
            return rootPath;
        }
        File currentDir = new File(parentDir);
        String[] packageParts = packageName.split("\\.");
        for (int i = 0; i < packageParts.length; i++) {
            currentDir = currentDir.getParentFile();
        }
        String rootPath = currentDir.getAbsolutePath();
        log.debug("Get Root Source Path (Dựa theo Package): {}", rootPath);
        return rootPath;
    }

    private static void setup(String path, String className, String methodName, TestGeneration.Coverage coverage) throws IOException, InterruptedException {
        log.info("Bắt đầu Setup phân tích hàm: [{}] trong file: {}", methodName, className);
        TestGeneration.compilationUnit = ProjectParserRewrite.parseFileToCompilationUnit(path);
        TestGeneration.funcAstNodeList = ProjectParserRewrite.parseFile(path, TestGeneration.compilationUnit);
        classKey = (TestGeneration.compilationUnit.getPackage() != null ? TestGeneration.compilationUnit.getPackage().getName().toString() : "") + className.replace(".java", "") + "totalStatement";

        setupFullyClonedClassName(className, path, coverage);
        setUpTestFunc(methodName);
        MarkedPath.resetFullTestSuiteCoveredStatements();

        MethodInvocationNode.resetNumberOfFunctionsCall();
    }

    private static void setupFullyClonedClassName(String className, String path, TestGeneration.Coverage coverage) throws IOException, InterruptedException {
        String newPath = getRootProjectPath(path);
        java.nio.file.Path rootPackage = CloneProject.findRootPackage(Paths.get(newPath));

        log.debug("Root Package tìm được: {}", rootPackage.toString());

        CloneProject.cloneProject(rootPackage.toString(), FilePath.clonedProjectPath, getCoverageType(coverage), className);
        className = className.replace(".java", "");
        simpleClassName = getClassFromCU(compilationUnit);

        // Xóa file .java (bỏ cả tên file)
        String relative = path.substring(rootPackage.toString().length() + 1);
//        relative = relative.replace("\\", "/");
        int lastSlash = relative.lastIndexOf(File.separator);
        if (lastSlash != -1) {
            relative = relative.substring(0, lastSlash + 1);
        } else {
            relative = "";
        }

        // Đổi "/" thành "."
        String packetName = relative.replace(File.separator, ".");
        System.out.println(packetName);

        log.debug("Package Name trích xuất: {}", packetName);

        originalFileLocation = "data.clonedProject." + packetName + className;
        fullyClonedClassName = "data.clonedProject." + packetName + simpleClassName;

        log.debug("Original File Location: {}", originalFileLocation);
        log.debug("Fully Cloned Class Name: {}", fullyClonedClassName);
    }

    private static String getClassFromCU(CompilationUnit compilationUnit) {
        List<TypeDeclaration> classes = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                classes.add(node);
                return super.visit(node);
            }
        });
        return classes.get(0).getName().toString();
    }

    private static Class<?> loadLatestClass(String className) throws Exception {
        // trỏ đường dẫn về thư mục chứa file .class
        File file = new File(core.FilePath.targetClassesFolderPath);
        java.net.URL[] urls = new java.net.URL[]{file.toURI().toURL()};

        log.debug("URLClassLoader đang nạp class [{}] từ thư mục: {}", className, file.getAbsolutePath());

        try (java.net.URLClassLoader cl = new java.net.URLClassLoader(urls, ClassLoader.getSystemClassLoader())) {
            return cl.loadClass(className);
        }
    }

    private static double calculateFullTestSuiteCoverage(Coverage coverage) throws Exception {
        Class<?> latestClass = loadLatestClass(fullyClonedClassName);
        Field field = latestClass.getField(getTotalFunctionCoverageVariableName((MethodDeclaration) TestGeneration.testFunc, coverage));
        field.setAccessible(true);
        int totalFunctionStatement = (int) field.get(null);
        int totalCovered = 0;
        if (coverage == Coverage.STATEMENT) {
            totalCovered = MarkedPath.getFullTestSuiteTotalCoveredStatements();
        } else { // branch
            totalCovered = MarkedPath.getFullTestSuiteTotalCoveredBranch();
        }
        return (totalCovered * 100.0) / totalFunctionStatement;
    }

    private static double calculateRequiredCoverage(TestGeneration.Coverage coverage) throws Exception {
        Class<?> latestClass = loadLatestClass(fullyClonedClassName);
        Field field = latestClass.getField(getTotalFunctionCoverageVariableName((MethodDeclaration) TestGeneration.testFunc, coverage));
        field.setAccessible(true);

        int totalFunctionCoverage = (int) field.get(null);
        int totalCovered = 0;
        if (coverage == TestGeneration.Coverage.STATEMENT) {
            totalCovered = MarkedPath.getTotalCoveredStatement();
        } else if (coverage == TestGeneration.Coverage.BRANCH || coverage == TestGeneration.Coverage.MCDC) {
            totalCovered = MarkedPath.getTotalCoveredBranch();
            System.out.println(totalCovered);
        }
        double currentCoverage = (totalCovered * 100.0) / totalFunctionCoverage;
        log.debug("Tiến độ Coverage [{}]: {}/{} ({}%)", coverage.toString(), totalCovered, totalFunctionCoverage, currentCoverage);

        return currentCoverage;
    }

    private static double calculateFunctionCoverage() throws Exception {
        Class<?> latestClass = loadLatestClass(fullyClonedClassName);
        Field field = latestClass.getField(getTotalFunctionCoverageVariableName((MethodDeclaration) TestGeneration.testFunc, TestGeneration.Coverage.STATEMENT));
        field.setAccessible(true);
        int totalFunctionStatement = (int) field.get(null);
        int totalCoveredStatement = MarkedPath.getTotalCoveredStatement();
        return (totalCoveredStatement * 100.0) / (totalFunctionStatement * 1.0);
    }

    private static double calculateSourceCodeCoverage() throws Exception {
        Class<?> latestClass = loadLatestClass(fullyClonedClassName);
        Field field = latestClass.getField(getTotalClassCoverageVariableName());
        field.setAccessible(true);
        int totalClassStatement = (int) field.get(null);
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

        //In ra danh sách tham số của hàm cần test
        if (TestGeneration.parameterNames != null && !TestGeneration.parameterNames.isEmpty()) {
            log.info("Phát hiện {} tham số đầu vào: {}", TestGeneration.parameterNames.size(), TestGeneration.parameterNames);
        } else {
            log.info("Hàm không có tham số đầu vào.");
        }
    }

    private static void setupCfgTree(TestGeneration.Coverage coverage) {
        log.info("Bắt đầu dựng đồ thị luồng điều khiển (CFG) cho hàm...");

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
        log.info("Dựng cây CFG thành công!");
    }

    private static Map<String, CfgNode> buildContentToNodeMap(CfgNode root) {
        Map<String, CfgNode> map = new HashMap<>();
        if (root == null) return map;

        Queue<CfgNode> q = new LinkedList<>();
        Set<CfgNode> visited = new HashSet<>();
        q.add(root);

        while (!q.isEmpty()) {
            CfgNode curr = q.poll();
            if (curr == null || visited.contains(curr)) continue;
            visited.add(curr);

            if (curr.getContent() != null) {
                map.put(curr.getContent().trim(), curr);
            }

            if (curr instanceof core.cfg.CfgBoolExprNode) {
                q.add(((core.cfg.CfgBoolExprNode) curr).getTrueNode());
                q.add(((core.cfg.CfgBoolExprNode) curr).getFalseNode());
            }
            q.add(curr.getAfterStatementNode());
        }
        return map;
    }

    private static boolean solveAndRunTest(Path path, TestResult testResult, TestGeneration.Coverage coverage)
            throws Exception {

        SymbolicExecutionRewrite solution = new SymbolicExecutionRewrite(path, TestGeneration.parameters);

        try {
            solution.execute();
        } catch (RuntimeException e) {
            log.error("HỆ THỐNG CRASH SAU KHI GIẢI PATH: {}", e.getMessage(), e);
            log.warn("Path hiện tại UNSATISFIABLE (Z3 không thể giải). Bỏ qua path này.");
            return false;
        }


        TestGeneration.parameterNames = TestDriverUtils.getParameterNames(TestGeneration.parameters);
        TestGeneration.parameterClasses = TestDriverUtils.getParameterClasses(TestGeneration.parameters);

        Object[] evaluatedValues = solution.getEvaluatedTestData(TestGeneration.parameterClasses);

        log.debug("Z3 giải ra bộ input thô: {}", Arrays.toString(evaluatedValues));

        List<Object[]> inputCandidates = new ArrayList<>();
        inputCandidates.add(evaluatedValues);
//
//        for (int i = 0; i < evaluatedValues.length; i++) {
//            if (evaluatedValues[i] instanceof Integer) {
//                int var = ((Integer) evaluatedValues[i]).intValue();
//
//                Object[] lower = evaluatedValues.clone();
//                lower[i] = var - 1;
//                Object[] upper = evaluatedValues.clone();
//                upper[i] = var + 1;
//
//                inputCandidates.add(lower);
//                inputCandidates.add(upper);
//            } else if (evaluatedValues[i] instanceof Double) {
//                double var = ((Double) evaluatedValues[i]).doubleValue();
//                Object[] lower = evaluatedValues.clone();
//                lower[i] = var - 1;
//                Object[] upper = evaluatedValues.clone();
//                upper[i] = var + 1;
//                inputCandidates.add(lower);
//                inputCandidates.add(upper);
//            }
//        }

        Set<String> executedInThisPath = new HashSet<>();

        for (Object[] input : inputCandidates) {
            String inputSign = Arrays.toString(input);

            // Kiểm tra trong lịch sử testResult xem input này đã từng xuất hiện chưa
            boolean isDuplicateGlobal = false;

            for (TestData oldData : testResult.getFullTestData()) {
                Object[] oldInput = oldData.getTestDataSet().toArray();
                String oldInputSign = Arrays.toString(oldInput);

                // Nếu trùng với input chuẩn bị chạy
                if (oldInputSign.equals(inputSign)) {
                    isDuplicateGlobal = true;
                    break;
                }
            }

            if (isDuplicateGlobal) {
                log.debug("Input {} đã tồn tại trong tập dữ liệu. Bỏ qua.", inputSign);
                // continue;
                return false;
            }

            if (executedInThisPath.contains(inputSign)) continue;
            executedInThisPath.add(inputSign);

            log.info("Chạy TestDriver với bộ Input mới sinh: {}", inputSign);

            TestGeneration.writeDataToFile("", FilePath.concreteExecuteResultPath, false);

            String testDriver = TestDriverGenerator.generateTestDriverNew(
                    (MethodDeclaration) TestGeneration.testFunc,
                    input,
                    TestGeneration.getCoverageType(coverage),
                    originalFileLocation,
                    simpleClassName
            );

            List<MarkedStatement> markedStatements = TestDriverRunner.newRunTestDriver(testDriver, originalFileLocation);

            MarkedPath.markPathToCFGV2(TestGeneration.cfgBeginNode, markedStatements);

            List<CoveredStatement> coveredStatements = CoveredStatement.switchToCoveredStatementList(markedStatements);

            testResult.addToFullTestData(new TestData(
                    TestGeneration.parameterNames,
                    TestGeneration.parameterClasses,
                    input,
                    coveredStatements,
                    TestDriverRunner.getOutput(),
                    TestDriverRunner.getRuntime(),
                    calculateRequiredCoverage(coverage),
                    calculateFunctionCoverage(),
                    calculateSourceCodeCoverage()
            ));
        }

        return true;
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

    private static String getRootProjectPath(String path) {
        // Chuẩn hóa dấu gạch chéo
        String newPath = path.replace("\\", "/");

        // Tách các phần theo "/"
        String[] parts = newPath.split("/");

        // Nối lại từ đầu tới folder thứ 6
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (i > 0) sb.append("/");
            sb.append(parts[i]);
        }

        return sb.toString();
    }

    private static int runMutationTest(String originalClassName, List<String> errorClassNames,
                                       List<Object[]> testDataList, String methodName,
                                       Class<?>[] paramTypes, String rootFolderPath) {
        int errorsDetected = 0;

        log.info(">>> BẮT ĐẦU QUÁ TRÌNH KIỂM THỬ ĐỘT BIẾN (ERROR DETECTION) <<<");

        ExecutorService executor = Executors.newFixedThreadPool(1);

        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                log.error("Không tìm thấy Java Compiler (JDK)! Dừng quá trình Mutation Testing.");
                return 0;
            }

            File root = new File(rootFolderPath);
            if (!root.exists()) return 0;

            String originalFilePath = rootFolderPath + File.separator + originalClassName.replace(".", File.separator) + ".java";
            File originalFile = new File(originalFilePath);

            if (originalFile.exists()) {
                compiler.run(null, null, null, originalFile.getAbsolutePath());
            } else {
                log.error("Không tìm thấy file source gốc: {}", originalFilePath);
                return 0;
            }

            for (String errorClassName : errorClassNames) {
                String errorFilePath = rootFolderPath + File.separator + errorClassName.replace(".", File.separator) + ".java";
                File errorFile = new File(errorFilePath);
                if (errorFile.exists()) {
                    compiler.run(null, null, null, errorFile.getAbsolutePath());
                }
            }

            java.net.URL[] urls = {root.toURI().toURL()};
            java.net.URLClassLoader classLoader = new java.net.URLClassLoader(urls);

            Class<?> originalClass = classLoader.loadClass(originalClassName);

            for (String errorClassName : errorClassNames) {
                boolean isDetected = false;

                try {
                    Class<?> errorClass = classLoader.loadClass(errorClassName);
                    StringBuilder checkLog = new StringBuilder("Checking Bug Case: ").append(errorClass.getSimpleName()).append("... ");

                    for (Object[] args : testDataList) {
                        try {
                            Object originalObj = originalClass.getDeclaredConstructor().newInstance();
                            Object errorObj = errorClass.getDeclaredConstructor().newInstance();

                            Method originalMethod = originalClass.getMethod(methodName, paramTypes);
                            Method errorMethod = errorClass.getMethod(errorClass.getSimpleName(), paramTypes);

                            // 1. Chạy hàm gốc
                            Object originalOutput = null;
                            Exception originalException = null;
                            try {
                                originalOutput = originalMethod.invoke(originalObj, args);
                            } catch (InvocationTargetException e) {
                                originalException = (Exception) e.getTargetException();
                            }

                            // 2. Chạy hàm Mutant
                            Object errorOutput = null;
                            Exception errorException = null;
                            boolean isTimeout = false;

                            Callable<Object> task = () -> errorMethod.invoke(errorObj, args);
                            Future<Object> future = executor.submit(task);

                            try {
                                errorOutput = future.get(2, TimeUnit.SECONDS);
                            } catch (TimeoutException e) {
                                isTimeout = true;
                                future.cancel(true);
                            } catch (ExecutionException e) {
                                if (e.getCause() instanceof InvocationTargetException) {
                                    errorException = (Exception) ((InvocationTargetException) e.getCause()).getTargetException();
                                } else {
                                    errorException = (Exception) e.getCause();
                                }
                            } catch (Exception e) {
                                errorException = e;
                            }

                            // 3. So sánh kết quả
                            if (isTimeout ||
                                    (originalException == null && errorException != null) ||
                                    (originalException == null && !isTimeout && errorException == null && !originalOutput.equals(errorOutput))) {
                                isDetected = true;
                                if (isTimeout) checkLog.append("[TIMEOUT - Infinite Loop] ");
                                break;
                            }
                        } catch (Exception e) {
                        }
                    }

                    if (isDetected) {
                        errorsDetected++;
                        checkLog.append("DETECTED (Đã tìm ra)");
                        log.info(checkLog.toString());
                    } else {
                        checkLog.append("UNDETECTED (Không tìm thấy)");
                        log.warn(checkLog.toString());
                    }
                } catch (ClassNotFoundException e) {
                    log.error("Không tìm thấy class lỗi (Mutant): {}", errorClassName);
                }
            }

            classLoader.close();

            DecimalFormat df = new DecimalFormat("#.##");
            double rate = errorClassNames.isEmpty() ? 0 : (double) errorsDetected / errorClassNames.size() * 100;

            StringBuilder report = new StringBuilder();
            report.append("\n================ ERROR DETECTION REPORT ================\n");
            report.append(String.format(" - Inserted Errors (Tổng lỗi cấy vào): %d\n", errorClassNames.size()));
            report.append(String.format(" - Detected Errors (Số lỗi tìm thấy):  %d\n", errorsDetected));
            report.append(String.format(" - Detection Rate (Tỷ lệ phát hiện):   %s%%\n", df.format(rate)));
            report.append("========================================================");

            log.info(report.toString());

        } catch (Exception e) {
            log.error("Lỗi sập luồng trong quá trình Mutation Testing: {}", e.getMessage(), e);
        } finally {
            executor.shutdownNow();
        }
        return errorsDetected;
    }

}

