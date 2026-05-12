package com.example.uploadprojectservice.ast.Node;

import java.io.Serializable;
import java.util.List;

public class JavaNode extends Node implements Serializable {

        public JavaNode() {
        }

        public JavaNode(int id, String name, List<Node> children, String path) {
            super(id, name, "JavaNode", children, path);
        }

    public JavaNode(int id, String name, String entityClass, List<Node> children, String path) {
        super(id, name, entityClass, children, path);
    }

    public JavaNode(int id, String path) {
            super(id, path);
        }

}
