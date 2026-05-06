package core.path;

import core.cfg.CfgBoolExprNode;
import core.cfg.CfgForEachExpressionNode;
import core.cfg.CfgNode;
import core.testResult.coveredStatement.CoveredStatement;

import java.util.*;

public final class MarkedPath {

    private static List<MarkedStatement> markedStatements = new ArrayList<>();
    private static Set<CoveredStatement> fullTestSuiteCoveredStatements;
    private static Set<CoveredStatement> totalCoveredStatement;
    private static Set<CoveredStatement> totalCoveredBranch;
    private static Set<CoveredStatement> fullTestSuiteCoveredBranches;

    private MarkedPath() {
    }

    public static boolean markOneStatement(String statement, boolean isTrueCondition, boolean isFalseCondition) {
        addNewStatementToPath(statement, isTrueCondition, isFalseCondition);
        if (!isTrueCondition && !isFalseCondition) return true;
        return !isFalseCondition;
    }

    private static void addNewStatementToPath(String statement, boolean isTrueCondition, boolean isFalseCondition) {
        MarkedStatement markedStatement = new MarkedStatement(statement, isTrueCondition, isFalseCondition, 0);
        markedStatements.add(markedStatement);
    }

    public static void setMarkedStatements(List<MarkedStatement> markedStatements) {
        MarkedPath.markedStatements = markedStatements;
    }

    private static void reset() {
        markedStatements = new ArrayList<>();
    }

