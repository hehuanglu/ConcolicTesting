package core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Parameter {
    protected String simpleName;

    protected String type;

    protected Object value;

    protected String variable;

    public Parameter(String simpleName) {
        this.simpleName = simpleName;
    }
}
