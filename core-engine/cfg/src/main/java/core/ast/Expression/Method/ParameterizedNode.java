package core.ast.Expression.Method;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.symbolicExecution.MemoryModel;
import core.symbolicExecution.SymbolicExecutionRewrite;
import core.variable.ParameterizedTypeVariable;
import core.variable.Variable;
import org.eclipse.jdt.core.dom.Expression;

import java.util.ArrayList;
import java.util.List;

import static core.symbolicExecution.SymbolicExecutionRewrite.CollectionKeys;

public class ParameterizedNode extends MethodInvocationNode {
    public  ParameterizedNode(String className, String methodName, List<AstNode> arguments,String targetName){
        super(className, methodName, arguments, targetName);
    }
    public static Expr createZ3Expression(MethodInvocationNode operand, MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars){
        MethodInvocationNode methodInvocationNode = (MethodInvocationNode) operand;
        String methodName = methodInvocationNode.getMethodName();
        String className = methodInvocationNode.getClassName();
        List<AstNode> args = methodInvocationNode.getArgument();
        String targetName = className;
        if (methodInvocationNode.getTargetName() != null){
            targetName = methodInvocationNode.getTargetName();
        }

        switch (methodName) {
            case "get": return handleGet(memoryModel, ctx, vars, args, targetName);
            case "size": return handleSize(memoryModel, ctx, vars, args, targetName);
        }
        return null;
    }
    public static Expr handleAdd(MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars,List<AstNode> args,String targetName){
        Variable var = memoryModel.getVariable(targetName);
        Expr z3ListBase = SymbolicExecutionRewrite.z3ArrayStateMap.get().get(targetName);
        if (z3ListBase == null) {
            throw new RuntimeException("Không tìm thấy trạng thái Z3 cho List: " + targetName);
        }
        ExpressionNode valueNode = (ExpressionNode) args.get(0);
        Expr z3ValueToAdd = OperationExpressionNode.createZ3Expression(valueNode, ctx, vars, memoryModel);
        ParameterizedTypeVariable listVar = (ParameterizedTypeVariable) var;
        Expr indexToStore = listVar.getLatestSize();

        listVar.incrementVersion();
        int newVersion = listVar.getVersion();

        String newSizeVarName = targetName + ".size_" + newVersion;
        BitVecExpr newSizeConstant = (BitVecExpr) ctx.mkBVConst(newSizeVarName, 32);
        Expr formula = ctx.mkBVAdd((BitVecExpr) listVar.getBaseSize(), ctx.mkBV(newVersion, 32));
        BoolExpr sizeConstraint = ctx.mkEq(newSizeConstant, (BitVecExpr) formula);
        SymbolicExecutionRewrite.extraConstraints.add(sizeConstraint);

        ArrayExpr newArrayState = ctx.mkStore((ArrayExpr) z3ListBase, indexToStore, z3ValueToAdd);
        SymbolicExecutionRewrite.z3ArrayStateMap.get().put(targetName, newArrayState);

        listVar.addNewSizeVersion(newSizeConstant);
        Z3VariableWrapper wrapper = new Z3VariableWrapper(newSizeConstant);
        if (!SymbolicExecutionRewrite.haveDuplicateVariable(wrapper, vars)) {
            vars.add(wrapper);
        }
        // Chi add size goc vao Z3Var thoi
//                Z3VariableWrapper wrapper = new Z3VariableWrapper(newSizeConstant);
//                if (!SymbolicExecutionRewrite.haveDuplicateVariable(wrapper, vars)) {
//                    vars.add(wrapper);
//                }

        return ctx.mkTrue();
    }
    public static Expr handleGet(MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars,List<AstNode> args,String targetName){
        Expr z3ListBase = SymbolicExecutionRewrite.z3ArrayStateMap.get().get(targetName);

        if (z3ListBase == null) {
            throw new RuntimeException("Không tìm thấy trạng thái Z3 cho List: " + targetName);
        }

        // index là phần tử đầu tiên của args
        ExpressionNode indexNode = (ExpressionNode) args.get(0);

        Expr z3IndexExpr = OperationExpressionNode.createZ3Expression(indexNode, ctx, vars, memoryModel);
        // thêm khóa vào đây để dễ truy xuất nghiệm khi giải xong
        if (!CollectionKeys.contains(z3IndexExpr)) {
            CollectionKeys.add(z3IndexExpr);
        }

        Expr result = ctx.mkSelect((ArrayExpr) z3ListBase, z3IndexExpr);
        Z3VariableWrapper wrapper = new Z3VariableWrapper(result);

        if (!SymbolicExecutionRewrite.haveDuplicateVariable(wrapper, vars)) {
            vars.add(wrapper);
        }
        return result;
    }

    public static Expr handleSize(MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars,List<AstNode> args,String targetName){
        Variable var = memoryModel.getVariable(targetName);
        ParameterizedTypeVariable listVar = (ParameterizedTypeVariable) var;
        Expr sizeVar = listVar.getSize();

        // CẬP NHẬT THEO DÕI SIZE: Lưu biến size symbolic của Collection này để phục vụ việc giải nghiệm sau này
        SymbolicExecutionRewrite.symbolicMapSizeMap.get().put(targetName, sizeVar);

        Z3VariableWrapper wrapper = new Z3VariableWrapper(sizeVar);

        if (!SymbolicExecutionRewrite.haveDuplicateVariable(wrapper, vars)) {
            vars.add(wrapper);
        }
        return sizeVar;
    }
}
