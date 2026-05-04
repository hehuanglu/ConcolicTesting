package core.ast.Expression.Method;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.ast.Type.AnnotatableType.SimpleTypeNode;
import core.symbolicExecution.MemoryModel;
import java.util.List;

public class StringMethodNode extends MethodInvocationNode {
    private final AstNode target;
    private final String methodName;
    private final List<AstNode> arguments;

    public StringMethodNode(AstNode target, String methodName, List<AstNode> arguments) {
        this.target = target;
        this.methodName = methodName;
        this.arguments = arguments;
    }

    public static Expr createZ3Expression(StringMethodNode node, MemoryModel memoryModel,
                                          Context ctx, List<Z3VariableWrapper> vars) {
        Expr targetExpr = getTargetExpr(node.target, memoryModel, ctx, vars);
        SeqExpr targetStr = (SeqExpr) targetExpr;

        switch (node.methodName) {
            case "concat":
                return handleConcat(node, targetStr, memoryModel, ctx, vars);
            case "replace":
                return handleReplace(node, targetStr, memoryModel, ctx, vars);
            case "replaceAll":
                return handleReplaceAll(node, targetStr, memoryModel, ctx, vars);
            case "substring":
                return handleSubstring(node, targetStr, memoryModel, ctx, vars);
            case "length":
                return ctx.mkLength(targetStr);
            case "charAt":
                return handleCharAt(node, targetStr, memoryModel, ctx, vars);
            case "isBlank":
            case "isEmpty":
                return handleIsBlank(targetStr, ctx);
            default:
                throw new RuntimeException("Unsupported String method: " + node.methodName);
        }
    }

    private static Expr getTargetExpr(AstNode target, MemoryModel memoryModel,
                                      Context ctx, List<Z3VariableWrapper> vars) {
        if (target instanceof SimpleTypeNode)
            return SimpleTypeNode.createZ3Expression((SimpleTypeNode) target, memoryModel, ctx, vars);
        if (target instanceof StringMethodNode)
            return createZ3Expression((StringMethodNode) target, memoryModel, ctx, vars);
        throw new RuntimeException("Invalid target type");
    }

    private static Expr handleConcat(StringMethodNode node, SeqExpr targetStr,
                                     MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) node.arguments.get(0);
        Expr argExpr = OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);
        if (!argExpr.getSort().equals(ctx.getStringSort()))
            throw new RuntimeException("concat argument not string");
        return ctx.mkConcat(targetStr, (SeqExpr) argExpr);
    }

    private static Expr handleReplace(StringMethodNode node, SeqExpr targetStr,
                                      MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {

        try {
            ExpressionNode oldNode = (ExpressionNode) node.arguments.get(0);
            ExpressionNode newNode = (ExpressionNode) node.arguments.get(1);
            Expr oldExpr = OperationExpressionNode.createZ3Expression(oldNode, ctx, vars, memoryModel);
            Expr newExpr = OperationExpressionNode.createZ3Expression(newNode, ctx, vars, memoryModel);
            if (!oldExpr.getSort().equals(ctx.getStringSort()) || !newExpr.getSort().equals(ctx.getStringSort()))
                throw new RuntimeException("replace arguments must be strings");
            return ctx.mkReplace(targetStr, (SeqExpr) oldExpr, (SeqExpr) newExpr);
        } catch (Exception e) {
            throw new RuntimeException("mkReplace not available in this Z3 version. Please upgrade or use stub.", e);
        }
    }

    private static Expr handleSubstring(StringMethodNode node, SeqExpr targetStr,
                                        MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {

        ExpressionNode beginNode = (ExpressionNode) node.arguments.get(0);
        Expr beginExpr = OperationExpressionNode.createZ3Expression(beginNode, ctx, vars, memoryModel);

        if (!(beginExpr instanceof IntExpr)) {
            throw new RuntimeException("substring begin index must be int");
        }
        IntExpr beginInt = (IntExpr) beginExpr;

        if (node.arguments.size() == 1) {
            IntExpr len = ctx.mkLength(targetStr);

            // mkSub -> ArithExpr → cast về IntExpr
            ArithExpr tmpLength = ctx.mkSub(len, beginInt);
            IntExpr length = (IntExpr) tmpLength;

            return ctx.mkExtract(targetStr, beginInt, length);
        } else {
            ExpressionNode endNode = (ExpressionNode) node.arguments.get(1);
            Expr endExpr = OperationExpressionNode.createZ3Expression(endNode, ctx, vars, memoryModel);

            if (!(endExpr instanceof IntExpr)) {
                throw new RuntimeException("substring end index must be int");
            }
            IntExpr endInt = (IntExpr) endExpr;

            ArithExpr tmpLength = ctx.mkSub(endInt, beginInt);
            IntExpr length = (IntExpr) tmpLength;

            return ctx.mkExtract(targetStr, beginInt, length);
        }
    }

    private static Expr handleCharAt(StringMethodNode node, SeqExpr targetStr,
                                     MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {

        ExpressionNode indexNode = (ExpressionNode) node.arguments.get(0);
        Expr indexExpr = OperationExpressionNode.createZ3Expression(indexNode, ctx, vars, memoryModel);

        if (!(indexExpr instanceof IntExpr)) {
            throw new RuntimeException("charAt index must be int");
        }
        IntExpr indexInt = (IntExpr) indexExpr;

        IntExpr one = ctx.mkInt(1);
        return ctx.mkExtract(targetStr, indexInt, one);
    }

    private static BoolExpr handleIsBlank(SeqExpr s, Context ctx) {
        IntExpr len = ctx.mkLength(s);
        IntExpr i = ctx.mkIntConst("i_" + s.hashCode());

        SeqExpr space = ctx.mkString(" ");
        BoolExpr range = ctx.mkAnd(
                ctx.mkGe(i, ctx.mkInt(0)),
                ctx.mkLt(i, len)
        );

        BoolExpr isSpace = ctx.mkEq(
                ctx.mkExtract(s, i, ctx.mkInt(1)),
                space
        );

        BoolExpr forallSpaces = ctx.mkForall(
                new Expr[]{i},
                ctx.mkImplies(range, isSpace),
                1, null, null, null, null
        );

        return forallSpaces;
    }

    private static Expr handleReplaceAll(StringMethodNode node, SeqExpr targetStr,
                                         MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {

        ExpressionNode oldNode = (ExpressionNode) node.arguments.get(0);
        ExpressionNode newNode = (ExpressionNode) node.arguments.get(1);

        Expr oldExpr = OperationExpressionNode.createZ3Expression(oldNode, ctx, vars, memoryModel);
        Expr newExpr = OperationExpressionNode.createZ3Expression(newNode, ctx, vars, memoryModel);

        if (!oldExpr.getSort().equals(ctx.getStringSort()) ||
                !newExpr.getSort().equals(ctx.getStringSort())) {
            throw new RuntimeException("replaceAll arguments must be strings");
        }

        // Tạo (hoặc reuse) uninterpreted function: replaceAll(s, old, new)
        FuncDecl replaceAllFunc = ctx.mkFuncDecl(
                "replaceAll",
                new Sort[]{ctx.getStringSort(), ctx.getStringSort(), ctx.getStringSort()},
                ctx.getStringSort()
        );

        return ctx.mkApp(replaceAllFunc, targetStr, oldExpr, newExpr);
    }
}