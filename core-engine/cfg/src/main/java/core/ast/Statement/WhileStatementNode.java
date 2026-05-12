package core.ast.Statement;

import core.cfg.CfgNode;
import core.symbolicExecution.SymbolicExecution;
import core.cfg.utils.CfgUtils;
import org.eclipse.jdt.core.dom.*;

public class WhileStatementNode extends StatementNode {
    public static void replaceMethodInvocationWithStub(WhileStatement originWhileStatement, MethodInvocation originMethodInvocation, ASTNode replacement) {
        if (originWhileStatement.getExpression() == originMethodInvocation){
            originWhileStatement.setExpression((Expression) replacement);

            CfgNode currentCfgNode = SymbolicExecution.getCurrentCfgNode();
            if (currentCfgNode != null) {
                currentCfgNode.setAst(replacement);
            } else {
                CfgUtils.getCurrentCfgNode().setAst(replacement);
            }
        }
        // body
    }
}
