package core.cfg;

import core.utils.Utils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.EnhancedForStatement;

public class CfgEnhancedForStatementBlockNode extends CfgNode implements IEvaluateCoverage {

    public CfgEnhancedForStatementBlockNode() {
    }

    @Override
    public String markContent(String testPath) {
        StringBuilder content = new StringBuilder();
        content.append(getStartPosition())
                .append(getClass().getSimpleName())
                .append("{StartAt:")
                .append(getStartPosition())
                .append(",EndAt:")
                .append(getEndPosition());

        return Utils.getWriteToTestPathContent(content.toString(), testPath);
    }

    @Override
    public String getContentReport() {
        ASTNode ast = getAst();
        if (ast instanceof EnhancedForStatement) {
            return ((EnhancedForStatement) ast).getExpression().toString();
        }
        return getContent();
    }
}