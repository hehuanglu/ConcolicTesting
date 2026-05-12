package com.jcia.xml.condition;

import com.jcia.xml.dom.Jsp.JspFileNode;
import com.jcia.xml.dom.Node;

public class JspFileNodeCondition implements ICondition {
    @Override
    public boolean isSatisfiable(Node node) {
        if (node instanceof JspFileNode) {
            return true;
        }
        return false;
    }
}
