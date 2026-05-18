package core.variable;

import com.microsoft.z3.Expr;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;

public class ParameterizedTypeVariable extends Variable {
    private ParameterizedType parameterizedType;
    private String collectionType;
    private Expr size;

    public ParameterizedTypeVariable(ParameterizedType parameterizedType, String name, Expr size) {
        this.parameterizedType = parameterizedType;
        super.setName(name);
        this.collectionType = parameterizedType.getType().toString();
        this.size = size;
    }

    public String getCollectionType() {
        return collectionType;
    }

    public Expr getSize() {
        return size;
    }


    @Override
    public Type getType() {
        return this.parameterizedType;
    }
}