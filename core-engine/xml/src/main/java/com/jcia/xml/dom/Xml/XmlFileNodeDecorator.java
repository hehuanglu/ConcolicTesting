package com.jcia.xml.dom.Xml;

import com.jcia.xml.dom.Node;
import com.jcia.xml.utils.Type.ComponentType;
import com.jcia.xml.utils.Type.Tier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Set;

public class XmlFileNodeDecorator extends XmlFileNode {

    @JsonIgnore
    protected XmlFileNode xmlFileNode;

    public XmlFileNodeDecorator() {
        xmlFileNode = new XmlFileNode();
    }

    public XmlFileNodeDecorator(XmlFileNode xmlFileNode) {
        this.xmlFileNode = xmlFileNode;
    }

    public XmlFileNode getXmlFileNode() {
        return xmlFileNode;
    }

    @Override
    public Document getDocument() {
        return xmlFileNode.getDocument();
    }

    @Override
    public void setDocument(Document document) {
        xmlFileNode.setDocument(document);
    }

    @Override
    public String getAbsolutePath() {
        return xmlFileNode.getAbsolutePath();
    }

    @Override
    public void setAbsolutePath(String absolutePath) {
        xmlFileNode.setAbsolutePath(absolutePath);
    }

    @Override
    public Node getParent() {
        return xmlFileNode.getParent();
    }

    @Override
    public void setParent(Node parent) {
        xmlFileNode.setParent(parent);
    }

    @Override
    public List<Node> getChildren() {
        return xmlFileNode.getChildren();
    }

    @Override
    public void setChildren(List<Node> children) {
        xmlFileNode.setChildren(children);
    }

    @Override
    public void addChild(Node child) {
        xmlFileNode.addChild(child);
    }

    @Override
    public String getName() {
        return xmlFileNode.getName();
    }

    @Override
    public void setName(String name) {
        xmlFileNode.setName(name);
    }

    @Override
    public void setAbsolutePathByName() {
        xmlFileNode.setAbsolutePathByName();
    }

    @Override
    public void setId(int id) {
        xmlFileNode.setId(id);
    }

    @Override
    public int getId() {
        return xmlFileNode.getId();
    }

//    @Override
//    public List<Dependency> getDependencies() {
//        return xmlFileNode.getDependencies();
//    }
//
//    @Override
//    public void addDependency(Dependency dependency) {
//        xmlFileNode.addDependency(dependency);
//    }
//
//    @Override
//    public List<Node> getCallees() {
//        return xmlFileNode.getCallees();
//    }
//
//    @Override
//    public List<Node> getCallers() {
//        return xmlFileNode.getCallers();
//    }

    @Override
    public boolean equals(Object obj) {
        return xmlFileNode.equals(obj);
    }

    @Override
    public String getRelativePath() {
        return xmlFileNode.getRelativePath();
    }

    @Override
    public Node getRootNode(Node n) {
        return xmlFileNode.getRootNode(n);
    }

    @Override
    public Node getSubProjectNode(Node n) {
        return xmlFileNode.getSubProjectNode(n);
    }

    @Override
    public Set<Tier> getTiers() {
        return xmlFileNode.getTiers();
    }

    @Override
    public void addComponentType(ComponentType type) {
        xmlFileNode.addComponentType(type);
    }

    @Override
    public int getLeft() {
        return xmlFileNode.getLeft();
    }

    @Override
    public int getRight() {
        return xmlFileNode.getRight();
    }

    @Override
    public void setLeft(int l) {
        xmlFileNode.setLeft(l);
    }

    @Override
    public void setRight(int r) {
        xmlFileNode.setRight(r);
    }

    public void setXmlFileNode(XmlFileNode node) {
        this.xmlFileNode = node;
    }
}

