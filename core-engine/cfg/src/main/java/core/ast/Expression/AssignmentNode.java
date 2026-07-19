package core.ast.Expression;


import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.Array.ArrayAccessNode;
import core.ast.Expression.Array.ArrayNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Name.NameNode;
import core.ast.Expression.OperationExpression.InfixExpressionNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.symbolicExecution.MemoryModel;
import core.symbolicExecution.SymbolicExecutionRewrite;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;

import java.util.List;
import java.util.Map;


public class AssignmentNode extends ExpressionNode {

    private Assignment.Operator operator;
    private ExpressionNode leftHandSide;
    private ExpressionNode rightHandSide;

    public AssignmentNode() {}

    public AssignmentNode(ExpressionNode leftHandSide, ExpressionNode rightHandSide, Assignment.Operator operator) {}

    public static void executeAssignment(Assignment assignment, MemoryModel memoryModel) {
        AssignmentNode assignmentNode = new AssignmentNode();
        assignmentNode.operator = assignment.getOperator();
        assignmentNode.rightHandSide = (ExpressionNode) ExpressionNode.executeExpression(assignment.getRightHandSide(), memoryModel);
        assignmentNode.leftHandSide = (ExpressionNode) ExpressionNode.executeExpression(assignment.getLeftHandSide(), memoryModel);

        ExpressionNode assignValue = analyzeAssignValue(assignmentNode.leftHandSide, assignmentNode.rightHandSide, assignmentNode.operator);
        Expression leftHandSide = assignment.getLeftHandSide();

        writeAssignedValue(leftHandSide, assignValue, memoryModel);
    }

    private static ExpressionNode analyzeAssignValue(ExpressionNode variable, ExpressionNode initialValue, Assignment.Operator assignmentOperator) {
        InfixExpressionNode assignValue = new InfixExpressionNode();
        assignValue.setLeftOperand(variable);
        assignValue.setRightOperand(initialValue);

        if (assignmentOperator.equals(Assignment.Operator.ASSIGN)) {
            return initialValue;
        } else if (assignmentOperator.equals(Assignment.Operator.PLUS_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.PLUS);
        } else if (assignmentOperator.equals(Assignment.Operator.MINUS_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.MINUS);
        } else if (assignmentOperator.equals(Assignment.Operator.DIVIDE_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.DIVIDE);
        } else if (assignmentOperator.equals(Assignment.Operator.TIMES_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.TIMES);
        } else if (assignmentOperator.equals(Assignment.Operator.REMAINDER_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.REMAINDER);
        } else if (assignmentOperator.equals(Assignment.Operator.BIT_OR_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.OR);
        } else if (assignmentOperator.equals(Assignment.Operator.BIT_AND_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.AND);
        } else if (assignmentOperator.equals(Assignment.Operator.BIT_XOR_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.XOR);
        } else if (assignmentOperator.equals(Assignment.Operator.LEFT_SHIFT_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.LEFT_SHIFT);
        } else if (assignmentOperator.equals(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED);
        } else if (assignmentOperator.equals(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN)) {
            assignValue.setOperator(InfixExpression.Operator.RIGHT_SHIFT_SIGNED);
        } else {
            throw new RuntimeException("Invalid operator");
        }

        if (initialValue instanceof LiteralNode && variable instanceof LiteralNode) {
            return LiteralNode.analyzeTwoInfixLiteral((LiteralNode) initialValue, assignValue.getOperator(), (LiteralNode) assignValue.getRightOperand());
        } else {
            return assignValue;
        }
    }

    public static ExpressionNode executeIncrementDecrement(Expression originalOperand,
                                                           ExpressionNode currentValue,
                                                           boolean isIncrement,
                                                           MemoryModel memoryModel) {
        IntegerLiteralNode one = new IntegerLiteralNode();
        one.setTokenValue(1);

        Assignment.Operator op = isIncrement ? Assignment.Operator.PLUS_ASSIGN : Assignment.Operator.MINUS_ASSIGN;
        ExpressionNode assignValue = analyzeAssignValue(currentValue, one, op);

        writeAssignedValue(originalOperand, assignValue, memoryModel);

        return assignValue; // giá trị SAU khi tăng/giảm — caller (prefix) trả thẳng, postfix bỏ qua
    }

