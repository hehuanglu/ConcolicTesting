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
        SymbolicExecutionRewrite.variableTypeMap.put(name, code);
        int bitSize;

        if (code.equals(PrimitiveType.BYTE)) {
            bitSize = 8;
            return ctx.mkBVConst(name, bitSize);
        } else if (code.equals(PrimitiveType.SHORT)) {
            bitSize = 16;
            return ctx.mkBVConst(name, bitSize);
        } else if (code.equals(PrimitiveType.CHAR)) {
            bitSize = 16; // unsigned
            return ctx.mkBVConst(name, bitSize);
        } else if (code.equals(PrimitiveType.INT)) {
            bitSize = 32;
            return ctx.mkBVConst(name, bitSize);
        } else if (code.equals(PrimitiveType.LONG)) {
            bitSize = 64;
            return ctx.mkBVConst(name, bitSize);
        } else if (code.equals(PrimitiveType.FLOAT)) {
            FPSort f32 = ctx.mkFPSort32(); // tạo sort float (1/8/23) cho biến
            return ctx.mkConst(name, f32);
        } else if (code.equals(PrimitiveType.DOUBLE)) {
            FPSort f64 = ctx.mkFPSort64(); // tạo sort double (1/11/52) cho biến
            return ctx.mkConst(name, f64);
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
