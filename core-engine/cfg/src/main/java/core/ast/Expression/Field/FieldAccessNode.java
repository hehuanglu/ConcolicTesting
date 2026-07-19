package core.ast.Expression.Field;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPSort;
import com.microsoft.z3.Sort;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.Array.ArrayNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.BooleanLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.Name.SimpleNameNode;
import core.symbolicExecution.MemoryModel;
import core.testDriver.TestDriverUtils;
import core.testGeneration.TestGeneration;
import org.eclipse.jdt.core.dom.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldAccessNode extends ExpressionNode {
    private static int numberOfFieldAccess = 1;
    private static AST ast;

    private String targetName;   // variable name or class name (like className)
    private String fieldName;    // the field identifier
    private boolean isStatic;    // distinguishes static from instance fields
    private Type astType;        // JDT Type for memory model declaration
    private Class<?> fieldClass; // for Z3 sort creation
    // no arguments list – field access doesn’t have arguments

    public FieldAccessNode(String targetName, String fieldName, boolean isStatic,
                           Type astType, Class<?> fieldClass) {
        this.targetName = targetName;
        this.fieldName = fieldName;
        this.isStatic = isStatic;
        this.astType = astType;
        this.fieldClass = fieldClass;
    }

    public FieldAccessNode() {}  // needed for instanceof checks

    public String getTargetName() { return targetName; }
    public String getFieldName() { return fieldName; }
    public boolean isStatic() { return isStatic; }
    public Type getAstType() { return astType; }
    public Class<?> getFieldClass() { return fieldClass; }

    public static AstNode executeFieldAccess(FieldAccess fieldAccess, MemoryModel memoryModel) {
        Expression arrExpr = fieldAccess.getExpression();
        String fieldName = fieldAccess.getName().getIdentifier();
        if ("length".equals(fieldName)) {
            AstNode arr = ExpressionNode.executeExpression(arrExpr, memoryModel);
            if (arr instanceof ArrayNode) {
                return ((ArrayNode) arr).getLengthOfDimensions();
            } else {
                throw new IllegalArgumentException("Chưa hỗ trợ FieldAccess cho các Object khác Array");
            }
        }
        throw new IllegalArgumentException("Chưa hỗ trợ cho FieldAcess khác ngoài length");
    }
}