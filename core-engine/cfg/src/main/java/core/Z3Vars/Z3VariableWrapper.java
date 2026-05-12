package core.Z3Vars;

import com.microsoft.z3.Expr;
import core.variable.ArrayTypeVariable;

public class Z3VariableWrapper {
    private Expr primitiveVar = null;
    private ArrayTypeVariable arrayVar = null;

    public Z3VariableWrapper(Expr primitiveVar) {
        this.primitiveVar = primitiveVar;
    }

    public Z3VariableWrapper(ArrayTypeVariable arrayVar) {
        this.arrayVar = arrayVar;
    }

    public Expr getPrimitiveVar() {
        return primitiveVar;
    }

    public ArrayTypeVariable getArrayVar() {
        return arrayVar;
    }

    @Override
    public boolean equals(Object another) {
        if (another instanceof Z3VariableWrapper) {
            Z3VariableWrapper z3Variable = (Z3VariableWrapper) another;
            if (primitiveVar != null && z3Variable.primitiveVar != null) {
                return primitiveVar.toString().equals(z3Variable.primitiveVar.toString());
            } else if (arrayVar != null && z3Variable.arrayVar != null) {
                return arrayVar.equals(z3Variable.arrayVar);
            } else return false;
        }
        return false;
    }
}
