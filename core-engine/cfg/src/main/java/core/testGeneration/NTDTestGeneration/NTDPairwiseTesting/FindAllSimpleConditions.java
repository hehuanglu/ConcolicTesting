package core.testGeneration.NTDTestGeneration.NTDPairwiseTesting;

import core.ast.Expression.Literal.BooleanLiteralNode;
import core.ast.Expression.Literal.CharacterLiteralNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.OperationExpression.InfixExpressionNode;
import core.cfg.CfgBoolExprNode;
import core.cfg.CfgForEachExpressionNode;
import core.cfg.CfgNode;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class FindAllSimpleConditions {
    private List<CfgNode> currentPath = new ArrayList<>();
    private List<String> parameterNames;
    private Class<?>[] parameterClasses;
    private Map<String, Set<Object>> extractedValues;
    private final int DEPTH = 2;

    private FindAllSimpleConditions() {
    }

    public FindAllSimpleConditions(CfgNode cfgBeginNode, List<String> parameterNames, Class<?>[] parameterClasses) {
        this.parameterNames = parameterNames;
        this.parameterClasses = parameterClasses;
        this.extractedValues = new HashMap<>();
        for (String name : parameterNames) {
            extractedValues.put(name, new HashSet<>());
        }

        findAndExtractAllSimpleConditions(cfgBeginNode);
    }

    private void findAndExtractAllSimpleConditions(CfgNode cfgNode) {
        if (cfgNode == null) return;

        // Add a path to the list of path if the node is endNode
        if (!cfgNode.getIsEndCfgNode()) {
            currentPath.add(cfgNode);
            if (cfgNode instanceof CfgBoolExprNode) {
                int numberOfDuplicateNode = numberOfDuplicateNode(cfgNode);
                CfgBoolExprNode boolExprNode = (CfgBoolExprNode) cfgNode;

                extractSimpleConditions(boolExprNode.getAst());

                // CfgBoolExprNode has 2 child node is trueNode and falseNode
                if (numberOfDuplicateNode < DEPTH) {
                    findAndExtractAllSimpleConditions(boolExprNode.getTrueNode());
                }
                CfgNode falseNode = boolExprNode.getFalseNode();
                falseNode.setIsFalseNode(true);
                findAndExtractAllSimpleConditions(falseNode);

            } else if (cfgNode instanceof CfgForEachExpressionNode) {
                int duplicateNode = numberOfDuplicateNode(cfgNode);

                // CfgForEachExpressionNode has 2 child node is hasElementNode and noMoreElementNode
                if (duplicateNode < DEPTH) {
                    findAndExtractAllSimpleConditions(((CfgForEachExpressionNode) cfgNode).getHasElementAfterNode());
                }
                findAndExtractAllSimpleConditions(((CfgForEachExpressionNode) cfgNode).getNoMoreElementAfterNode());

            } else {

                // Every other node has only one child node
                findAndExtractAllSimpleConditions(cfgNode.getAfterStatementNode());

            }
            currentPath.remove(currentPath.size() - 1);
        }
    }

    private void extractSimpleConditions(ASTNode astNode) {
        if (astNode instanceof Expression) {
            Expression expression = (Expression) astNode;
            if (expression instanceof InfixExpression) {
                extractInfixExpression((InfixExpression) expression);
            } else if (expression instanceof PrefixExpression) {
                extractSimpleConditions(((PrefixExpression) expression).getOperand());
            } else if (expression instanceof PostfixExpression) {
                extractSimpleConditions(((PostfixExpression) expression).getOperand());
            } else if (expression instanceof ParenthesizedExpression) {
                extractSimpleConditions(((ParenthesizedExpression) expression).getExpression());
            }
        }
    }

    private void extractInfixExpression(InfixExpression infixExpression) {
        Expression leftOperand = infixExpression.getLeftOperand();
        Expression rightOperand = infixExpression.getRightOperand();
        InfixExpression.Operator operator = infixExpression.getOperator();
        List<ASTNode> extendedOperands = infixExpression.extendedOperands();

        if (extendedOperands.size() == 0) {
            if (InfixExpressionNode.isComparisonOperator(operator)) {
                if (!checkInfixExpressionOperands(leftOperand, rightOperand)) {
                    checkInfixExpressionOperands(rightOperand, leftOperand);
                }
            } else {
                extractSimpleConditions(leftOperand);
                extractSimpleConditions(rightOperand);
            }
        } else {
            extractSimpleConditions(leftOperand);
            extractSimpleConditions(rightOperand);
            for (ASTNode astNode : extendedOperands) {
                extractSimpleConditions(astNode);
            }
        }
    }

    private boolean checkInfixExpressionOperands(Expression leftOperand, Expression rightOperand) {
        if (leftOperand instanceof Name) {
            String name = ((Name) leftOperand).getFullyQualifiedName();
            if (LiteralNode.isLiteral(rightOperand)) {
                return addNewValueToExtractedValues(name, rightOperand);
            } else if (rightOperand instanceof PrefixExpression) {
                PrefixExpression prefixExpression = (PrefixExpression) rightOperand;
                Expression operand = prefixExpression.getOperand();
                PrefixExpression.Operator operator = prefixExpression.getOperator();

                if (LiteralNode.isLiteral(operand)) {
                    LiteralNode literalResult = LiteralNode.analyzeOnePrefixLiteral(operator, LiteralNode.executeLiteral(operand));

                    for (int i = 0; i < parameterNames.size(); i++) {
                        if (name.equals(parameterNames.get(i))) {
                            String className = parameterClasses[i].getName();

                            extractedValues.get(name).add(extractValue(literalResult, className));

                            return true;
                        }
                    }
                }
            } else if (rightOperand instanceof PostfixExpression) {
                PostfixExpression postfixExpression = (PostfixExpression) rightOperand;
                Expression operand = postfixExpression.getOperand();

                if (LiteralNode.isLiteral(operand)) {

                    return addNewValueToExtractedValues(name, operand);
                }
            } else if (rightOperand instanceof ParenthesizedExpression) {
                ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) rightOperand;
                Expression operand = parenthesizedExpression.getExpression();

                if (LiteralNode.isLiteral(operand)) {

                    return addNewValueToExtractedValues(name, operand);
                }
            }
        } else {
            extractSimpleConditions(leftOperand);
            extractSimpleConditions(rightOperand);
        }
        return false;
    }

    private boolean addNewValueToExtractedValues(String name, Expression operand) {
        for (int i = 0; i < parameterNames.size(); i++) {
            if (name.equals(parameterNames.get(i))) {
                String className = parameterClasses[i].getName();

                extractedValues.get(name).add(extractValue(operand, className));

                return true;
            }
        }
        return false;
    }

    private Object extractValue(Expression operand, String className) {
        if ("int".equals(className)) {
            NumberLiteral numberLiteral = (NumberLiteral) operand;
            return Integer.parseInt(numberLiteral.getToken());
        } else if ("boolean".equals(className)) {
            BooleanLiteral booleanLiteral = (BooleanLiteral) operand;
            return booleanLiteral.booleanValue();
        } else if ("byte".equals(className)) {
            NumberLiteral numberLiteral = (NumberLiteral) operand;
            return Byte.parseByte(numberLiteral.getToken());
        } else if ("short".equals(className)) {
            NumberLiteral numberLiteral = (NumberLiteral) operand;
            return Short.parseShort(numberLiteral.getToken());
        } else if ("char".equals(className)) {
            CharacterLiteral characterLiteral = (CharacterLiteral) operand;
            return characterLiteral.charValue();
        } else if ("long".equals(className)) {
            NumberLiteral numberLiteral = (NumberLiteral) operand;
            return Long.parseLong(numberLiteral.getToken());
        } else if ("float".equals(className)) {
            NumberLiteral numberLiteral = (NumberLiteral) operand;
            return Float.parseFloat(numberLiteral.getToken());
        } else if ("double".equals(className)) {
            NumberLiteral numberLiteral = (NumberLiteral) operand;
            return Double.parseDouble(numberLiteral.getToken());
        } else if ("void".equals(className)) {
            return null;
        } else {
            throw new RuntimeException("Unsupported type: " + className);
        }
    }

    private Object extractValue(LiteralNode literalNode, String className) {
        if ("int".equals(className)) {
            NumberLiteralNode numberLiteral = (NumberLiteralNode) literalNode;
            return Integer.parseInt(numberLiteral.getTokenValue());
        } else if ("boolean".equals(className)) {
            BooleanLiteralNode booleanLiteral = (BooleanLiteralNode) literalNode;
            return booleanLiteral.getValue();
        } else if ("byte".equals(className)) {
            NumberLiteralNode numberLiteral = (NumberLiteralNode) literalNode;
            return Byte.parseByte(numberLiteral.getTokenValue());
        } else if ("short".equals(className)) {
            NumberLiteralNode numberLiteral = (NumberLiteralNode) literalNode;
            return Short.parseShort(numberLiteral.getTokenValue());
        } else if ("char".equals(className)) {
            CharacterLiteralNode characterLiteral = (CharacterLiteralNode) literalNode;
            return characterLiteral.getCharacterValue();
        } else if ("long".equals(className)) {
            NumberLiteralNode numberLiteral = (NumberLiteralNode) literalNode;
            return Long.parseLong(numberLiteral.getTokenValue());
        } else if ("float".equals(className)) {
            NumberLiteralNode numberLiteral = (NumberLiteralNode) literalNode;
            return Float.parseFloat(numberLiteral.getTokenValue());
        } else if ("double".equals(className)) {
            NumberLiteralNode numberLiteral = (NumberLiteralNode) literalNode;
            return Double.parseDouble(numberLiteral.getTokenValue());
        } else if ("void".equals(className)) {
            return null;
        } else {
            throw new RuntimeException("Unsupported type: " + className);
        }
    }

    private int numberOfDuplicateNode(CfgNode node) {
        int duplicateNode = 0;
        for (CfgNode nodeI : currentPath) {
            if (nodeI == node) duplicateNode++;
        }
        return duplicateNode;
    }

    public Map<String, Set<Object>> getExtractedValues() {
        return extractedValues;
    }
}
