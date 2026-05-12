package core.cfg.utils;

import core.cfg.CfgBoolExprNode;
import core.cfg.CfgForEachExpressionNode;
import core.cfg.CfgNode;
import core.cfg.dataFlow.DefUsePair;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

/**
 * Hàm bổ sung thông tin 1 node đã định nghĩa biến nào, có sử dụng biến nào không
 */
public class DataFlowHelper {
    public static void ComputeDefUse(CfgNode cfgNode) {
        Set<CfgNode> visited = new HashSet<>();
        Queue<CfgNode> queue = new LinkedList<>();
        queue.add(cfgNode);
        while (!queue.isEmpty()) {
            CfgNode currentNode = queue.poll();
            if (visited.contains(currentNode)) {
                continue;
            }
            visited.add(currentNode);

            // Phân tích AST của node này để tìm Def/Use
            if (currentNode.getAst() != null) {
                extractDefUseFromAST(currentNode, currentNode.getAst());
            }

            // Duyệt tiếp các node con
            if (currentNode.getAfterStatementNode() != null) {
                queue.add(currentNode.getAfterStatementNode());
            }

            if (currentNode instanceof CfgBoolExprNode) {
                CfgBoolExprNode b = (CfgBoolExprNode) currentNode;
                if (b.getTrueNode() != null) queue.add(b.getTrueNode());
                if (b.getFalseNode() != null) queue.add(b.getFalseNode());
            } else if (currentNode instanceof CfgForEachExpressionNode) {
                CfgForEachExpressionNode fe = (CfgForEachExpressionNode) currentNode;
                if (fe.getHasElementAfterNode() != null) queue.add(fe.getHasElementAfterNode());
                if (fe.getNoMoreElementAfterNode() != null) queue.add(fe.getNoMoreElementAfterNode());
            }
        }

    }

    // Hàm bóc tách biến từ ASTNode
    private static void extractDefUseFromAST(CfgNode cfgNode, ASTNode astNode) {
        astNode.accept(new ASTVisitor() {

            // bắt các phép gán (assignment) x = y +1
            @Override
            public boolean visit(Assignment node) {
                if (node.getLeftHandSide() instanceof SimpleName) {
                    cfgNode.addDefVar(((SimpleName) node.getLeftHandSide()).getIdentifier());
                }

                return true;
            }

            // biến khởi tạo
            @Override
            public boolean visit(VariableDeclarationFragment node) {
                cfgNode.addDefVar(node.getName().getIdentifier());

                return true;
            }

            // câu lệnh khai báo biến
            @Override
            public boolean visit(VariableDeclarationStatement node) {
                for (Object obj : node.fragments()) {
                    VariableDeclarationFragment fragment = (VariableDeclarationFragment) obj;
                    cfgNode.addDefVar(fragment.getName().getIdentifier());
                }

                return true;
            }

            // lệnh khai báo biến trong vòng for
            @Override
            public boolean visit(VariableDeclarationExpression node) {
                for (Object obj : node.fragments()) {
                    VariableDeclarationFragment fragment = (VariableDeclarationFragment) obj;
                    cfgNode.addDefVar(fragment.getName().getIdentifier());
                }

                return true;
            }

            @Override
            public boolean visit(SimpleName node) {
                if (!isDef(node)) {
                    cfgNode.addUseVar(node.getIdentifier());
                }
                return false;
            }

            // biểu thức --x, ++y
            @Override
            public boolean visit(PostfixExpression node) {
                if (node.getOperand() instanceof SimpleName) {
                    String name = ((SimpleName) node.getOperand()).getIdentifier();
                    cfgNode.addDefVar(name);
                    cfgNode.addUseVar(name);
                }

                return false;
            }

            @Override
            public boolean visit(PrefixExpression node) {
                if (node.getOperand() instanceof SimpleName) {
                    String name = ((SimpleName) node.getOperand()).getIdentifier();
                    if (node.getOperator() == PrefixExpression.Operator.INCREMENT ||
                            node.getOperator() == PrefixExpression.Operator.DECREMENT) {
                        cfgNode.addDefVar(name);
                    }
                    cfgNode.addUseVar(name);
                }
                return false;
            }
        });
    }

