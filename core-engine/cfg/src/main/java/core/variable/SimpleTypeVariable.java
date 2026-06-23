package core.variable;

import com.microsoft.z3.*;
import core.symbolicExecution.SymbolicExecutionRewrite;
import org.eclipse.jdt.core.dom.SimpleType;

import java.util.HashMap;
import java.util.Map;

public class SimpleTypeVariable extends Variable {
    private SimpleType simpleType;
    private static final Map<String, Sort> sortCache = new HashMap<>();

    public SimpleTypeVariable(SimpleType simpleType, String name) {
        this.simpleType = simpleType;
        super.setName(name);
    }

    public SimpleType getType() {
        return simpleType;
    }

    public String getTypeName() {
        return simpleType.getName().getFullyQualifiedName();
    }

    public static Expr createZ3SimpleTypeVariable(SimpleTypeVariable simpleTypeVariable, Context ctx) {
        String name = simpleTypeVariable.getName();
        String typeName = simpleTypeVariable.getTypeName();
        SymbolicExecutionRewrite.variableTypeMap.put(name, typeName.toString());
        Sort sort; switch (typeName) {
            case "String":
                sort = ctx.mkStringSort();
                break;
            default :
                sort = ctx.mkStringSort();
        }
        return (SeqExpr<CharSort>) ctx.mkConst(name, ctx.mkStringSort());
    }
}