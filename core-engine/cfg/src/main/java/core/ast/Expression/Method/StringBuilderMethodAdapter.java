package core.ast.Expression.Method;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public final class StringBuilderMethodAdapter {

    private StringBuilderMethodAdapter() {}

    public static StringMethodNode adapt(StringBuilderMethodNode builderNode) {
        StringMethodNode stringNode = new StringMethodNode();

        stringNode.setTarget(builderNode.getTarget());
        stringNode.setMethodName(mapMethod(builderNode.getMethodName()));
        stringNode.setArguments(builderNode.getArguments());

        return stringNode;
    }

    private static String mapMethod(String method) {
        switch (method) {
            case "length":
            case "substring":
            case "charAt":
            case "indexOf":
            case "lastIndexOf":
            case "compareTo":
                return method;

            case "append":
                return "concat";

            default:
                throw new RuntimeException(
                        "Unsupported StringBuilder method: " + method);
        }
    }

    public static ExpressionNode executeStringBuilderMethodNode(
            MethodInvocation methodInvocation,
            MemoryModel memoryModel) {

        StringBuilderMethodNode builderNode = new StringBuilderMethodNode();

        builderNode.setMethodName(methodInvocation.getName().toString());

        List<AstNode> arguments = new ArrayList<>();
        for (Object arg : methodInvocation.arguments()) {
            arguments.add(ExpressionNode.executeExpression((Expression) arg, memoryModel));
        }
        builderNode.setArguments(arguments);
        builderNode.setTarget(ExpressionNode.executeExpression(methodInvocation.getExpression(), memoryModel));

        return StringMethodNode.executeStringMethodNode(adapt(builderNode), memoryModel);
    }

    public static Expr createZ3Expression(
            StringBuilderMethodNode builderNode,
            MemoryModel memoryModel,
            Context ctx,
            List<Z3VariableWrapper> vars) {

        return StringMethodNode.createZ3Expression(
                adapt(builderNode),
                memoryModel,
                ctx,
                vars);
    }
}