package com.jcia.xml.condition;

import com.jcia.xml.dom.Node;
import com.jcia.xml.dom.Xml.XmlTagNode;

public class XmlTagNodeCondition implements ICondition {

    @Override
    public boolean isSatisfiable(Node node) {
        return (node instanceof XmlTagNode);
    }
    
}
