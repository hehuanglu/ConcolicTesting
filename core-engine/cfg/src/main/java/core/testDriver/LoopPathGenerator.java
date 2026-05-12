package core.testDriver;

import core.cfg.CfgBoolExprNode;
import core.cfg.CfgForEachExpressionNode;
import core.cfg.CfgNode;
import core.path.Path;

import java.util.*;

public class LoopPathGenerator {

    private static final int MAX_DEPTH = 400;
    private static final int MAX_PATHS_PER_SCENARIO = 450;
    private static final int M = 6;

    /**
     * Sinh các đường đi kiểm thử theo chiến lược LICO.
     *
     * @param cfgNode Node gốc của Control Flow Graph.
     * @param endNode Node kết thúc của CFG.
     * @return Danh sách các đường đi thỏa mãn các kịch bản lặp.
     */
    public static List<Path> generateLicoPaths(CfgNode cfgNode, CfgNode endNode) {
        List<Path> result = new ArrayList<>();

        // 1. Tìm tất cả các vòng lặp (loop header và back-edge)
        Map<CfgNode, Set<CfgNode>> loops = detectLoops(cfgNode);

        if (loops.isEmpty()) {
            return result;
        }

        // Các kịch bản số lần lặp cần test
        //int[] scenarios = {0, 1, 3,M-1 ,M, M + 1};
        int[] scenarios = {0, 1, 3, M - 1, M, M + 1};
        Set<String> globalUniquePaths = new HashSet<>();

        Map<CfgNode, CfgNode> conditionToHeaderMap = new HashMap<>();
        for (CfgNode header : loops.keySet()) {
            CfgNode h = header;
            if (!(h instanceof CfgBoolExprNode)) {
                List<CfgNode> c = getChildren(h);
                if (!c.isEmpty() && c.get(0) instanceof CfgBoolExprNode) {
                    h = c.get(0);
                }
            }
            conditionToHeaderMap.put(h, header);
        }

        for (CfgNode focusedLoopHeader : loops.keySet()) {

            // Tìm header thật của vòng lặp đang focus
            CfgNode realFocusedHeader = focusedLoopHeader;
            if (!(realFocusedHeader instanceof CfgBoolExprNode)) {
                List<CfgNode> children = getChildren(realFocusedHeader);
                if (!children.isEmpty() && children.get(0) instanceof CfgBoolExprNode) {
                    realFocusedHeader = children.get(0);
                }
            }

            for (int k : scenarios) {

                // targets chỉ chứa 1 vòng lặp
                Map<CfgNode, Integer> targets = new HashMap<>();
                targets.put(realFocusedHeader, k);

                List<Path> pathsForThisScenario = new ArrayList<>();

                // Gọi DFS
                dfsLico(cfgNode, new Path(), new HashMap<>(), targets,
                        pathsForThisScenario, loops, endNode, 0, globalUniquePaths,
                        conditionToHeaderMap, k);

                result.addAll(pathsForThisScenario);
            }
        }

        return result;
    }

    /**
     * Hàm tìm vòng lặp bằng DFS.
     * Sử dụng tập hợp visited và recursionStack để phát hiện back-edge.
     *
     * @param root Node gốc của CFG.
     * @return Map với Key là loop header (nơi back-edge trỏ về), Value là các nút cuối (nơi back-edge đi ra).
     */
    private static Map<CfgNode, Set<CfgNode>> detectLoops(CfgNode root) {
        Map<CfgNode, Set<CfgNode>> loops = new HashMap<>();
        detectLoopsRecursive(root, new HashSet<>(), new HashSet<>(), loops);
        return loops;
    }

    private static void detectLoopsRecursive(CfgNode root, Set<CfgNode> visited,
                                             Set<CfgNode> recursionStack, Map<CfgNode, Set<CfgNode>> loops) {
        if (root == null) return;

        visited.add(root);
        recursionStack.add(root);

        List<CfgNode> children = getChildren(root);

        for (CfgNode child : children) {
            if (!visited.contains(child)) {
                detectLoopsRecursive(child, visited, recursionStack, loops);
            } else if (recursionStack.contains(child)) {
                // child đã được thăm và đang ở trong stack -> Phát hiện back-edge
                loops.computeIfAbsent(child, k -> new HashSet<>()).add(root);
            }
        }

        recursionStack.remove(root);
    }

