package core.ast.Statement;

import core.cfg.CfgNode;
import core.symbolicExecution.SymbolicExecution;
import core.cfg.utils.CfgUtils;
import org.eclipse.jdt.core.dom.*;

public class IfStatementNode extends StatementNode {
    public static void replaceMethodInvocationWithStub(IfStatement originIfStatement, MethodInvocation originMethodInvocation, ASTNode replacement) {
        if (originIfStatement.getExpression() == originMethodInvocation){
            originIfStatement.setExpression((Expression) replacement);

            CfgNode currentCfgNode = SymbolicExecution.getCurrentCfgNode();
            if (currentCfgNode != null) {
                currentCfgNode.setAst(replacement);
            } else {
                CfgUtils.getCurrentCfgNode().setAst(replacement);
            }
        }
        // then statement
        // else statement
    }
}
