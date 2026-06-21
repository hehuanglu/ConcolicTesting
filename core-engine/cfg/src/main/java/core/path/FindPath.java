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

    public FindPath(CfgNode beginNode, CfgNode middleNode, CfgNode endNode) {}

    public FindPath(CfgNode beginNode, CfgNode middleNode) {
        findPath(beginNode, middleNode, 2);
    }

    public FindPath(CfgNode beginNode, CfgNode middleNode, boolean isGoingTrueBranch) {
        findPath(beginNode, middleNode, isGoingTrueBranch ? 1 : 0);
    }

    private void findPath(CfgNode beginNode, CfgNode endNode, int type) {
        if (beginNode == null || path != null) return;
        if (visited.contains(beginNode)) return;

        // Nếu đã đến đích
        if (beginNode == endNode) {
            currentPath.add(beginNode);
            path = new Path();
            for (CfgNode node : currentPath) {
                path.addLast(node);
            }
            if (type < 2) {
                if (endNode instanceof CfgBoolExprNode) {
                    CfgBoolExprNode boolExprNode = (CfgBoolExprNode) endNode;
                    if (type == 1) {
                        path.addLast(boolExprNode.getTrueNode());
                    } else if (type == 0) {
                        path.addLast(boolExprNode.getFalseNode());
                    }
                }
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
                findPath(firstNode, endNode, type);
            }
            if (path == null) {
                secondNode.setIsFalseNode(secondNode == falseNode);
                findPath(secondNode, endNode, type);
            }

        } else if (beginNode instanceof CfgForEachExpressionNode) {
            CfgForEachExpressionNode forNode = (CfgForEachExpressionNode) beginNode;
            if (path == null) {
                findPath(forNode.getHasElementAfterNode(), endNode, type);
            }
            if (path == null) {
                findPath(forNode.getNoMoreElementAfterNode(), endNode, type);
            }

        } else {
            if (path == null) {
                findPath(beginNode.getAfterStatementNode(), endNode, type);
            }
        }

        currentPath.remove(currentPath.size() - 1);
        visited.remove(beginNode);
    }

    public Path getPath() {
        return path;
    }
}
