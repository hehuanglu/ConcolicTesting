package core.ast.Expression.OperationExpression;

import com.microsoft.z3.*;
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

    public static AstNode executeCastExpression(CastExpression castExpression, MemoryModel memoryModel) {
        Type type = castExpression.getType();
        Expression expression = castExpression.getExpression();
        ExpressionNode innerNode = (ExpressionNode) ExpressionNode.executeExpression(expression, memoryModel);
        return new CastExpressionNode(type, innerNode);
    }

    @SuppressWarnings("unchecked")
    public static Expr createZ3Expression(CastExpressionNode castNode,
                                          MemoryModel memoryModel,
                                          Context ctx,
                                          List<Z3VariableWrapper> vars) {
        String targetType = normalizeType(castNode.getTargetNode().toString());

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

            switch (targetType) {
                case "long":
                    return resizeSignedBV(ctx, arg, 64);

                case "int":
                    return resizeSignedBV(ctx, arg, 32);

                case "short":
                    return resizeSignedBV(ctx, arg, 16);

                case "byte":
                    return resizeSignedBV(ctx, arg, 8);

                case "char":
                    return bvToCharSeq(ctx, arg);

                case "float":
                    return ctx.mkFPToFP(ctx.mkFPRoundNearestTiesToEven(), arg, ctx.mkFPSort32(), true);

                case "double":
                    return ctx.mkFPToFP(ctx.mkFPRoundNearestTiesToEven(), arg, ctx.mkFPSort64(), true);

                default:
                    return z3Inner;
            }
        }

        if (z3Inner instanceof SeqExpr) {
            SeqExpr<CharSort> arg = (SeqExpr<CharSort>) z3Inner;

            switch (targetType) {
                case "char":
                    return arg;

                case "int":
                    return charSeqToUnsignedBV(ctx, arg, 32);

                case "long":
                    return charSeqToUnsignedBV(ctx, arg, 64);

                case "short":
                    return charSeqToUnsignedBV(ctx, arg, 16);

                case "byte":
                    return charSeqToUnsignedBV(ctx, arg, 8);

                case "float": {
                    BitVecExpr bv16 = charSeqToUnsignedBV(ctx, arg, 16);
                    return ctx.mkFPToFP(ctx.mkFPRoundNearestTiesToEven(), bv16, ctx.mkFPSort32(), false);
                }

                case "double": {
                    BitVecExpr bv16 = charSeqToUnsignedBV(ctx, arg, 16);
                    return ctx.mkFPToFP(ctx.mkFPRoundNearestTiesToEven(), bv16, ctx.mkFPSort64(), false);
                }

                default:
                    return z3Inner;
            }
        }

        return z3Inner;
    }

    private static String normalizeType(String type) {
        if (type == null) return "";

        type = type.trim();

        switch (type) {
            case "java.lang.String":
                return "String";
            case "java.lang.Integer":
                return "int";
            case "java.lang.Long":
                return "long";
            case "java.lang.Short":
                return "short";
            case "java.lang.Byte":
                return "byte";
            case "java.lang.Character":
                return "char";
            case "java.lang.Float":
                return "float";
            case "java.lang.Double":
                return "double";
            default:
                return type;
        }
    }

    private static BitVecExpr resizeSignedBV(Context ctx, BitVecExpr arg, int targetSize) {
        int currentSize = arg.getSortSize();

        if (currentSize == targetSize) {
            return arg;
        }

        if (currentSize > targetSize) {
            return ctx.mkExtract(targetSize - 1, 0, arg);
        }

        return ctx.mkSignExt(targetSize - currentSize, arg);
    }

    private static BitVecExpr resizeUnsignedBV(Context ctx, BitVecExpr arg, int targetSize) {
        int currentSize = arg.getSortSize();

        if (currentSize == targetSize) {
            return arg;
        }

        if (currentSize > targetSize) {
            return ctx.mkExtract(targetSize - 1, 0, arg);
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