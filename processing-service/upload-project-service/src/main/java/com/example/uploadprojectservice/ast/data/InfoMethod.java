package com.example.uploadprojectservice.ast.data;

import com.example.uploadprojectservice.ast.Node.MethodNode;
import com.example.uploadprojectservice.ast.Node.Parameter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InfoMethod extends MethodNode implements Serializable{

    @JsonProperty("content")
    private StringBuilder content;

    private List<Parameter>  parameters;
    private ArrayList<Integer> numberOfSentences;

    public InfoMethod() {
    }

    public InfoMethod(int id, String name, List children, String path, String qualifiedName, String uniqueName, StringBuilder content, List<Parameter>  parameters, ArrayList<Integer> numberOfSentences) {
        super(id, name, (ArrayList<Object>) children, path, qualifiedName, uniqueName);
        this.content = content;
        this.parameters = parameters;
        this.numberOfSentences = numberOfSentences;
    }

    public InfoMethod(int id, String name, ArrayList<Object> children, String path, String qualifiedName, String uniqueName, StringBuilder content) {
        super(id, name, children, path, qualifiedName, uniqueName);
        this.content = content;
    }

    public StringBuilder getContent() {
        return content;
    }

    public void setContent(StringBuilder content) {
        this.content = content;
    }

    public List getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public ArrayList<Integer> getNumberOfSentences() {
        return numberOfSentences;
    }

    public void setNumberOfSentences(ArrayList<Integer> numberOfSentences) {
        this.numberOfSentences = numberOfSentences;
    }
}
