package core.ast.Expression.Method;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.Literal.StringLiteralNode;
import core.ast.Expression.Name.NameNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.symbolicExecution.MemoryModel;

import java.util.List;

public class LongMethodNode extends MethodInvocationNode{
    private final AstNode target;
    private final String methodName;
    private final List<AstNode> arguments;
    public LongMethodNode(AstNode target, String methodName, List<AstNode> arguments) {
        this.target = target;
        this.methodName = methodName;
        this.arguments = arguments;
    }
    public static Expr createZ3Expression(LongMethodNode node, MemoryModel memoryModel,
                                          Context ctx, List<Z3VariableWrapper> vars) {
        String methodName = node.getMethodName();
        AstNode target = node.getTarget();
        List<AstNode> arguments = node.getArguments();
        Expr result;
        switch (methodName) {
            case "equals":
                result = handleEquals(target,arguments,memoryModel,ctx,vars);
                break;
            case "toHexString":
                result = handleToHexString(arguments, memoryModel, ctx, vars);
                break;
            case "toBinaryString":
                result = handleToBinaryString(arguments, memoryModel, ctx, vars);
                break;
            case "parseLong":
                result = handleParseLong(arguments, memoryModel, ctx, vars);
                break;
            case "valueOf":
                result = handleValueOf(arguments, memoryModel, ctx, vars);
                break;
            default:
                throw new Z3Exception("Method " + methodName + " not implemented yet");
        }
        return result;
    }

    private static Expr handleToHexString(List<AstNode> arguments, MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) arguments.get(0);
        Expr argExpr = OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);

        if (argExpr instanceof BitVecNum) {
            long val = ((BitVecNum) argExpr).getLong();
            return ctx.mkString(Long.toHexString(val));
        }

        FuncDecl toHexStringFunc = ctx.mkFuncDecl("toHexString", ctx.mkBitVecSort(64), ctx.getStringSort());
        return ctx.mkApp(toHexStringFunc, argExpr);
    }

    private static Expr handleToBinaryString(List<AstNode> arguments, MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) arguments.get(0);
        Expr argExpr = OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);

        if (argExpr instanceof BitVecNum) {
            long val = ((BitVecNum) argExpr).getLong();
            return ctx.mkString(Long.toBinaryString(val));
        }

        FuncDecl toBinaryStringFunc = ctx.mkFuncDecl("toBinaryString", ctx.mkBitVecSort(64), ctx.getStringSort());
        return ctx.mkApp(toBinaryStringFunc, argExpr);
    }

    private static Expr handleParseLong(List<AstNode> arguments, MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        AstNode arg = arguments.get(0);
        String stringValue = null;

        if (arg instanceof StringLiteralNode) {
            stringValue = ((StringLiteralNode) arg).getStringValue();
        } else if (arg instanceof NameNode) {
            try {
                AstNode valueNode = memoryModel.getValue((NameNode) arg);
                if (valueNode instanceof StringLiteralNode) {
                    stringValue = ((StringLiteralNode) valueNode).getStringValue();
                }
            } catch (Exception ignored) {}
        }

        if (stringValue != null) {
            int radix = 10;
            if (arguments.size() > 1) {
                AstNode radixNode = arguments.get(1);
                if (radixNode instanceof NumberLiteralNode) {
                    radix = Integer.parseInt(((NumberLiteralNode) radixNode).getTokenValue());
                }
            }
            return ctx.mkBV(Long.parseLong(stringValue, radix), 64);
        }

        throw new Z3Exception("parseLong only supported for concrete variables");
    }

    private static Expr handleValueOf(List<AstNode> arguments, MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) arguments.get(0);
        Expr argExpr = OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);
        if (argExpr.getSort().equals(ctx.getStringSort())) {
            return handleParseLong(arguments, memoryModel, ctx, vars);
        } else {
            return argExpr;
        }
    }

    private static Expr handleEquals(AstNode target,List<AstNode> arguments, MemoryModel memoryModel,
                                     Context ctx, List<Z3VariableWrapper> vars) throws Z3Exception {
        Expr targetExpr = OperationExpressionNode.createZ3Expression((ExpressionNode)target,ctx,vars,memoryModel);
        Expr argumentExpr = OperationExpressionNode.createZ3Expression((ExpressionNode)arguments.get(0),ctx,vars,memoryModel);
        return ctx.mkEq(targetExpr,argumentExpr);
    }

    public AstNode getTarget() {
        return target;
    }
    public String getMethodName() {
        return methodName;
    };
    public List<AstNode> getArguments() {
        return arguments;
    };

}
