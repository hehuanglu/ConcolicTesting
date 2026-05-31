package core.ast.Expression.ObjectCreation;

import com.microsoft.z3.ArraySort;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Sort;
import core.ast.AstNode;
import core.ast.Expression.Array.ArrayNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Type.AnnotatableType.PrimitiveTypeNode;
import core.ast.Type.ParameterizedTypeNode;
import core.symbolicExecution.SymbolicExecutionRewrite;
import core.symbolicExecution.MemoryModel;
import core.variable.ParameterizedTypeVariable;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class ClassInstanceCreationNode extends ExpressionNode{

    private Type type;
    private String typeStr;
    private List<AstNode> arguments = new ArrayList<>();
    private Expr emptyZ3Array;

    public ClassInstanceCreationNode(Type type, String typeStr, List<AstNode> arguments) {
        this.type = type;
        this.typeStr = typeStr;
        this.arguments = arguments;
    }

    public static AstNode executeClassInstanceCreation(ClassInstanceCreation expression, MemoryModel memoryModel) {
        Type type = expression.getType();
        String typeStr = type.toString();

        if (typeStr.startsWith("ArrayList") || typeStr.startsWith("List")) {
            Context ctx = SymbolicExecutionRewrite.globalCtx.get();


            List<AstNode> arguments = new ArrayList<>();
            for (Object arg : expression.arguments()) {
                arguments.add(ExpressionNode.executeExpression((Expression) arg, memoryModel));
            }

            ClassInstanceCreationNode classInstanceNode = new ClassInstanceCreationNode(type, typeStr, arguments);

            Sort domain = ctx.mkBitVecSort(32);
            Sort range = ctx.mkBitVecSort(32);

            ArraySort z3ArraySort = ctx.mkArraySort(domain, range);
            Expr emptyZ3List = ctx.mkConst("empty_list_" + System.nanoTime(), z3ArraySort);


            classInstanceNode.setZ3Expression(emptyZ3List);

            return classInstanceNode;
        }

        return null;
    }

    public void setZ3Expression(Expr z3Expression) {
        this.emptyZ3Array = z3Expression;
    }

    public Expr getZ3Expression() {
        return this.emptyZ3Array;
    }

    public Type getType() { return type; }
    public String getTypeStr() { return typeStr; }
    public List<AstNode> getArguments() { return arguments; }

}