    public static void markPathToCFGV2(CfgNode rootNode, List<MarkedStatement> markedStatements) {
        // reset tập coverage cho lần chạy hiện tại
        totalCoveredBranch = new HashSet<>();
        totalCoveredStatement = new HashSet<>();

        if (rootNode == null || markedStatements == null || markedStatements.isEmpty()) {
            return;
        }

        // ===== BƯỚC 1: duyệt CFG 1 lần để build map: statement -> list<CfgNode> =====
        Map<String, List<CfgNode>> statementToNodes = new HashMap<>();
        Queue<CfgNode> queue = new LinkedList<>();
        Set<CfgNode> visited = new HashSet<>();

        if (rootNode != null) {
            queue.add(rootNode);
        }

        while (!queue.isEmpty()) {
            CfgNode node = queue.poll();
            if (node == null || visited.contains(node)) continue;
            visited.add(node);

            String content = node.getContent();
            if (content != null && !content.trim().isEmpty()) {
                String key = content.trim();
                statementToNodes.computeIfAbsent(key, k -> new ArrayList<>()).add(node);
            }

            // 1. Luôn thêm node "sau"
            if (node.getAfterStatementNode() != null) {
                queue.add(node.getAfterStatementNode());
            }

            // 2. Xử lý các node rẽ nhánh IF
            if (node instanceof core.cfg.CfgBoolExprNode) {
                core.cfg.CfgBoolExprNode b = (core.cfg.CfgBoolExprNode) node;
                if (b.getTrueNode() != null) queue.add(b.getTrueNode());
                if (b.getFalseNode() != null) queue.add(b.getFalseNode());
            }
//
            // 3. Xử lý For-Each
            else if (node instanceof CfgForEachExpressionNode) {
                CfgForEachExpressionNode fe = (CfgForEachExpressionNode) node;
                if (fe.getHasElementAfterNode() != null) queue.add(fe.getHasElementAfterNode());
                if (fe.getNoMoreElementAfterNode() != null) queue.add(fe.getNoMoreElementAfterNode());
            }

        }

        // ===== BƯỚC 2: ánh xạ MarkedStatement -> CfgNode và cập nhật các tập coverage =====
        for (MarkedStatement marked : markedStatements) {
            if (marked == null) continue;
            String stmt = marked.getStatement();
            if (stmt == null || stmt.trim().isEmpty()) continue;
            String key = stmt.trim();

            // tìm candidate nodes có cùng content
            List<CfgNode> candidates = statementToNodes.get(key);
            CfgNode matched = null;

            if (candidates != null && !candidates.isEmpty()) {
                // ưu tiên node chưa được mark để tránh reuse
                for (CfgNode n : candidates) {
                    if (!n.isMarked()) {
                        matched = n;
                        break;
                    }
                }
                if (matched == null) matched = candidates.get(0); // fallback
            } else {
                // fallback tìm tương tự: tìm key chứa/được chứa (giúp giảm mismatch do khoảng trắng)
                for (Map.Entry<String, List<CfgNode>> e : statementToNodes.entrySet()) {
                    String k = e.getKey();
                    if (k.contains(key) || key.contains(k)) {
                        List<CfgNode> list = e.getValue();
                        matched = list.stream().filter(n -> !n.isMarked()).findFirst().orElse(list.get(0));
                        break;
                    }
                }
            }

            if (matched == null) {
                // không tìm thấy node tương ứng -> log và bỏ qua
                System.err.println("⚠ Không tìm thấy CFG node cho statement: [" + stmt + "]");
                continue;
            }

            // Kiểm tra xem node đã được đánh dấu trước đó (tức đã nằm trong fullTestSuite)
            boolean wasMarkedBefore = matched.isMarked();

            // 1) Statement coverage (tổng cho lần chạy hiện tại)
            CoveredStatement csStmt = new CoveredStatement(matched.getContent(), matched.getLineNumber(), "");
            totalCoveredStatement.add(csStmt);

            // 2) Nếu node chưa được mark trước đó thì thêm vào fullTestSuite
            if (!wasMarkedBefore) {
                fullTestSuiteCoveredStatements.add(csStmt);
            }

            // Liên kết và mark node
            matched.setMarked(true);
            marked.setCfgNode(matched);

            // 3) Nếu node là boolean expression thì xử lý branch coverage
            if (matched instanceof CfgBoolExprNode) {
                CfgBoolExprNode boolNode = (CfgBoolExprNode) matched;

                if (marked.isTrueConditionalStatement()) {
                    CoveredStatement csBranch = new CoveredStatement(boolNode.getContent(), boolNode.getLineNumber(), "true");
                    totalCoveredBranch.add(csBranch);
                    if (!boolNode.isTrueMarked()) {
                        fullTestSuiteCoveredBranches.add(csBranch);
                    }
                    boolNode.setTrueMarked(true);
                }

                if (marked.isFalseConditionalStatement()) {
                    CoveredStatement csBranch = new CoveredStatement(boolNode.getContent(), boolNode.getLineNumber(), "false");
                    totalCoveredBranch.add(csBranch);
                    if (!boolNode.isFalseMarked()) {
                        fullTestSuiteCoveredBranches.add(csBranch);
                    }
                    boolNode.setFalseMarked(true);
                }
            }
        }
    }
//    public static void markPathToCFGV2(CfgNode rootNode, List<MarkedStatement> markedStatements) {
//        totalCoveredBranch = new HashSet<>();
//        totalCoveredStatement = new HashSet<>();
//
//        int i = 0;
//        while (rootNode != null && i < markedStatements.size()) {
//            // Kiểm tra những CfgNode không có content
//            if (rootNode.getContent().equals("")) {
//                rootNode.setMarked(true);
//                rootNode = rootNode.getAfterStatementNode();
//                continue;
//            }
//            MarkedStatement markedStatement = markedStatements.get(i);
//            if (rootNode.getContent().equals(markedStatement.getStatement())) {
//                if (!rootNode.isMarked()) {
//                    fullTestSuiteCoveredStatements.add(new CoveredStatement(rootNode.getContent(), rootNode.getLineNumber(), ""));
//                }
//                totalCoveredStatement.add(new CoveredStatement(rootNode.getContent(), rootNode.getLineNumber(), ""));
//                rootNode.setMarked(true);
//                markedStatement.setCfgNode(rootNode);
//            } else {
//                return;
//            }
//
//            if (rootNode instanceof CfgBoolExprNode) {
//                CfgBoolExprNode boolExprNode = (CfgBoolExprNode) rootNode;
//                if (markedStatement.isFalseConditionalStatement()) {
//                    if (!boolExprNode.isFalseMarked()) {
//                        fullTestSuiteCoveredBranches.add(new CoveredStatement(boolExprNode.getContent(), boolExprNode.getLineNumber(), "false"));
//                    }
//                    totalCoveredBranch.add(new CoveredStatement(boolExprNode.getContent(), boolExprNode.getLineNumber(), "false"));
//                    boolExprNode.setFalseMarked(true);
//                    rootNode = boolExprNode.getFalseNode();
//                } else if (markedStatement.isTrueConditionalStatement()) {
//                    if (!boolExprNode.isTrueMarked()) {
//                        fullTestSuiteCoveredBranches.add(new CoveredStatement(boolExprNode.getContent(), boolExprNode.getLineNumber(), "true"));
//                    }
//                    totalCoveredBranch.add(new CoveredStatement(boolExprNode.getContent(), boolExprNode.getLineNumber(), "true"));
//                    boolExprNode.setTrueMarked(true);
//                    rootNode = boolExprNode.getTrueNode();
//                }
//                i++;
//                continue;
//            }
//
//            // Updater
//            i++;
//            rootNode = rootNode.getAfterStatementNode();
//        }
//        while (rootNode != null) {
//            if (rootNode.getContent().equals("")) {
//                rootNode.setMarked(true);
//                rootNode = rootNode.getAfterStatementNode();
//            }
//        }
//    }

//    public static List<MarkedStatement> markPathToCFG(CfgNode rootNode) {
////        List<CfgNode> coveredStatements = new ArrayList<>();
//        List<MarkedStatement> result = markedStatements;
//        totalCoveredBranch = 0;
//        totalCoveredStatement = 0;
//
//        int i = 0;
//        while (rootNode != null && i < markedStatements.size()) {
//            // Kiểm tra những CfgNode không có content
//            if (rootNode.getContent().equals("")) {
//                rootNode.setMarked(true);
//                rootNode = rootNode.getAfterStatementNode();
//                continue;
//            }
//
//            MarkedStatement markedStatement = markedStatements.get(i);
//            if (rootNode.getContent().equals(markedStatement.getStatement())) {
//                if (!rootNode.isMarked()) {
//                    System.out.println(rootNode);
//                    totalCoveredStatement++;
//                    fullTestSuiteCoveredStatements.add(rootNode.getContent());
//                }
//                rootNode.setMarked(true);
//                markedStatement.setCfgNode(rootNode);
////                coveredStatements.add(rootNode);
//
//            } else {
//                reset();
//                return result;
////                return coveredStatements;
//            }
//
//            if (rootNode instanceof CfgBoolExprNode) {
//                CfgBoolExprNode boolExprNode = (CfgBoolExprNode) rootNode;
//                if (markedStatement.isFalseConditionalStatement()) {
//                    if (!boolExprNode.isFalseMarked()) {
//                        totalCoveredBranch++;
//                    }
//                    boolExprNode.setFalseMarked(true);
//                    rootNode = boolExprNode.getFalseNode();
//                } else if (markedStatement.isTrueConditionalStatement()) {
//                    if (!boolExprNode.isTrueMarked()) {
//                        totalCoveredBranch++;
//                    }
//                    boolExprNode.setTrueMarked(true);
//                    rootNode = boolExprNode.getTrueNode();
//                }
//                i++;
//                continue;
//            }
//
//            // Updater
//            i++;
//            rootNode = rootNode.getAfterStatementNode();
//        }
//        while (rootNode != null) {
//            if (rootNode.getContent().equals("")) {
//                rootNode.setMarked(true);
//                rootNode = rootNode.getAfterStatementNode();
//            }
//        }
//
//        reset();
//        return result;

