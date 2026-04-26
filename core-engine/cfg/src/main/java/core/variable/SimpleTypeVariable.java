package core.variable;

import com.microsoft.z3.Expr;
import org.eclipse.jdt.core.dom.SimpleType;
import com.microsoft.z3.Context;

public class SimpleTypeVariable extends Variable{
    private SimpleType type;
    public SimpleTypeVariable(SimpleType type,String name){
        this.type = type;
        super.setName(name);
    }
    // tạm thời chỉ hỗ trợ String
    public static Expr createSimpleTypeVarible(Variable variable,Context ctx){
        return ctx.mkConst(variable.getName(),ctx.getStringSort());
    }

    @Override
    public SimpleType getType() {
        return type;
    }

    @Override
    public String getName() {
        return super.getName();
    }
}
