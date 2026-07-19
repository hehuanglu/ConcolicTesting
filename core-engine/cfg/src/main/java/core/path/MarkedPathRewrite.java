package core.path;

import core.cfg.CfgBoolExprNode;
import core.cfg.CfgForEachExpressionNode;
import core.cfg.CfgNode;
import core.testResult.coveredStatement.CoveredStatement;

import java.util.*;

public class MarkedPathRewrite {
    private static Set<CoveredStatement> remainingUncoveredStatements;  // Những statement chưa được phủ

    // CÔNG VIỆC 1: SETUP - build và add hết tất cả statement vào set
    public static void setupUncoveredStatements(CfgNode rootNode) {
        remainingUncoveredStatements = new HashSet<>();

        if (rootNode == null) {
            return;
        }

        Map<String, Queue<CfgNode>> statementNodeMap = buildStatementNodeMap(rootNode);

        // Add hết tất cả statement vào set
        for (Queue<CfgNode> nodes : statementNodeMap.values()) {
            for (CfgNode node : nodes) {
                CoveredStatement stmt = new CoveredStatement(
                        node.getContent(),
                        node.getLineNumber(),
                        ""
                );
                remainingUncoveredStatements.add(stmt);
            }
        }
    }

    // CÔNG VIỆC 2: MARK PATH - chỉ xoá những statement mới được phủ
    public static void markPathAndRemove(CfgNode rootNode, List<MarkedStatement> markedStatements) {
        if (rootNode == null || markedStatements == null || markedStatements.isEmpty()) {
            return;
        }

        // Nếu chưa setup thì tự động setup
        if (remainingUncoveredStatements == null) {
            setupUncoveredStatements(rootNode);
        }

        Map<String, Queue<CfgNode>> statementNodeMap = buildStatementNodeMap(rootNode);

        // Xoá những statement được mark ra khỏi set
        for (MarkedStatement markedStatement : markedStatements) {
            if (markedStatement == null) continue;

            String statement = markedStatement.getStatement();
            if (statement == null) continue;

            String key = statement.trim();
            if (key.isEmpty()) continue;

            Queue<CfgNode> matchedNodes = statementNodeMap.get(key);
            if (matchedNodes == null || matchedNodes.isEmpty()) {
                System.err.println("⚠ Cannot find CFG node for statement: [" + statement + "]");
                continue;
            }

            CfgNode matchedNode = matchedNodes.poll();
            markedStatement.setCfgNode(matchedNode);
            matchedNode.setMarked(true);

            CoveredStatement coveredStmt = new CoveredStatement(
                    matchedNode.getContent(),
                    matchedNode.getLineNumber(),
                    ""
            );

            // XOÁ khỏi set (vì statement này đã được phủ)
            if (remainingUncoveredStatements.contains(coveredStmt)) {
                remainingUncoveredStatements.remove(coveredStmt);
            }
        }
    }

    // Getter để lấy những statement chưa được phủ
    public static Set<CoveredStatement> getRemainingUncoveredStatements() {
        return remainingUncoveredStatements;
    }

    // Getter để lấy số lượng statement chưa được phủ
    public static int getRemainingUncoveredCount() {
        return remainingUncoveredStatements != null ? remainingUncoveredStatements.size() : 0;
    }

    // Reset để chạy test case mới
    public static void reset() {
        remainingUncoveredStatements = null;
    }

    // Các method giữ nguyên
    private static Map<String, Queue<CfgNode>> buildStatementNodeMap(CfgNode rootNode) {
        Map<String, Queue<CfgNode>> statementNodeMap = new HashMap<>();
        Queue<CfgNode> bfsQueue = new LinkedList<>();
        Set<CfgNode> visited = new HashSet<>();

        bfsQueue.add(rootNode);

        while (!bfsQueue.isEmpty()) {
            CfgNode node = bfsQueue.poll();

            if (node == null || visited.contains(node)) {
                continue;
            }

            visited.add(node);

            String content = node.getContent();
            if (content != null) {
                String statement = content.trim();
                if (!statement.isEmpty()) {
                    statementNodeMap.computeIfAbsent(statement, k -> new LinkedList<>()).add(node);
                }
            }

            if (node.getAfterStatementNode() != null) {
                bfsQueue.add(node.getAfterStatementNode());
            }

            if (node instanceof CfgBoolExprNode) {
                CfgBoolExprNode boolNode = (CfgBoolExprNode) node;
                if (boolNode.getTrueNode() != null) {
                    bfsQueue.add(boolNode.getTrueNode());
                }
                if (boolNode.getFalseNode() != null) {
                    bfsQueue.add(boolNode.getFalseNode());
                }
            }
            else if (node instanceof CfgForEachExpressionNode) {
                CfgForEachExpressionNode forEachNode = (CfgForEachExpressionNode) node;
                if (forEachNode.getHasElementAfterNode() != null) {
                    bfsQueue.add(forEachNode.getHasElementAfterNode());
                }
                if (forEachNode.getNoMoreElementAfterNode() != null) {
                    bfsQueue.add(forEachNode.getNoMoreElementAfterNode());
                }
            }
        }

        return statementNodeMap;
    }
}