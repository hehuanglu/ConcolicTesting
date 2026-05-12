package core.symbolicExecution;

import core.ast.AstNode;
import core.ast.DimensionNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Statement.ExpressionStatementNode;
import core.ast.Statement.StatementNode;
import core.ast.VariableDeclaration.VariableDeclarationNode;
import org.eclipse.jdt.core.dom.*;

public class Rewrite {
    static public AstNode reStm(ASTNode astNode, MemoryModel symbolicMap) {
        if (astNode instanceof ExpressionStatement) { // e1 = e2;
            return ExpressionStatementNode.executeExpressionStatement((ExpressionStatement) astNode, symbolicMap);
        } else if (astNode instanceof Expression) { // e1 <= e2
            return ExpressionNode.executeExpression((Expression) astNode, symbolicMap);
        }
        return null;
    }

    public static void main(String[] args) {

    }
}
