package core.ast.Expression.Array;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Sort;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
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
        if (indexNode instanceof IntegerLiteralNode) {
            int id = ((IntegerLiteralNode) indexNode).getIntegerValue();
            String tmpName = name + "[" + id + "]";
            AstNode tmpNode = memoryModel.getValue(tmpName);
            if (tmpNode != null) {
                return (ExpressionNode) tmpNode;
            }
        }

        AstNode arr = memoryModel.getValue(name);
        if (arr instanceof ArrayNode) {
            return ((ArrayNode) arr).getElements(name, indexNode);
        }
        throw new IllegalStateException("Array access node not found: " + name);
    }

    public static Expr createZ3ArrayAccessExpression(ArrayAccessNode arrayAccess,
                                                     MemoryModel memoryModel,
                                                     Context ctx,
                                                     List<Z3VariableWrapper> vars) {

        // Tên mảng
        String arrayName = arrayAccess.getArrayName();
        Expr z3ArrayBase = SymbolicExecutionRewrite.z3ArrayStateMap.get().get(arrayName);

        if (z3ArrayBase == null) {
<<<<<<< HEAD

=======
            Sort rangeSort = ctx.mkBitVecSort(32);
>>>>>>> 4719efc0cc44b3e122543b87d7578eb9575b2a7a
            Map<String, String> typeMap = SymbolicExecutionRewrite.variableTypeMap;

            Sort rangeSort = ctx.getIntSort();   // mặc định

            if (typeMap != null) {
                String type = typeMap.get(arrayName);

                if (type != null) {
                    switch (type) {

                        // Các kiểu nguyên
                        case "byte[]":
                        case "short[]":
                        case "int[]":
                        case "long[]":
                        case "char[]":
                            rangeSort = ctx.getIntSort();
                            break;

                        // Kiểu thực
                        case "float[]":
                        case "double[]":
                            rangeSort = ctx.getRealSort();
                            break;

                        // Boolean
                        case "boolean[]":
                            rangeSort = ctx.getBoolSort();
                            break;

                        default:
                            rangeSort = ctx.getIntSort();
                    }
                }
            }

            Sort indexSort = ctx.getIntSort();

            z3ArrayBase = ctx.mkConst(arrayName, ctx.mkArraySort(indexSort, rangeSort));

            SymbolicExecutionRewrite.z3ArrayStateMap.get().put(arrayName, z3ArrayBase);
        }

        // Lấy index
        ExpressionNode rawIndexNode = (ExpressionNode) arrayAccess.getIndex();

        Expr z3IndexExpr = OperationExpressionNode.createZ3Expression(
                rawIndexNode,
                ctx,
                vars,
                memoryModel
        );

        if (!z3IndexExpr.getSort().equals(ctx.getIntSort())) {
            throw new IllegalStateException(
                    "Array index must be Int, found: " + z3IndexExpr.getSort()
            );
        }

        log.debug("Đã dịch Array Access: {}[{}] sang biểu thức Z3 mkSelect",
                arrayName, z3IndexExpr);

        return ctx.mkSelect((ArrayExpr) z3ArrayBase, z3IndexExpr);
    }
}