    /// /        return coveredStatements;
//    }
    public static int getTotalCoveredStatement() {
        return totalCoveredStatement.size();
    }

    public static int getTotalCoveredBranch() {
        return totalCoveredBranch.size();
    }

    public static void resetFullTestSuiteCoveredStatements() {
        fullTestSuiteCoveredStatements = new HashSet<>();
        fullTestSuiteCoveredBranches = new HashSet<>();
    }

    public static int getFullTestSuiteTotalCoveredStatements() {
        return fullTestSuiteCoveredStatements.size();
    }

    public static int getFullTestSuiteTotalCoveredBranch() {
        return fullTestSuiteCoveredBranches.size();
    }

//    public static List<MarkedStatement> isPathActuallyCovered(Path path) {
//        totalCoveredStatement = 0;
//        int i = 0;
//        Node currentNode = path.getCurrentFirst();
//        while (currentNode != null && i < markedStatements.size()) {
//            CfgNode cfgNode = currentNode.getData();
//            if (cfgNode.getContent().equals("")) {
//                currentNode = currentNode.getNext();
//                continue;
//            }
//
//            if (!cfgNode.getContent().equals(markedStatements.get(i).getStatement())) {
//                reset();
//                return null;
//            } else {
//                markedStatements.get(i).setCfgNode(cfgNode);
//                totalCoveredStatement++;
//                fullTestSuiteCoveredStatements.add(cfgNode.getContent());
//            }
//
//            // Updater
//            i++;
//            currentNode = currentNode.getNext();
//        }
//
//        List<MarkedStatement> result = markedStatements;
//        reset();
//        return result;
//    }

//    public static List<String> getMarkedStatementsStringList() {
//        List<String> result = new ArrayList<>();
//
//        for (MarkedStatement markedStatement : markedStatements) {
//            result.add(markedStatement.getStatement());
//        }
//
//        return result;
//    }

//    public static CfgNode findUncoveredNode(CfgNode rootNode, CfgNode duplicateNode) {
//        if (rootNode == null || !rootNode.isMarked()) {
//            return rootNode;
//        }
//        if (rootNode instanceof CfgBoolExprNode) {
//            CfgBoolExprNode boolExprNode = (CfgBoolExprNode) rootNode;
//
//
//            if (!boolExprNode.isTrueMarked()) {
//                return boolExprNode.getTrueNode();
//            }
//            if (!boolExprNode.isFalseMarked()) {
//                return boolExprNode.getFalseNode();
//            }
//
//            if (boolExprNode != duplicateNode) {
//                duplicateNode = boolExprNode;
//                return findUncoveredNode(boolExprNode.getTrueNode(), duplicateNode);
//            } else {
//                return findUncoveredNode(boolExprNode.getFalseNode(), duplicateNode);
//            }
//        }
//
//        CfgNode cfgNode = findUncoveredNode(rootNode.getAfterStatementNode(), duplicateNode);
//        return cfgNode;
//    }

//    public static CfgNode findUncoveredNode(CfgNode rootNode, CfgNode duplicateNode) {
//        if (rootNode == null || !rootNode.isMarked()) {
//            return rootNode;
//        }
//        if (rootNode instanceof CfgBoolExprNode) {
//            CfgBoolExprNode boolExprNode = (CfgBoolExprNode) rootNode;
//
//            // Check for duplicateNode. Nếu có node trùng lặp tức là đã duyệt qua 1 vòng của loop đấy và k thấy node chưa mark nên return null.
//            if(boolExprNode != duplicateNode) duplicateNode = boolExprNode;
//            else {
//                return null;
//            }
//
//            if (!boolExprNode.isTrueMarked()) {
//                return boolExprNode.getTrueNode();
//            }
//            if (!boolExprNode.isFalseMarked()) {
//                return boolExprNode.getFalseNode();
//            }
//
//            CfgNode falseBranchUncoveredNode = findUncoveredNode(boolExprNode.getFalseNode(), duplicateNode);
//            CfgNode trueBranchUncoveredNode = findUncoveredNode(boolExprNode.getTrueNode(), duplicateNode);
    /// /            if(tmpUncoveredNode == null) {
    /// /                return findUncoveredNode(boolExprNode.getTrueNode(), duplicateNode);
    /// /            } else {
    /// /                return tmpUncoveredNode;
    /// /            }
//            return falseBranchUncoveredNode == null ? trueBranchUncoveredNode : falseBranchUncoveredNode;
//        }
//
//        CfgNode cfgNode =  findUncoveredNode(rootNode.getAfterStatementNode(), duplicateNode);
//        return cfgNode;
//    }