    private static void writeAssignedValue(Expression leftHandSide, ExpressionNode assignValue, MemoryModel memoryModel) {
        if (leftHandSide instanceof Name) {
            String key = NameNode.getStringName((Name) leftHandSide);
            memoryModel.assignVariable(key, assignValue);
        } else if (leftHandSide instanceof ArrayAccess) {
            ArrayAccess arrayAccess = (ArrayAccess) leftHandSide;

            // 1. Index "luộc chín" (Dành riêng cho việc lưu RAM)
            ExpressionNode cookedArrayIndex = (ExpressionNode) AstNode.executeASTNode(arrayAccess.getIndex(), memoryModel);

            // Chỉ gán vào RAM nếu Index là số cụ thể.
            if (cookedArrayIndex instanceof LiteralNode) {
                int index = LiteralNode.changeLiteralNodeToInteger((LiteralNode) cookedArrayIndex);
                Expression arrayExpression = arrayAccess.getArray();
                ArrayNode arrayNode;
                if (arrayExpression instanceof ArrayAccess) {
                    arrayNode = (ArrayNode) ArrayAccessNode.executeArrayAccessNode((ArrayAccess) arrayExpression, memoryModel);
                } else if (arrayExpression instanceof Name) {
                    String name = NameNode.getStringName((Name) arrayExpression);
                    arrayNode = (ArrayNode) memoryModel.getValue(name);
                } else {
                    throw new RuntimeException("Can't execute ArrayAccess");
                }
                arrayNode.assignElements(index, assignValue);
            } else {
                System.out.println("Bỏ qua gán RAM do Index là symbolic");
            }

            try {
                String arrayName = arrayAccess.getArray().toString();

                Context ctx = core.symbolicExecution.SymbolicExecutionRewrite.globalCtx.get();
                List<Z3VariableWrapper> vars = core.symbolicExecution.SymbolicExecutionRewrite.globalZ3Vars.get();
                Map<String, Expr> stateMap = core.symbolicExecution.SymbolicExecutionRewrite.z3ArrayStateMap.get();

                if (ctx != null && stateMap != null) {
                    // Lấy mảng cũ
                    Expr z3OldArray = stateMap.get(arrayName);
                    System.out.println(SymbolicExecutionRewrite.variableTypeMap.get(arrayName));
                    System.out.println("arrayName = " + arrayName);
                    System.out.println("variableTypeMap = " + SymbolicExecutionRewrite.variableTypeMap);
                    if (z3OldArray == null) {
                        Map<String, String> typeMap = SymbolicExecutionRewrite.variableTypeMap;

                        Sort rangeSort = ctx.mkIntSort();

                        if (typeMap != null) {
                            String typeStr = typeMap.get(arrayName);
                            if (typeStr != null) {
                                switch (typeStr) {
                                    case "boolean[]":
                                        rangeSort = ctx.mkBoolSort();
                                        break;
                                    case "float[]":
                                        rangeSort = ctx.mkFPSortSingle();
                                        break;
                                    case "double[]":
                                        rangeSort = ctx.mkFPSortDouble();
                                        break;
                                    case "byte[]":
                                    case "short[]":
                                    case "char[]":
                                    case "int[]":
                                    case "long[]":
                                        rangeSort = ctx.mkIntSort();
                                        break;
                                    default:
                                        throw new RuntimeException("Can't execute ArrayAccess with type " + arrayName);
                                }
                            }
                        }

                        z3OldArray = ctx.mkConst(
                                arrayName,
                                ctx.mkArraySort(ctx.mkIntSort(), rangeSort)
                        );

                        System.out.println("z3OldArray sortType: " + rangeSort);
                    }

                    Expr z3Index = OperationExpressionNode.createZ3Expression(cookedArrayIndex, ctx, vars, memoryModel);
                    Expr z3Value = OperationExpressionNode.createZ3Expression(assignValue, ctx, vars, memoryModel);
                    Expr z3NewArray = ctx.mkStore((ArrayExpr) z3OldArray, z3Index, z3Value);
                    System.out.println("oldArray = " + z3OldArray);
                    System.out.println("index    = " + z3Index);
                    System.out.println("value    = " + z3Value);
                    stateMap.put(arrayName, z3NewArray);
                }
            } catch (Exception e) {
                System.out.println("   ---> Lỗi Z3 mkStore: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void replaceMethodInvocationWithStub(Assignment originAssignment, MethodInvocation originMethodInvocation, ASTNode replacement) {
        if (originAssignment.getRightHandSide() == originMethodInvocation)
            originAssignment.setRightHandSide((Expression) replacement);
    }
}
