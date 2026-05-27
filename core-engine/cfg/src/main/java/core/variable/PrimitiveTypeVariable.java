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

        // Nhóm các kiểu số nguyên (Byte, Short, Char, Int, Long) về Z3 Int
        if (code.equals(PrimitiveType.BYTE) ||
                code.equals(PrimitiveType.SHORT) ||
                code.equals(PrimitiveType.CHAR) ||
                code.equals(PrimitiveType.INT) ||
                code.equals(PrimitiveType.LONG)) {

            return ctx.mkIntConst(name);
        }
        // Nhóm các kiểu số thực (Float, Double) về Z3 Real
        else if (code.equals(PrimitiveType.FLOAT) || code.equals(PrimitiveType.DOUBLE)) {
            return ctx.mkRealConst(name);
        }
        // Kiểu Boolean giữ nguyên
        else if (code.equals(PrimitiveType.BOOLEAN)) {
            return ctx.mkBoolConst(name);
        }
        else {
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
