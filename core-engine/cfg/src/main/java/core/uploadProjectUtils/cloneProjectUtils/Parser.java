package core.uploadProjectUtils.cloneProjectUtils;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class Parser {
    public static CompilationUnit parseFileToCompilationUnit(String filePath) throws IOException {
        return parseFileToCompilationUnit(filePath, null);
    }

    public static CompilationUnit parseFileToCompilationUnit(String filePath, String sourceRootPath) throws IOException {
        File file = new File(filePath);

        CompilationUnit compilationUnit = null;

        if (file.isFile() && file.getName().endsWith(".java")) {
            String sourceCode = parseFileToSourceCode(file.getPath());
            compilationUnit = parseSourceCodeToCompilationUnit(sourceCode, file, sourceRootPath);
        }
        return compilationUnit;
    }

    private static String parseFileToSourceCode(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        char[] buf = new char[10];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            //System.out.println(numRead);
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }

        reader.close();

        return fileData.toString();
    }

    private static CompilationUnit parseSourceCodeToCompilationUnit(String sourceCode, File sourceFile, String sourceRootPath) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        configureBindingEnvironment(parser, sourceFile, sourceRootPath);

        Map options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        return (CompilationUnit) parser.createAST(null);
    }

    private static void configureBindingEnvironment(ASTParser parser, File sourceFile, String sourceRootPath) {
        String[] sourcePaths = null;
        String unitName = sourceFile.getName();

        if (sourceRootPath != null && !sourceRootPath.isBlank()) {
            try {
                Path rootPath = Path.of(sourceRootPath).toAbsolutePath().normalize();
                Path filePath = sourceFile.toPath().toAbsolutePath().normalize();

                if (filePath.startsWith(rootPath)) {
                    sourcePaths = new String[]{rootPath.toString()};
                    unitName = rootPath.relativize(filePath).toString().replace(File.separatorChar, '/');
                }
            } catch (Exception ignored) {
                sourcePaths = null;
                unitName = sourceFile.getName();
            }
        }

        parser.setEnvironment(getClasspathEntries(), sourcePaths, null, true);
        parser.setUnitName(unitName);
    }

    private static String[] getClasspathEntries() {
        String classpath = System.getProperty("java.class.path");
        if (classpath == null || classpath.isBlank()) {
            return new String[0];
        }

        return java.util.Arrays.stream(classpath.split(File.pathSeparator))
                .filter(path -> path != null && !path.isBlank())
                .filter(path -> new File(path).exists())
                .toArray(String[]::new);
    }
}
