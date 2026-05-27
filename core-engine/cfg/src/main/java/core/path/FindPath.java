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
        /*
        Path firstHaft = path;
        path = null;
        findPath(middleNode.getAfterStatementNode(), endNode);
        Path lastHaft = path;
        if(lastHaft != null) {
            firstHaft.getCurrentLast().setNext(lastHaft.getCurrentFirst());
        }
        path = firstHaft;

         */
    }

    private void findPath(CfgNode beginNode, CfgNode endNode) {
        if (beginNode == null || path != null) return;
        if (visited.contains(beginNode)) return;

        // Nếu đã đến đích
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
        }

        currentPath.add(beginNode);
        visited.add(beginNode);

        if (beginNode instanceof CfgBoolExprNode) {
            CfgBoolExprNode boolNode = (CfgBoolExprNode) beginNode;
            CfgNode falseNode = boolNode.getFalseNode();
            CfgNode trueNode = boolNode.getTrueNode();

            // Lấy số lần fake đã đánh dấu
            int falseFake = boolNode.getFakeFalseMarked();
            int trueFake = boolNode.getFakeTrueMarked();

            // Chọn thứ tự thử: nhánh ít fake trước
            CfgNode firstNode, secondNode;
            if (falseFake < trueFake) {
                firstNode = falseNode;
                secondNode = trueNode;
            } else {
                firstNode = trueNode;
                secondNode = falseNode;
            }

            // Thử nhánh ít fake trước
            if (path == null) {
                firstNode.setIsFalseNode(firstNode == falseNode);
                findPath(firstNode, endNode);
            }
            if (path == null) {
                secondNode.setIsFalseNode(secondNode == falseNode);
                findPath(secondNode, endNode);
            }

        } else if (beginNode instanceof CfgForEachExpressionNode) {
            CfgForEachExpressionNode forNode = (CfgForEachExpressionNode) beginNode;
            if (path == null) {
                findPath(forNode.getHasElementAfterNode(), endNode);
            }
            if (path == null) {
                findPath(forNode.getNoMoreElementAfterNode(), endNode);
            }

        } else {
            if (path == null) {
                findPath(beginNode.getAfterStatementNode(), endNode);
            }
        }

        currentPath.remove(currentPath.size() - 1);
        visited.remove(beginNode);
    }

    public Path getPath() {
        return path;
    }
}
