package core.ast.Expression.Array;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Name.NameNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.ArrayAccess;

import java.util.List;

import static core.ast.Expression.OperationExpression.OperationExpressionNode.getDuplicateVariableIndex;

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

        // lấy chỉ số
        ExpressionNode indexNode = arrayAccess.getIndex();
        int concreteIndex = 0;

        // Lấy node từ memoryModel
        if (indexNode instanceof NameNode) {
            indexNode = NameNode.executeNameNode((NameNode) indexNode, memoryModel);
        }

        // Chuyển thành số int
        if (indexNode instanceof LiteralNode) {
            concreteIndex = LiteralNode.changeLiteralNodeToInteger((LiteralNode) indexNode);
        } else {
            throw new RuntimeException("Không thể tính index của mảng hiện tại");
        }

        // ghép tên mảng và index thành 1 biến riêng
        String flatName = arrayName + "_" + concreteIndex;
        System.out.println("Đã trải: " + arrayName + "[" + concreteIndex + "] thành biến Z3: " + flatName);

        // khai báo biến cho z3
        Expr z3ArrayElement = ctx.mkBVConst(flatName, 32);

        Z3VariableWrapper wrapper = new Z3VariableWrapper(z3ArrayElement);
        if (getDuplicateVariableIndex(wrapper, vars) == -1) {
            vars.add(wrapper);
        }

        return z3ArrayElement;
    }
}