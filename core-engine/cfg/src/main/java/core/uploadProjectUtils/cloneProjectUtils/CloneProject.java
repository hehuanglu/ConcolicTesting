package core.uploadProjectUtils.cloneProjectUtils;

import core.FilePath;
import core.cfg.utils.ASTHelper;
import core.cmd.CommandLine;
import core.uploadProjectUtils.cloneProjectUtils.dataModel.ClassData;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static core.cfg.utils.ASTHelper.convertTernaryToIf;

public final class CloneProject {
    private static int totalFunctionStatement;
    private static int totalClassStatement;
    private static int totalFunctionBranch;
    private static CompilationUnit classCompilationUnit;
    private static int firstLine;

    private enum CoverageType {
        STATEMENT,
        BRANCH
    }

    private static StringBuilder command;

    public static String getJavaDirPath(String originDir) {
        File dir = new File(originDir);

        if (!dir.isDirectory()) {
            throw new RuntimeException("Invalid Dir");
        }
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                if (file.getName().equals("java")) {
                    return file.getPath();
                } else {
                    String dirPath = getJavaDirPath(file.getPath());
                    if (dirPath.endsWith("java")) return dirPath;
                }
            }
        }
        return "";
    }

    public static Path findRootPackage(Path sourceDir) throws IOException {
        if (!Files.exists(sourceDir)) {
            throw new NoSuchFileException(sourceDir.toString());
        }
        Path base = Files.isDirectory(sourceDir) ? sourceDir : sourceDir.getParent();

        List<Path> javaFiles;
        try (Stream<Path> s = Files.walk(base)) {
            javaFiles = s.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        }
        if (javaFiles.isEmpty()) {
            return base;
        }
        List<Path> candidates = new ArrayList<>();
        int count = 0;
        for (Path jf : javaFiles) {
            count++;
            // System.out.println(jf.toString());
            String pkg = readPackageDecl(jf);
            int depth = (pkg == null || pkg.isEmpty()) ? 0 : pkg.split("\\.").length;

            // System.out.println(depth);
            Path p = jf.getParent();
            for (int i = 0; i < depth && p != null; i++) {
                p = p.getParent();
            }
            if (p != null) {
                candidates.add(p.toAbsolutePath().normalize());
            }

            if (count == 1) break;
        }

        // System.out.println("pp");

        Path root = candidates.get(0);
        for (int i = 1; i < candidates.size(); i++) {
            root = commonPrefix(root, candidates.get(i));
            if (root == null) {
                return base.toAbsolutePath().normalize();
            }
        }

        if (!root.startsWith(base.toAbsolutePath().normalize())) {
            return base.toAbsolutePath().normalize();
        }
        return root;
    }

    public static void cloneProject(String originalDirPath, String destinationDirPath, ASTHelper.Coverage coverage, String fileName) throws IOException, InterruptedException {
        command = new StringBuilder("javac -d " + FilePath.targetClassesFolderPath + " ");
        iCloneProject(originalDirPath, destinationDirPath, coverage, fileName);
        System.out.println(command);

        CommandLine.executeCommand(command.toString());
    }

    private static void iCloneProject(String originalDirPath, String destinationDirPath, ASTHelper.Coverage coverage, String fileToTestName) throws IOException {
        deleteFilesInDirectory(destinationDirPath);
        boolean existJavaFile = false;

        File[] files = getFilesInDirectory(originalDirPath);

        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();
                createCloneDirectory(destinationDirPath, dirName);
                iCloneProject(originalDirPath + "/" + dirName, destinationDirPath + "/" + dirName, coverage, fileToTestName);
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                existJavaFile = true;
                String fileName = file.getName();
                String sourcePath = originalDirPath + "/" + fileName;

                if (fileName.equals(fileToTestName)) {
                    //File chính -> Parse AST và cấy mã theo dõi
                    totalClassStatement = 0;
                    CompilationUnit compilationUnit = Parser.parseFileToCompilationUnit(sourcePath);
                    classCompilationUnit = compilationUnit;

                    createCloneFile(destinationDirPath, fileName);
                    String sourceCode = createCloneSourceCode(compilationUnit, destinationDirPath, coverage);
                    writeDataToFile(sourceCode, destinationDirPath + "/" + fileName);
                } else {
                    //File họ hàng -> Copy sang và tự động truyền package
                    try {
                        String originalCode = new String(Files.readAllBytes(Path.of(sourcePath)));
                        String correctPackage = buildPackage(destinationDirPath); // Lấy tên package

                        //chưa có package thì tự động nhét vào đầu file
                        if (!originalCode.contains("package ")) {
                            originalCode = correctPackage + "\n" + originalCode;
                        } else {
                            // Nếu có package lạ, thay bằng package chính xác
                            originalCode = originalCode.replaceAll("package\\s+[^;]+;", correctPackage);
                        }

                        createCloneFile(destinationDirPath, fileName);
                        writeDataToFile(originalCode, destinationDirPath + "/" + fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (existJavaFile) {
            command.append(destinationDirPath).append("/*.java ");
        }
    }


    private static File[] getFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);

        if (!directory.isDirectory()) {
            throw new RuntimeException("Invalid Dir: " + directory.getPath());
        }

        return directory.listFiles();
    }

    public static void deleteFilesInDirectory(String directoryPath) throws IOException {
        if (Files.exists(Path.of(directoryPath))) {
            FileUtils.cleanDirectory(new File(directoryPath));
        } else {
            FileUtils.forceMkdir(new File(directoryPath));
        }
    }

    public static void createCloneDirectory(String parent, String child) {
        File newDirectory = new File(parent, child);

        boolean created = newDirectory.mkdir();

        if (!created) {
            System.out.println("Existed Dir");
        }
    }

    private static void createCloneFile(String directoryPath, String fileName) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            throw new RuntimeException("Invalid dir");
        }

        File newFile = new File(directory, fileName);

        try {
            boolean created = newFile.createNewFile();

            if (!created) {
                System.out.println("Existed file");
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't create file");
        }
    }

    private static String createCloneSourceCode(CompilationUnit compilationUnit, String destinationDirPath, ASTHelper.Coverage coverage) throws IOException {
        StringBuilder result = new StringBuilder();

        //Packet
        if (compilationUnit.getPackage() != null) {
            result.append("package data.clonedProject.").append(compilationUnit.getPackage().getName().toString()).append(";\n");
        } else {
            result.append(buildPackage(destinationDirPath)).append("\n");
        }

        //Imports
        for (ASTNode iImport : (List<ASTNode>) compilationUnit.imports()) {
            result.append(iImport);
        }
        result.append("import java.io.FileWriter;\n");

        List<ClassData> classDataArr = new ArrayList<>();
        ASTVisitor classVisitor = new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                classDataArr.add(new ClassData(node));
                return true;
            }
        };
        compilationUnit.accept(classVisitor);

        ClassData classData = classDataArr.get(0);

        String modifier = classData.getClassModifier();
        if (modifier.equals("default") || modifier.equals("private")) {
            result.append(classData.getTypeOfClass()).append(" ").append(classData.getClassName());
        } else {
            result.append(modifier).append(" ").append(classData.getTypeOfClass()).append(" ").append(classData.getClassName());
        }

        if (classData.getSuperClassName() != null) {
            result.append(" extends ").append(classData.getSuperClassName());
        }

        if (classData.getSuperInterfaceName() != null) {
            result.append(" implements ");
            List<String> interfaceList = classData.getSuperInterfaceName();
            for (int i = 0; i < interfaceList.size(); i++) {
                result.append(interfaceList.get(i));
                if (i != interfaceList.size() - 1) {
                    result.append(", ");
                }
            }
        }

        result.append(" {\n");

        result.append(classData.getFields());

        result.append("private static void writeDataToFile(String data, String path, boolean append) {\n" +
                "try {\n" +
                "FileWriter writer = new FileWriter(path, append);\n" +
                "writer.write(data);\n" +
                "writer.close();\n" +
                "} catch (Exception e) {\n" +
                "e.printStackTrace();\n" +
                "}\n" +
                "}\n" +
                "private static boolean mark(String statement, boolean isTrueCondition, boolean isFalseCondition, int id) {\n" +
                "StringBuilder markResult = new StringBuilder();\n" +
                "markResult.append(statement).append(\"===\");\n" +
                "markResult.append(isTrueCondition).append(\"===\");\n" +
                "markResult.append(isFalseCondition).append(\"===\");\n" +
                "markResult.append(id).append(\"---end---\");\n" +
                "writeDataToFile(markResult.toString(), \"" + FilePath.concreteExecuteResultPath + "\", true);\n" +
                "if (!isTrueCondition && !isFalseCondition) return true;\n" +
                "return !isFalseCondition;\n" +
                "}\n");

        List<ASTNode> methods = new ArrayList<>();
        ASTVisitor methodsVisitor = new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                for (MethodDeclaration method : node.getMethods()) {
                    methods.add(method);
                }
                return true;
            }
        };
        compilationUnit.accept(methodsVisitor);
        result.append("private static int MAX_RECURSION_DEPTH = ").append(1).append(";\n");
        for (ASTNode astNode : methods) {
            totalFunctionStatement = 0;
            totalFunctionBranch = 0;
            MethodDeclaration methodDeclaration = (MethodDeclaration) astNode;
            firstLine = compilationUnit.getLineNumber(methodDeclaration.getBody().getStartPosition());

            if (!((MethodDeclaration) astNode).isConstructor()) {
                result.append(createCloneMethod(methodDeclaration, coverage));
                // note
                result.append(createTotalFunctionCoverageVariable(methodDeclaration, totalFunctionStatement, CoverageType.STATEMENT));
                result.append(createTotalFunctionCoverageVariable(methodDeclaration, totalFunctionBranch, CoverageType.BRANCH));
            } else {
                result.append(methodDeclaration);
            }
        }

        result.append(createTotalClassStatementVariable(classData));

        result.append("}");

        return result.toString();
    }

    private static String createTotalFunctionCoverageVariable(MethodDeclaration methodDeclaration, int totalStatement, CoverageType coverageType) {
        StringBuilder result = new StringBuilder();
        result.append(methodDeclaration.getReturnType2());
        result.append(methodDeclaration.getName());
        for (int i = 0; i < methodDeclaration.parameters().size(); i++) {
            result.append(methodDeclaration.parameters().get(i));
        }
        if (coverageType == CoverageType.STATEMENT) {
            result.append("TotalStatement");
        } else if (coverageType == CoverageType.BRANCH) {
            result.append("TotalBranch");
        } else {
            throw new RuntimeException("Invalid Coverage");
        }
        return "public static final int ".concat(reformatVariableName(result.toString())).concat(" = " + totalStatement + ";\n");
    }

    private static String reformatVariableName(String name) {
        return name.replace(" ", "").replace(".", "")
                .replace("[", "").replace("]", "")
                .replace("<", "").replace(">", "")
                .replace(",", "");
    }

    private static String createTotalClassStatementVariable(ClassData classData) {
        StringBuilder result = new StringBuilder();
        result.append(classData.getClassName()).append("TotalStatement");
        return "public static final int ".concat(reformatVariableName(result.toString())).concat(" = " + totalClassStatement + ";\n");
    }

    private static String createCloneMethod(MethodDeclaration method, ASTHelper.Coverage coverage) {
        StringBuilder cloneMethod = new StringBuilder();
        List<ASTNode> modifiers = method.modifiers();
        for (ASTNode modifier : modifiers) {
            if (modifier.toString().equals("private")) {
                cloneMethod.append("public").append(" ");
                continue;
            }
            cloneMethod.append(modifier).append(" ");
        }

        cloneMethod.append(method.getReturnType2() != null ? method.getReturnType2() : "").append(" ").append(method.getName()).append("(");
        List<ASTNode> parameters = method.parameters();
        for (int i = 0; i < parameters.size(); i++) {
            cloneMethod.append(parameters.get(i));
            if (i != parameters.size() - 1) cloneMethod.append(", ");
        }
        cloneMethod.append(") {\n");
        cloneMethod.append("if (MAX_RECURSION_DEPTH <= 0) {\n")
                .append("System.out.println(\"Recursion depth exceeded. Returning default value.\");\n")
                .append("return ").append(getDefaultValue(method.getReturnType2())).append(";\n")
                .append("}\n");
        cloneMethod.append("MAX_RECURSION_DEPTH--;\n");
        cloneMethod.append(generateCodeForBlock(method.getBody(), coverage)).append("\n");

        cloneMethod.append("}\n");
        return cloneMethod.toString();
    }

    private static String generateCodeForOneStatement(ASTNode statement, String markMethodSeparator, ASTHelper.Coverage coverage) {
        if (statement == null) {
            return "";
        }

        if (statement instanceof Block) {
            return generateCodeForBlock((Block) statement, coverage);
        } else if (statement instanceof IfStatement) {
            return generateCodeForIfStatement((IfStatement) statement, coverage);
        } else if (statement instanceof ForStatement) {
            return generateCodeForForStatement((ForStatement) statement, coverage);
        } else if (statement instanceof WhileStatement) {
            return generateCodeForWhileStatement((WhileStatement) statement, coverage);
        } else if (statement instanceof DoStatement) {
            return generateCodeForDoStatement((DoStatement) statement, coverage);
        } else if (statement instanceof EnhancedForStatement) {
            return generateCodeForForEachStatement((EnhancedForStatement) statement, coverage);
        }

        // Xử lý tách toán tử 3 ngôi (Ternary)
        if (statement instanceof ReturnStatement ||
                statement instanceof ExpressionStatement ||
                statement instanceof VariableDeclarationStatement) {

            ASTNode converted = convertTernaryToIf(statement);
            // Nếu có sự thay đổi (đã tách thành công), gọi đệ quy cho node mới
            if (converted != statement) {
                return generateCodeForOneStatement(converted, markMethodSeparator, coverage);
            }
        }

        return generateCodeForNormalStatement(statement, markMethodSeparator);
    }

    private static String generateCodeForBlock(Block block, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        result.append("{\n");
        if (block != null) {
            List<ASTNode> statements = block.statements();
            for (int i = 0; i < statements.size(); i++) {
                result.append(generateCodeForOneStatement(statements.get(i), ";", coverage));
            }
        }
        result.append("}\n");

        return result.toString();
    }

    private static String generateCodeForIfStatement(IfStatement ifStatement, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        result.append("if (").append(generateCodeForCondition(ifStatement.getExpression(), coverage)).append(")\n");
        result.append("{\n");
        result.append(generateCodeForOneStatement(ifStatement.getThenStatement(), ";", coverage));
        result.append("}\n");


        String elseCode = generateCodeForOneStatement(ifStatement.getElseStatement(), ";", coverage);
        if (!elseCode.equals("")) {
            result.append("else {\n").append(elseCode).append("}\n");
        }

        return result.toString();
    }

    private static String generateCodeForForEachStatement(EnhancedForStatement forEachStatement, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        result.append("for (");
        result.append(forEachStatement.getParameter());
        result.append(" : ");
        result.append(forEachStatement.getExpression());
        result.append(") {\n");

        // Lấy dòng code để mark
        int lineNumber = 0;
        if (classCompilationUnit != null && forEachStatement.getStartPosition() != -1) {
            lineNumber = classCompilationUnit.getLineNumber(forEachStatement.getStartPosition()) - firstLine;
        }

        // Tạo mark cho việc vào vòng lặp
        String exprStr = escapeString(forEachStatement.getExpression().toString());
        result.append("mark(\"").append(exprStr).append("\", true, false, ").append(lineNumber).append(");\n");
        totalFunctionStatement++;
        totalClassStatement++;
        totalFunctionBranch++; // Đếm nhánh True

        result.append(generateCodeForOneStatement(forEachStatement.getBody(), ";", coverage));

        result.append("}\n");

        return result.toString();
    }

    private static String generateCodeForForStatement(ForStatement forStatement, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        // Initializers
        List<ASTNode> initializers = forStatement.initializers();
        for (ASTNode initializer : initializers) {
            result.append(generateCodeForMarkMethod(initializer, ";"));
        }
        result.append("for (");
        for (int i = 0; i < initializers.size(); i++) {
            result.append(initializers.get(i));
            if (i != initializers.size() - 1) result.append(", ");
        }

        // Condition
        result.append("; ");
        if (forStatement.getExpression() != null) {
            result.append(generateCodeForCondition(forStatement.getExpression(), coverage));
        }

        // Updaters
        result.append("; ");
        List<ASTNode> updaters = forStatement.updaters();
        for (int i = 0; i < updaters.size(); i++) {
            result.append(generateCodeForOneStatement(updaters.get(i), ",", coverage));
            if (i != updaters.size() - 1) result.append(", ");
        }

        // Body
        result.append(") {\n");
        result.append(generateCodeForOneStatement(forStatement.getBody(), ";", coverage));
        result.append("}\n");

        return result.toString();
    }

    private static String generateCodeForWhileStatement(WhileStatement whileStatement, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        // Condition
        result.append("while (");
        result.append(generateCodeForCondition(whileStatement.getExpression(), coverage));
        result.append(") {\n");

        result.append(generateCodeForOneStatement(whileStatement.getBody(), ";", coverage));
        result.append("}\n");

        return result.toString();
    }

    private static String generateCodeForDoStatement(DoStatement doStatement, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        // Do body
        result.append("do {");
        result.append(generateCodeForOneStatement(doStatement.getBody(), ";", coverage));
        result.append("}\n");

        // Condition
        result.append("while (");
        result.append(generateCodeForCondition(doStatement.getExpression(), coverage));
        result.append(");\n");

        return result.toString();
    }

    private static String generateCodeForNormalStatement(ASTNode statement, String markMethodSeparator) {
        StringBuilder result = new StringBuilder();

        result.append(generateCodeForMarkMethod(statement, markMethodSeparator));
        result.append(statement);

        return result.toString();
    }

    private static String generateCodeForMarkMethod(ASTNode statement, String markMethodSeparator) {
        StringBuilder result = new StringBuilder();

        String newStatement = escapeString(statement.toString());

        // Kiểm tra xem node có vị trí thực trong source file không
        int lineNumber = 0;
        int startPos = statement.getStartPosition();

        if (startPos != -1 && classCompilationUnit != null) {
            // Node thật -> Lấy line number từ compilation unit
            lineNumber = classCompilationUnit.getLineNumber(startPos) - firstLine;
        } else {
            lineNumber = 0;
        }

        result.append("mark(\"").append(newStatement).
                append("\", false, false, ").append(lineNumber).append(')')
                .append(markMethodSeparator).append("\n");
        totalFunctionStatement++;
        totalClassStatement++;

        return result.toString();
    }

    private static String generateCodeForCondition(Expression condition, ASTHelper.Coverage coverage) {
        if (coverage == ASTHelper.Coverage.MCDC) {
            return generateCodeForConditionForMCDCCoverage(condition);
        } else if (coverage == ASTHelper.Coverage.BRANCH || coverage == ASTHelper.Coverage.STATEMENT) {
            return generateCodeForConditionForBranchAndStatementCoverage(condition);
        } else {
            throw new RuntimeException("Invalid coverage!");
        }
    }

    private static String generateCodeForConditionForBranchAndStatementCoverage(Expression condition) {
        totalFunctionStatement++;
        totalClassStatement++;
        totalFunctionBranch += 2;

        int lineNumber = 0;
        int startPos = condition.getStartPosition();
        if (startPos != -1 && classCompilationUnit != null) {
            lineNumber = classCompilationUnit.getLineNumber(startPos) - firstLine;
        }

        String escapedCondition = escapeString(condition.toString());
        return "((" + condition + ") && mark(\"" + escapedCondition + "\", true, false, " + lineNumber + "))" +
                " || mark(\"" + escapedCondition + "\", false, true, " + lineNumber + ")";
    }

    private static String escapeString(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String generateCodeForConditionForMCDCCoverage(Expression condition) {
        StringBuilder result = new StringBuilder();

        if (condition instanceof InfixExpression && isSeparableOperator(((InfixExpression) condition).getOperator())) {
            InfixExpression infixCondition = (InfixExpression) condition;

            result.append("(").append(generateCodeForConditionForMCDCCoverage(infixCondition.getLeftOperand())).
                    append(") ").append(infixCondition.getOperator()).append(" (");
            result.append(generateCodeForConditionForMCDCCoverage(infixCondition.getRightOperand())).append(")");

            List<ASTNode> extendedOperands = infixCondition.extendedOperands();
            for (ASTNode operand : extendedOperands) {
                result.append(" ").append(infixCondition.getOperator()).append(" ");
                result.append("(").append(generateCodeForConditionForMCDCCoverage((Expression) operand)).append(")");
            }
        } else {
            totalFunctionStatement++;
            totalClassStatement++;
            totalFunctionBranch += 2;

            int lineNumber = 0;
            int startPos = condition.getStartPosition();
            if (startPos != -1 && classCompilationUnit != null) {
                lineNumber = classCompilationUnit.getLineNumber(startPos) - firstLine;
            }

            String escapedCondition = escapeString(condition.toString());
            result.append("((").append(condition).append(") && mark(\"").append(escapedCondition).
                    append("\", true, false, ").append(lineNumber).append("))");
            result.append(" || mark(\"").append(escapedCondition).append("\", false, true, ").
                    append(lineNumber).append(")");
            }


        return result.toString();
    }


    private static boolean isSeparableOperator(InfixExpression.Operator operator) {
        return operator.equals(InfixExpression.Operator.CONDITIONAL_OR) ||
                operator.equals(InfixExpression.Operator.OR) ||
                operator.equals(InfixExpression.Operator.CONDITIONAL_AND) ||
                operator.equals(InfixExpression.Operator.AND);
    }

    private static void writeDataToFile(String data, String path) {
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(data + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readPackageDecl(Path file) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("//") || line.startsWith("/*") || line.isEmpty()) continue;
                if (line.startsWith("package ")) {
                    int semi = line.indexOf(';');
                    if (semi > 0) {
                        return line.substring("package ".length(), semi).trim();
                    }
                }
                if (line.startsWith("class ") || line.startsWith("interface ")
                        || line.startsWith("enum ") || line.startsWith("@interface ")) {
                    return null;
                }
            }
        }
        return null;
    }

    private static Path commonPrefix(Path a, Path b) {
        a = a.toAbsolutePath().normalize();
        b = b.toAbsolutePath().normalize();

        if (a.getRoot() == null || b.getRoot() == null || !Objects.equals(a.getRoot(), b.getRoot()))
            return null;

        int n = Math.min(a.getNameCount(), b.getNameCount());
        Path res = a.getRoot();
        for (int i = 0; i < n; i++) {
            if (!a.getName(i).equals(b.getName(i))) break;
            res = res.resolve(a.getName(i).toString());
        }
        return res;
    }

    public static String buildPackage(String sourceDir) {
        String normalized = sourceDir.replace("\\", "/");
        String[] parts = normalized.split("/");
        int idx = -1;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("clonedProject")) {
                idx = i;
                break;
            }
        }
        StringBuilder sb = new StringBuilder("package data.clonedProject");
        for (int i = idx + 1; i < parts.length; i++) {
            sb.append('.').append(parts[i]);
        }
        sb.append(';');
        return sb.toString();
    }

    private static String getDefaultValue(Type returnType) {
        if (returnType.isPrimitiveType()) {
            PrimitiveType primitiveType = (PrimitiveType) returnType;
            switch (primitiveType.getPrimitiveTypeCode().toString()) {
                case "boolean":
                    return "false";
                case "char":
                    return "'" + File.separator + "0'";
                case "byte":
                    return "0";
                case "short":
                    return "0";
                case "int":
                    return "0";
                case "long":
                    return "0";
                case "float":
                    return "0.0f";
                case "double":
                    return "0";
                case "void":
                    return "";
                default:
                    throw new IllegalArgumentException("Unknown primitive type");
            }
        }
        return "null";
    }
}