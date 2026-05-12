package com.example.uploadprojectservice.ast.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MethodNode extends Node implements Serializable {

    private String qualifiedName;
    private String uniqueName;

    private boolean changed = false;
    private boolean added = false;
    private boolean impacted = false;

    public MethodNode() {
    }

    public MethodNode(int id, String name, List children, String path, String qualifiedName) {
        super(id, name, "JavaMethodNode", children, path);
        this.qualifiedName = qualifiedName;
    }

    public MethodNode(int methodNodeId, String methodName, ArrayList<Object> children, String absolutePath, String qualifiedName, String uniqueName) {
        super(methodNodeId, methodName, "JavaMethodNode", children, absolutePath);
        this.qualifiedName = qualifiedName;
        this.uniqueName = uniqueName;
    }


    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public boolean isAdded() {
        return added;
    }

    public void setAdded(boolean added) {
        this.added = added;
    }

    public boolean isImpacted() {
        return impacted;
    }

    public void setImpacted(boolean impacted) {
        this.impacted = impacted;
    }
}
