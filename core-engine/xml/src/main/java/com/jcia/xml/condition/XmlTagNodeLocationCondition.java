package com.jcia.xml.condition;

import com.jcia.xml.dom.Node;
import com.jcia.xml.dom.Xml.XmlTagNode;

public class XmlTagNodeLocationCondition implements ICondition {

    private int lineNum;
    private int colNum;

    public XmlTagNodeLocationCondition(int lineNum, int colNum) {
        this.lineNum = lineNum;
        this.colNum = colNum;
    }

    @Override
    public boolean isSatisfiable(Node node) {
        if (node instanceof XmlTagNode) {
            XmlTagNode xmlTagNode = (XmlTagNode) node;
            return xmlTagNode.getLineNumber() == lineNum && xmlTagNode.getColumnNumber() == colNum;
        }
        return false;
    }
}