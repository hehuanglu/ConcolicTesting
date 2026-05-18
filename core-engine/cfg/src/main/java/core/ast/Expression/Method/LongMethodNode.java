package core.ast.Expression.Method;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
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
            default:
                throw new Z3Exception("Method " + methodName + " not implemented yet");
        }
        return result;
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
