package core.symbolicExecution;

import core.ast.AstNode;
import core.ast.Expression.Name.NameNode;
import core.variable.ArrayTypeVariable;
import core.variable.PrimitiveTypeVariable;
import core.variable.Variable;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.PrimitiveType;

import java.util.HashMap;
import java.util.Map;

public class MemoryModel { // ONLY FOR PRIMITIVE TYPES!!!!
    private HashMap<Variable, AstNode> S = new HashMap<>();

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
        S.put(new PrimitiveTypeVariable(primitiveType, name), element);
    }

    public void declareArrayTypeVariable(ArrayType type, String name, int numberOfDimensions, AstNode element) {
        S.put(new ArrayTypeVariable(type, name, numberOfDimensions), element);
    }

    public AstNode getValue(String name) {
        for (Map.Entry<Variable, AstNode> set : S.entrySet()) {
            if (set.getKey().getName().equals(name)) {
                return set.getValue();
            }
        }
        throw new RuntimeException("There's no variable with name: " + name + " in memory model!");
    }

    public AstNode getValue(NameNode nameNode) {
        String name = NameNode.getStringNameNode(nameNode);
        for (Map.Entry<Variable, AstNode> set : S.entrySet()) {
            if (set.getKey().getName().equals(name)) {
                return set.getValue();
            }
        }
        return nameNode;
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
