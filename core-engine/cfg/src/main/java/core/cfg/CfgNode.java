package core.cfg;

import core.cfg.utils.ASTHelper;
import core.structureTree.structureNode.SFunctionNode;
import core.utils.Utils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class CfgNode {
    private int startPosition;
    private int endPosition;
    private int lineNumber;
    private CfgNode beforeStatementNode;//Lenh ngay truoc
    private CfgNode afterStatementNode;//Lenh ngay sau
    private boolean isBeginCfgNode = false; //Nut dau CFG (nút giả)
    private boolean isEndCfgNode = false; //Nut cuoi CFG (nút giả)
    private boolean isFalseNode = false; //Nut false cua cau lenh dieu kien
    private String content = "";
    private boolean isMarked = false;
    private boolean isFakeMarked = false;
    private ASTNode ast;
    private CfgNode parent;
    private List<CfgNode> children = new ArrayList<>();
    private Set<String> defVars = new HashSet<>();
    private Set<String> useVars = new HashSet<>();

    public CfgNode(ASTNode ast) {
        this.ast = ast;
        setStartPosition(ast.getStartPosition());
        setEndPosition(ast.getStartPosition() + ast.getLength());
    }

    public CfgNode() {
    }

    public ASTNode getAst() {
        return ast;
    }

    public void setAst(ASTNode ast) {
        this.ast = ast;
        this.content = ast.toString();
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public String getContent() {
        if (ast != null)
            return ast.toString();
        else return content;
    }

    public Set<String> getDefVars() {
        return defVars;
    }

    public Set<String> getUseVars() {
        return useVars;
    }

    public void addDefVar(String var) {
        defVars.add(var);
    }

    public void addUseVar(String var) {
        useVars.add(var);
    }

    public String getContentReport() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public CfgNode getParent() {
        return parent;
    }

    public void setParent(CfgNode parent) {
        this.parent = parent;
    }

    public List<CfgNode> getChildren() {
        return children;
    }

    public void setChildren(List<CfgNode> children) {
        this.children = children;
    }

    public boolean isFalseNode() {
        return isFalseNode;
    }

    public void setIsFalseNode(boolean falseNode) {
        isFalseNode = falseNode;
    }

    public static CfgNode parseToCFG(SFunctionNode functionNode) {
        ASTNode astNode = functionNode.getAst().getAstNode();
        CfgNode cfgNode = new CfgStartNode(astNode);
        ASTHelper.generateCFGTreeFromASTNode(astNode, cfgNode);
        return cfgNode;
    }

    public static CfgNode parserToCFG(String sourceCode) {
        CfgNode cfg = new CfgNode();

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {

                List<ASTNode> children = Utils.getChildren(node);

                for (ASTNode func : children) {

                }

                ASTHelper.generateCFGTreeFromASTNode(node, cfg);
                return true;
            }
        };

        cu.accept(visitor);

        return cfg;
    }


    public static ArrayList<ASTNode> parserToAstFuncList(String sourceCodeFile) {
        ArrayList<ASTNode> AstFuncList = new ArrayList<>();
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourceCodeFile.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                Utils.getFunctionChildren(node, AstFuncList);

                return true;
            }
        };

        cu.accept(visitor);

        return AstFuncList;
    }

    public static List<MethodDeclaration> parserToConstructorList(String sourceCode) {
        List<MethodDeclaration> constructorList = new ArrayList<>();
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                Utils.getConstructorChildren(node, constructorList);

                return true;
            }
        };

        cu.accept(visitor);

        return constructorList;
    }

    public static CompilationUnit parserToCompilationUnit(String sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        Map options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        return (CompilationUnit) parser.createAST(null);
    }

    public static ASTNode parserToAstFuncList0(String sourceCodeFile, String funcName) {
        ArrayList<ASTNode> AstFuncList = new ArrayList<>();
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourceCodeFile.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                List<MethodDeclaration> methods = Arrays.asList(node.getMethods());
                for (MethodDeclaration method : methods) {
                    if (method.isConstructor() == false) {
                        AstFuncList.add(method);
                    }
                }

                return true;
            }
        };

        cu.accept(visitor);

        for (int i = 0; i < AstFuncList.size(); i++) {
            if (((MethodDeclaration) AstFuncList.get(i)).getName().getIdentifier().equals("foo")) {
                return AstFuncList.get(i);
            }
        }

        return null;
    }

    public String markContent(String testPath) {
        return "";
    }

    @Override
    public String toString() {
        return "CFGNode{" +
//                "start=" + startPosition +
//                ", end=" + endPosition +
                ("".equals(content) ? "null" : ", content='" + content + '}');
//                ", isRootNode=" + isBeginCfgNode +
//                ", isEndNode=" + isEndCfgNode +
        //", children=" + children +
//                ", isVisited=" + isVisited +
//                '}';
    }

    public CfgNode getBeforeStatementNode() {
        return beforeStatementNode;
    }

    public void setBeforeStatementNode(CfgNode beforeStatementNode) {
        this.beforeStatementNode = beforeStatementNode;
    }

    public CfgNode getAfterStatementNode() {
        return afterStatementNode;
    }

    public void setAfterStatementNode(CfgNode afterStatementNode) {
        this.afterStatementNode = afterStatementNode;
    }

    public boolean getIsBeginCfgNode() {
        return isBeginCfgNode;
    }

    public void setIsBeginCfgNode(boolean isBeginCfgNode) {
        this.isBeginCfgNode = isBeginCfgNode;
    }

    public boolean getIsEndCfgNode() {
        return isEndCfgNode;
    }

    public void setIsEndCfgNode(boolean isEndCfgNode) {
        this.isEndCfgNode = isEndCfgNode;
    }

    public boolean isMarked() {
        return isMarked;
    }

    public void setMarked(boolean marked) {
        isMarked = marked;
    }

    public boolean isFakeMarked() {
        return isFakeMarked;
    }

    public void setFakeMarked(boolean fakeMarked) {
        isFakeMarked = fakeMarked;
    }
}