    private static List<CfgNode> coveredNodeInPath;

    public static CfgNode findUncoveredStatement(CfgNode rootNode) {
        coveredNodeInPath = new ArrayList<>();
        return findUncoveredStatement(rootNode, null);
    }

    private static CfgNode findUncoveredStatement(CfgNode rootNode, CfgNode duplicateNode) {
        if (rootNode == null) {
            return null;
        }
        if (!coveredNodeInPath.contains(rootNode)) {
            coveredNodeInPath.add(rootNode);
            if (!rootNode.isMarked() && !rootNode.isFakeMarked()
                    && !rootNode.getContent().isEmpty()) return rootNode;
            if (rootNode instanceof CfgBoolExprNode) {
                CfgBoolExprNode boolExprNode = (CfgBoolExprNode) rootNode;
                CfgNode falseBranchUncoveredNode = findUncoveredStatement(boolExprNode.getFalseNode(), duplicateNode);
                CfgNode trueBranchUncoveredNode = findUncoveredStatement(boolExprNode.getTrueNode(), duplicateNode);
                return falseBranchUncoveredNode == null ? trueBranchUncoveredNode : falseBranchUncoveredNode;
            } else {
                return findUncoveredStatement(rootNode.getAfterStatementNode(), duplicateNode);
            }
        } else {
            return null;
        }
    }

