package core.testDriver;

import core.FilePath;
import core.cfg.utils.ASTHelper;
import core.symbolicExecution.SymbolicExecutionRewrite;
import core.testGeneration.TestGeneration;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class TestDriverGenerator {

    private static String markMethodUtility =
            "private static boolean mark(String statement, boolean isTrueCondition, boolean isFalseCondition) {\n" +
                    "StringBuilder markResult = new StringBuilder();\n" +
                    "markResult.append(statement).append(\"===\");\n" +
                    "markResult.append(isTrueCondition).append(\"===\");\n" +
                    "markResult.append(isFalseCondition).append(\"---end---\");\n" +
                    "writeDataToFile(markResult.toString(), \"" + FilePath.concreteExecuteResultPath + "\", true);\n" +
                    "if (!isTrueCondition && !isFalseCondition) return true;\n" +
                    "return !isFalseCondition;\n" +
                    "}\n";

    private static String writeDataToFileUtility =
            "private static void writeDataToFile(String data, String path, boolean append) {\n" +
                    "try {\n" +
                    "FileWriter writer = new FileWriter(path, append);\n" +
                    "writer.write(data);\n" +
                    "writer.close();\n" +
                    "} catch(Exception e) {\n" +
                    "e.printStackTrace();\n" +
                    "}\n" +
                    "}\n";

    private TestDriverGenerator() {
    }

    public static String generateTestDriverNew(MethodDeclaration method, Object[] testData, ASTHelper.Coverage coverage, String fullyClonedClassName, String simpleClassName) {
        StringBuilder result = new StringBuilder();

        String path = fullyClonedClassName.contains(".") ? fullyClonedClassName.substring(0, fullyClonedClassName.lastIndexOf('.')) : fullyClonedClassName;
        result.append("package ").append(path).append(";\n");
        result.append("import java.io.FileWriter;\n");
        result.append("import org.mockito.MockedStatic;\n");
        result.append("import org.mockito.Mockito;\n");
        result.append("import static org.mockito.ArgumentMatchers.any;\n");
        result.append("public class TestDriver {\n");
        result.append(markMethodUtility);
        result.append(writeDataToFileUtility);
        result.append(newGenerateTestRunner(method, testData, simpleClassName));
        result.append("}");

        return result.toString();
    }

    public static String generateTestDriver(MethodDeclaration method, Object[] testData, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        result.append(generatePreSetup());

        result.append("public class TestDriver {\n");
        result.append(generateUtilities(method, coverage));
        result.append(generateTestRunner(method.getName().toString(), testData));
        result.append("}");

        return result.toString();
    }

    private static String generatePreSetup() {
        StringBuilder result = new StringBuilder();
        result.append("package data.testDriverData;\n");
        result.append("import java.io.FileWriter;\n");
        return result.toString();
    }

    private static String generateTestRunner(String methodName, Object[] testData) {
        StringBuilder result = new StringBuilder();
        result.append("public static void main(String[] args) {\n");
        result.append("writeDataToFile(\"\", \"" + FilePath.concreteExecuteResultPath + "\", false);\n");
        result.append("long startRunTestTime = System.nanoTime();\n");
        result.append("Object output = ").append(methodName).append("(");
        for (int i = 0; i < testData.length; i++) {
            if (testData[i] instanceof Character) {
                result.append("'").append(testData[i]).append("'");
            } else {
                result.append(testData[i]);
            }
            if (i != testData.length - 1) result.append(", ");
        }
        result.append(");\n");
        result.append("long endRunTestTime = System.nanoTime();\n");
        result.append("double runTestDuration = (endRunTestTime - startRunTestTime) / 1000000.0;\n");
        result.append("writeDataToFile(runTestDuration + \"===\" + output, \"" + FilePath.concreteExecuteResultPath + "\", true);\n");
        result.append("}\n");
        return result.toString();
    }

    private static String newGenerateTestRunner(MethodDeclaration method, Object[] testData, String simpleClassName) {
        StringBuilder result = new StringBuilder();
        result.append("public static void main(String[] args) {\n");
        result.append("writeDataToFile(\"\", \"" + FilePath.concreteExecuteResultPath + "\", false);\n");
        result.append("long startRunTestTime = System.nanoTime();\n");
        result.append("Object output = null;\n");

        // Bắt đầu khối kiểm thử cô lập
        result.append("try {\n");

        List<ASTNode> modifiers = method.modifiers();
        boolean isStatic = false;
        for (ASTNode modifier : modifiers) {
            if (modifier.toString().equals("static")) {
                isStatic = true;
                break;
            }
        }

        // Cô lập đơn vị kiểm thử bằng cách đóng băng các lệnh gọi ra bên ngoài
        List<SymbolicExecutionRewrite.MockInfo> mocks = SymbolicExecutionRewrite.currentMockInfos;
        java.util.Set<String> processedClasses = new java.util.HashSet<>();
        int tryBlockCount = 0;

        for (int i = 0; i < mocks.size(); i++) {
            SymbolicExecutionRewrite.MockInfo mock = mocks.get(i);
            String className = mock.className;

            if (className == null || className.isEmpty()) continue;

            // Sử dụng HashSet để tránh khởi tạo trùng lặp MockedStatic cho cùng một lớp,
            if (!processedClasses.contains(className)) {
                processedClasses.add(className);
                String mockVarName = "mocked" + className;

                result.append("try (org.mockito.MockedStatic<").append(className).append("> ").append(mockVarName)
                        .append(" = org.mockito.Mockito.mockStatic(").append(className).append(".class, org.mockito.Mockito.CALLS_REAL_METHODS)) {\n");
                tryBlockCount++; // Bộ đếm theo dõi số lượng tài nguyên cần giải phóng

                // Duyệt qua tất cả các hàm thuộc lớp hiện tại để thiết lập cấu hình trả về
                for (int j = 0; j < mocks.size(); j++) {
                    SymbolicExecutionRewrite.MockInfo innerMock = mocks.get(j);
                    if (innerMock.className.equals(className)) {

                        // Trích xuất giá trị cho biến Mock từ mảng nghiệm do bộ giải Z3 trả về
                        String valueAsString = "0"; // Giá trị mặc định dự phòng
                        int z3Index = method.parameters().size() + j;
                        if (testData != null && z3Index < testData.length) {
                            Object value = testData[z3Index];
                            if (value != null) valueAsString = String.valueOf(value);
                        }

                        if (innerMock.solveValue != null) {
                            valueAsString = String.valueOf(innerMock.solveValue);
                        }

                        // Cắm lệnh thenReturn cho từng hàm
                        result.append("    ").append(mockVarName).append(".when(() -> ")
                                .append(className).append(".").append(innerMock.methodName)
                                .append("(org.mockito.Mockito.anyInt())).thenReturn(").append(valueAsString).append(");\n");
                    }
                }
            }
        }

        // Gọi phương thức mục tiêu với bộ tham số thực từ Z3.
        if (isStatic) {
            result.append("    output = ").append(simpleClassName).append(".");
        } else {
            result.append("    output = new ").append(simpleClassName).append("().");
        }
        result.append(method.getName().toString()).append("(");

        int actualParamCount = 0;
        for (Object obj : method.parameters()) {
            org.eclipse.jdt.core.dom.SingleVariableDeclaration param = (org.eclipse.jdt.core.dom.SingleVariableDeclaration) obj;
            String paramName = param.getName().getIdentifier();

            //Nếu tên tham số không chứa chữ "_call_" thì Nó là tham số thật!
            if (!paramName.contains("_call_")) {
                actualParamCount++;
            }
        }
        for (int i = 0; i < actualParamCount; i++) {
            String valueAsString = "0"; // Giá trị khởi tạo mặc định tránh lỗi NullReference

            // Xử lý và ép kiểu dữ liệu từ Object của Z3 sang định dạng mã nguồn Java hợp lệ
            if (testData != null && i < testData.length) {
                Object value = testData[i];
                if (value == null) {
                    valueAsString = "null";
                } else if (value.getClass().isArray()) {
                    if (value instanceof int[]) {
                        valueAsString = "new int[]" + java.util.Arrays.toString((int[]) value).replace('[', '{').replace(']', '}');
                    } else if (value instanceof long[]) {
                        valueAsString = "new long[]" + java.util.Arrays.toString((long[]) value).replace('[', '{').replace(']', '}');
                    } else if (value instanceof double[]) {
                        valueAsString = "new double[]" + java.util.Arrays.toString((double[]) value).replace('[', '{').replace(']', '}');
                    } else if (value instanceof float[]) {
                        valueAsString = "new float[]" + java.util.Arrays.toString((float[]) value).replace('[', '{').replace(']', '}');
                    } else if (value instanceof boolean[]) {
                        valueAsString = "new boolean[]" + java.util.Arrays.toString((boolean[]) value).replace('[', '{').replace(']', '}');
                    } else {
                        valueAsString = "null"; // Fallback an toàn
                    }
                } else {
                    // Xử lý các kiểu dữ liệu nguyên thủy và chuỗi
                    valueAsString = String.valueOf(value);
                    if (value instanceof String) valueAsString = "\"" + valueAsString + "\"";
                    else if (value instanceof Long) valueAsString += "L";
                    else if (value instanceof Character) valueAsString = "'" + valueAsString + "'";
                    else if (value instanceof Float) valueAsString += "f";
                    else if (value instanceof Double) {
                        Double dVal = (Double) value;
                        if (Double.isNaN(dVal)) valueAsString = "Double.NaN";
                        else if (Double.isInfinite(dVal))
                            valueAsString = dVal > 0 ? "Double.POSITIVE_INFINITY" : "Double.NEGATIVE_INFINITY";
                    }
                }
            }
            result.append(valueAsString);

            // Bổ sung dấu phân cách tham số
            if (i < actualParamCount - 1) {
                result.append(", ");
            }
        }
        result.append(");\n");

        // Cần đóng toàn bộ các block try-with-resources của Mockito
        for (int i = 0; i < tryBlockCount; i++) {
            result.append("}\n");
        }

        // Đóng khối try-catch tổng quản lý Runtime Exception trong quá trình test
        result.append("} catch (Throwable e) {\n");
        result.append("e.printStackTrace();\n");
        result.append("}\n");

        result.append("long endRunTestTime = System.nanoTime();\n");
        result.append("double runTestDuration = (endRunTestTime - startRunTestTime) / 1000000.0;\n");

        // Ghi nhận kết quả thực thi thô ra tệp tin
        result.append("writeDataToFile(runTestDuration + \"===\" + output, \"" + FilePath.concreteExecuteResultPath + "\", true);\n");

        // Sử dụng Regex để ánh xạ các lời gọi hàm gốc thành các biến Mock tương ứng.
        result.append("try {\n");
        result.append("    java.nio.file.Path tracePath = java.nio.file.Paths.get(\"")
                .append(FilePath.concreteExecuteResultPath.replace("\\", "\\\\")).append("\");\n");
        result.append("    String traceContent = new String(java.nio.file.Files.readAllBytes(tracePath));\n");

        for (SymbolicExecutionRewrite.MockInfo mock : mocks) {
            if (mock.className != null && !mock.className.isEmpty()) {
                // Xây dựng biểu thức chính quy bắt chính xác cú pháp phương thức gốc
                String originalCallRegex = mock.className + "\\\\." + mock.methodName + "\\\\([^)]*\\\\)";

                // Thực hiện ghi đè dữ liệu vết, đánh lừa bộ phân tích CFG
                result.append("    traceContent = traceContent.replaceAll(\"")
                        .append(originalCallRegex).append("\", \"").append(mock.mockVarName).append("\");\n");
            }
        }
        // Lưu trữ lại tệp tin vết đã qua tiền xử lý
        result.append("    java.nio.file.Files.write(tracePath, traceContent.getBytes());\n");
        result.append("} catch (Exception ex) { ex.printStackTrace(); }\n");

        result.append("}\n"); // Kết thúc hàm main của TestDriver
        return result.toString();
    }

    private static String generateUtilities(MethodDeclaration method, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        // Generate mark method
        result.append(markMethodUtility);

        // Generate writeDataToFile method
        result.append(writeDataToFileUtility);

        // Generate inclass variables
        result.append(generateVariables(method));

        // Generate testing method with instruments
        result.append(createCloneMethod(method, coverage));

        // Generate MethodDeclaration form MethodInvocation
        result.append(generateAllMethodDeclarationFromMethodInvocation(method));


        return result.toString();
    }

    private static String generateAllMethodDeclarationFromMethodInvocation(MethodDeclaration methodDeclaration) {
        StringBuilder result = new StringBuilder();
        if (methodDeclaration == null) {
            throw new IllegalArgumentException("MethodDeclaration cannot be null");
        }
        HashSet<MethodDeclaration> methodDeclarations = new HashSet<>();
        methodDeclarations = findAllDistinctMethods(methodDeclaration, methodDeclarations);
        methodDeclarations.remove(methodDeclaration);
        for (MethodDeclaration newMethodDeclaration : methodDeclarations) {
            result.append("\n").append(newMethodDeclaration);
        }
        return result.toString();
    }

    private static String generateVariables(MethodDeclaration method) {
        StringBuilder result = new StringBuilder();
        // Lần ngược lên tới class
        ASTNode parent = method.getParent();
        while (parent != null && !(parent instanceof TypeDeclaration)) {
            parent = parent.getParent();
        }
        if (parent != null) {
            TypeDeclaration classDecl = (TypeDeclaration) parent;
            for (FieldDeclaration field : classDecl.getFields()) {
                String type = field.getType().toString();
                for (Object fragObj : field.fragments()) {
                    VariableDeclarationFragment frag = (VariableDeclarationFragment) fragObj;

                    // Lấy modifier (public/private/...)
                    String modifiers = field.modifiers().toString()
                            .replaceAll("[\\[\\],]", "").trim();

                    // Nếu chưa có static thì thêm vào
                    if (!modifiers.contains("static")) {
                        if (!modifiers.isEmpty()) {
                            modifiers = modifiers + " static";
                        } else {
                            modifiers = "static";
                        }
                    }

                    // Ghép thành một dòng code khai báo
                    String decl = (modifiers.isEmpty() ? "" : modifiers + " ")
                            + type + " " + frag.toString() + ";";

                    result.append(decl).append("\n");
                }
            }
        }
        return result.toString();
    }

    private static MethodDeclaration getInvokeAdMethodST(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            throw new IllegalArgumentException("Method name cannot be null or empty");
        }
        ArrayList<ASTNode> funcAstNodeList = TestGeneration.getFuncAstNodeList();
        for (ASTNode astNode : funcAstNodeList) {
            if (astNode instanceof MethodDeclaration &&
                    ((MethodDeclaration) astNode).getName().getIdentifier().equals(methodName)) {
                return (MethodDeclaration) astNode;
            }
        }
        throw new RuntimeException("There is no method named: " + methodName);
    }

    private static HashSet<MethodDeclaration> findAllDistinctMethods(MethodDeclaration methodDeclaration,
                                                                     HashSet<MethodDeclaration> methodDeclarations) {
        List<MethodInvocation> methodInvocations = new ArrayList<>();
        Block body = methodDeclaration.getBody();
        if (body != null) {
            body.accept(new MethodInvocationVisitor(methodInvocations));
        }
        for (MethodInvocation methodInvocation : methodInvocations) {
            String methodName = methodInvocation.getName().toString();
            MethodDeclaration newMethodDeclaration = getInvokeAdMethodST(methodName);
            if (newMethodDeclaration != null && !methodDeclarations.contains(newMethodDeclaration)) {
                methodDeclarations.add(newMethodDeclaration);
                methodDeclarations = findAllDistinctMethods(newMethodDeclaration, methodDeclarations);
            }
        }
        return methodDeclarations;
    }

    private static String createCloneMethod(MethodDeclaration method, ASTHelper.Coverage coverage) {
        int limit = 1;
        StringBuilder cloneMethod = new StringBuilder();
//        cloneMethod.append(createMethodWithRecurLimit(limit, method, coverage));
        cloneMethod.append("private static int MAX_RECURSION_DEPTH = ").append(limit).append(";\n");
        cloneMethod.append("public static ").append(method.getReturnType2()).append(" ").append(method.getName()).append("(");
        List<ASTNode> parameters = method.parameters();
        for (int i = 0; i < parameters.size(); i++) {
            cloneMethod.append(parameters.get(i));
            if (i != parameters.size() - 1) cloneMethod.append(", ");
        }
        cloneMethod.append(")\n{\n");

//        cloneMethod.append("{\n").append("return ").append(method.getName()).append("(");
//        for (int i = 0; i < parameters.size(); i++) {
//            SingleVariableDeclaration param = (SingleVariableDeclaration) parameters.get(i);
//            cloneMethod.append(param.getName());
//            if (i != parameters.size() - 1) cloneMethod.append(", ");
//        }
//        cloneMethod.append(parameters.isEmpty() ? "" : ", ").append("0);\n");
        cloneMethod.append("if (MAX_RECURSION_DEPTH <= 0) {\n")
                .append("System.out.println(\"Recursion depth exceeded. Returning default value.\");\n")
                .append("return ").append(getDefaultValue(method.getReturnType2())).append(";\n")
                .append("}\n");
        cloneMethod.append("MAX_RECURSION_DEPTH--;\n");

        cloneMethod.append(generateCodeForBlock(method.getBody(), coverage));


        cloneMethod.append("}\n");

        return cloneMethod.toString();
    }

    private static String createMethodWithRecurLimit(int limit, MethodDeclaration method, ASTHelper.Coverage coverage) {
        StringBuilder cloneMethod = new StringBuilder();

        cloneMethod.append("private static final int MAX_RECURSION_DEPTH = ").append(limit).append(";\n");

        cloneMethod.append("public static ").append(method.getReturnType2()).append(" ")
                .append(method.getName()).append("(");
        List<ASTNode> parameters = method.parameters();
        for (int i = 0; i < parameters.size(); i++) {
            cloneMethod.append(parameters.get(i));
            if (i != parameters.size() - 1) cloneMethod.append(", ");
        }
        cloneMethod.append(parameters.isEmpty() ? "" : ", ").append("int currentDepth");
        cloneMethod.append(") {\n");

        cloneMethod.append("if (currentDepth > MAX_RECURSION_DEPTH) {\n")
                .append("System.out.println(\"Recursion depth exceeded. Returning default value.\");\n")
                .append("return ").append(getDefaultValue(method.getReturnType2())).append(";\n")
                .append("}\n");

        cloneMethod.append(generateCodeForBlock(method.getBody(), coverage)
                .replace(method.getName() + "(", method.getName() + "(currentDepth + 1, "));

        cloneMethod.append("}\n");

        return cloneMethod.toString();
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
        return "null"; // Default for non-primitive types
    }

    private static class MethodInvocationVisitor extends ASTVisitor {
        private final List<MethodInvocation> collector;

        public MethodInvocationVisitor(List<MethodInvocation> collector) {
            this.collector = collector;
        }

        @Override
        public boolean visit(MethodInvocation node) {
            if (node.getExpression() == null) {
                collector.add(node);
            }
            return super.visit(node);
        }
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
        } else {
            return generateCodeForNormalStatement(statement, markMethodSeparator);
        }

    }

    private static String generateCodeForBlock(Block block, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();
        List<ASTNode> statements = block.statements();

        result.append("{\n");
        for (int i = 0; i < statements.size(); i++) {
            result.append(generateCodeForOneStatement(statements.get(i), ";", coverage));
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
        result.append(generateCodeForCondition(forStatement.getExpression(), coverage));

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

        String stringStatement = statement.toString();
        StringBuilder newStatement = new StringBuilder();

        // Rewrite Statement for mark method
        for (int i = 0; i < stringStatement.length(); i++) {
            char charAt = stringStatement.charAt(i);

            if (charAt == '\n') {
                newStatement.append("\\n");
                continue;
            } else if (charAt == '"') {
                newStatement.append("\\").append('"');
                continue;
            } else if (i != stringStatement.length() - 1 && charAt == '\\' && stringStatement.charAt(i + 1) == 'n') {
                newStatement.append("\" + \"").append("\\n").append("\" + \"");
                i++;
                continue;
            }

            newStatement.append(charAt);
        }

        result.append("mark(\"").append(newStatement).append("\", false, false)").append(markMethodSeparator).append("\n");

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
        return "((" + condition + ") && mark(\"" + condition + "\", true, false))" +
                " || mark(\"" + condition + "\", false, true)";
    }

    private static String generateCodeForConditionForMCDCCoverage(Expression condition) {
        StringBuilder result = new StringBuilder();

        if (condition instanceof InfixExpression && isSeparableOperator(((InfixExpression) condition).getOperator())) {
            InfixExpression infixCondition = (InfixExpression) condition;

            result.append("(").append(generateCodeForConditionForMCDCCoverage(infixCondition.getLeftOperand())).append(") ").append(infixCondition.getOperator()).append(" (");
            result.append(generateCodeForConditionForMCDCCoverage(infixCondition.getRightOperand())).append(")");

            List<ASTNode> extendedOperands = infixCondition.extendedOperands();
            for (ASTNode operand : extendedOperands) {
                result.append(" ").append(infixCondition.getOperator()).append(" ");
                result.append("(").append(generateCodeForConditionForMCDCCoverage((Expression) operand)).append(")");
            }
        } else {
            result.append(generateCodeForConditionForBranchAndStatementCoverage(condition));
        }

        return result.toString();
    }

    private static boolean isSeparableOperator(InfixExpression.Operator operator) {
        return operator.equals(InfixExpression.Operator.CONDITIONAL_OR) ||
                operator.equals(InfixExpression.Operator.OR) ||
                operator.equals(InfixExpression.Operator.CONDITIONAL_AND) ||
                operator.equals(InfixExpression.Operator.AND);
    }
}
