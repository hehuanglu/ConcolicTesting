package com.example.uploadprojectservice.model;

import com.example.uploadprojectservice.ast.Node.Node;

public class Response {
    private Node rootNode;

    public Response() {
    }

    public Response(Node rootNode) {
        this.rootNode = rootNode;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public void setRootNode(Node rootNode) {
        this.rootNode = rootNode;
    }

}
