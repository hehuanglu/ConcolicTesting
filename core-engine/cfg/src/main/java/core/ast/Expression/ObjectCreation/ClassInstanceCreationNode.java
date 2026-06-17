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
import core.utils.Utils;
import core.variable.ParameterizedTypeVariable;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class ClassInstanceCreationNode extends ExpressionNode{

    private Type type;
    private List<AstNode> arguments = new ArrayList<>();
    private Expr emptyZ3Array;

    public ClassInstanceCreationNode(Type type, List<AstNode> arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    public static AstNode executeClassInstanceCreation(ClassInstanceCreation expression, MemoryModel memoryModel) {
        Type type = expression.getType();
        String typeStr = type.toString();
        if (typeStr.startsWith("ArrayList") || typeStr.startsWith("List")) {
            return handleListInstanceCreation(expression, memoryModel, type);
        }
        return null;
    }

    private static AstNode handleListInstanceCreation(ClassInstanceCreation expression, MemoryModel memoryModel, Type type) {
        Context ctx = SymbolicExecutionRewrite.globalCtx.get();
        List<AstNode> arguments = new ArrayList<>();
        for (Object arg : expression.arguments()) {
            arguments.add(ExpressionNode.executeExpression((Expression) arg, memoryModel));
        }

        ClassInstanceCreationNode classInstanceNode = new ClassInstanceCreationNode(type, arguments);
        Sort domain = ctx.mkBitVecSort(32);
        Sort range = ctx.mkBitVecSort(32);
        String varName = "_empty_list";


        ASTNode parent = expression.getParent();
        if (parent instanceof VariableDeclarationFragment) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) parent;
            varName = fragment.getName().getIdentifier() + varName;

            ASTNode grandParent = parent.getParent();
            Type leftType = null;

            if (grandParent instanceof VariableDeclarationStatement) {
                leftType = ((VariableDeclarationStatement) grandParent).getType();
            } else {
                throw new UnsupportedOperationException("VariableDeclarationExpression is not supported yet");
            }

            if (leftType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) leftType;
                if (!pType.typeArguments().isEmpty()) {
                    String typeArgStr = pType.typeArguments().get(0).toString();
                    range = Utils.getZ3Sort(typeArgStr, ctx);
                }
            }
        }

        ArraySort z3ArraySort = ctx.mkArraySort(domain, range);
        Expr emptyZ3List = ctx.mkConst(varName, z3ArraySort);
        classInstanceNode.setZ3Expression(emptyZ3List);

        return classInstanceNode;
    }

    public void setZ3Expression(Expr z3Expression) {
        this.emptyZ3Array = z3Expression;
    }

    public Expr getZ3Expression() {
        return this.emptyZ3Array;
    }

    public Type getType() { return type; }
    public List<AstNode> getArguments() { return arguments; }

}