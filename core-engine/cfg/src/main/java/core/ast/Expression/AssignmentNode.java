package core.ast.Expression;


import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Sort;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.Array.ArrayAccessNode;
import core.ast.Expression.Array.ArrayNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Name.NameNode;
import core.ast.Expression.OperationExpression.InfixExpressionNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.symbolicExecution.MemoryModel;
import core.symbolicExecution.SymbolicExecutionRewrite;
import org.eclipse.jdt.core.dom.*;

import java.util.List;
import java.util.Map;


public class AssignmentNode extends ExpressionNode {

    private Assignment.Operator operator;
    private ExpressionNode leftHandSide;
    private ExpressionNode rightHandSide;

    public static void executeAssignment(Assignment assignment, MemoryModel memoryModel) {
        AssignmentNode assignmentNode = new AssignmentNode();
        assignmentNode.operator = assignment.getOperator();
        assignmentNode.rightHandSide = (ExpressionNode) ExpressionNode.executeExpression(assignment.getRightHandSide(), memoryModel);
        assignmentNode.leftHandSide = (ExpressionNode) ExpressionNode.executeExpression(assignment.getLeftHandSide(), memoryModel);

        ExpressionNode assignValue = analyzeAssignValue(assignmentNode.leftHandSide, assignmentNode.rightHandSide, assignmentNode.operator);
        Expression leftHandSide = assignment.getLeftHandSide();

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
                    if (z3OldArray == null) {
                        // CMặc định Range là BitVec 32-bit
                        Sort rangeSort = ctx.mkBitVecSort(32);
                        Map<String, String> typeMap = SymbolicExecutionRewrite.variableTypeMap;

                        if (typeMap != null && typeMap.get(arrayName) != null) {
                            String typeStr = typeMap.get(arrayName).toString();
                            if (typeStr.equals("long")) {
                                rangeSort = ctx.mkBitVecSort(64);
                            } else if (typeStr.equals("double")) {
                                rangeSort = ctx.mkFPSortDouble();
                            } else if (typeStr.equals("float")) {
                                rangeSort = ctx.mkFPSortSingle();
                            }
                        }

                        z3OldArray = ctx.mkConst(arrayName, ctx.mkArraySort(ctx.mkBitVecSort(32), rangeSort));
                    }

                    // Dịch Index
                    Expr z3Index = OperationExpressionNode.createZ3Expression(cookedArrayIndex, ctx, vars, memoryModel);

                    // Dịch Value
                    Expr z3Value = OperationExpressionNode.createZ3Expression(assignValue, ctx, vars, memoryModel);

                    // Đẻ mảng mới và cập nhật vô sổ tay
                    Expr z3NewArray = ctx.mkStore((ArrayExpr) z3OldArray, z3Index, z3Value);
                    stateMap.put(arrayName, z3NewArray);
                }
            } catch (Exception e) {
                System.out.println("   ---> Lỗi Z3 mkStore: " + e.getMessage());
                e.printStackTrace();
            }
        }
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

    public static void replaceMethodInvocationWithStub(Assignment originAssignment, MethodInvocation originMethodInvocation, ASTNode replacement) {
        if (originAssignment.getRightHandSide() == originMethodInvocation)
            originAssignment.setRightHandSide((Expression) replacement);
    }
}