    // Kiểm tra xem SimpleName có phải là Def không
    private static boolean isDef(SimpleName simpleName) {
        ASTNode parent = simpleName.getParent();

        // nếu là vế trái của phép gán
        if (parent instanceof Assignment && ((Assignment) parent).getLeftHandSide() == simpleName) {
            return true;
        }

        if (parent instanceof VariableDeclarationFragment && ((VariableDeclarationFragment) parent).getName() == simpleName) {
            return true;
        }

        return false;
    }

    // Tìm tất cả các cặp DUA
    public static Set<DefUsePair> findAllDUAs(CfgNode cfgNode) {
        Set<DefUsePair> result = new HashSet<>();
        List<CfgNode> allNodes = getAllNode(cfgNode);

        for (CfgNode d : allNodes) {
            for (String var : d.getDefVars()) {
                for (CfgNode u : allNodes) {
                    if (u.getUseVars().contains(var)) {
                        result.add(new DefUsePair(var, d, u));
                    }
                }
            }
        }
        return result;
    }

    // Lấy tất cả các node của CFG
    private static List<CfgNode> getAllNode(CfgNode cfgNode) {
        List<CfgNode> result = new ArrayList<>();
        Queue<CfgNode> queue = new LinkedList<>();
        Set<CfgNode> visited = new HashSet<>();
        queue.add(cfgNode);
        while (!queue.isEmpty()) {
            CfgNode currentNode = queue.poll();
            if (visited.contains(currentNode)) {
                continue;
            }

            visited.add(currentNode);
            result.add(currentNode);
            if (currentNode.getAfterStatementNode() != null) {
                queue.add(currentNode.getAfterStatementNode());
            }

            if (currentNode instanceof CfgBoolExprNode) {
                CfgBoolExprNode b = (CfgBoolExprNode) currentNode;
                if (b.getTrueNode() != null) queue.add(b.getTrueNode());
                if (b.getFalseNode() != null) queue.add(b.getFalseNode());
            } else if (currentNode instanceof CfgForEachExpressionNode) {
                CfgForEachExpressionNode fe = (CfgForEachExpressionNode) currentNode;
                if (fe.getHasElementAfterNode() != null) queue.add(fe.getHasElementAfterNode());
                if (fe.getNoMoreElementAfterNode() != null) queue.add(fe.getNoMoreElementAfterNode());
            }
        }
        return result;
    }

    // Tìm tối đa 'limit' đường đi từ start -> end
    public static List<List<CfgNode>> findAllPaths(CfgNode start, CfgNode end, int limit) {
        List<List<CfgNode>> result = new ArrayList<>();
        List<CfgNode> currentPath = new ArrayList<>();
        Set<CfgNode> visited = new HashSet<>();

        currentPath.add(start);
        visited.add(start);

        dfsFindPaths(start, end, visited, currentPath, result, limit);

        return result;
    }

    private static void dfsFindPaths(CfgNode current, CfgNode target, Set<CfgNode> visited, List<CfgNode> currentPath,
                                     List<List<CfgNode>> allPath, int limit) {
        // Điều kiện dừng: Đã tìm đủ số lượng đường cần thiết
        if (allPath.size() > limit) {
            return;
        }

        // Nếu đến đích
        if (current == target) {
            allPath.add(new ArrayList<>(currentPath));
            return;
        }

        List<CfgNode> nextNodes = getNextNode(current);
        for (CfgNode nextNode : nextNodes) {
            if (!visited.contains(nextNode)) {
                visited.add(nextNode);
                currentPath.add(nextNode);
                dfsFindPaths(nextNode, target, visited, currentPath, allPath, limit);

                currentPath.remove(currentPath.size() - 1);
                visited.remove(nextNode);
            }
        }
    }

    public static List<List<CfgNode>> findAllDefClearPaths(CfgNode defNode, CfgNode useNode, String varName, int limit) {
        List<List<CfgNode>> allPaths = new ArrayList<>();
        List<CfgNode> currentPath = new ArrayList<>();
        Set<CfgNode> visited = new HashSet<>();

        currentPath.add(defNode);
        visited.add(defNode); // DefNode được phép đi qua lúc đầu

        dfsDefClear(defNode, useNode, varName, visited, currentPath, allPaths, limit);
        return allPaths;
    }

