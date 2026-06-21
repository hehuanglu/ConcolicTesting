package core.path;

import core.cfg.CfgBoolExprNode;
import core.cfg.CfgForEachExpressionNode;
import core.cfg.CfgNode;
import core.testGeneration.TestGeneration;
import core.testResult.coveredStatement.CoveredStatement;

import java.util.*;

public final class MarkedPath {

    private static class StatementKey {
        private final String content;
        private final int lineNumber;

        public StatementKey(String content, int lineNumber) {
            this.content = normalizeStatement(content);
            this.lineNumber = lineNumber;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof StatementKey)) return false;

            StatementKey other = (StatementKey) obj;
            return this.lineNumber == other.lineNumber
                    && Objects.equals(this.content, other.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(content, lineNumber);
        }
    }

    private static String normalizeStatement(String stmt) {
        if (stmt == null) return "";
        return stmt.trim().replaceAll("\\s+", " ");
    }

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

    public static int ngu = 0;

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
        Map<StatementKey, List<CfgNode>> statementLineToNodes = new HashMap<>();
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
                String key = normalizeStatement(content);
                int lineNumber = node.getLineNumber();

                statementToNodes
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add(node);

                StatementKey statementKey = new StatementKey(key, lineNumber);
                statementLineToNodes
                        .computeIfAbsent(statementKey, k -> new ArrayList<>())
                        .add(node);
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
            int markedLineNumber = marked.getLineNumber();
            if (stmt == null || stmt.trim().isEmpty()) continue;

            String key = normalizeStatement(stmt);

            CfgNode matched = null;

            // ===== ƯU TIÊN 1: match chính xác bằng content + lineNumber =====
            StatementKey statementKey = new StatementKey(key, markedLineNumber);
            List<CfgNode> exactCandidates = statementLineToNodes.get(statementKey);

            if (exactCandidates != null && !exactCandidates.isEmpty()) {
                for (CfgNode n : exactCandidates) {
                    if (!n.isMarked()) {
                        matched = n;
                        break;
                    }
                }

                if (matched == null) {
                    matched = exactCandidates.get(0);
                }
            }

            // ===== ƯU TIÊN 2: fallback match bằng content nếu không tìm thấy line chính xác =====
            if (matched == null) {
                List<CfgNode> candidates = statementToNodes.get(key);

                if (candidates != null && !candidates.isEmpty()) {
                    for (CfgNode n : candidates) {
                        if (!n.isMarked()) {
                            matched = n;
                            break;
                        }
                    }

                    if (matched == null) {
                        matched = candidates.get(0);
                    }
                }
            }

            // ===== ƯU TIÊN 3: fallback gần đúng, nhưng vẫn ưu tiên cùng lineNumber =====
            if (matched == null) {
                for (Map.Entry<String, List<CfgNode>> e : statementToNodes.entrySet()) {
                    String cfgContent = e.getKey();

                    if (cfgContent.contains(key) || key.contains(cfgContent)) {
                        List<CfgNode> list = e.getValue();

                        // Ưu tiên node cùng lineNumber và chưa mark
                        matched = list.stream()
                                .filter(n -> n.getLineNumber() == markedLineNumber)
                                .filter(n -> !n.isMarked())
                                .findFirst()
                                .orElse(null);

                        // Nếu cùng lineNumber nhưng đã mark rồi
                        if (matched == null) {
                            matched = list.stream()
                                    .filter(n -> n.getLineNumber() == markedLineNumber)
                                    .findFirst()
                                    .orElse(null);
                        }

                        // Fallback cuối: node chưa mark bất kỳ
                        if (matched == null) {
                            matched = list.stream()
                                    .filter(n -> !n.isMarked())
                                    .findFirst()
                                    .orElse(list.get(0));
                        }

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
                if (!boolNode.isTrueMarked() || !boolNode.isFalseMarked()) {
                    System.out.println("Bool Node được phủ: " + boolNode.getContent() + " -> "
                            + (marked.isTrueConditionalStatement() ? "true" : "false"));
                }

                if (marked.isTrueConditionalStatement()) {
                    CoveredStatement csBranch = new CoveredStatement(boolNode.getContent(), boolNode.getLineNumber(), "true");
                    totalCoveredBranch.add(csBranch);
                    if (!boolNode.isTrueMarked()) {
                        fullTestSuiteCoveredBranches.add(csBranch);
                    }
                    boolNode.setTrueMarked(true);
                    boolNode.getTrueNode().setMarked(true);
                }

                if (marked.isFalseConditionalStatement()) {
                    CoveredStatement csBranch = new CoveredStatement(boolNode.getContent(), boolNode.getLineNumber(), "false");
                    totalCoveredBranch.add(csBranch);
                    if (!boolNode.isFalseMarked()) {
                        fullTestSuiteCoveredBranches.add(csBranch);
                    }
                    boolNode.setFalseMarked(true);
                    boolNode.getFalseNode().setMarked(true);
                }
            }
        }
    }

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

    private static CfgNode findUncoveredBranch(CfgNode rootNode, CfgNode duplicateNode) {
        if (rootNode == null) {
            return null;
        }
        if (!coveredNodeInPath.contains(rootNode)) {
            coveredNodeInPath.add(rootNode);

            if (rootNode instanceof CfgBoolExprNode) {
                CfgBoolExprNode boolExprNode = (CfgBoolExprNode) rootNode;

                if (!boolExprNode.isTrueMarked() && !boolExprNode.isFakeTrueMarked()) {
                    return boolExprNode;
                }
                if (!boolExprNode.isFalseMarked() && !boolExprNode.isFakeFalseMarked()) {
                    return boolExprNode;
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

    public static void printCoverageReport(TestGeneration.Coverage coverage) {
        System.out.println("=== COVERAGE REPORT: " + coverage + " ===");

        switch (coverage) {
            case STATEMENT:
                System.out.println("Total Covered Statements:");
                totalCoveredStatement.forEach(s -> System.out.println("  > " + s));

                System.out.println("Full Test Suite Covered Statements:");
                fullTestSuiteCoveredStatements.forEach(s -> System.out.println("  > " + s));
                break;

            case MCDC:
            case BRANCH:
                System.out.println("Total Covered Branches:");
                // Tận dụng toString của CoveredStatement để in thông tin branch (true/false)
                totalCoveredBranch.forEach(b -> System.out.println("  > Branch: " + b));

                System.out.println("Full Test Suite Covered Branches:");
                fullTestSuiteCoveredBranches.forEach(b -> System.out.println("  > Branch: " + b));
                break;
        }
        System.out.println("=".repeat(40));
    }


}