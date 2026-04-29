package core.ast.Expression.Array;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Sort;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.symbolicExecution.MemoryModel;
import core.symbolicExecution.SymbolicExecutionRewrite;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.PrimitiveType;

import java.util.List;
import java.util.Map;

@Slf4j
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
        Expr z3ArrayBase = SymbolicExecutionRewrite.z3ArrayStateMap.get().get(arrayName);

        if (z3ArrayBase == null) {
            // Lấy kích cỡ sort dựa vào kiểu dữ liệu đã lưu trong map
            Sort rangeSort = ctx.mkBitVecSort(32);
            Map<String, PrimitiveType.Code> typeMap = SymbolicExecutionRewrite.variableTypeMap;

            if (typeMap != null && typeMap.get(arrayName) != null) {
                String typeStr = typeMap.get(arrayName).toString();
                if (typeStr.equals("long")) rangeSort = ctx.mkBitVecSort(64);
                else if (typeStr.equals("double")) rangeSort = ctx.mkFPSortDouble();
                else if (typeStr.equals("float")) rangeSort = ctx.mkFPSortSingle();
            }

            z3ArrayBase = ctx.mkConst(arrayName, ctx.mkArraySort(ctx.mkBitVecSort(32), rangeSort));
            SymbolicExecutionRewrite.z3ArrayStateMap.get().put(arrayName, z3ArrayBase);
        }

        // Lấy raw index từ cây AST
        ExpressionNode rawIndexNode = (ExpressionNode) arrayAccess.getIndex();

        Expr z3IndexExpr = OperationExpressionNode.createZ3Expression(rawIndexNode, ctx, vars, memoryModel);

        log.debug("Đã dịch Array Access: {}[{}] sang biểu thức Z3 mkSelect", arrayName, z3IndexExpr);

        // Kết quả sẽ là 1 giá trị theo đúng Sort
        return ctx.mkSelect((ArrayExpr) z3ArrayBase, z3IndexExpr);
    }
}