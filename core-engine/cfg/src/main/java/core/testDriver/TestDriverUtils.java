package core.testDriver;

import core.ast.Expression.Literal.LiteralNode;
import core.symbolicExecution.MemoryModel;
import core.variable.Variable;
import org.eclipse.jdt.core.dom.*;

import java.lang.reflect.Array;
import java.util.*;

public final class TestDriverUtils {
    private TestDriverUtils() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    /**
     * Get parameter's classes list from AST Node List
     *
     * @param parameters (get from MethodDeclaration)
     * @return parameter's classes list
     */
    public static Class<?>[] getParameterClasses(List<ASTNode> parameters) {
        Class<?>[] types = new Class[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            ASTNode param = parameters.get(i);
            if (param instanceof SingleVariableDeclaration) {
                SingleVariableDeclaration declaration = (SingleVariableDeclaration) param;
                Type type = declaration.getType();
                types[i] = getTypeClass(type);
            } else if (param instanceof VariableDeclarationFragment) {
                VariableDeclarationFragment declaration = (VariableDeclarationFragment) param;
                Type type = (Type) declaration.resolveBinding().getType();
                types[i] = getTypeClass(type);
            } else {
                throw new RuntimeException("Unsupported parameter: " + param.getClass());
            }
        }
        return types;
    }

    public static Class<?>[] getVariableClasses(List<ASTNode> variables, MemoryModel memoryModel) {
        Class<?>[] types = new Class[variables.size()];
        for (int i = 0; i < variables.size(); i++) {
            ASTNode variableNode = variables.get(i);
            if (LiteralNode.isLiteral(variableNode)) {
                types[i] = LiteralNode.getLiteralClass(variableNode);
            } else if (variableNode instanceof Name) {
                String key = ((Name) variableNode).getFullyQualifiedName();
                Variable variable = memoryModel.getVariable(key);
                types[i] = getTypeClass(variable.getType());
            } else {
                throw new RuntimeException("Unsupported variable: " + variableNode.getClass());
            }
        }

        return types;
    }

    public static List<String> getParameterNames(List<ASTNode> parameters) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            ASTNode param = parameters.get(i);
            if (param instanceof SingleVariableDeclaration) {
                SingleVariableDeclaration declaration = (SingleVariableDeclaration) param;
                names.add(declaration.getName().getIdentifier());
            } else if (param instanceof VariableDeclarationFragment) {
                VariableDeclarationFragment declaration = (VariableDeclarationFragment) param;
                names.add(declaration.getName().getIdentifier());
            } else {
                throw new RuntimeException("Unsupported parameter: " + param.getClass());
            }
        }
        return names;
    }

    /**
     * Get class base on the type (can be PrimitiveType, ArrayType)
     *
     * @param type
     * @return the class of the type
     */
    private static Class<?> getTypeClass(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.Code primitiveTypeCode = (((PrimitiveType) type).getPrimitiveTypeCode());
            return getPrimitiveClass(primitiveTypeCode);
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            Type componentType = arrayType.getElementType();
            Class<?> componentClass = getTypeClass(componentType); // get clas of the component of array like int, double, ...
            int dimension = arrayType.getDimensions();
            Class<?> arrayClass = Array.newInstance(componentClass, new int[dimension]).getClass();
            return arrayClass;
        } else if (type instanceof SimpleType) {
            SimpleType simpleType = (SimpleType) type;
            return getClassForName(simpleType.getName().getFullyQualifiedName());
        } else {
            throw new RuntimeException("Unsupported parameter type: " + type.getClass());
        }
    }

    /**
     * Get primitive class by its type code
     *
     * @param primitiveTypeCode
     * @return class of the code
     */
    private static Class<?> getPrimitiveClass(PrimitiveType.Code primitiveTypeCode) {
        String primitiveTypeStr = primitiveTypeCode.toString();
        switch (primitiveTypeStr) {
            case "int":
                return int.class;
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "char":
                return char.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
            default:
                throw new RuntimeException("Unsupported primitive type: " + primitiveTypeStr);
        }
    }

    public static PrimitiveType.Code getPrimitiveCode(Class<?> typeClass) {
        switch (typeClass.getName()) {
            case "int":
                return PrimitiveType.INT;
            case "boolean":
                return PrimitiveType.BOOLEAN;
            case "byte":
                return PrimitiveType.BYTE;
            case "short":
                return PrimitiveType.SHORT;
            case "char":
                return PrimitiveType.CHAR;
            case "long":
                return PrimitiveType.LONG;
            case "float":
                return PrimitiveType.FLOAT;
            case "double":
                return PrimitiveType.DOUBLE;
            case "void":
                return PrimitiveType.VOID;
            default:
                throw new RuntimeException("Unsupported primitive type: " + typeClass);
        }
    }

    public static Type cloneTypeAST(Type type, AST ast) {
        if (type instanceof PrimitiveType) {
            PrimitiveType primitiveType = (PrimitiveType) type;
            return ast.newPrimitiveType(primitiveType.getPrimitiveTypeCode());
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            return ast.newArrayType(cloneTypeAST(arrayType.getElementType(), ast), arrayType.getDimensions());
        } else {
            throw new RuntimeException("Invalid type");
        }
    }

    /**
     * Get class of SimpleType by is name
     *
     * @param className
     * @return class
     */
    private static Class<?> getClassForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }
}
