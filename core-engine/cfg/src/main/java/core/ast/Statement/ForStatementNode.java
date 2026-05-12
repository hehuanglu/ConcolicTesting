package core.ast.Statement;

import core.cfg.CfgNode;
import core.symbolicExecution.SymbolicExecution;
import core.cfg.utils.CfgUtils;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class ForStatementNode extends StatementNode {

    public static void replaceMethodInvocationWithStub(ForStatement originForStatement, MethodInvocation originMethodInvocation, ASTNode replacement) {
        if (originForStatement.getExpression() == originMethodInvocation) {
            originForStatement.setExpression((Expression) replacement);

            CfgNode currentCfgNode = SymbolicExecution.getCurrentCfgNode();
            if (currentCfgNode != null) {
                currentCfgNode.setAst(replacement);
            } else {
                CfgUtils.getCurrentCfgNode().setAst(replacement);
            }
        } else {
            List<ASTNode> updaters = originForStatement.updaters();
            for (int i = 0; i < updaters.size(); i++) {
                if (updaters.get(i) == originMethodInvocation) {
                    updaters.set(i, replacement);
                    return;
                }
            }

            List<ASTNode> initializers = originForStatement.initializers();
            for (int i = 0; i < initializers.size(); i++) {
                if (initializers.get(i) == originMethodInvocation) {
                    initializers.set(i, replacement);
                    return;
                }
            }
        }
    }
}
