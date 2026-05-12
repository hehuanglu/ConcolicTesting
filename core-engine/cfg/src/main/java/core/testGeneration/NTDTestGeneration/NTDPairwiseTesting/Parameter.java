package core.testGeneration.NTDTestGeneration.NTDPairwiseTesting;

import java.util.ArrayList;
import java.util.List;

public class Parameter {
    private String name;
    private List<Object> values = new ArrayList<>();

    public Parameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Object> getValues() {
        return values;
    }

    public void addValue(Object value) {
        this.values.add(value);
    }

    public void addDistinctValue(Object distinctValue) {
        for (Object value : values) {
            if (value.equals(distinctValue)) {
                return;
            }
        }
        values.add(distinctValue);
    }

    public void addDistinctValues(List<Object> distinctValues) {
        for (Object distinctValue : distinctValues) {
            if (!values.contains(distinctValue)) values.add(distinctValue);
        }
    }
}
