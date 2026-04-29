package core.ast.Expression.OperationExpression;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class InfixExpressionNode extends OperationExpressionNode {

    private ExpressionNode leftOperand;
    private ExpressionNode rightOperand;
    private InfixExpression.Operator operator;
    private List<AstNode> extendedOperands;

    public static void replaceMethodInvocationWithStub(InfixExpression originInfixExpression, MethodInvocation originMethodInvocation, ASTNode replacement) {
        Expression leftOperand = originInfixExpression.getLeftOperand();
        Expression rightOperand = originInfixExpression.getRightOperand();
        List<ASTNode> extendedOperands = originInfixExpression.extendedOperands();
        if (leftOperand == originMethodInvocation)
            originInfixExpression.setLeftOperand((Expression) replacement);
        else if (rightOperand == originMethodInvocation) {
            originInfixExpression.setRightOperand((Expression) replacement);
        } else {
            for (int i = 0; i < extendedOperands.size(); i++) {
                if (extendedOperands.get(i) == originMethodInvocation) {
                    extendedOperands.set(i, replacement);
                }
            }
        }
    }

    public static Expr createZ3Expression(InfixExpressionNode infixExpressionNode, Context ctx, List<Z3VariableWrapper> vars, MemoryModel memoryModel) {
        ExpressionNode leftOperand = infixExpressionNode.leftOperand;
        ExpressionNode rightOperand = infixExpressionNode.rightOperand;
        InfixExpression.Operator operator = infixExpressionNode.operator;
        List<AstNode> extendedOperands = infixExpressionNode.extendedOperands;

        Expr Z3LeftOperand = OperationExpressionNode.createZ3Expression(leftOperand, ctx, vars, memoryModel);
        Expr Z3RightOperand = OperationExpressionNode.createZ3Expression(rightOperand, ctx, vars, memoryModel);

        Expr result = createInfixZ3Expression(ctx, Z3LeftOperand, operator, Z3RightOperand);

        if (extendedOperands == null) {
            return result;
        }
        for (int i = 0; i < extendedOperands.size(); i++) {
            Expr extendedOperand = OperationExpressionNode.createZ3Expression((ExpressionNode) extendedOperands.get(i), ctx, vars, memoryModel);
            result = createInfixZ3Expression(ctx, result, operator, extendedOperand);
        }

        return result;
    }

    private static Expr createInfixZ3Expression(Context ctx, Expr Z3LeftOperand, InfixExpression.Operator operator, Expr Z3RightOperand) {
        //Boolean operations
        if (Z3LeftOperand instanceof BoolExpr || Z3RightOperand instanceof BoolExpr) {
            if (operator.equals(InfixExpression.Operator.CONDITIONAL_AND)) {
                return ctx.mkAnd((BoolExpr) Z3LeftOperand, (BoolExpr) Z3RightOperand);
            } else if (operator.equals(InfixExpression.Operator.CONDITIONAL_OR)) {
                return ctx.mkOr((BoolExpr) Z3LeftOperand, (BoolExpr) Z3RightOperand);
            } else if (operator.equals(InfixExpression.Operator.EQUALS)) {
                return ctx.mkEq(Z3LeftOperand, Z3RightOperand);
            } else if (operator.equals(InfixExpression.Operator.NOT_EQUALS)) {
                return ctx.mkDistinct(Z3LeftOperand, Z3RightOperand);
            } else if (operator.equals(InfixExpression.Operator.AND)) {
                return ctx.mkAnd((BoolExpr) Z3LeftOperand, (BoolExpr) Z3RightOperand);
            } else if (operator.equals(InfixExpression.Operator.OR)) {
                return ctx.mkOr((BoolExpr) Z3LeftOperand, (BoolExpr) Z3RightOperand);
            } else if (operator.equals(InfixExpression.Operator.XOR)) {
                return ctx.mkXor((BoolExpr) Z3LeftOperand, (BoolExpr) Z3RightOperand);
            } else {
                throw new RuntimeException("Invalid operator for boolean operands: " + operator);
            }
        }

        boolean fpLeft = isFP(Z3LeftOperand);
        boolean fpRight = isFP(Z3RightOperand);
        boolean isFPExpr = fpLeft || fpRight;
        // Nếu 1 trong 2 là FP thì ép kiểu cái còn lại sang FP
        if (isFPExpr) {
            // Rounding mode mặc định cho Java float/double
            FPRMExpr rm = ctx.mkFPRoundNearestTiesToEven();
            FPSort fpSort = pickFpSort(Z3LeftOperand, Z3RightOperand, ctx);
            FPExpr l = coerceToFP(ctx, Z3LeftOperand, fpSort, rm);
            FPExpr r = coerceToFP(ctx, Z3RightOperand, fpSort, rm);
            if (operator.equals(InfixExpression.Operator.PLUS)) {
                return ctx.mkFPAdd(rm, l, r);
            } else if (operator.equals(InfixExpression.Operator.MINUS)) {
                return ctx.mkFPSub(rm, l, r);
            } else if (operator.equals(InfixExpression.Operator.TIMES)) {
                return ctx.mkFPMul(rm, l, r);
            } else if (operator.equals(InfixExpression.Operator.DIVIDE)) {
                return ctx.mkFPDiv(rm, l, r);
            } else if (operator.equals(InfixExpression.Operator.REMAINDER)) {
                return ctx.mkFPRem(l, r);
            } else if (operator.equals(InfixExpression.Operator.EQUALS)) {
                return ctx.mkFPEq(l, r);
            } else if (operator.equals(InfixExpression.Operator.NOT_EQUALS)) {
                return ctx.mkDistinct(l, r);
            } else if (operator.equals(InfixExpression.Operator.LESS)) {
                return ctx.mkFPLt(l, r);
            } else if (operator.equals(InfixExpression.Operator.GREATER)) {
                return ctx.mkFPGt(l, r);
            } else if (operator.equals(InfixExpression.Operator.LESS_EQUALS)) {
                return ctx.mkFPLEq(l, r);
            } else if (operator.equals(InfixExpression.Operator.GREATER_EQUALS)) {
                return ctx.mkFPGEq(l, r);
            } else {
                throw new RuntimeException("Invalid operator for floating-point operands: " + operator);
            }
        }

        //Xử lý phép toán BitVec
        boolean isShift = operator.equals(InfixExpression.Operator.LEFT_SHIFT)
                || operator.equals(InfixExpression.Operator.RIGHT_SHIFT_SIGNED)
                || operator.equals(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED);
        //Nếu là shift thì ép right theo left và mask, còn không thì ép cả 2 theo max size
        if (isShift) {
            int targetSize = ((BitVecExpr) Z3LeftOperand).getSortSize();
            Z3RightOperand = fixBvWidth(ctx, (BitVecExpr) Z3RightOperand, targetSize, false);
            BitVecExpr mask = ctx.mkBV(targetSize - 1, targetSize);
            Z3RightOperand = ctx.mkBVAND((BitVecExpr) Z3RightOperand, mask);
        } else {
            int max = Math.max(((BitVecExpr) Z3LeftOperand).getSortSize(), ((BitVecExpr) Z3RightOperand).getSortSize());
            Z3LeftOperand = fixBvWidth(ctx, (BitVecExpr) Z3LeftOperand, max, true);
            Z3RightOperand = fixBvWidth(ctx, (BitVecExpr) Z3RightOperand, max, true);
        }

        if (operator.equals(InfixExpression.Operator.PLUS)) {
            return ctx.mkBVAdd((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.MINUS)) {
            return ctx.mkBVSub((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.TIMES)) {
            return ctx.mkBVMul((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.DIVIDE)) {
            return ctx.mkBVSDiv((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.REMAINDER)) {
            return ctx.mkBVSRem((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.LESS)) {
            return ctx.mkBVSLT((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.GREATER)) {
            return ctx.mkBVSGT((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.LESS_EQUALS)) {
            return ctx.mkBVSLE((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.GREATER_EQUALS)) {
            return ctx.mkBVSGE((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.EQUALS)) {
            return ctx.mkEq(Z3LeftOperand, Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.NOT_EQUALS)) {
            return ctx.mkDistinct(Z3LeftOperand, Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.AND)) {
            return ctx.mkBVAND((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.OR)) {
            return ctx.mkBVOR((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.XOR)) {
            return ctx.mkBVXOR((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.LEFT_SHIFT)) {
            return ctx.mkBVSHL((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.RIGHT_SHIFT_SIGNED)) {
            return ctx.mkBVASHR((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else if (operator.equals(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED)) {
            return ctx.mkBVLSHR((BitVecExpr) Z3LeftOperand, (BitVecExpr) Z3RightOperand);
        } else {
            throw new RuntimeException("Invalid operator for bit-vectors: " + operator);
        }
    }

    public static ExpressionNode executeInfixExpression(InfixExpression infixExpression, MemoryModel memoryModel) {
        InfixExpressionNode infixExpressionNode = new InfixExpressionNode();
        infixExpressionNode.leftOperand = (ExpressionNode) ExpressionNode.executeExpression(infixExpression.getLeftOperand(), memoryModel);
        infixExpressionNode.rightOperand = (ExpressionNode) ExpressionNode.executeExpression(infixExpression.getRightOperand(), memoryModel);
        infixExpressionNode.operator = infixExpression.getOperator();

        List<AstNode> extendedOperands = new ArrayList<>();
        if (infixExpression.extendedOperands().size() > 0) {
            for (int i = 0; i < infixExpression.extendedOperands().size(); i++) {
                extendedOperands.add(AstNode.executeASTNode((ASTNode) infixExpression.extendedOperands().get(i), memoryModel));
            }
        }
        infixExpressionNode.extendedOperands = extendedOperands;

        ExpressionNode expressionNode = executeInfixExpressionNode(infixExpressionNode, memoryModel);

        return expressionNode;
    }

    public static ExpressionNode executeInfixExpressionNode(InfixExpressionNode infixExpressionNode,
                                                            MemoryModel memoryModel) {
        ExpressionNode leftOperand = infixExpressionNode.leftOperand;
        ExpressionNode rightOperand = infixExpressionNode.rightOperand;
        InfixExpression.Operator operator = infixExpressionNode.operator;
        List<AstNode> extendedOperands = infixExpressionNode.extendedOperands;

        if (leftOperand.isLiteralNode() && rightOperand.isLiteralNode()) {
            LiteralNode literalResult = LiteralNode.analyzeTwoInfixLiteral((LiteralNode) leftOperand, operator, (LiteralNode) rightOperand);

            if (extendedOperands != null && !extendedOperands.isEmpty()) {
                List<AstNode> newExtended = new ArrayList<>(extendedOperands);

                InfixExpressionNode nextStepNode = new InfixExpressionNode();
                nextStepNode.leftOperand = literalResult;
                nextStepNode.rightOperand = (ExpressionNode) newExtended.remove(0);
                nextStepNode.operator = operator;
                nextStepNode.extendedOperands = newExtended;

                return executeInfixExpressionNode(nextStepNode, memoryModel);
            } else {
                return literalResult;
            }
        } else {
            InfixExpressionNode newNode = new InfixExpressionNode();
            newNode.setOperator(operator);

            if (!leftOperand.isLiteralNode()) {
                ExpressionNode tmp = OperationExpressionNode.executeOperandNode(leftOperand, memoryModel);
                newNode.setLeftOperand(tmp != null ? tmp : leftOperand);
            } else {
                newNode.setLeftOperand(leftOperand);
            }

            if (!rightOperand.isLiteralNode()) {
                ExpressionNode tmp = OperationExpressionNode.executeOperandNode(rightOperand, memoryModel);
                newNode.setRightOperand(tmp != null ? tmp : rightOperand);
            } else {
                newNode.setRightOperand(rightOperand);
            }

            if (extendedOperands != null) {
                List<AstNode> newExtendedList = new ArrayList<>();
                for (AstNode op : extendedOperands) {
                    if (op instanceof ExpressionNode) {
                        ExpressionNode exprOp = (ExpressionNode) op;
                        ExpressionNode executedOp = OperationExpressionNode.executeOperandNode(exprOp, memoryModel);
                        newExtendedList.add(executedOp);
                    } else {
                        newExtendedList.add(op);
                    }
                }
                newNode.extendedOperands = newExtendedList;
            }

            return newNode;
        }
//        if (leftOperand.isLiteralNode() && rightOperand.isLiteralNode()) {
//            LiteralNode literalResult = LiteralNode.analyzeTwoInfixLiteral((LiteralNode) leftOperand, operator, (LiteralNode) rightOperand);
//
//            if (extendedOperands != null) {
//                if (extendedOperands.isEmpty()) {
//                    return literalResult;
//                }
//            } else {
//                infixExpressionNode.leftOperand = literalResult;
//                infixExpressionNode.rightOperand = (ExpressionNode) extendedOperands.remove(0);
//                return executeInfixExpressionNode(infixExpressionNode, memoryModel);
//            }
//        } else {
//            ExpressionNode oldLeftOperand = infixExpressionNode.leftOperand;
//            ExpressionNode oldRightOperand = infixExpressionNode.rightOperand;
//
//            if (!leftOperand.isLiteralNode()) {
//                ExpressionNode tmp = OperationExpressionNode.executeOperandNode(leftOperand, memoryModel);
//                if (tmp != infixExpressionNode) {
//                    infixExpressionNode.leftOperand = tmp;
//                } else if (infixExpressionNode.leftOperand instanceof NameNode) {
//                    infixExpressionNode.leftOperand.markFake();
//                }
//            }
//            if (!rightOperand.isLiteralNode()) {
//                infixExpressionNode.rightOperand = OperationExpressionNode.executeOperandNode(rightOperand, memoryModel);
//            }
//
//            if (oldLeftOperand != infixExpressionNode.leftOperand || oldRightOperand != infixExpressionNode.rightOperand) {
//                return executeInfixExpressionNode(infixExpressionNode, memoryModel);
//            } else {
//                return infixExpressionNode;
//            }
//        }
//        return infixExpressionNode;
    }

    public static boolean isBitwiseOperator(InfixExpression.Operator operator) {
        return (operator.equals(InfixExpression.Operator.OR) ||
                operator.equals(InfixExpression.Operator.XOR) ||
                operator.equals(InfixExpression.Operator.AND) ||
                operator.equals(InfixExpression.Operator.LEFT_SHIFT) ||
                operator.equals(InfixExpression.Operator.RIGHT_SHIFT_SIGNED) ||
                operator.equals(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED));
    }

    public static boolean isBooleanBitwiseOperator(InfixExpression.Operator operator) {
        return (operator.equals(InfixExpression.Operator.OR) ||
                operator.equals(InfixExpression.Operator.XOR) ||
                operator.equals(InfixExpression.Operator.AND));
    }

    public static boolean isArithmeticOperator(InfixExpression.Operator operator) {
        return (operator.equals(InfixExpression.Operator.PLUS) ||
                operator.equals(InfixExpression.Operator.MINUS) ||
                operator.equals(InfixExpression.Operator.DIVIDE) ||
                operator.equals(InfixExpression.Operator.TIMES) ||
                operator.equals(InfixExpression.Operator.REMAINDER));
    }

    public static boolean isComparisonOperator(InfixExpression.Operator operator) {
        return (operator.equals(InfixExpression.Operator.EQUALS) ||
                operator.equals(InfixExpression.Operator.NOT_EQUALS) ||
                operator.equals(InfixExpression.Operator.LESS) ||
                operator.equals(InfixExpression.Operator.GREATER) ||
                operator.equals(InfixExpression.Operator.LESS_EQUALS) ||
                operator.equals(InfixExpression.Operator.GREATER_EQUALS));
    }

    public static boolean isConditionalOperator(InfixExpression.Operator operator) {
        return (operator.equals(InfixExpression.Operator.CONDITIONAL_OR) ||
                operator.equals(InfixExpression.Operator.CONDITIONAL_AND));
    }

    public static boolean isBooleanComparisonOperator(InfixExpression.Operator operator) {
        return (operator.equals(InfixExpression.Operator.EQUALS) ||
                operator.equals(InfixExpression.Operator.NOT_EQUALS));
    }

    public static boolean isStringComparisonOperator(InfixExpression.Operator operator) {
        return (operator.equals(InfixExpression.Operator.EQUALS) ||
                operator.equals(InfixExpression.Operator.NOT_EQUALS));
    }

    public static boolean isStringConcatenationOperator(InfixExpression.Operator operator) {
        return operator.equals(InfixExpression.Operator.PLUS);
    }

    public void setLeftOperand(ExpressionNode leftOperand) {
        this.leftOperand = leftOperand;
    }

    public void setRightOperand(ExpressionNode rightOperand) {
        this.rightOperand = rightOperand;
    }

    public void setOperator(InfixExpression.Operator operator) {
        this.operator = operator;
    }

    public ExpressionNode getLeftOperand() {
        return leftOperand;
    }

    public ExpressionNode getRightOperand() {
        return rightOperand;
    }

    public InfixExpression.Operator getOperator() {
        return operator;
    }

    private static boolean isFP(Expr e) {
        return e.getSort() instanceof FPSort;
    }

    private static FPSort pickFpSort(Expr a, Expr b, Context ctx) {
        // Nếu 1 trong 2 là FP -> lấy sort của cái đó
        Sort sa = a.getSort();
        Sort sb = b.getSort();
        FPSort fa = sa instanceof FPSort ? (FPSort) sa : null;
        FPSort fb = sb instanceof FPSort ? (FPSort) sb : null;

        if (fa != null && fb != null) {
            // Nếu khác nhau (float vs double) thì “nâng” lên double
            if (fa.getEBits() == fb.getEBits() && fa.getSBits() == fb.getSBits()) {
                return fa;
            }
            // Java: nếu trộn float/double => double
            return ctx.mkFPSort64();
        } else if (fa != null) {
            return fa;
        } else if (fb != null) {
            return fb;
        } else {
            throw new IllegalArgumentException("No FP sort in operands");
        }
    }

    // Convert Expr (BitVec hoặc FP) sang FPExpr với sort mong muốn
    private static FPExpr coerceToFP(Context ctx, Expr e, FPSort target, FPRMExpr rm) {
        if (e instanceof FPExpr) {
            FPSort s = (FPSort) e.getSort();
            if (s.getEBits() == target.getEBits() && s.getSBits() == target.getSBits()) {
                return (FPExpr) e;
            }
            // FP32 -> FP64 (hoặc ngược lại nếu bạn rất muốn, nhưng Java chỉ nâng lên)
            return ctx.mkFPToFP(rm, (FPExpr) e, target);
        }
        if (e instanceof BitVecExpr) {
            // BV (int/long) -> FP theo 2's complement signed, giống cast Java
            return ctx.mkFPToFP(rm, (BitVecExpr) e, target, true); // signed=true
        }
        throw new IllegalArgumentException("Cannot coerce non-numeric expr " + e);
    }

    private static BitVecExpr fixBvWidth(Context ctx, BitVecExpr e, int targetSz, boolean signExt) {
        int curSz = e.getSortSize();
        if (curSz == targetSz) return e;
        return curSz < targetSz
                ? (signExt ? ctx.mkSignExt(targetSz - curSz, e) : ctx.mkZeroExt(targetSz - curSz, e))
                : ctx.mkExtract(targetSz - 1, 0, e); // Cắt bớt nếu thừa
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(leftOperand != null ? leftOperand.toString() : "null");
        result.append(operator != null ? operator.toString() : "null");
        result.append(rightOperand != null ? rightOperand.toString() : "null");

        if (extendedOperands != null) {
            for (AstNode extendedOperand : extendedOperands) {
                result.append(operator != null ? operator.toString() : "null");
                result.append(extendedOperand != null ? extendedOperand.toString() : "null");
            }
        }

        return result.toString();
    }
}
