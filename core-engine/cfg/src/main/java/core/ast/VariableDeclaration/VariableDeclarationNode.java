package core.ast.VariableDeclaration;

import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public abstract class VariableDeclarationNode extends AstNode {
    int extraArrayDimensions = 0;
    List<AstNode> extraDimensions = null;
    ExpressionNode optionalInitializer = null;

    public static void executeVariableDeclaration(VariableDeclaration variableDeclaration, MemoryModel memoryModel) {
        if (variableDeclaration instanceof SingleVariableDeclaration) {
            SingleVariableDeclarationNode.executeSingleVariableDeclaration((SingleVariableDeclaration) variableDeclaration,
                    memoryModel);
        } else if (variableDeclaration instanceof VariableDeclarationFragment) {
//           /*????*/
        } else {
            throw new RuntimeException(variableDeclaration.getClass() + " is not a VariableDeclaration");
        }
    }

    public static void replaceMethodInvocationWithStub(VariableDeclaration originVariableDeclaration, MethodInvocation originMethodInvocation, ASTNode replacement) {
        if (originVariableDeclaration instanceof SingleVariableDeclaration) {
            SingleVariableDeclarationNode.replaceMethodInvocationWithStub((SingleVariableDeclaration) originVariableDeclaration, originMethodInvocation, replacement);
        } else if (originVariableDeclaration instanceof VariableDeclarationFragment) {
            VariableDeclarationFragmentNode.replaceMethodInvocationWithStub((VariableDeclarationFragment) originVariableDeclaration, originMethodInvocation, replacement);
        } else {
            throw new RuntimeException(originVariableDeclaration.getClass() + " is not a VariableDeclaration");
        }
    }
}
