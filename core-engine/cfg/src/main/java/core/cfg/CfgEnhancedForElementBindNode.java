package core.cfg;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class CfgEnhancedForElementBindNode extends CfgNormalNode {

    private final CfgEnhancedForConditionNode.IterableKind kind;
    private final String elementVarName;
    private final String indexVarName;
    private final ITypeBinding elementTypeBinding;

    public CfgEnhancedForElementBindNode(CfgEnhancedForConditionNode.IterableKind kind,
                                         String elementVarName,
                                         String indexVarName,
                                         ITypeBinding elementTypeBinding) {
        super();
        this.kind = kind;
        this.elementVarName = elementVarName;
        this.indexVarName = indexVarName;
        this.elementTypeBinding = elementTypeBinding;
    }

    public CfgEnhancedForConditionNode.IterableKind getKind() {
        return kind;
    }

    public String getElementVarName() {
        return elementVarName;
    }

    public String getIndexVarName() {
        return indexVarName;
    }

    public ITypeBinding getElementTypeBinding() {
        return elementTypeBinding;
    }
}