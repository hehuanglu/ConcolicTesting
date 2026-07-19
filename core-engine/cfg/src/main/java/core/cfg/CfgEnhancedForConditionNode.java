package core.cfg;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class CfgEnhancedForConditionNode extends CfgBoolExprNode {

    public enum IterableKind { ARRAY, LIST, ITERABLE }

    private final IterableKind kind;
    private final String indexVarName;
    private final Expression originalIterableExpr; // AST GỐC, chưa copy -> vẫn còn binding nếu cần
    private final ITypeBinding elementTypeBinding;  // chốt sẵn 1 lần, không cần resolve lại

    public CfgEnhancedForConditionNode(IterableKind kind,
                                       String indexVarName,
                                       Expression originalIterableExpr,
                                       ITypeBinding elementTypeBinding) {
        this.kind = kind;
        this.indexVarName = indexVarName;
        this.originalIterableExpr = originalIterableExpr;
        this.elementTypeBinding = elementTypeBinding;
    }

    public IterableKind getKind() { return kind; }
    public String getIndexVarName() { return indexVarName; }
    public Expression getOriginalIterableExpr() { return originalIterableExpr; }
    public ITypeBinding getElementTypeBinding() { return elementTypeBinding; }
}