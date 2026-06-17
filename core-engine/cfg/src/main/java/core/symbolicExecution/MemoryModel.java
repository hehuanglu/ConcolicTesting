package core.symbolicExecution;

import com.microsoft.z3.Expr;
import core.ast.AstNode;
import core.ast.Expression.Method.MethodInvocationNode;
import core.ast.Expression.Name.NameNode;
import core.ast.Expression.Name.SimpleNameNode;
import core.variable.ArrayTypeVariable;
import core.variable.ParameterizedTypeVariable;
import core.variable.PrimitiveTypeVariable;
import core.variable.SimpleTypeVariable;
import core.variable.Variable;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;

import java.util.HashMap;
import java.util.Map;

import static core.symbolicExecution.SymbolicExecutionRewrite.CollectionKeys;

public class MemoryModel { // ONLY FOR PRIMITIVE TYPES!!!!
    private HashMap<Variable, AstNode> S = new HashMap<>();
    // map astNode --> z3expr ?
    public MemoryModel() {
    }

    public void assignVariable(String name, AstNode element) {
        for (Map.Entry<Variable, AstNode> set : S.entrySet()) {
            if (set.getKey().getName().equals(name)) {
                set.setValue(element);
                break;
            }
        }
    }

    public void declarePrimitiveTypeVariable(PrimitiveType primitiveType, String name, AstNode element) {
        PrimitiveTypeVariable newPrimitiveVar = new PrimitiveTypeVariable(primitiveType, name);
        S.put(newPrimitiveVar, element);
    }

    public void declarePrimitiveTypeVariableWithCachExpr(PrimitiveType primitiveType, String name, AstNode element,Expr cacheExpr) {
        PrimitiveTypeVariable newPrimitiveVar = new PrimitiveTypeVariable(primitiveType, name);
        newPrimitiveVar.setCacheExpr(cacheExpr);
        S.put(newPrimitiveVar, element);
    }

    public void declareArrayTypeVariable(ArrayType type, String name, int numberOfDimensions, AstNode element) {
        S.put(new ArrayTypeVariable(type, name, numberOfDimensions), element);
    }

    public void declareParameterizedTypeVariable(ParameterizedType type, String name, AstNode element, boolean isParameter) {
        S.put(new ParameterizedTypeVariable(type, name, SymbolicExecutionRewrite.globalCtx.get().mkBVConst(name + ".size", 32), isParameter), element);
    }

    public void declareSimpleTypeVariable(SimpleType simpleType, String name, AstNode element,Expr cacheExpr) {
        SimpleTypeVariable simpleTypeVariable = new SimpleTypeVariable(simpleType, name);
        simpleTypeVariable.setCacheExpr(cacheExpr);
        S.put(simpleTypeVariable, element);
    }



    public AstNode getValue(String name) {
        for (Map.Entry<Variable, AstNode> set : S.entrySet()) {
            if (set.getKey().getName().equals(name)) {
                AstNode node = set.getValue();
                if (node instanceof SimpleNameNode) {
                    SimpleNameNode sn = (SimpleNameNode) node;
                    return sn.isReference() ? sn.getTarget() : sn;
                }
                return node;
            }
        }
        throw new RuntimeException("There's no variable with name: " + name + " in memory model!");
    }

    public AstNode getValue(NameNode nameNode) {
        String name = NameNode.getStringNameNode(nameNode);
         for (Map.Entry<Variable, AstNode> set : S.entrySet()) {
            if (set.getKey().getName().equals(name)) {
                AstNode node = set.getValue();
                if (node instanceof SimpleNameNode) {
                    SimpleNameNode sn = (SimpleNameNode) node;
                    return sn.isReference() ? sn.getTarget() : sn;
                }
                return node;
            }
        }
        throw new RuntimeException("There's no variable with name: " + name + " in memory model!");
    }

    public Variable getVariable(String name) {
        for (Map.Entry<Variable, AstNode> set : S.entrySet()) {
            if(set.getKey().getName().equals(name)) {
                return set.getKey();
            }
        }
        throw new RuntimeException("There's no variable with name: " + name + " in memory model!");
    }
}