    /**
     * Sinh đường đi bằng thuật toán DFS có giới hạn (LICO).
     */
    private static void dfsLico(CfgNode currentNode, Path currentPath, Map<CfgNode,
                                        Integer> loopCounters, Map<CfgNode, Integer> targetIterations, List<Path> resultPaths,
                                Map<CfgNode, Set<CfgNode>> loops, CfgNode endNode, int currentLength,
                                Set<String> uniquePaths,
                                Map<CfgNode, CfgNode> conditionToHeaderMap,
                                int currentScenarioK) {

        // 1. Giới hạn độ sâu
        if (resultPaths.size() >= MAX_PATHS_PER_SCENARIO || currentLength > MAX_DEPTH) return;

        currentPath.addLast(currentNode);

        // 2. ĐẾN ĐÍCH
        if (currentNode == endNode) {
            String signature = currentPath.toString();
            if (!uniquePaths.contains(signature)) {
                uniquePaths.add(signature);
                resultPaths.add(new Path(currentPath));
            }
            currentPath.removeLast();
            return;
        }

        List<CfgNode> nextNodes = getChildren(currentNode);
        if (nextNodes.isEmpty()) {
            currentPath.removeLast();
            return;
        }

        // Tạo list node cho phép đi
        List<CfgNode> allowedNodes = new ArrayList<>();

        if (currentNode instanceof CfgBoolExprNode) {
            CfgBoolExprNode boolNode = (CfgBoolExprNode) currentNode;

            // Vòng lặp có Target -> Ép chặt
            if (targetIterations.containsKey(currentNode)) {
                int target = targetIterations.get(currentNode);
                CfgNode counterHeaderNode = conditionToHeaderMap.getOrDefault(currentNode, currentNode);
                int current = loopCounters.getOrDefault(counterHeaderNode, 0);

                if (current < target) {
                    if (boolNode.getTrueNode() != null) allowedNodes.add(boolNode.getTrueNode());
                } else {
                    if (boolNode.getFalseNode() != null) allowedNodes.add(boolNode.getFalseNode());
                }
            }
            // Vòng lặp tự do (Sử dụng chiến thuật Sync với K)
            else if (conditionToHeaderMap.containsKey(currentNode)) {
                CfgNode counterHeaderNode = conditionToHeaderMap.get(currentNode);
                int current = loopCounters.getOrDefault(counterHeaderNode, 0);

                int limit = currentScenarioK;

                // Luôn cho phép thoát tại 0
                if (current == 0 && boolNode.getFalseNode() != null) {
                    allowedNodes.add(boolNode.getFalseNode());
                }

                // Chiến thuật Sync
                if (current < limit) {
                    if (boolNode.getTrueNode() != null) allowedNodes.add(boolNode.getTrueNode());
                } else {
                    if (boolNode.getFalseNode() != null) allowedNodes.add(boolNode.getFalseNode());
                }
            }
        }

        // Cập nhật nextNodes nếu có giới hạn
        if (!allowedNodes.isEmpty()) {
            nextNodes = allowedNodes;
        }

        // 4. DUYỆT CÁC NHÁNH TIẾP THEO
        for (CfgNode nextNode : nextNodes) {
            if (resultPaths.size() >= MAX_PATHS_PER_SCENARIO) break;

            // --- [A] XỬ LÝ BACK-EDGE (ĐẾM VÒNG LẶP) ---
            boolean isBackEdge = loops.containsKey(nextNode) && loops.get(nextNode).contains(currentNode);
            if (isBackEdge) {
                loopCounters.put(nextNode, loopCounters.getOrDefault(nextNode, 0) + 1);
            }

            // XỬ LÝ LOOP EXIT
            boolean isLoopExit = false;
            int savedCountBeforeExit = 0;
            CfgNode loopHeaderOfThisCondition = null;

            if (currentNode instanceof CfgBoolExprNode && conditionToHeaderMap.containsKey(currentNode)) {
                CfgBoolExprNode boolNode = (CfgBoolExprNode) currentNode;

                if (nextNode == boolNode.getFalseNode()) {
                    isLoopExit = true;
                    loopHeaderOfThisCondition = conditionToHeaderMap.getOrDefault(currentNode, currentNode);

                    if (loopCounters.containsKey(loopHeaderOfThisCondition)) {
                        savedCountBeforeExit = loopCounters.get(loopHeaderOfThisCondition);
                        loopCounters.remove(loopHeaderOfThisCondition);
                    }
                }
            }

            // ĐỆ QUY
            dfsLico(nextNode, currentPath, loopCounters, targetIterations,
                    resultPaths, loops, endNode, currentLength + 1, uniquePaths,
                    conditionToHeaderMap, currentScenarioK);

            // BACKTRACK
            if (isLoopExit) {
                if (savedCountBeforeExit > 0) {
                    loopCounters.put(loopHeaderOfThisCondition, savedCountBeforeExit);
                }
            }

            // 2. Khôi phục Back-edge
            if (isBackEdge) {
                int count = loopCounters.get(nextNode);
                if (count > 0) loopCounters.put(nextNode, count - 1);
                else loopCounters.remove(nextNode);
            }
        }

        currentPath.removeLast();
    }

    /**
     * Trả về danh sách các node con của node hiện tại.
     */
    private static List<CfgNode> getChildren(CfgNode root) {
        List<CfgNode> result = new ArrayList<>();
        if (root instanceof CfgBoolExprNode) {
            CfgBoolExprNode boolNode = (CfgBoolExprNode) root;
            if (boolNode.getTrueNode() != null) result.add(boolNode.getTrueNode());
            if (boolNode.getFalseNode() != null) result.add(boolNode.getFalseNode());
        } else if (root instanceof CfgForEachExpressionNode) {
            CfgForEachExpressionNode feNode = (CfgForEachExpressionNode) root;
            if (feNode.getHasElementAfterNode() != null) result.add(feNode.getHasElementAfterNode());
            if (feNode.getNoMoreElementAfterNode() != null) result.add(feNode.getNoMoreElementAfterNode());
        }
        if (root.getAfterStatementNode() != null) {
            result.add(root.getAfterStatementNode());
        }
        return result;
    }
}