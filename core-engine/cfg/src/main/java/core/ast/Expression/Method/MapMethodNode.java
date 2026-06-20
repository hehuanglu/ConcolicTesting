package core.ast.Expression.Method;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.symbolicExecution.MemoryModel;
import core.symbolicExecution.SymbolicExecutionRewrite;
import core.variable.Variable;

import java.util.List;

import static core.symbolicExecution.SymbolicExecutionRewrite.CollectionKeys;

public class MapMethodNode extends ParameterizedNode{
    public MapMethodNode(String className, String methodName, List<AstNode> arguments,String targetName){
        super(className, methodName, arguments, targetName);
    }
    public static Expr createZ3Expression(MethodInvocationNode operand, MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars){
        MethodInvocationNode methodInvocationNode = (MethodInvocationNode) operand;
        String methodName = methodInvocationNode.getMethodName();
        String className = methodInvocationNode.getClassName();
        List<AstNode> args = methodInvocationNode.getArgument();
        String targetName = className;
        Variable var = memoryModel.getVariable(targetName);

        switch (methodName) {
            case "get": return handleGet(memoryModel,ctx,vars,args,targetName);
            case "put": return handlePut(memoryModel,ctx,vars,args,targetName);
            case "size": return handleSize(memoryModel,ctx,vars,args,targetName);
        }
        return null;
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
        if(!CollectionKeys.contains(z3IndexExpr)){
            CollectionKeys.add(z3IndexExpr);
        }
        Expr newZ3MapState = ctx.mkSelect((ArrayExpr) z3ListBase, z3IndexExpr);
        return newZ3MapState;
    }

    public static Expr handlePut(MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars, List<AstNode> args, String targetName) {
        Expr z3MapBase = SymbolicExecutionRewrite.z3ArrayStateMap.get().get(targetName);

        if (z3MapBase == null) {
            // Tuỳ chỉnh lại thông báo lỗi cho phù hợp với Map hoặc Collection
            throw new RuntimeException("Không tìm thấy trạng thái Z3 cho Map: " + targetName);
        }// variable --> Z3expr  //

        // Map.put(key, value) nhận 2 đối số từ AST
        ExpressionNode keyNode = (ExpressionNode) args.get(0);
        ExpressionNode valueNode = (ExpressionNode) args.get(1);

        // Chuyển đổi key và value sang biểu thức Z3
        Expr z3KeyExpr = OperationExpressionNode.createZ3Expression(keyNode, ctx, vars, memoryModel);
        Expr z3ValueExpr = OperationExpressionNode.createZ3Expression(valueNode, ctx, vars, memoryModel);

        // Lưu trữ key để dễ dàng truy xuất nghiệm từ SMT solver khi giải xong
        if(!CollectionKeys.contains(z3KeyExpr)){
            CollectionKeys.add(z3KeyExpr);
        }
        // Lấy giá trị cũ (old value) bằng phép select trước khi thực hiện ghi đè
        // Điều này đảm bảo trả về đúng logic của Map.put trong Java
        Expr oldValue = ctx.mkSelect((ArrayExpr) z3MapBase, z3KeyExpr);  // lấy giá cũ của Map trước đó

        // Thực hiện phép store để tạo trạng thái Z3 mới: A' = store(A, k, v)
        Expr newZ3MapState = ctx.mkStore((ArrayExpr) z3MapBase, z3KeyExpr, z3ValueExpr);

        // thêm mảng vào danh sách biến
        Z3VariableWrapper wrapper = new Z3VariableWrapper(oldValue);

        if (!SymbolicExecutionRewrite.haveDuplicateVariable(wrapper, vars)) {
            vars.add(wrapper);
        }
        // QUAN TRỌNG: Cập nhật lại state map với trạng thái mảng mới
        // Nếu không cập nhật, các lệnh handleGet tiếp theo sẽ không thấy được dữ liệu vừa put
        SymbolicExecutionRewrite.z3ArrayStateMap.get().put(targetName, newZ3MapState);

        // Trả về giá trị mảng cũ
        return oldValue;
    }

}
