package core.ast.Statement;

import core.ast.AstNode;
import core.ast.VariableDeclaration.VariableDeclarationFragmentNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class VariableDeclarationStatementNode extends StatementNode {
    private List<ASTNode> modifiers = null;
    private Type baseType = null;
    private List<AstNode> variableDeclarationFragments;

    public static void executeVariableDeclarationStatement(VariableDeclarationStatement statement, MemoryModel memoryModel) {
        List<VariableDeclarationFragment> fragments = statement.fragments();
        for(VariableDeclarationFragment fragment : fragments) {
            VariableDeclarationFragmentNode.executeVariableDeclarationFragment(fragment, statement.getType(), memoryModel);
        }
    }

    public static void replaceMethodInvocationWithStub(VariableDeclarationStatement originStatement, MethodInvocation originMethodInvocation, ASTNode replacement) {

    }
}
