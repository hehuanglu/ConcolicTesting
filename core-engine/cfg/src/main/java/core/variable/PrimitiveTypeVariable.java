package core.variable;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FPSort;
import core.symbolicExecution.SymbolicExecutionRewrite;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

import java.util.Objects;

public class PrimitiveTypeVariable extends Variable {
    private PrimitiveType primitiveType;

    public PrimitiveTypeVariable(PrimitiveType primitiveType, String name) {
        this.primitiveType = primitiveType;
        super.setName(name);
    }

    public static Expr createZ3PrimitiveTypeVariable(PrimitiveTypeVariable primitiveTypeVariable, Context ctx) {
        PrimitiveType.Code code = primitiveTypeVariable.getCode();
        String name = primitiveTypeVariable.getName();
        SymbolicExecutionRewrite.variableTypeMap.put(name, name);

        if (code.equals(PrimitiveType.BYTE)) {
            return ctx.mkIntConst(name);
        } else if (code.equals(PrimitiveType.SHORT)) {
            return ctx.mkIntConst(name);
        } else if (code.equals(PrimitiveType.CHAR)) {
            return ctx.mkIntConst(name); // range 0–65535
        } else if (code.equals(PrimitiveType.INT)) {
            return ctx.mkIntConst(name);
        } else if (code.equals(PrimitiveType.LONG)) {
            return ctx.mkIntConst(name);
        } else if (code.equals(PrimitiveType.FLOAT)) {
            return ctx.mkConst(name, ctx.mkFPSort32());
        } else if (code.equals(PrimitiveType.DOUBLE)) {
            return ctx.mkConst(name, ctx.mkFPSort64());
        } else if (code.equals(PrimitiveType.BOOLEAN)) {
            return ctx.mkBoolConst(name);
        } else {
            throw new RuntimeException("Invalid type: " + code);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimitiveTypeVariable that = (PrimitiveTypeVariable) o;
        // So sánh dựa trên tên biến (name). Giả sử bạn có field 'name'
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }


    public PrimitiveType.Code getCode() {
        return primitiveType.getPrimitiveTypeCode();
    }

    @Override
    public Type getType() {
        return this.primitiveType;
    }
}
