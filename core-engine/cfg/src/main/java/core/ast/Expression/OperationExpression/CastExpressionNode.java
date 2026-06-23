package core.ast.Expression.OperationExpression;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Type;

import java.util.List;

public class CastExpressionNode extends ExpressionNode {
    private static final int JAVA_CHAR_SIZE = 16;
    private static final int Z3_CHAR_SIZE = 18;

    private Type targetNode;
    // Lưu biểu thức bị ép kiểu
    private ExpressionNode innerExpression;

    public CastExpressionNode(Type targetNode, ExpressionNode innerExpression) {
        this.targetNode = targetNode;
        this.innerExpression = innerExpression;
    }

    public CastExpressionNode() {
    }

    public Type getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(Type targetNode) {
        this.targetNode = targetNode;
    }

    public ExpressionNode getInnerExpression() {
        return innerExpression;
    }

    public void setInnerExpression(ExpressionNode innerExpression) {
        this.innerExpression = innerExpression;
    }

    // Hàm thực thi: Bóc tách AST của Eclipse JDT
    public static AstNode executeCastExpression(CastExpression castExpression, MemoryModel memoryModel) {
        Type type = castExpression.getType();
        Expression expression = castExpression.getExpression();

        // Gọi đệ quy để xử lý cái ruột bên trong (ví dụ nó sẽ trả về NameNode của biến y)
        ExpressionNode innerNode = (ExpressionNode) ExpressionNode.executeExpression(expression, memoryModel);

        return new CastExpressionNode(type, innerNode);
    }

