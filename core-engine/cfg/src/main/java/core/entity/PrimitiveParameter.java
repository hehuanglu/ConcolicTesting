package core.entity;

public class PrimitiveParameter extends Parameter{
    public PrimitiveParameter(String simpleName) {
        super(simpleName);
        this.type = "PRIMITIVE";
    }
}
