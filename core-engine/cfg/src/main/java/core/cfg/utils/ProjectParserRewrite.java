package core.cfg.utils;

import core.node.FolderNode;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectParserRewrite {

    private FolderNode folderNode = new FolderNode();
    private static ProjectParserRewrite parser = null;

    public static ProjectParserRewrite getParser() {
        if (parser == null){
            parser = new ProjectParserRewrite();
        }
        return parser;
    }

    public static ArrayList<ASTNode> parseFile(String filePath, CompilationUnit cu) throws IOException {
        File file = new File(filePath);

        ArrayList<ASTNode> retFuncList = new ArrayList<>();

        if (file.isFile() && file.getName().endsWith(".java")) {
            String fileToString = FileService.readFileToString(file.getPath());
            retFuncList = parserToAstFuncList(fileToString, cu);

            System.out.println("retFuncList.count = " + retFuncList.size());
        }

        return retFuncList;
    }

    public static ArrayList<ASTNode> parserToAstFuncList(String sourceCodeFile, CompilationUnit cu) {
        ArrayList<ASTNode> astFuncList = new ArrayList<>();

        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                astFuncList.add(node);
                return true;
            }
        };

        if (cu != null) {
            cu.accept(visitor);
        }

        return astFuncList;
    }

    public static CompilationUnit parseFileToCompilationUnit(String filePath) throws IOException {
        File file = new File(filePath);

        CompilationUnit compilationUnit = null;

        if (file.isFile() && file.getName().endsWith(".java")) {
            String fileToString = FileService.readFileToString(file.getPath());
            compilationUnit = parserToCompilationUnit(fileToString);
            checkBindings(compilationUnit);
        }
        return compilationUnit;
    }

    public static CompilationUnit parserToCompilationUnit(String sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);

        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        try {
            parser.setEnvironment(getValidClasspath(), new String[0], null, true);
            parser.setUnitName("Test.java");
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure ASTParser.", e);
        }

        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);

        return (CompilationUnit) parser.createAST(null);
    }

    public static void checkBindings(CompilationUnit cu) {
        System.out.println("\n========== AST BINDING CHECK ==========");

        cu.accept(new ASTVisitor() {

            @Override
            public boolean visit(VariableDeclarationFragment node) {
                IVariableBinding vb = node.resolveBinding();

                System.out.println("Variable Declaration: " + node.getName());

                if (vb == null) {
                    System.out.println("  Binding: NULL");
                } else {
                    System.out.println("  Binding: " + vb);
                    System.out.println("  Name: " + vb.getName());

                    ITypeBinding type = vb.getType();
                    if (type != null) {
                        System.out.println("  Type: " + type.getQualifiedName());
                        System.out.println("  Is Array: " + type.isArray());

                        if (type.isArray()) {
                            System.out.println("  Element Type: "
                                    + type.getElementType().getQualifiedName());
                        }
                    }
                }

                return true;
            }

            @Override
            public boolean visit(SimpleName node) {

                IBinding binding = node.resolveBinding();

                if (binding instanceof IVariableBinding) {

                    IVariableBinding vb = (IVariableBinding) binding;

                    System.out.println("\nSimpleName: " + node);

                    System.out.println("  Binding: " + vb);
                    System.out.println("  isField: " + vb.isField());
                    System.out.println("  isParameter: " + vb.isParameter());

                    ITypeBinding type = vb.getType();
                    if (type != null) {
                        System.out.println("  Type: " + type.getQualifiedName());
                        System.out.println("  Is Array: " + type.isArray());

                        if (type.isArray()) {
                            System.out.println("  Element Type: "
                                    + type.getElementType().getQualifiedName());
                        }
                    }
                }

                return true;
            }

            @Override
            public boolean visit(ArrayAccess node) {

                System.out.println("\nArrayAccess: " + node);

                Expression array = node.getArray();

                System.out.println("  Array Expr: " + array);
                System.out.println("  Array Expr Class: " + array.getClass().getSimpleName());

                ITypeBinding tb = array.resolveTypeBinding();
                if (tb != null) {
                    System.out.println("  Array Type: " + tb.getQualifiedName());
                    System.out.println("  Is Array: " + tb.isArray());

                    if (tb.isArray()) {
                        System.out.println("  Element Type: "
                                + tb.getElementType().getQualifiedName());
                    }
                } else {
                    System.out.println("  Array Type Binding: NULL");
                }

                return true;
            }

        });

        System.out.println("========== END AST BINDING CHECK ==========\n");
    }

    private static String[] getValidClasspath() {
        List<String> validPaths = new ArrayList<>();

        String javaHome = System.getProperty("java.home");
        File rtJar = new File(javaHome, "lib/rt.jar");
        if (rtJar.exists()) {
            validPaths.add(rtJar.getAbsolutePath());
        }

        File jceJar = new File(javaHome, "lib/jce.jar");
        if (jceJar.exists()) {
            validPaths.add(jceJar.getAbsolutePath());
        }

        String systemClasspath = System.getProperty("java.class.path");
        if (systemClasspath != null) {
            for (String path : systemClasspath.split(File.pathSeparator)) {
                if (new File(path).exists()) {
                    validPaths.add(path);
                }
            }
        }

        return validPaths.toArray(new String[0]);
    }
}