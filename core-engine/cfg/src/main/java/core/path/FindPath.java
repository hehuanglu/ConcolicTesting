package core.path;

import core.cfg.CfgBoolExprNode;
import core.cfg.CfgForEachExpressionNode;
import core.cfg.CfgNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FindPath {

    private List<CfgNode> currentPath = new ArrayList<>();
    private Path path;
    private CfgNode currentDuplicateNode;
    private Set<CfgNode> visited = new HashSet<>();

    private FindPath() {}

    public FindPath(CfgNode beginNode, CfgNode middleNode, CfgNode endNode) {
        findPath(beginNode, middleNode);
        Path firstHaft = path;
        path = null;
        findPath(middleNode.getAfterStatementNode(), endNode);
        Path lastHaft = path;
        if(lastHaft != null) {
            firstHaft.getCurrentLast().setNext(lastHaft.getCurrentFirst());
        }
        path = firstHaft;
    }

    private void findPath(CfgNode beginNode, CfgNode endNode) {
        if (beginNode == null || path != null) return;
        if (visited.contains(beginNode)) return;

        // Add a path to the list of path if the node is endNode
        if (beginNode == endNode) {
            currentPath.add(beginNode);
            path = new Path();
            for (CfgNode node : currentPath) {
                path.addLast(node);
            }
            currentPath.remove(currentPath.size() - 1);
            visited.remove(beginNode);
            return;
        } else if (beginNode.getIsEndCfgNode()) {
            return;
        } else {
            currentPath.add(beginNode);
            visited.add(beginNode);
            if (beginNode instanceof CfgBoolExprNode) {
                CfgBoolExprNode boolExprNode = (CfgBoolExprNode) beginNode;
                CfgNode falseNode = boolExprNode.getFalseNode();
                CfgNode trueNode = boolExprNode.getTrueNode();

                if (path == null) {
                    if (falseNode == endNode) {
                        falseNode.setIsFalseNode(true);
                        findPath(falseNode, endNode);
                    }
                }
                if (path == null) {
                    findPath(trueNode, endNode);
                }
                if (path == null) {
                    falseNode.setIsFalseNode(true);
                    findPath(falseNode, endNode);
                }
            } else if (beginNode instanceof CfgForEachExpressionNode) {
                if (path == null) {
                    findPath(((CfgForEachExpressionNode) beginNode).getHasElementAfterNode(), endNode);
                }
                if (path == null) {
                    findPath(((CfgForEachExpressionNode) beginNode).getNoMoreElementAfterNode(), endNode);
                }
            } else {
                if (path == null) {
                    findPath(beginNode.getAfterStatementNode(), endNode);
                }
            }
            currentPath.remove(currentPath.size() - 1);
            visited.remove(beginNode);
        }
    }

    public Path getPath() {
        return path;
    }
}
