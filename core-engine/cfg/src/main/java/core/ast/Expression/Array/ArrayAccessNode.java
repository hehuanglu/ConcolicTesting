package core.ast.Expression.Array;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Name.NameNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.ArrayAccess;

import java.util.List;

import static core.symbolicExecution.SymbolicExecutionRewrite.z3ArrayStateMap;

public class ArrayAccessNode extends ExpressionNode {

    private String arrayName;
    private ExpressionNode index;

    public ArrayAccessNode(String arrayName, ExpressionNode index) {
        this.arrayName = arrayName;
        this.index = index;
    }

    public String getArrayName() {
        return arrayName;
    }

    public ExpressionNode getIndex() {
        return index;
    }

    public static ExpressionNode executeArrayAccessNode(ArrayAccess arrayAccess, MemoryModel memoryModel) {
        // Lấy index
        ExpressionNode indexNode = (ExpressionNode) AstNode.executeASTNode(arrayAccess.getIndex(), memoryModel);

        // Lấy tên mảng
        String name = arrayAccess.getArray().toString();

        return new ArrayAccessNode(name, indexNode);
    }

    public static Expr createZ3ArrayAccessExpression(ArrayAccessNode arrayAccess, MemoryModel memoryModel,
                                                     Context ctx, List<Z3VariableWrapper> vars) {

        // lấy tên mảng
        String arrayName = arrayAccess.getArrayName();


        // lấy phiên bản mảng mới nhất
        Expr z3ArrayBase = z3ArrayStateMap.get().get(arrayName);

        if (z3ArrayBase == null) {
            z3ArrayBase = ctx.mkConst(arrayName, ctx.mkArraySort(ctx.mkBitVecSort(32), ctx.mkBitVecSort(32)));
            z3ArrayStateMap.get().put(arrayName, z3ArrayBase); // Lưu ngược lại sổ
        }

        // lấy chỉ số và dịch sang z3
        ExpressionNode indexNode = arrayAccess.getIndex();

        // nếu node từ memory model thì lấy
        if (indexNode instanceof NameNode) {
            indexNode = NameNode.executeNameNode((NameNode) indexNode, memoryModel);
        }

        Expr z3IndexExpr = OperationExpressionNode.createZ3Expression(indexNode, ctx, vars, memoryModel);

        System.out.println("Đã dịch phép " + arrayName + ", " + z3IndexExpr);

        // Kết quả sẽ là 1 giá trị Int.
        return ctx.mkSelect((com.microsoft.z3.ArrayExpr) z3ArrayBase, z3IndexExpr);
    }
}