    private static void dfsDefClear(CfgNode current, CfgNode target, String varName, Set<CfgNode> visited,
                                    List<CfgNode> currentPath, List<List<CfgNode>> allPaths, int limit) {
        if (allPaths.size() >= limit) return;

        if (current == target) {
            allPaths.add(new ArrayList<>(currentPath));
            return;
        }

        List<CfgNode> nextNodes = getNextNode(current);

        for (CfgNode nextNode : nextNodes) {
            // Nếu nextNode định nghĩa lại biến và nextNode không phải đích -> bỏ qua
            if (nextNode.getDefVars().contains(varName) && nextNode != target) {
                continue;
            }

            if (!visited.contains(nextNode)) {
                visited.add(nextNode);
                currentPath.add(nextNode);

                dfsDefClear(nextNode, target, varName, visited, currentPath, allPaths, limit);

                currentPath.remove(currentPath.size() - 1);
                visited.remove(nextNode);
            }
        }
    }

    // tìm đường đi giữa hai node. Dùng cho BeginNode -> DefNode và UseNode -> EndNode
    public static List<CfgNode> findStandardPath(CfgNode begin, CfgNode end) {
        Queue<List<CfgNode>> queue = new LinkedList<>();
        Set<CfgNode> visited = new HashSet<>();
        List<CfgNode> initialPath = new ArrayList<>();
        initialPath.add(begin);
        queue.add(initialPath);
        visited.add(begin);
        while (!queue.isEmpty()) {
            List<CfgNode> currentPath = queue.poll();
            CfgNode lastNode = currentPath.get(currentPath.size() - 1);
            // tới đích trả về đường thi hành
            if (lastNode == end) {
                return currentPath;
            }

            List<CfgNode> nextPath = getNextNode(lastNode);
            for (CfgNode nextNode : nextPath) {
                if (!visited.contains(nextNode)) {
                    visited.add(nextNode);
                    List<CfgNode> newPath = new ArrayList<>(currentPath);
                    newPath.add(nextNode);
                    queue.add(newPath);
                }
            }
        }
        return null;
    }

    // lấy danh sách các node tiếp theo của node hiện tại
    private static List<CfgNode> getNextNode(CfgNode cfgNode) {
        List<CfgNode> result = new ArrayList<>();

        if (cfgNode.getAfterStatementNode() != null) {
            result.add(cfgNode.getAfterStatementNode());
        }

        if (cfgNode instanceof CfgBoolExprNode) {
            CfgBoolExprNode fe = (CfgBoolExprNode) cfgNode;
            if (fe.getTrueNode() != null) result.add(fe.getTrueNode());
            if (fe.getFalseNode() != null) result.add(fe.getFalseNode());
        } else if (cfgNode instanceof CfgForEachExpressionNode) {
            CfgForEachExpressionNode fe = (CfgForEachExpressionNode) cfgNode;
            if (fe.getHasElementAfterNode() != null) result.add(fe.getHasElementAfterNode());
            if (fe.getNoMoreElementAfterNode() != null) {
                result.add(fe.getNoMoreElementAfterNode());
            }
        }
        return result;
    }

//    // Tìm đường từ Def -> Use mà biến không bị định nghĩa lại
//    public static List<CfgNode> findDefClearPath(CfgNode defNode, CfgNode useNode, String varName) {
//        Queue<List<CfgNode>> queue = new LinkedList<>();
//        Set<CfgNode> visited = new HashSet<>();
//
//        List<CfgNode> initialPath = new ArrayList<>();
//        initialPath.add(defNode);
//        queue.add(initialPath);
//        visited.add(defNode);
//
//        while (!queue.isEmpty()) {
//            List<CfgNode> currentPath = queue.poll();
//            CfgNode lastNode = currentPath.get(currentPath.size() - 1);
//
//            // tới đích trả về đường thi hành
//            if (lastNode == useNode) {
//                return currentPath;
//            }
//
//            List<CfgNode> nextPath = getNextNode(lastNode);
//            for (CfgNode nextNode : nextPath) {
//                // Nếu node tiếp có định nghĩa lại biến varName -> bỏ qua
//                // Trừ khi hàng xóm chính là node đến thì vẫn cho đi vào.
//                if (nextNode.getDefVars().contains(varName) && nextNode != useNode) {
//                    continue;
//                }
//
//                if (!visited.contains(nextNode)) {
//                    visited.add(nextNode);
//
//                    List<CfgNode> newPath = new ArrayList<>(currentPath);
//                    newPath.add(nextNode);
//                    queue.add(newPath);
//                }
//            }
//        }
//        return null;
//    }
}
