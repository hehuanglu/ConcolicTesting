package core.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ReferenceArgument extends Argument{
    private String path;

    private Map<String, Object> stubMethods;

    private Map<String, Object> valueAttributes;

    public ReferenceArgument(String name, String type) {
        super(name, type);
        this.stubMethods = new HashMap<>();
        this.valueAttributes = new HashMap<>();
    }

    public void addMethod(String nameMethod) {
        if (!stubMethods.containsKey(nameMethod)) {
            stubMethods.put(nameMethod, null);
        }
    }

    public void stubMethod(String nameMethod, Object value) {
        stubMethods.put(nameMethod, value);
    }
}