    public static CfgNode findUncoveredBranch(CfgNode rootNode) {
        coveredNodeInPath = new ArrayList<>();
        return findUncoveredBranch(rootNode, null);
    }
//    private static CfgNode findUncoveredBranch(CfgNode rootNode, CfgNode duplicateNode) {
//        if (rootNode == null) {
//            return null;
//        }
//        if (rootNode instanceof CfgBoolExprNode) {
//            CfgBoolExprNode boolExprNode = (CfgBoolExprNode) rootNode;
//
//            if (!boolExprNode.isTrueMarked()) {
//                return boolExprNode.getTrueNode();
//            }
//            if (!boolExprNode.isFalseMarked()) {
//                return boolExprNode.getFalseNode();
//            }
//

    /// /            if (boolExprNode != duplicateNode) {
    /// /                duplicateNode = boolExprNode;
    /// /                return findUncoveredBranch(boolExprNode.getTrueNode(), duplicateNode);
    /// /            } else {
    /// /                return findUncoveredBranch(boolExprNode.getFalseNode(), duplicateNode);
    /// /            }
//            if(coveredNodeInPath.contains(boolExprNode)) {
//                return findUncoveredBranch(boolExprNode.getFalseNode(), duplicateNode);
//            } else {
//                coveredNodeInPath.add(boolExprNode);
//                CfgNode falseBranchUncoveredNode = findUncoveredBranch(boolExprNode.getFalseNode(), duplicateNode);
//                CfgNode trueBranchUncoveredNode = findUncoveredBranch(boolExprNode.getTrueNode(), duplicateNode);
//                return falseBranchUncoveredNode == null ? trueBranchUncoveredNode : falseBranchUncoveredNode;
//            }
//        }
//
//        coveredNodeInPath.add(rootNode);
//        return findUncoveredBranch(rootNode.getAfterStatementNode(), duplicateNode);
//    }
    private static CfgNode findUncoveredBranch(CfgNode rootNode, CfgNode duplicateNode) {
        if (rootNode == null) {
            return null;
        }
        if (!coveredNodeInPath.contains(rootNode)) {
            coveredNodeInPath.add(rootNode);
            if (!rootNode.isMarked() && !rootNode.isFakeMarked()
                    && !rootNode.getContent().isEmpty()) return rootNode;
            if (rootNode instanceof CfgBoolExprNode) {
                CfgBoolExprNode boolExprNode = (CfgBoolExprNode) rootNode;

                if (!boolExprNode.isTrueMarked() && !boolExprNode.isFakeTrueMarked()) {
                    CfgNode r = boolExprNode.getTrueNode();
                    if (!r.isFakeMarked() && !r.isMarked()) return r;
                }
                if (!boolExprNode.isFalseMarked() && !boolExprNode.isFakeFalseMarked()) {
                    CfgNode r = boolExprNode.getFalseNode();
                    if (!r.isFakeMarked() && !r.isMarked()) return r;
                }

                CfgNode falseBranchUncoveredNode = findUncoveredBranch(boolExprNode.getFalseNode(), duplicateNode);
                CfgNode trueBranchUncoveredNode = findUncoveredBranch(boolExprNode.getTrueNode(), duplicateNode);
                return falseBranchUncoveredNode == null ? trueBranchUncoveredNode : falseBranchUncoveredNode;
            } else {
                return findUncoveredBranch(rootNode.getAfterStatementNode(), duplicateNode);
            }
        } else {
            return null;
        }
    }

//    private static void checkDuplicateAndAddToSet(Set<CoveredStatement> coveredStatements, CoveredStatement newCoveredStatement) {
//        if (!coveredStatements.contains(newCoveredStatement)) {
//            for (CoveredStatement coveredStatement : coveredStatements) {
//                if (coveredStatement)
//            }
//        }
//    }
}
