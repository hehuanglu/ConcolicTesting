package com.jcia.xml.parser;

import com.jcia.xml.dom.Node;
import com.jcia.xml.utils.Exception.JciaNotFoundException;

public interface IPathParser {
    Node parse(String path) throws JciaNotFoundException;
}
