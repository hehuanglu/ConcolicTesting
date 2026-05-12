package com.jcia.xml.condition;

import com.jcia.xml.dom.Node;

public interface ICondition {
    boolean isSatisfiable(Node node);
}
