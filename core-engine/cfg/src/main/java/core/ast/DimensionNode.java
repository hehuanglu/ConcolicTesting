package core.ast;

import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Dimension;

import java.util.List;

public class DimensionNode extends AstNode {
    private List<AstNode> annotations;

    public static IntegerLiteralNode executeDimension(Dimension dimension) {
        IntegerLiteralNode integerLiteralNode = new IntegerLiteralNode();
        integerLiteralNode.setTokenValue(0);
        return integerLiteralNode;
    }

    public static void replaceMethodInvocationWithStub(ASTNode originStatement, MethodInvocation originMethodInvocation, ASTNode replacement) {

    }

}
