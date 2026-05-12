package core.testGeneration.LoopTestGeneration;

import core.symbolicExecution.SymbolicExecution;
import core.testGeneration.TestGeneration;
import core.utils.Utils;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

import core.testGeneration.LoopTestGeneration.LoopInfo;

public class LoopAnalyzer {
    private MethodDeclaration method;
    private final List<ASTNode> statements;
    private List<LoopInfo> detectedLoops;
    private Map<String, Integer> stepValues;
    private Class<?>[] parameterClasses;

    public LoopAnalyzer(MethodDeclaration method) {
        this.method = method;
        this.statements = new ArrayList<>();
        Block methodBody = method.getBody();
        if (methodBody != null) {
            for (Object stmt : methodBody.statements()) {
                if (stmt instanceof Statement) {
                    statements.add((ASTNode) stmt);
                }
            }
        }
        this.detectedLoops = new ArrayList<>();
        this.stepValues = new HashMap<>();
    }

    public List<LoopInfo> detectLoops() {
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = (Statement) statements.get(i);
            if (    statement instanceof ForStatement ||
                    statement instanceof WhileStatement ||
                    statement instanceof DoStatement    ) {
                LoopInfo loopInfo = analyzeLoop(statement);
                if (loopInfo != null) {
                    detectedLoops.add(loopInfo);
                }
            }
        }

