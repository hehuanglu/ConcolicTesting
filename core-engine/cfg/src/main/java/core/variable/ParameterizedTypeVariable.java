package core.variable;

import com.microsoft.z3.Expr;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.List;

public class ParameterizedTypeVariable extends Variable {
    private ParameterizedType parameterizedType;
    private String collectionType;
    private List<Expr> sizeHistory = new ArrayList<>();
    private int version = 0;

    public ParameterizedTypeVariable(ParameterizedType parameterizedType, String name, Expr size) {
        super.setName(name);
        this.parameterizedType = parameterizedType;
        this.collectionType = parameterizedType.getType().toString();
        this.sizeHistory.add(size);
        this.version = 0;
    }


    public String getCollectionType() {
        return collectionType;
    }

    public int getVersion() {
        return version;
    }

    public Expr getLatestSize() {
        return this.sizeHistory.get(this.sizeHistory.size() - 1);
    }

    public Expr getBaseSize() {
        return this.sizeHistory.get(0);
    }

    public void addNewSizeVersion(Expr newSize) {
        this.sizeHistory.add(newSize);
    }

    public void incrementVersion() {
        this.version++;
    }


    @Override
    public Type getType() {
        return this.parameterizedType;
    }
}