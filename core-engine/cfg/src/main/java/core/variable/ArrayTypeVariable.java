package core.variable;

import org.eclipse.jdt.core.dom.ArrayType;

import java.util.ArrayList;
import java.util.List;

public class ArrayTypeVariable extends Variable {
    private ArrayType type;
    private int numberOfDimensions;
    private int[] dimensionsCapacity;
    private List<String> specificElementsConstraint = new ArrayList<>();

    /*
     * number of dimensions
     * capacity for each dimensions
     * any specific element value
     *
     * ex:
     * 2 10 10 2|2####5
     *
     * */

    public ArrayTypeVariable(ArrayType type, String name, int numberOfDimensions) {
        this.type = type;
        super.setName(name);
        this.numberOfDimensions = numberOfDimensions;
        this.dimensionsCapacity = new int[numberOfDimensions];
        for (int i = 0; i < numberOfDimensions; i++) {
            dimensionsCapacity[i] = 10;
        }
    }

    public String getConstraints() {
        StringBuilder result = new StringBuilder();
        result.append(numberOfDimensions);
        for (int i : dimensionsCapacity) {
            result.append(" ").append(i);
        }
        for (String i : specificElementsConstraint) {
            result.append(" ").append(i);
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object another) {
        if (another instanceof ArrayTypeVariable) {
            ArrayTypeVariable arrayTypeVariable = (ArrayTypeVariable) another;
            if (arrayTypeVariable.getName().equals(this.getName())) return true;
            else return false;
        }
        return false;
    }

    @Override
    public ArrayType getType() {
        return type;
    }
}
