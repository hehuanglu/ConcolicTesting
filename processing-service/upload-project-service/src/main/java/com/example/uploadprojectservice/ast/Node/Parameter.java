package com.example.uploadprojectservice.ast.Node;

public class Parameter {
    private String nameParameter;
    private String describe;
    private String value;
    public Parameter(){

    }

    public Parameter(String nameParameter, String describe) {
        this.nameParameter = nameParameter;
        this.describe = describe;
    }

    public Parameter(String nameParameter, String describe, String value) {
        this.nameParameter = nameParameter;
        this.describe = describe;
        this.value = value;
    }

    public String getNameParameter() {
        return nameParameter;
    }

    public void setNameParameter(String nameParameter) {
        this.nameParameter = nameParameter;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
