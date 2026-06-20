package core.cfg.utils;

import core.node.FolderNode;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;


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

    public static ArrayList<ASTNode> parseFile(String filePath, CompilationUnit cu) throws IOException
    {
        File file = new File(filePath);

        ArrayList<ASTNode> retFuncList = new ArrayList<>();

        if (file.isFile() && file.getName().endsWith(".java")) {
            String fileToString = FileService.readFileToString(file.getPath());

            retFuncList = parserToAstFuncList(fileToString, cu);

            System.out.println("retFuncList.count = " + retFuncList.size());
        }

        return retFuncList;
    }

    public static CompilationUnit parseFileToCompilationUnit(String filePath) throws IOException {
        File file = new File(filePath);

        CompilationUnit compilationUnit = null;

        if (file.isFile() && file.getName().endsWith(".java")) {
            String fileToString = FileService.readFileToString(file.getPath());
            compilationUnit = parserToCompilationUnit(fileToString);
            
            // Apply desugaring and re-parse to ensure bindings for new nodes
            compilationUnit = ASTHelper.applyDesugaringAndReparse(compilationUnit, fileToString);
        }
        return compilationUnit;
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

    public static CompilationUnit parserToCompilationUnit(String sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        try {
            String[] classpathEntries = getValidClasspath();
            String[] sourcepathEntries = new String[0];

            if (classpathEntries.length > 0) {
                parser.setEnvironment(classpathEntries, sourcepathEntries, null, true);
                parser.setUnitName("temp.java");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not set environment: " + e.getMessage());
        }

        Map options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        return (CompilationUnit) parser.createAST(null);
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