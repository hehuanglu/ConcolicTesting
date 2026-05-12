package core.path;

import core.cfg.CfgBoolExprNode;
import core.cfg.CfgForEachExpressionNode;
import core.cfg.CfgNode;

import java.util.ArrayList;
import java.util.List;

public class FindAllPath {

    private List<Path> paths = new ArrayList<>();
    private List<CfgNode> currentPath = new ArrayList<>();

    private final int DEPTH = 2;

    private FindAllPath() {}

    public FindAllPath(CfgNode cfgRootNode) {
//        findPaths(cfgRootNode, null);
        findPaths(cfgRootNode);
    }

    private void findPaths(CfgNode cfgNode) {
        if(cfgNode == null) return;

        // Add a path to the list of path if the node is endNode
        if(cfgNode.getIsEndCfgNode()) {
            currentPath.add(cfgNode);
            Path path = new Path();
            for(CfgNode node : currentPath) {
                path.addLast(node);
            }
            paths.add(path);
            currentPath.remove(currentPath.size() - 1);
        } else {
            currentPath.add(cfgNode);
            if(cfgNode instanceof CfgBoolExprNode) {
                int numberOfDuplicateNode = numberOfDuplicateNode(cfgNode);
                CfgBoolExprNode boolExprNode = (CfgBoolExprNode) cfgNode;

                // CfgBoolExprNode has 2 child node is trueNode and falseNode
                if(numberOfDuplicateNode < DEPTH) {
                    findPaths(boolExprNode.getTrueNode());
                }
                CfgNode falseNode = boolExprNode.getFalseNode();
                falseNode.setIsFalseNode(true);
                findPaths(falseNode);

            } else if(cfgNode instanceof CfgForEachExpressionNode) {
                int duplicateNode = numberOfDuplicateNode(cfgNode);

                // CfgForEachExpressionNode has 2 child node is hasElementNode and noMoreElementNode
                if(duplicateNode < DEPTH) {
                    findPaths(((CfgForEachExpressionNode) cfgNode).getHasElementAfterNode());
                }
                findPaths(((CfgForEachExpressionNode) cfgNode).getNoMoreElementAfterNode());

            } else {

                // Every other node has only one child node
                findPaths(cfgNode.getAfterStatementNode());

            }
            currentPath.remove(currentPath.size() - 1);
        }
    }

//    private void findPaths(CfgNode cfgNode, CfgNode duplicateNode) {
//        if(cfgNode == null) return;
//
//        // Add a path to the list of path if the node is endNode
//        if(cfgNode.getIsEndCfgNode()) {
//            currentPath.add(cfgNode);
//            Path path = new Path();
//            for(CfgNode node : currentPath) {
//                path.addLast(node);
//            }
//            paths.add(path);
//            currentPath.remove(currentPath.size() - 1);
//        } else {
//            currentPath.add(cfgNode);
//            if(cfgNode instanceof CfgBoolExprNode) {
//                CfgBoolExprNode boolExprNode = (CfgBoolExprNode) cfgNode;
//
//                if(cfgNode != duplicateNode) {
//                    duplicateNode = cfgNode;
//                    findPaths(boolExprNode.getTrueNode(), duplicateNode);
//                }
//
//                CfgNode falseNode = boolExprNode.getFalseNode();
//                falseNode.setIsFalseNode(true);
//                findPaths(falseNode, duplicateNode);
//
//            } else if(cfgNode instanceof CfgForEachExpressionNode) {
//                // CfgForEachExpressionNode has 2 child node is hasElementNode and noMoreElementNode
//                if(cfgNode != duplicateNode) {
//                    duplicateNode = cfgNode;
//                    findPaths(((CfgForEachExpressionNode) cfgNode).getHasElementAfterNode(), duplicateNode);
//                }
//                findPaths(((CfgForEachExpressionNode) cfgNode).getNoMoreElementAfterNode(), duplicateNode);
//
//            } else {
//
//                // Every other node has only one child node
//                findPaths(cfgNode.getAfterStatementNode(), duplicateNode);
//
//            }
//            currentPath.remove(currentPath.size() - 1);
//        }
//    }

    private int numberOfDuplicateNode(CfgNode node) {
        int duplicateNode = 0;
        for(CfgNode nodeI : currentPath) {
            if(nodeI == node) duplicateNode++;
        }
        return duplicateNode;
    }

    public List<Path> getPaths() {
        return paths;
    }
}
