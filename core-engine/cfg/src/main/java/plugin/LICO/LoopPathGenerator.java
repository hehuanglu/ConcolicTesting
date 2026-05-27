package plugin.LICO;

import core.cfg.CfgBoolExprNode;
import core.cfg.CfgForEachExpressionNode;
import core.cfg.CfgNode;
import core.path.Path;

import java.util.*;
import java.util.stream.Collectors;

public class LoopPathGenerator {
    private static final int MAX_DEPTH = 400;
    private static final int MAX_PATHS_PER_SCENARIO = 450;
    private static final int M = 6;

    public static List<Path> generateLicoPaths(CfgNode root, CfgNode endNode) {
        List<Path> result = new ArrayList<>();

        // 1. Tìm tất cả các vòng lặp (loop header và back-edge)
        Map<CfgNode, Set<CfgNode>> loops = detectLoops(root);
        if (loops.isEmpty()) {
            return result;
        }

        // 2. Tạo scenarios cho mỗi loop header
        Map<CfgNode, int[]> loopScenarios = new HashMap<>();
        for (CfgNode loopHeader : loops.keySet()) {
            loopScenarios.put(loopHeader, new int[]{0, 1, 3, M - 1, M, M + 1});
        }

        // 3. Lấy tích cartesian
        List<Map<CfgNode, Integer>> allCombinations = cartesianProduct(loopScenarios);

        // 4. Duyệt từng kịch bản
        List<Path> paths = new ArrayList<>();
        for (Map<CfgNode, Integer> scenario : allCombinations) {
            Path path = executeScenario(root, scenario);
            paths.add(path);
        }
        return paths;
    }

    private static Map<CfgNode, Set<CfgNode>> detectLoops(CfgNode root) {
        Map<CfgNode, Set<CfgNode>> loops = new HashMap<>();
        detectLoopsRecursive(root, new HashSet<>(), new HashSet<>(), loops);
        return loops;
    }

    private static void detectLoopsRecursive(CfgNode node, Set<CfgNode> visited,
                                             Set<CfgNode> inStack, Map<CfgNode, Set<CfgNode>> loops) {
        if (node == null) return;

        visited.add(node);
        inStack.add(node);

        for (CfgNode child : getChildren(node)) {
            if (inStack.contains(child)) {
                // Back-edge: node → child, child là loop header
                loops.computeIfAbsent(child, k -> new HashSet<>()).add(node);
            } else if (!visited.contains(child)) {
                detectLoopsRecursive(child, visited, inStack, loops);
            }
            // Nếu visited nhưng không inStack → cross/forward edge, bỏ qua
        }

        inStack.remove(node);
    }

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

    private static List<Map<CfgNode, Integer>> cartesianProduct(
            Map<CfgNode, int[]> loopScenarios) {

        List<CfgNode> nodes = new ArrayList<>(loopScenarios.keySet());
        List<int[]> scenariosList = nodes.stream()
                .map(loopScenarios::get)
                .collect(Collectors.toList());

        List<Map<CfgNode, Integer>> result = new ArrayList<>();
        cartesianProductRecursive(nodes, scenariosList, 0, new HashMap<>(), result);
        return result;
    }

    private static void cartesianProductRecursive(
            List<CfgNode> nodes, List<int[]> scenariosList,
            int index, Map<CfgNode, Integer> current,
            List<Map<CfgNode, Integer>> result) {

        if (index == nodes.size()) {
            result.add(new HashMap<>(current));
            return;
        }

        CfgNode node = nodes.get(index);
        for (int scenario : scenariosList.get(index)) {
            current.put(node, scenario);
            cartesianProductRecursive(nodes, scenariosList, index + 1, current, result);
        }
        current.remove(node);
    }

    private static Path executeScenario(CfgNode root, Map<CfgNode, Integer> scenario) {
        Path path = new Path();
        Map<CfgNode, Integer> loopCounters = new HashMap<>();

        CfgNode current = root;
        while (current != null) {
            path.addLast(current);
            current = getNextNode(current, scenario, loopCounters);
        }
        return path;
    }

    private static CfgNode getNextNode(CfgNode node, Map<CfgNode, Integer> scenario,
                                       Map<CfgNode, Integer> loopCounters) {
        List<CfgNode> children = getChildren(node);

        if (children.isEmpty()) return null;
        if (children.size() == 1) return children.get(0);

        // Node điều kiện (có 2 nhánh: true/false)
        // Quy ước: children.get(0) = true branch, children.get(1) = false branch
        if (scenario.containsKey(node)) {
            int maxIterations = scenario.get(node);
            int currentCount = loopCounters.getOrDefault(node, 0);

            if (currentCount < maxIterations) {
                loopCounters.put(node, currentCount + 1);
                return children.get(0); // true → tiếp tục lặp
            } else {
                loopCounters.remove(node); // reset counter cho lần chạy khác
                return children.get(1); // false → thoát loop
            }
        }

        // Node điều kiện thường (không phải loop) → cần xử lý riêng
        return children.get(0);
    }
}
