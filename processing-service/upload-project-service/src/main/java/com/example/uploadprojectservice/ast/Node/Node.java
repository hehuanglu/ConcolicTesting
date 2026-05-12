package com.example.uploadprojectservice.ast.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Node implements Serializable {
    private int id;
    private String simpleName;
    private String entityClass;
    private String path;
    private List children = new ArrayList<>();

    public Node() {
    }

    public Node(int id, String simpleName, String entityClass, List children, String path) {
        this.id = id;
        this.simpleName = simpleName;
        this.entityClass = entityClass;
        this.children = children;
        this.path = path;
    }
    public Node(int id, String path){
        this.id = id;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(String name) {
        this.simpleName = name;
    }

    public String getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
    }

    public List getChildren() {
        return children;
    }

    public void setChildren(List children) {
        this.children = children;
    }

}