        return detectedLoops;
    }

    private LoopInfo analyzeLoop(Statement statement) {
        LoopInfo loopInfo = new LoopInfo();
        if (statement instanceof ForStatement) {
            loopInfo = analyzeForLoop((ForStatement) statement);
        } else if (statement instanceof WhileStatement) {
            loopInfo = analyzeWhileLoop((WhileStatement) statement);
        } else if (statement instanceof DoStatement) {
            loopInfo = analyzeDoWhileLoop((DoStatement) statement);
        } else {
            return null; // Not a recognized loop type
        }
        return loopInfo;
    }

    private LoopInfo analyzeForLoop(ForStatement statement) {
        LoopInfo loopInfo = new LoopInfo();
        loopInfo.setLoopStatement(statement);

        Expression condition = statement.getExpression();
        loopInfo.setConditionExpr(condition);

        String iteratorVar = getIteratorVariable(condition);
        loopInfo.setIteratorVariable(iteratorVar);

        // Tìm cả tham số bắt đầu và kết thúc của vòng lặp
        if (condition instanceof InfixExpression) {
            InfixExpression infixExpr = (InfixExpression) condition;
            Expression leftOp = infixExpr.getLeftOperand();
            Expression rightOp = infixExpr.getRightOperand();

            // Kiểm tra xem bên phải có phải là tham số không
            if (rightOp instanceof SimpleName) {
                String rightVarName = ((SimpleName) rightOp).getIdentifier();

                // Kiểm tra xem đây có phải tham số của phương thức không
                for (Object param : method.parameters()) {
                    SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
                    if (parameter.getName().getIdentifier().equals(rightVarName)) {
                        loopInfo.setLoopParameter(rightVarName); // Tham số kết thúc
                        break;
                    }
                }
            }
        }

        String startParam = findLoopStartParameter(statement, iteratorVar);
        if (startParam != null) {
            loopInfo.setStartParameter(startParam);
        }

        int startValue = determineStartValue(statement, iteratorVar);
        loopInfo.setStartValue(startValue);

        int stepValue = determineStepValue(statement, iteratorVar);
        loopInfo.setStepValue(stepValue);

        int endValue = determineEndValue(condition);
        loopInfo.setEndValue(endValue);

        int maxIterations = calculateMaxIterations(startValue, endValue, stepValue); // Giá trị mặc định an toàn
        loopInfo.setMaxIterations(maxIterations);

        return loopInfo;
    }

    private String findLoopStartParameter(ForStatement statement, String iteratorVar) {
        for (Object initializer : statement.initializers()) {
            if (initializer instanceof VariableDeclarationExpression) {
                VariableDeclarationExpression varDecl = (VariableDeclarationExpression) initializer;
                for (Object fragment : varDecl.fragments()) {
                    VariableDeclarationFragment varFragment = (VariableDeclarationFragment) fragment;
                    if (varFragment.getName().getIdentifier().equals(iteratorVar)) {
                        Expression initializerExpr = varFragment.getInitializer();
                        if (initializerExpr instanceof SimpleName) {
                            String paramName = ((SimpleName) initializerExpr).getIdentifier();
                            for (Object param : method.parameters()) {
                                SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
                                if (parameter.getName().getIdentifier().equals(paramName)) {
                                    return paramName; // Tham số bắt đầu
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private int determineStartValue(ForStatement statement, String iteratorVar) {
        for (Object initializer : statement.initializers()) {
            if (initializer instanceof VariableDeclarationExpression) {
                VariableDeclarationExpression varDecl = (VariableDeclarationExpression) initializer;
                for (Object fragment : varDecl.fragments()) {
                    VariableDeclarationFragment varFragment = (VariableDeclarationFragment) fragment;
                    if (varFragment.getName().getIdentifier().equals(iteratorVar)) {
                        Expression initializerExpr = varFragment.getInitializer();
                            if (initializerExpr instanceof NumberLiteral) {
                                return Integer.parseInt(initializerExpr.toString());
                            }
                    }
                }
            }
        }

        return 1; // Giá trị mặc định nếu không tìm thấy
    }

    private int determineStepValue(ForStatement statement, String iteratorVar) {
        int result = 1;
        for (Object updater : statement.updaters()) {
            Expression updaterExpr = (Expression) updater;
            Map.Entry<String, Integer> stepEntry = extractStepValue(updaterExpr);
            if (stepEntry != null) {
                stepValues.put(stepEntry.getKey(), stepEntry.getValue());
                if (stepEntry.getKey().equals(iteratorVar)) {
                    result = stepEntry.getValue();
                }
            }
        }
        return result;
    }

    private LoopInfo analyzeWhileLoop(WhileStatement statement) {
        LoopInfo loopInfo = new LoopInfo();
        loopInfo.setLoopStatement(statement);

        Expression condition = statement.getExpression();
        loopInfo.setConditionExpr(condition);

        String iteratorVar = getIteratorVariable(condition);
        loopInfo.setIteratorVariable(iteratorVar);

        // Tìm cả tham số bắt đầu và kết thúc của vòng lặp
        if (condition instanceof InfixExpression) {
            InfixExpression infixExpr = (InfixExpression) condition;
            Expression leftOp = infixExpr.getLeftOperand();
            Expression rightOp = infixExpr.getRightOperand();

            // Kiểm tra xem bên phải có phải là tham số không
            if (rightOp instanceof SimpleName) {
                String rightVarName = ((SimpleName) rightOp).getIdentifier();

                // Kiểm tra xem đây có phải tham số của phương thức không
                for (Object param : method.parameters()) {
                    SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
                    if (parameter.getName().getIdentifier().equals(rightVarName)) {
                        loopInfo.setLoopParameter(rightVarName); // Tham số kết thúc
                        break;
                    }
                }
            }
        }

        int startValue = determineStartValue(statement, iteratorVar);
        loopInfo.setStartValue(startValue);

        int stepValue = determineStepValue(statement, iteratorVar);
        loopInfo.setStepValue(stepValue);

        int endValue = determineEndValue(condition);
        loopInfo.setEndValue(endValue);

        int maxIterations = calculateMaxIterations(startValue, endValue, stepValue); // Giá trị mặc định an toàn
        loopInfo.setMaxIterations(maxIterations);

        return loopInfo;
    }

    private int determineStartValue(WhileStatement statement, String iteratorVar) {
        for (int i = 0; i < statements.size(); i++) {
            Statement stmt = (Statement) statements.get(i);
            if (stmt instanceof VariableDeclarationStatement) {
                VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) stmt;
                for (Object fragment : varDeclStmt.fragments()) {
                    if (fragment instanceof VariableDeclarationFragment) {
                        VariableDeclarationFragment varFragment = (VariableDeclarationFragment) fragment;
                        if (varFragment.getName().getIdentifier().equals(iteratorVar)) {
                            Expression initializer = varFragment.getInitializer();
                            if (initializer instanceof NumberLiteral) {
                                return Integer.parseInt(initializer.toString());
                            }
                        }
                    }
                }
            }
        }
        return 1;
    }

    private int determineStepValue(WhileStatement statement, String iteratorVar) {
        int result = 1;
        Statement body = statement.getBody();
        if (body instanceof Block) {
            Block block = (Block) body;
            for (Object stmt : block.statements()) {
                if (stmt instanceof ExpressionStatement) {
                    ExpressionStatement exprStmt = (ExpressionStatement) stmt;
                    Expression expr = exprStmt.getExpression();
                    if (isModifyingVariable(expr, iteratorVar)) {
                        Map.Entry<String, Integer> stepEntry = extractStepValue(expr);
                        if (stepEntry != null) {
                            stepValues.put(stepEntry.getKey(), stepEntry.getValue());
                            if (stepEntry.getKey().equals(iteratorVar)) {
                                return stepEntry.getValue();
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private LoopInfo analyzeDoWhileLoop(DoStatement statement) {
        LoopInfo loopInfo = new LoopInfo();
        loopInfo.setLoopStatement(statement);

        Expression condition = statement.getExpression();
        loopInfo.setConditionExpr(condition);

        String iteratorVar = getIteratorVariable(condition);
        loopInfo.setIteratorVariable(iteratorVar);

        // Tìm cả tham số bắt đầu và kết thúc của vòng lặp
        if (condition instanceof InfixExpression) {
            InfixExpression infixExpr = (InfixExpression) condition;
            Expression leftOp = infixExpr.getLeftOperand();
            Expression rightOp = infixExpr.getRightOperand();

            // Kiểm tra xem bên phải có phải là tham số không
            if (rightOp instanceof SimpleName) {
                String rightVarName = ((SimpleName) rightOp).getIdentifier();

                // Kiểm tra xem đây có phải tham số của phương thức không
                for (Object param : method.parameters()) {
                    SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
                    if (parameter.getName().getIdentifier().equals(rightVarName)) {
                        loopInfo.setLoopParameter(rightVarName); // Tham số kết thúc
                        break;
                    }
                }
            }
        }

        int startValue = determineStartValue(statement, iteratorVar);
        loopInfo.setStartValue(startValue);

        int stepValue = determineStepValue(statement, iteratorVar);
        loopInfo.setStepValue(stepValue);

        int endValue = determineEndValue(condition);
        loopInfo.setEndValue(endValue);

        int maxIterations = calculateMaxIterations(startValue, endValue, stepValue); // Giá trị mặc định an toàn
        loopInfo.setMaxIterations(maxIterations);

        return loopInfo;
    }

    private int determineStartValue(DoStatement statement, String iteratorVar) {
        for (int i = 0; i < statements.size(); i++) {
            Statement stmt = (Statement) statements.get(i);
            if (stmt instanceof VariableDeclarationStatement) {
                VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) stmt;
                for (Object fragment : varDeclStmt.fragments()) {
                    if (fragment instanceof VariableDeclarationFragment) {
                        VariableDeclarationFragment varFragment = (VariableDeclarationFragment) fragment;
                        if (varFragment.getName().getIdentifier().equals(iteratorVar)) {
                            Expression initializer = varFragment.getInitializer();
                            if (initializer instanceof NumberLiteral) {
                                return Integer.parseInt(initializer.toString());
                            }
                        }
                    }
                }
            }
        }
        return 1;
    }

    private int determineStepValue(DoStatement statement, String iteratorVar) {
        int result = 1;
        Statement body = statement.getBody();
        if (body instanceof Block) {
            Block block = (Block) body;
            for (Object stmt : block.statements()) {
                if (stmt instanceof ExpressionStatement) {
                    ExpressionStatement exprStmt = (ExpressionStatement) stmt;
                    Expression expr = exprStmt.getExpression();
                    if (isModifyingVariable(expr, iteratorVar)) {
                        Map.Entry<String, Integer> stepEntry = extractStepValue(expr);
                        if (stepEntry != null) {
                            stepValues.put(stepEntry.getKey(), stepEntry.getValue());
                            if (stepEntry.getKey().equals(iteratorVar)) {
                                return stepEntry.getValue();
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private String getIteratorVariable(Expression condition) {
        if (condition instanceof InfixExpression) {
            InfixExpression infixExpr = (InfixExpression) condition;
            Expression leftOp = infixExpr.getLeftOperand();

            if (leftOp instanceof SimpleName) {
                return ((SimpleName) leftOp).getIdentifier();
            }
        }
        return null;
    }

    private Map.Entry<String, Integer> extractStepValue(Expression updaterExpr) {
        if (updaterExpr instanceof PostfixExpression) {
            PostfixExpression postfixExpr = (PostfixExpression) updaterExpr;
            String operand = postfixExpr.getOperand().toString();
            if (postfixExpr.getOperator() == PostfixExpression.Operator.INCREMENT) {
                return Map.entry(operand, 1);
            } else if (postfixExpr.getOperator() == PostfixExpression.Operator.DECREMENT) {
                return Map.entry(operand, -1);
            }
        } else if (updaterExpr instanceof PrefixExpression) {
            PrefixExpression prefixExpr = (PrefixExpression) updaterExpr;
            String operand = prefixExpr.getOperand().toString();
            if (prefixExpr.getOperator() == PrefixExpression.Operator.INCREMENT) {
                return Map.entry(operand, 1);
            } else if (prefixExpr.getOperator() == PrefixExpression.Operator.DECREMENT) {
                return Map.entry(operand, -1);
            }
        } else if (updaterExpr instanceof Assignment) {
            Assignment assignment = (Assignment) updaterExpr;
            String leftOperand = assignment.getLeftHandSide().toString();
            Expression rightSide = assignment.getRightHandSide();
            if (rightSide instanceof NumberLiteral) {
                int value = Integer.parseInt(rightSide.toString());
                if (assignment.getOperator() == Assignment.Operator.ASSIGN) {
                    // For direct assignment like i = 5, we need to compare with previous value
                    // But as a simple approximation, we assume increment by 1
                    return Map.entry(leftOperand, 1);
                } else if (assignment.getOperator() == Assignment.Operator.PLUS_ASSIGN) {
                    return Map.entry(leftOperand, value);
                } else if (assignment.getOperator() == Assignment.Operator.MINUS_ASSIGN) {
                    return Map.entry(leftOperand, -value);
                }
            } else if (rightSide instanceof InfixExpression) {
                InfixExpression infixExpr = (InfixExpression) rightSide;
                Expression leftOp = infixExpr.getLeftOperand();
                Expression rightOp = infixExpr.getRightOperand();

                // Handle i = i + 1 pattern
                if (assignment.getOperator() == Assignment.Operator.ASSIGN &&
                        leftOp.toString().equals(assignment.getLeftHandSide().toString()) &&
                        rightOp instanceof NumberLiteral) {

                    int value = Integer.parseInt(rightOp.toString());

                    if (infixExpr.getOperator() == InfixExpression.Operator.PLUS) {
                        return Map.entry(leftOperand, value);
                    } else if (infixExpr.getOperator() == InfixExpression.Operator.MINUS) {
                        return Map.entry(leftOperand, -value);
                    }
                }
                // Handle i = i * 2 or other non-linear updates (approximate)
                if (rightOp instanceof NumberLiteral) {
                    try {
                        return Map.entry(leftOperand, Integer.parseInt(rightOp.toString()));
                    } catch (NumberFormatException e) {
                        return Map.entry(leftOperand, 1);
                    }
                }

            }
        }
        return null; // Không tìm thấy giá trị bước
    }

    private boolean isModifyingVariable(Expression expr, String varName) {
        if (expr instanceof PostfixExpression) {
            PostfixExpression postfixExpr = (PostfixExpression) expr;
            return postfixExpr.getOperand().toString().equals(varName);
        } else if (expr instanceof PrefixExpression) {
            PrefixExpression prefixExpr = (PrefixExpression) expr;
            return prefixExpr.getOperand().toString().equals(varName);
        } else if (expr instanceof Assignment) {
            Assignment assignment = (Assignment) expr;
            return assignment.getLeftHandSide().toString().equals(varName);
        }
        return false;
    }

    private int determineEndValue(Expression condition) {
        if (condition instanceof InfixExpression) {
            InfixExpression infixExpr = (InfixExpression) condition;
            Expression rightOp = infixExpr.getRightOperand();
            InfixExpression.Operator operator = infixExpr.getOperator();

            if (rightOp instanceof NumberLiteral) {
                int endValue = Integer.parseInt(rightOp.toString());

                if (operator == InfixExpression.Operator.LESS) {
                    return endValue - 1;
                } else if (operator == InfixExpression.Operator.LESS_EQUALS) {
                    return endValue;
                } else if (operator == InfixExpression.Operator.GREATER) {
                    return endValue + 1;
                } else if (operator == InfixExpression.Operator.GREATER_EQUALS) {
                    return endValue;
                }
                return endValue;
            } else if (rightOp instanceof SimpleName) {
                // Đây có thể là một biến, cần tìm giá trị của biến
                String varName = ((SimpleName) rightOp).getIdentifier();

                //Can cai thien hon
                return 10;
            }
        }

        return 10;
    }

    private int calculateMaxIterations(int start, int end, int step) {
        if (step == 0) return 0;

        if ((step > 0 && start > end) || (step < 0 && start < end)) {
            return 0;
        }

        return Math.abs((end - start) / step) + 1;
    }

    public List<Map<String, Object[]>> generateSymbolicTestCases(Class<?>[] paramTypes) {
        this.parameterClasses = paramTypes;
        List<Map<String, Object[]>> testcases = new ArrayList<>();

        for (LoopInfo loopInfo : detectedLoops) {
            // Map để lưu trữ test case theo mục đích
            Map<String, Object[]> testCaseMap = new LinkedHashMap<>();

            String endParam = loopInfo.getLoopParameter();
            String startParam = loopInfo.getStartParameter();
            if (startParam != null && endParam != null) {
                // 1. 0 iterations - điều kiện a >= b
                testCaseMap.put("0_iterations", createDualParamTestCase(loopInfo, 5, 5));   // a=5, b=5 => 0 lần lặp (i=5; 5<5; i++)

                // 2. 1 iteration - điều kiện a+1 = b
                testCaseMap.put("1_iteration", createDualParamTestCase(loopInfo, 4, 5));    // a=4, b=5 => 1 lần lặp

                // 3. m iterations (m = 3) - điều kiện b = a+3
                testCaseMap.put("m_iterations", createDualParamTestCase(loopInfo, 2, 5));   // a=2, b=5 => 3 lần lặp

                // 4. n-1 iterations (n=5) - điều kiện b = a+4
                testCaseMap.put("n-1_iterations", createDualParamTestCase(loopInfo, 1, 5)); // a=1, b=5 => 4 lần lặp

                // 5. n iterations (n=5) - điều kiện b = a+5
                testCaseMap.put("n_iterations", createDualParamTestCase(loopInfo, 0, 5));   // a=0, b=5 => 5 lần lặp

                // 6. n+1 iterations - điều kiện b = a+6
                testCaseMap.put("n+1_iterations", createDualParamTestCase(loopInfo, 0, 6)); // a=0, b=6 => 6 lần lặp
            } else if (endParam != null) {
                // Vòng lặp chỉ phụ thuộc vào tham số kết thúc
                int start = loopInfo.getStartValue();
                int step = loopInfo.getStepValue();

                // 1. 0 iterations - b <= start
                testCaseMap.put("0_iterations", createParameterBasedTestCase(loopInfo, start));

                // 2. 1 iteration - b = start+1
                testCaseMap.put("1_iteration", createParameterBasedTestCase(loopInfo, start + 1));

                // 3. m iterations (m = 3) - b = start+3
                testCaseMap.put("m_iterations", createParameterBasedTestCase(loopInfo, start + 3));

                // 4. n-1 iterations (n=5) - b = start+4
                testCaseMap.put("n-1_iterations", createParameterBasedTestCase(loopInfo, start + 4));

                // 5. n iterations (n=5) - b = start+5
                testCaseMap.put("n_iterations", createParameterBasedTestCase(loopInfo, start + 5));

                // 6. n+1 iterations - b = start+6
                testCaseMap.put("n+1_iterations", createParameterBasedTestCase(loopInfo, start + 6));
            } else {
                // Vòng lặp không phụ thuộc vào tham số, sử dụng cách cũ
                int startVal = loopInfo.getStartValue();
                int step = loopInfo.getStepValue();
                int endVal = loopInfo.getEndValue();

                int m = 3;  // Số lần lặp trung bình
                int n = 5;  // Số lần lặp tối đa giả định

                // 1. 0 iterations
                testCaseMap.put("0_iterations", createDefaultTestCase(startVal, startVal));

                // 2. Exactly 1 iteration
                testCaseMap.put("1_iteration", createDefaultTestCase(startVal, startVal + step));

                // 3. m iterations (m < n)
                testCaseMap.put("m_iterations", createDefaultTestCase(startVal, startVal + m * step));

                // 4. n-1 iterations
                testCaseMap.put("n-1_iterations", createDefaultTestCase(startVal, startVal + (n-1) * step));

                // 5. n iterations
                testCaseMap.put("n_iterations", createDefaultTestCase(startVal, startVal + n * step));

                // 6. n+1 iterations
                testCaseMap.put("n+1_iterations", createDefaultTestCase(startVal, startVal + (n+1) * step));
            }

            for (Map.Entry<String, Object[]> entry : testCaseMap.entrySet()) {
                String purpose = entry.getKey();
                Object[] testcase = entry.getValue();

                // Chỉ thêm test case hợp lệ và không trùng lặp
                if (testcase != null && !(testcase.length == 0) && !containsDuplicate(testcases, testcase)) {;

                    // Set số lần lặp mong đợi dựa theo purpose
                    switch (purpose) {
                        case "0_iterations":
                            testcases.add(Map.of("0_iterations", testcase));
                            break;
                        case "1_iteration":
                            testcases.add(Map.of("1_iteration", testcase));
                            break;
                        case "m_iterations":
                            testcases.add(Map.of("m_iterations", testcase));
                            break;
                        case "n-1_iterations":
                            testcases.add(Map.of("n-1_iterations", testcase));
                            break;
                        case "n_iterations":
                            testcases.add(Map.of("n_iterations", testcase));
                            break;
                        case "n+1_iterations":
                            testcases.add(Map.of("n+1_iterations", testcase));
                            break;
                    }
                    
                }
            }

        }

        return testcases;
    }

    private boolean containsDuplicate(List<Map<String, Object[]>> testcases, Object[] testcase) {
        for (Map<String, Object[]> elements : testcases) {
            for (Object[] existingTestcase : elements.values()) {
                if (existingTestcase.length != testcase.length) continue;

                boolean same = true;
                for (int i = 0; i < testcase.length; i++) {
                    if (!Objects.equals(existingTestcase[i], testcase[i])) {
                        same = false;
                        break;
                    }
                }

                if (same) return true;
            }
        }
        return false;
    }

    private Object[] createDefaultTestCase(int startVal, int startVal1) {
        Object[] testcase = SymbolicExecution.createRandomTestData(this.parameterClasses);
        return testcase;
    }

    private Object[] createDualParamTestCase(LoopInfo loopInfo, int startValue, int endValue) {
        Object[] testcase = SymbolicExecution.createRandomTestData(this.parameterClasses);
        int i = 0;
        for (Object param: this.method.parameters()) {
            SingleVariableDeclaration variable = (SingleVariableDeclaration) param;
            String paramName = variable.getName().getIdentifier();

            if (paramName.equals(loopInfo.getStartParameter())) {
                testcase[i] = startValue;
            } else if (paramName.equals(loopInfo.getLoopParameter())) {
                testcase[i] = endValue;
            }
            i++;
        }
        return testcase;
    }

    private Object[] createParameterBasedTestCase(LoopInfo loopInfo, int targetValue) {
        Object[] testcase = SymbolicExecution.createRandomTestData(this.parameterClasses);
        int i = 0;
        for (Object param : this.method.parameters()) {
            SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
            String paramName = parameter.getName().getIdentifier();
            // Trường hợp 1: Tham số ảnh hưởng trực tiếp đến vòng lặp
            if (loopInfo.getLoopParameter() != null && paramName.equals(loopInfo.getLoopParameter())) {
                // Đối với vòng lặp for (i=2; i<=x; i++), giá trị x cần = targetValue
                testcase[i] = targetValue;
            }
            // Trường hợp 2: Không có tham số được xác định ảnh hưởng trực tiếp, sử dụng tham số đầu tiên
            else if (loopInfo.getLoopParameter() == null && method.parameters().size() == 1) {
                testcase[i] = targetValue; // Chỉ có một tham số, gán giá trị này
            }
            // Trường hợp 3: Các tham số khác không liên quan đến vòng lặp
            i++;
        }
        return testcase;
    }

    public List<Map<String, Object[]>> generateLoopBoundaryTestCases() {
        List<Map<String, Object[]>> testcases = new ArrayList<>();
        for (LoopInfo loopInfo : detectedLoops) {
            // Tạo testcase cho giá trị dưới giới hạn (0 lần lặp)
            testcases.add(Map.of("0_iterations",createTestCaseForLoopIterations(loopInfo, loopInfo.getStartValue() - 1)));

            // Tạo testcase cho giá trị tại giới hạn (1 lần lặp)
            testcases.add(Map.of("1_iteration", createTestCaseForLoopIterations(loopInfo, loopInfo.getStartValue())));

            // Tạo testcase cho giá trị trong giới hạn (m lần lặp, với m ngẫu nhiên)
            int m = new Random().nextInt(loopInfo.getMaxIterations() - 2) + 1;
            int n_m = loopInfo.getStartValue() + m * loopInfo.getStepValue();

            testcases.add(Map.of("m_iterations", createTestCaseForLoopIterations(loopInfo, n_m)));

            // Tạo testcase cho giá trị sát giới hạn (max-1 lần lặp)

            int n_minus = loopInfo.getEndValue() - loopInfo.getStepValue();
            testcases.add(Map.of("n-1_iterations", createTestCaseForLoopIterations(loopInfo, n_minus)));


            // Tạo testcase cho giá trị tại giới hạn max (max lần lặp)
            testcases.add(Map.of("n_iterations", createTestCaseForLoopIterations(loopInfo, loopInfo.getEndValue())));


            // Tạo testcase cho giá trị trên giới hạn (max+1 lần lặp)
            int n_plus = loopInfo.getEndValue() + loopInfo.getStepValue();
            testcases.add(Map.of("n_iterations", createTestCaseForLoopIterations(loopInfo, n_plus)));
        }

        return testcases;
    }

    private Object[] createTestCaseForLoopIterations(LoopInfo loopInfo, int targetValue) {
        Object[] testcase = SymbolicExecution.createRandomTestData(this.parameterClasses);
        int i = 0;
        for (Object param : this.method.parameters()) {
            SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
            String paramName = parameter.getName().getIdentifier();

            // Trường hợp 1: Nếu phát hiện tham số ảnh hưởng đến vòng lặp
            if (loopInfo.getLoopParameter() != null && paramName.equals(loopInfo.getLoopParameter())) {
                // Với vòng lặp for (i=2; i <= x; i++), giá trị x cần >= i
                if (loopInfo.getIteratorVariable() != null) {
                    // Trường hợp điều kiện dạng i <= x, với i tăng dần
                    if (targetValue >= loopInfo.getStartValue()) {
                        testcase[i] = targetValue;
                    }
                    // Trường hợp điều kiện dạng i >= x, với i giảm dần
                    else {
                        testcase[i] = loopInfo.getStartValue() - 1;
                    }
                } else {
                    testcase[i] = targetValue;
                }
            } else if (loopInfo.getLoopParameter() == null && method.parameters().size() == 1) {
                // Với factorial(int x), nếu điều kiện vòng lặp là i <= x
                if ("int".equals(parameter.getType().toString())) {
                    testcase[i] = Math.max(targetValue, loopInfo.getStartValue());
                }
            }
            // Trường hợp 3: Các tham số khác không liên quan đến vòng lặp
            i++;
        }
        return testcase;
    }
}