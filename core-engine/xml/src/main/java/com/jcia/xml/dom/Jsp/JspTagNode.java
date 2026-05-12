package com.jcia.xml.dom.Jsp;

import com.jcia.xml.dom.Node;
import com.jcia.xml.utils.Exception.JciaIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jsoup.nodes.Element;

import java.util.Map;

public class JspTagNode extends Node {

    private String tagName;

    @JsonIgnore
    @JciaIgnore
    private Element domaNode;

    private Map<String, String> attributes;
    private String ownerText;

    public Element getDomaNode() {
        return domaNode;
    }

    public void setDomaNode(Element domaNode) {
        this.domaNode = domaNode;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getOwnerText() {
        return ownerText;
    }

    public void setOwnerText(String ownerText) {
        this.ownerText = ownerText;
    }

    public String getTagName() {

        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

}
