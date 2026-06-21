package core.variable;

import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;

public class ParameterizedTypeVariable extends Variable {
    private ParameterizedType parameterizedType;
    private String collectionType;

    public ParameterizedTypeVariable(ParameterizedType parameterizedType, String name) {
        this.parameterizedType = parameterizedType;
        super.setName(name);
        this.collectionType = parameterizedType.getType().toString();
    }

    public String getCollectionType() {
        return collectionType;
    }


    @Override
    public Type getType() {
        return this.parameterizedType;
    }
}