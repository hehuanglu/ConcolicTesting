package core.uploadProjectUtils;

import core.uploadProjectUtils.cloneProjectUtils.Parser;
import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConcolicUploadUtil {

    public static final Map<String, Integer> totalStatementsInJavaFile = new HashMap<>();
    public static final Map<String, Integer> totalStatementsInUnits = new HashMap<>();
    public static final Map<String, Integer> totalBranchesInUnits = new HashMap<>();

    private static int totalFunctionStatements;
    private static int totalClassStatements;
    private static int totalFunctionBranches;

    private enum CoverageType {
        STATEMENT,
        BRANCH
    }

    public static void ConcolicPreprocessSourceCode (String dirPath) throws IOException {
        preprocessSourceCode(dirPath);
        System.out.println("abc");
    }

    private static void preprocessSourceCode(String dirPath) throws IOException {

        File[] files = getFilesInDirectory(dirPath);

        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();
                preprocessSourceCode(dirPath + "\\" + dirName);
            } else if (file.isFile() && file.getName().endsWith("java")) {
                totalClassStatements = 0;
                String fileName = file.getName();
                String javaFileName = fileName.replace(".java", "");
                CompilationUnit compilationUnit = Parser.parseFileToCompilationUnit(dirPath + "\\" + fileName);
                preprocessJavaFile(compilationUnit, javaFileName);
            }
        }
    }

    private static File[] getFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);

        if (!directory.isDirectory()) {
            throw new RuntimeException("Invalid Dir: " + directory.getPath());
        }

        return directory.listFiles();
    }

    private static void preprocessJavaFile(CompilationUnit compilationUnit, String javaFileName) {
        String key = (compilationUnit.getPackage() != null ? compilationUnit.getPackage().getName().toString() : "") + javaFileName + "totalStatement";

        List<ASTNode> methods = new ArrayList<>();
        ASTVisitor methodsVisitor = new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                for (MethodDeclaration method : node.getMethods()) {
//                    if (!method.isConstructor()) {
                    methods.add(method);
//                    }
                }
                return true;
            }
        };
        compilationUnit.accept(methodsVisitor);

        for (ASTNode astNode : methods) {
            totalFunctionStatements = 0;
            totalFunctionBranches = 0;
            MethodDeclaration methodDeclaration = (MethodDeclaration) astNode;
            preprocessUnit(methodDeclaration);
            createTotalFunctionCoverageVariable(methodDeclaration, key, totalFunctionStatements, CoverageType.STATEMENT);
            createTotalFunctionCoverageVariable(methodDeclaration, key, totalFunctionBranches, CoverageType.BRANCH);
        }

        createTotalClassStatementVariable(key);
    }

    private static void createTotalFunctionCoverageVariable(MethodDeclaration methodDeclaration, String classKey, int totalStatement, CoverageType coverageType) {
        StringBuilder result = new StringBuilder(classKey);
        result.append(methodDeclaration.getReturnType2());
        result.append(methodDeclaration.getName());
        for (int i = 0; i < methodDeclaration.parameters().size(); i++) {
            result.append(methodDeclaration.parameters().get(i));
        }
        if(coverageType == CoverageType.STATEMENT) {
            result.append("TotalStatement");
            totalStatementsInUnits.put(reformatVariableName(result.toString()), totalStatement);
        } else if (coverageType == CoverageType.BRANCH){
            result.append("TotalBranch");
            totalBranchesInUnits.put(reformatVariableName(result.toString()), totalStatement);
        } else {
            throw new RuntimeException("Invalid Coverage");
        }

    }

    private static String reformatVariableName(String name) {
        return name.replace(" ", "").replace(".", "")
                .replace("[", "").replace("]", "")
                .replace("<", "").replace(">", "")
                .replace(",", "");
    }

    private static void createTotalClassStatementVariable(String key) {
        totalStatementsInJavaFile.put(key, totalClassStatements);
    }

    private static void preprocessUnit(MethodDeclaration method) {
        preprocessBlock(method.getBody());
    }

    private static void preprocessOneStatement(ASTNode statement) {
        if (statement == null) {
            return;
        }

        if (statement instanceof Block) {
            preprocessBlock((Block) statement);
        } else if (statement instanceof IfStatement) {
            preprocessIfStatement((IfStatement) statement);
        } else if (statement instanceof ForStatement) {
            preprocessForStatement((ForStatement) statement);
        } else if (statement instanceof WhileStatement) {
            preprocessWhileStatement((WhileStatement) statement);
        } else if (statement instanceof DoStatement) {
            preprocessDoStatement((DoStatement) statement);
        } else {
            preprocessNormalStatement();
        }

    }

    private static void preprocessBlock(Block block) {
        if(block != null) {
            List<ASTNode> statements = block.statements();
            for (int i = 0; i < statements.size(); i++) {
                preprocessOneStatement(statements.get(i));
            }
        }
    }

    private static void preprocessIfStatement(IfStatement ifStatement) {
        generateForCondition();
        preprocessOneStatement(ifStatement.getThenStatement());
        preprocessOneStatement(ifStatement.getElseStatement());
    }

    private static void preprocessForStatement(ForStatement forStatement) {

        // Initializers
        List<ASTNode> initializers = forStatement.initializers();
        for (ASTNode initializer : initializers) {
            updateTotalStatements();
        }

        generateForCondition();

        List<ASTNode> updaters = forStatement.updaters();
        for (int i = 0; i < updaters.size(); i++) {
            preprocessOneStatement(updaters.get(i));
        }

        preprocessOneStatement(forStatement.getBody());
    }

    private static void preprocessWhileStatement(WhileStatement whileStatement) {
        generateForCondition();
        preprocessOneStatement(whileStatement.getBody());
    }

    private static void preprocessDoStatement(DoStatement doStatement) {
        preprocessOneStatement(doStatement.getBody());
        generateForCondition();
    }

    private static void preprocessNormalStatement() {
        updateTotalStatements();
    }

    private static void updateTotalStatements() {
        totalFunctionStatements++;
        totalClassStatements++;
    }

    private static void generateForCondition() {
        totalFunctionStatements++;
        totalClassStatements++;
        totalFunctionBranches += 2;
    }
}

