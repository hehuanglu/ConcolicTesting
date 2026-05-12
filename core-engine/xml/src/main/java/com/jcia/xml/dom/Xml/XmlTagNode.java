package com.jcia.xml.dom.Xml;

import com.jcia.xml.dom.Node;
import com.jcia.xml.utils.Exception.JciaIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XmlTagNode extends Node {
    private static final long serialVersionUID = -1245660851667263403L;
    protected String tagName;
    protected int numOfAttr;
    protected int lineNumber;
    protected int columnNumber;
    protected List<String> listAttr;


    public List<String> getListAttr() {
        return listAttr;
    }

    public void setListAttr(List<String> listAttr) {
        this.listAttr = listAttr;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    public int getNumOfAttr() {
        return numOfAttr;
    }

    public void setNumOfAttr(int numOfAttr) {
        this.numOfAttr = numOfAttr;
    }

    @JsonIgnore
    @JciaIgnore
    protected org.w3c.dom.Node domNode;

    protected String content;
    protected Map<String, String> attributes;

    public XmlTagNode() {
        listAttr = new ArrayList<>();
    }

    public void addToListAttr(String attr)
    {
        this.listAttr.add(attr);
    }
    public XmlTagNode(String name) {
        this();
        this.tagName = name;
    }

    public XmlTagNode(String name, String content) {
        this.tagName = name;
        this.content = content;
    }

    public XmlTagNode(String name, org.w3c.dom.Node domNode, Map<String, String> attributes, String content) {
        this.tagName = name;
        this.attributes = attributes;
    }

    public org.w3c.dom.Node getDomNode() {
        return domNode;
    }

    public void setDomNode(org.w3c.dom.Node domNode) {
        this.domNode = domNode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @JsonIgnore
    public int getLineNumber() {
        String lineStr = attributes.get("loc:line");
        return (lineStr != null && !lineStr.isEmpty()) ? Integer.parseInt(lineStr) : -1;
    }

    @JsonIgnore
    public int getColumnNumber() {
        String columnStr = attributes.get("loc:column");
        return (columnStr != null && !columnStr.isEmpty()) ? Integer.parseInt(columnStr) : -1;
    }

    @Override
    public String toString() {
        return "XmlTagNode{" +
                "tagName='" + tagName + '\'' +
                ", domNode=" + domNode +
                ", attributes=" + attributes +
                ", content='" + content + '\'' +
                ", children='" + getChildren() + '\'' +
                '}';
    }
}
