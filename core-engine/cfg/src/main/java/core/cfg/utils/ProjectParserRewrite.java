package core.cfg.utils;

import core.cfg.CfgNode;
import core.cfg.utils.FileService;
import core.node.ClassAbstractableElementVisibleElementJavaNode;
import core.parser.JavaFileParser;
import core.structureTree.SNode;
import core.node.FileNode;
import core.node.Node;
import core.node.FolderNode;
import core.utils.Utils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectParserRewrite {

    private FolderNode folderNode = new FolderNode();
    private String projectPath;
    private static ProjectParserRewrite parser = null;


    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public static ProjectParserRewrite getParser() {
        if (parser == null){
            parser = new ProjectParserRewrite();
        }
        return parser;
    }

    public FolderNode getFolderNode() {
        return folderNode;
    }

    public void setFolderNode(FolderNode folderNode) {
        this.folderNode = folderNode;
    }


    public static ArrayList<ASTNode> parseFile(String filePath, CompilationUnit cu) throws IOException
    {
        File file = new File(filePath);

        ArrayList<ASTNode> retFuncList = new ArrayList<>();

        if (file.isFile() && file.getName().endsWith(".java")) {
            String fileToString = FileService.readFileToString(file.getPath());

            retFuncList = CfgNode.parserToAstFuncList(fileToString, cu);

            System.out.println("retFuncList.count = " + retFuncList.size());
        }

        return retFuncList;
    }

    public static CompilationUnit parseFileToCompilationUnit(String filePath) throws IOException {
        File file = new File(filePath);

        CompilationUnit compilationUnit = null;

        if (file.isFile() && file.getName().endsWith(".java")) {
            String fileToString = FileService.readFileToString(file.getPath());
            compilationUnit = CfgNode.parserToCompilationUnit(fileToString);
        }
        return compilationUnit;
    }
}