    public static Expr createZ3Expression(CastExpressionNode castNode, MemoryModel memoryModel,
                                          Context ctx, List<Z3VariableWrapper> vars) {

        String targetType = castNode.getTargetNode().toString();

        ExpressionNode innerExpr = castNode.getInnerExpression();

        Expr z3Inner = OperationExpressionNode.createZ3Expression(innerExpr, ctx, vars, memoryModel);

        if (z3Inner instanceof FPExpr) {
            FPExpr arg = (FPExpr) z3Inner;

            switch (targetType) {
                case "double":
                    return ctx.mkFPToFP(ctx.mkFPRoundNearestTiesToEven(), arg, ctx.mkFPSort64());

                case "float":
                    return ctx.mkFPToFP(ctx.mkFPRoundNearestTiesToEven(), arg, ctx.mkFPSort32());

                case "long":
                    return ctx.mkFPToBV(ctx.mkFPRoundTowardZero(), arg, 64, true);

                case "int":
                    return ctx.mkFPToBV(ctx.mkFPRoundTowardZero(), arg, 32, true);

                case "short": {
                    BitVecExpr bv32 = ctx.mkFPToBV(ctx.mkFPRoundTowardZero(), arg, 32, true);
                    return ctx.mkExtract(15, 0, bv32);
                }

                case "byte": {
                    BitVecExpr bv32 = ctx.mkFPToBV(ctx.mkFPRoundTowardZero(), arg, 32, true);
                    return ctx.mkExtract(7, 0, bv32);
                }

                case "char": {
                    BitVecExpr bv32 = ctx.mkFPToBV(ctx.mkFPRoundTowardZero(), arg, 32, true);
                    return bvToCharSeq(ctx, bv32);
                }

                default:
                    return z3Inner;
            }
        }

        if (z3Inner.isReal()) {
            Expr<RealSort> arg = (Expr<RealSort>) z3Inner;

            switch (targetType) {
                case "double":
                    return realToFP(ctx, arg, ctx.mkFPSort64());

                case "float":
                    return realToFP(ctx, arg, ctx.mkFPSort32());

                case "long":
                    return realToSignedBV(ctx, arg, 64);

                case "int":
                    return realToSignedBV(ctx, arg, 32);

                case "short":
                    return realToSignedBV(ctx, arg, 16);

                case "byte":
                    return realToSignedBV(ctx, arg, 8);

                case "char": {
                    BitVecExpr bv32 = realToSignedBV(ctx, arg, 32);
                    return bvToCharSeq(ctx, bv32);
                }

                default:
                    return z3Inner;
            }
        }

        if (z3Inner.isInt()) {
            Expr<IntSort> arg = (Expr<IntSort>) z3Inner;

            switch (targetType) {
                case "double":
                    return realToFP(ctx, ctx.mkInt2Real(arg), ctx.mkFPSort64());

                case "float":
                    return realToFP(ctx, ctx.mkInt2Real(arg), ctx.mkFPSort32());

                case "long":
                    return ctx.mkInt2BV(64, arg);

                case "int":
                    return ctx.mkInt2BV(32, arg);

                case "short":
                    return ctx.mkExtract(15, 0, ctx.mkInt2BV(32, arg));

                case "byte":
                    return ctx.mkExtract(7, 0, ctx.mkInt2BV(32, arg));

                case "char":
                    return bvToCharSeq(ctx, ctx.mkInt2BV(32, arg));

                default:
                    return z3Inner;
            }
        }

        if (z3Inner instanceof BitVecExpr) {
            BitVecExpr arg = (BitVecExpr) z3Inner;

            int currentSize = arg.getSortSize();

            if ("long".equals(targetType) && currentSize == 32) {
                System.out.println("Đã ép kiểu int thành long cho Z3");
                return ctx.mkSignExt(32, arg);
            } else if ("int".equals(targetType) && currentSize == 64) {
                System.out.println(" Đã ép kiểu long thành int cho Z3");
                return ctx.mkExtract(31, 0, arg);
            } else if ("short".equals(targetType) && currentSize == 32) {
                System.out.println(" Đã ép kiểu int thành short cho Z3");
                return ctx.mkExtract(15, 0, arg);
            } else if ("int".equals(targetType) && currentSize == 8) {
                return ctx.mkSignExt(24, arg);
            }
        }

        return z3Inner;
    }
}

        return ctx.mkZeroExt(targetSize - currentSize, arg);
    }

    private static SeqExpr<CharSort> bvToCharSeq(Context ctx, BitVecExpr arg) {
        BitVecExpr bv16 = resizeUnsignedBV(ctx, arg, JAVA_CHAR_SIZE);
        BitVecExpr codePoint = resizeUnsignedBV(ctx, bv16, Z3_CHAR_SIZE);
        Expr<CharSort> ch = ctx.charFromBv(codePoint);
        return ctx.mkUnit(ch);
    }

    private static BitVecExpr charSeqToUnsignedBV(Context ctx,
                                                  SeqExpr<CharSort> seq,
                                                  int targetSize) {
        Expr<CharSort> ch = ctx.mkNth(seq, ctx.mkInt(0));
        BitVecExpr codePoint = ctx.charToBv(ch);
        BitVecExpr bv16 = resizeUnsignedBV(ctx, codePoint, JAVA_CHAR_SIZE);
        return resizeUnsignedBV(ctx, bv16, targetSize);
    }

    private static FPExpr realToFP(Context ctx, Expr<RealSort> real, FPSort targetSort) {
        return ctx.mkFPToFP(ctx.mkFPRoundNearestTiesToEven(), (RealExpr) real, targetSort);
    }

    private static BitVecExpr realToSignedBV(Context ctx, Expr<RealSort> real, int bitSize) {
        Expr<IntSort> intExpr = realToIntTowardZero(ctx, real);
        return ctx.mkInt2BV(bitSize, intExpr);
    }

    private static Expr<IntSort> realToIntTowardZero(Context ctx, Expr<RealSort> real) {
        BoolExpr nonNegative = ctx.mkGe(real, ctx.mkReal(0));
        Expr<IntSort> positiveCase = ctx.mkReal2Int(real);
        Expr<RealSort> negReal = ctx.mkUnaryMinus(real);
        Expr<IntSort> negativeCase = ctx.mkUnaryMinus(ctx.mkReal2Int(negReal));
        return ctx.mkITE(nonNegative, positiveCase, negativeCase);
    }
}