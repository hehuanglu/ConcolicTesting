package core.path;

import core.cfg.CfgBoolExprNode;
import core.cfg.CfgForEachExpressionNode;
import core.cfg.CfgNode;
import core.cfg.CfgReturnStatementNode;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;

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
        findPath(middleNode, endNode);
    }

    public FindPath(CfgNode beginNode, CfgNode middleNode) {
        findPath(beginNode, middleNode);
    }


    private void findPath(CfgNode beginNode, CfgNode endNode) {
        if (beginNode == null || path != null) return;
        if (visited.contains(beginNode)) return;

        if (beginNode == endNode) {
            currentPath.add(beginNode);
            path = new Path();
            for (CfgNode node : currentPath) {
                path.addLast(node);
            }
            currentPath.remove(currentPath.size() - 1);
            visited.remove(beginNode);
            return;
        } else if (beginNode.getIsEndCfgNode()
                || beginNode.getAst() instanceof ReturnStatement
                || beginNode.getAst() instanceof ThrowStatement) {
            return;
        }

        currentPath.add(beginNode);
        visited.add(beginNode);

        if (beginNode instanceof CfgBoolExprNode) {
            CfgBoolExprNode boolNode = (CfgBoolExprNode) beginNode;
            CfgNode falseNode = boolNode.getFalseNode();
            CfgNode trueNode = boolNode.getTrueNode();

            CfgNode firstNode, secondNode;
            boolean firstIsTrueBranch;
            if (boolNode.falseCounting < boolNode.trueCounting) {
                firstNode = falseNode;
                secondNode = trueNode;
                firstIsTrueBranch = false;
            } else {
                firstNode = trueNode;
                secondNode = falseNode;
                firstIsTrueBranch = true;
            }

            if (path == null) {
                findPath(firstNode, endNode);
                if (path == null) {
                    // Ghi nhận thất bại VĨNH VIỄN, không giảm lại
                    if (firstIsTrueBranch) boolNode.trueCounting++;
                    else boolNode.falseCounting++;
                }
            }
            if (path == null) {
                findPath(secondNode, endNode);
                if (path == null) {
                    if (firstIsTrueBranch) boolNode.falseCounting++;
                    else boolNode.trueCounting++;
                }
            }

        } else if (beginNode instanceof CfgForEachExpressionNode) {
            CfgForEachExpressionNode forNode = (CfgForEachExpressionNode) beginNode;
            CfgNode hasElementNode = forNode.getHasElementAfterNode();
            CfgNode noMoreNode = forNode.getNoMoreElementAfterNode();

            CfgNode firstNode, secondNode;
            boolean firstIsHasElement;
            if (forNode.noMoreElementCounting < forNode.hasElementCounting) {
                firstNode = noMoreNode;
                secondNode = hasElementNode;
                firstIsHasElement = false;
            } else {
                firstNode = hasElementNode;
                secondNode = noMoreNode;
                firstIsHasElement = true;
            }

            if (path == null) {
                findPath(firstNode, endNode);
                if (path == null) {
                    if (firstIsHasElement) forNode.hasElementCounting++;
                    else forNode.noMoreElementCounting++;
                }
            }
            if (path == null) {
                findPath(secondNode, endNode);
                if (path == null) {
                    if (firstIsHasElement) forNode.noMoreElementCounting++;
                    else forNode.hasElementCounting++;
                }
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
