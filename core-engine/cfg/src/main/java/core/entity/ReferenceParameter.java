package core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceParameter extends Parameter{
    private String qualifiedName;

    private Map<String, Object> attributeToValue;

    private Map<String, Object> stubMethods;

    private Class<?> classParameter;

    public ReferenceParameter(String qualifiedName, String simpleName) {
        super(simpleName);
        this.qualifiedName = qualifiedName;
        try {
            this.classParameter = Class.forName(qualifiedName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.stubMethods = new HashMap<>();
        this.type = "REFERENCE";
    }

    public void stubMethod(String nameMethod, Object value) {
        stubMethods.put(nameMethod, value);
    }

}
