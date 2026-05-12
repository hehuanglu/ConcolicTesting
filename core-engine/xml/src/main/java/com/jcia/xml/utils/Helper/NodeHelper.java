package com.jcia.xml.utils.Helper;

import com.jcia.xml.dom.FileNode;
import com.jcia.xml.dom.Node;
import com.jcia.xml.dom.Xml.XmlTagNode;

public class NodeHelper {
    public static Node getFileAndTableScopedNode(Node node) {
        if (node instanceof XmlTagNode) {
            node = getFileNode(node);
        }
        return node;
    }
    public static Node getFileNode(Node node) {
        if (node == null || node instanceof FileNode)
            return node;
        else return getFileNode(node.getParent());
    }
}
