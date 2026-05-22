package core.ast.Type.AnnotatableType;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Sort;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.StringLiteralNode;
import core.symbolicExecution.MemoryModel;
import core.variable.SimpleTypeVariable;
import core.variable.Variable;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;

import java.util.List;

public class SimpleTypeNode extends ExpressionNode {
    private final String name;
    private Sort sort;
    private SimpleType simpleType;

    public SimpleTypeNode(SimpleType type, String name) {
        this.name = name;
        this.simpleType = type;
        this.sort = null;
    }

    public SimpleTypeNode(SimpleType type, SimpleName name) {
        this(type, name.getIdentifier());
    }

    public String getName() {
        return name;
    }

    public Sort getSort() {
        return sort;
    }

    public SimpleType getSimpleType() {
        return simpleType;
    }

    public static Expr createZ3Expression(SimpleTypeNode node, MemoryModel memoryModel,
                                          Context ctx, List<Z3VariableWrapper> vars) {
        Variable variable = memoryModel.getVariable(node.name);
        if (variable == null) {
            throw new RuntimeException("Variable not found: " + node.name);
        }
        Expr expr = Variable.createZ3Variable(variable, ctx);

        Z3VariableWrapper wrapper = new Z3VariableWrapper(expr);

        int idx = getDuplicateVariableIndex(wrapper, vars);
        if (idx != -1) {
            return vars.get(idx).getPrimitiveVar();
        } else {
            vars.add(wrapper);
        }
        return expr;
    }

    private static int getDuplicateVariableIndex(Z3VariableWrapper wrapper, List<Z3VariableWrapper> vars) {
        for (int i = 0; i < vars.size(); i++) {
            if (wrapper.equals(vars.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static LiteralNode changeSimpleTypeToLiteralInitialization(SimpleType type) {
        String typeName = type.toString();
        switch (typeName) {
            case "String":
                return new StringLiteralNode();
            default:
                throw new RuntimeException("Unsupported type for literal initialization: " + typeName);
        }
    }
}