package core.testResult.result.autoTestResult;

public class ParameterData {
    private String name;
    private String type;
    private Object value;

    public ParameterData() {
    }

    public ParameterData(String name, String type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

//        result.append(type + " " + name + " = " + value);
        result.append(value);

        return result.toString();
    }
}

