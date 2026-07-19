package core.ast.Expression.Method;

import com.microsoft.z3.*;
import com.microsoft.z3.enumerations.Z3_sort_kind;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.BooleanLiteralNode;
import core.ast.Expression.Literal.CharacterLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.DoubleLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.Literal.StringLiteralNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class MathMethodNode extends MethodInvocationNode {
    public String ownerName;
    public String methodName;
    public List<AstNode> arguments;
    public AstNode target;

    public static AstNode executeMathMethod(MethodInvocation methodInvocation,
                                            MemoryModel memoryModel) {
        MathMethodNode mathMethodNode = new MathMethodNode();

        mathMethodNode.methodName = methodInvocation.getName().toString();

        List<AstNode> arguments = new ArrayList<>();
        for (int i = 0; i < methodInvocation.arguments().size(); i++) {
            AstNode argNode = ExpressionNode.executeExpression(
                    (Expression) methodInvocation.arguments().get(i),
                    memoryModel
            );
            arguments.add(argNode);
        }
        mathMethodNode.arguments = arguments;

        Expression expression = methodInvocation.getExpression();
        if (expression != null) {
            mathMethodNode.ownerName = expression.toString();

            // All Math methods are static; if owner is not "Math", treat as target
            // (should not happen in valid code, but follow the pattern)
            if (!"Math".equals(mathMethodNode.ownerName)) {
                mathMethodNode.target =
                        ExpressionNode.executeExpression(expression, memoryModel);
            }
        }

        ExpressionNode expressionNode = executeMathMethodNode(mathMethodNode, memoryModel);
        return expressionNode;
    }

    public static ExpressionNode executeMathMethodNode(MathMethodNode mathMethodNode,
                                                       MemoryModel memoryModel) {
        String methodName = mathMethodNode.methodName;
        List<AstNode> arguments = mathMethodNode.arguments;

        try {
            // Only support static Math methods (ownerName == "Math")
            if (!"Math".equals(mathMethodNode.ownerName)) {
                throw new RuntimeException("Only static Math methods are supported");
            }

            // Try constant folding if all arguments are numeric literals
            boolean allNumeric = true;
            List<Number> numericArgs = new ArrayList<>();
            for (AstNode arg : arguments) {
                Number val = getNumericValue(arg);
                if (val == null) {
                    allNumeric = false;
                    break;
                }
                numericArgs.add(val);
            }

            if (allNumeric) {
                Number result = computeMathLiteral(methodName, numericArgs);
                return wrapNumericLiteral(result);
            }

            // Cannot fold, return the node itself for symbolic execution
            return mathMethodNode;

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Tries to extract a numeric value from an AST node.
     * Supports NumberLiteralNode (int, long, float, double) and CharacterLiteralNode.
     */
    private static Number getNumericValue(AstNode node) {
        if (node == null) return null;

        if (node instanceof IntegerLiteralNode) {
            return ((IntegerLiteralNode) node).getIntegerValue();
        }
        if (node instanceof DoubleLiteralNode) {
            return ((DoubleLiteralNode) node).getDoubleValue();
        }
        if (node instanceof CharacterLiteralNode) {
            return (int) ((CharacterLiteralNode) node).getCharacterValue();
        }
        // fallback: try to parse token if it's a NumberLiteralNode (generic)
        if (node instanceof NumberLiteralNode) {
            // could implement parsing, but above specialized nodes cover it
            return null;
        }
        return null;
    }

    /**
     * Computes the result of a Math static method call with literal numeric arguments.
     */
    private static Number computeMathLiteral(String methodName, List<Number> args) {
        switch (methodName) {
            case "abs": {
                Number a = args.get(0);
                if (a instanceof Double)       return Math.abs((Double) a);
                else if (a instanceof Float)   return Math.abs((Float) a);
                else if (a instanceof Long)    return Math.abs((Long) a);
                else                           return Math.abs(a.intValue());
            }
            case "max": {
                Number a = args.get(0);
                Number b = args.get(1);
                // Use Java's promotion rules
                if (a instanceof Double || b instanceof Double)
                    return Math.max(a.doubleValue(), b.doubleValue());
                if (a instanceof Float || b instanceof Float)
                    return Math.max(a.floatValue(), b.floatValue());
                if (a instanceof Long || b instanceof Long)
                    return Math.max(a.longValue(), b.longValue());
                return Math.max(a.intValue(), b.intValue());
            }
            case "min": {
                Number a = args.get(0);
                Number b = args.get(1);
                if (a instanceof Double || b instanceof Double)
                    return Math.min(a.doubleValue(), b.doubleValue());
                if (a instanceof Float || b instanceof Float)
                    return Math.min(a.floatValue(), b.floatValue());
                if (a instanceof Long || b instanceof Long)
                    return Math.min(a.longValue(), b.longValue());
                return Math.min(a.intValue(), b.intValue());
            }
            case "signum": {
                Number a = args.get(0);
                // signum returns a double for double, float for float
                if (a instanceof Double) return Math.signum((Double) a);
                else                     return (double) Math.signum(a.floatValue());
            }
            case "copySign": {
                Number magnitude = args.get(0);
                Number sign = args.get(1);
                if (magnitude instanceof Double || sign instanceof Double)
                    return Math.copySign(magnitude.doubleValue(), sign.doubleValue());
                else
                    return Math.copySign(magnitude.floatValue(), sign.floatValue());
            }
            default:
                throw new RuntimeException("Unsupported Math method for constant folding: " + methodName);
        }
    }

    /**
     * Wraps a Number into the appropriate LiteralNode.
     */
    private static ExpressionNode wrapNumericLiteral(Number value) {
        if (value instanceof Integer)
            return new IntegerLiteralNode((Integer) value);
        if (value instanceof Double)
            return new DoubleLiteralNode((Double) value);
        throw new RuntimeException("Unsupported numeric type: " + value.getClass());
    }

    public static Expr createZ3Expression(MathMethodNode node,
                                          MemoryModel memoryModel,
                                          Context ctx,
                                          List<Z3VariableWrapper> vars) {
        switch (node.methodName) {
            case "abs":
                return handleMathAbs(node, memoryModel, ctx, vars);
            case "max":
                return handleMathMax(node, memoryModel, ctx, vars);
            case "min":
                return handleMathMin(node, memoryModel, ctx, vars);
            case "signum":
                return handleMathSignum(node, memoryModel, ctx, vars);
            case "copySign":
                return handleMathCopySign(node, memoryModel, ctx, vars);
            case "sqrt":
                return handleMathSqrt(node, memoryModel, ctx, vars);
            default:
                throw new RuntimeException("Unsupported Math method for Z3: " + node.methodName);
        }
    }

    /* ================================================================
     *  Z3 Handler Helpers
     * ================================================================ */

    /**
     * Converts an AST node to an ArithExpr (IntExpr or RealExpr).
     */
    private static ArithExpr getArithExpr(AstNode node,
                                          MemoryModel memoryModel,
                                          Context ctx,
                                          List<Z3VariableWrapper> vars) {
        if (node == null) {
            throw new RuntimeException("Math argument is null");
        }
        Expr expr = OperationExpressionNode.createZ3Expression(
                (ExpressionNode) node, ctx, vars, memoryModel);

        if (expr.getSort() instanceof ArithSort) {
            return (ArithExpr) expr;
        }
        // Char to int promotion
        if (expr.getSort().equals(ctx.mkCharSort())) {
            return ctx.charToInt((Expr<CharSort>) expr);
        }
        throw new RuntimeException("Expected arithmetic expression but got: " + expr.getSort());
    }

    private static ArithExpr mkZero(ArithExpr e, Context ctx) {
        return e instanceof IntExpr ? ctx.mkInt(0) : ctx.mkReal(0);
    }

    /**
     * Promotes two arithmetic expressions to a common sort (if one is real, both become real).
     */
    private static ArithExpr[] promoteToCommonSort(Context ctx, ArithExpr a, ArithExpr b) {
        if (a instanceof RealExpr || b instanceof RealExpr) {
            if (!(a instanceof RealExpr)) a = ctx.mkInt2Real((IntExpr) a);
            if (!(b instanceof RealExpr)) b = ctx.mkInt2Real((IntExpr) b);
        }
        return new ArithExpr[]{a, b};
    }

    /* ================================================================
     *  Method Handlers
     * ================================================================ */

    private static Expr handleMathAbs(MathMethodNode node,
                                      MemoryModel memoryModel,
                                      Context ctx,
                                      List<Z3VariableWrapper> vars) {
        ArithExpr arg = getArithExpr(node.arguments.get(0), memoryModel, ctx, vars);
        ArithExpr zero = mkZero(arg, ctx);
        return ctx.mkITE(ctx.mkGe(arg, zero), arg, ctx.mkUnaryMinus(arg));
    }

    private static Expr handleMathMax(MathMethodNode node,
                                      MemoryModel memoryModel,
                                      Context ctx,
                                      List<Z3VariableWrapper> vars) {
        ArithExpr a = getArithExpr(node.arguments.get(0), memoryModel, ctx, vars);
        ArithExpr b = getArithExpr(node.arguments.get(1), memoryModel, ctx, vars);
        ArithExpr[] p = promoteToCommonSort(ctx, a, b);
        return ctx.mkITE(ctx.mkGt(p[0], p[1]), p[0], p[1]);
    }

    private static Expr handleMathMin(MathMethodNode node,
                                      MemoryModel memoryModel,
                                      Context ctx,
                                      List<Z3VariableWrapper> vars) {
        ArithExpr a = getArithExpr(node.arguments.get(0), memoryModel, ctx, vars);
        ArithExpr b = getArithExpr(node.arguments.get(1), memoryModel, ctx, vars);
        ArithExpr[] p = promoteToCommonSort(ctx, a, b);
        return ctx.mkITE(ctx.mkLt(p[0], p[1]), p[0], p[1]);
    }

    private static Expr handleMathSignum(MathMethodNode node,
                                         MemoryModel memoryModel,
                                         Context ctx,
                                         List<Z3VariableWrapper> vars) {
        ArithExpr arg = getArithExpr(node.arguments.get(0), memoryModel, ctx, vars);
        // signum expects float/double, so promote to real if necessary
        ArithExpr realArg = arg instanceof RealExpr ? arg : ctx.mkInt2Real((IntExpr) arg);
        ArithExpr zero = ctx.mkReal(0);
        ArithExpr one = ctx.mkReal(1);
        ArithExpr minusOne = ctx.mkReal(-1);
        return ctx.mkITE(ctx.mkGt(realArg, zero), one,
                ctx.mkITE(ctx.mkLt(realArg, zero), minusOne, zero));
    }

    private static Expr handleMathCopySign(MathMethodNode node,
                                           MemoryModel memoryModel,
                                           Context ctx,
                                           List<Z3VariableWrapper> vars) {
        ArithExpr magnitude = getArithExpr(node.arguments.get(0), memoryModel, ctx, vars);
        ArithExpr sign = getArithExpr(node.arguments.get(1), memoryModel, ctx, vars);
        ArithExpr[] p = promoteToCommonSort(ctx, magnitude, sign);
        ArithExpr absMag = (ArithExpr) ctx.mkITE(
                ctx.mkGe(p[0], mkZero(p[0], ctx)),
                p[0],
                ctx.mkUnaryMinus(p[0]));
        return ctx.mkITE(ctx.mkLt(p[1], mkZero(p[1], ctx)),
                ctx.mkUnaryMinus(absMag), absMag);
    }

    private static Expr handleMathSqrt(MathMethodNode node,
                                       MemoryModel memoryModel,
                                       Context ctx,
                                       List<Z3VariableWrapper> vars) {

        ArithExpr arg = getArithExpr(node.arguments.get(0), memoryModel, ctx, vars);

        FuncDecl sqrt = ctx.mkFuncDecl(
                "sqrt",
                new Sort[]{arg.getSort()},
                arg.getSort()
        );

        return ctx.mkApp(sqrt, arg);
    }

    public String getClassName() {
        return ownerName;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<AstNode> getArgument() {
        return arguments;
    }

    public AstNode getTarget() {
        return target;
    }
}