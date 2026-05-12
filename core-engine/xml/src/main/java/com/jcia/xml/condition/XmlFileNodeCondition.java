package com.jcia.xml.condition;

import com.jcia.xml.dom.Node;
import com.jcia.xml.dom.Xml.XmlFileNode;

public class XmlFileNodeCondition implements ICondition {
    @Override
    public boolean isSatisfiable(Node node) {
        if (node instanceof XmlFileNode) {
            return true;
        }
        return false;
    }
}
