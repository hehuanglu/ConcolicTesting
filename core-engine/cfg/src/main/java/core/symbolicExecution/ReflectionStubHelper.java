package core.symbolicExecution;

import com.microsoft.z3.Sort;
import core.testDriver.TestDriverUtils;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectionStubHelper {

    /**
     * Hàm tìm kiểu trả về
     * Biết nhìn vế trái để đoán kiểu trả về mong đợi
     */
    public static Class<?> getReturnType(org.eclipse.jdt.core.dom.MethodInvocation methodInvocation, String className, String methodName, int argCount, String clonedDirPath) {
        try {
            //Quét thư viện chuẩn của Java đầu tiên
            if (!className.contains("data.clonedProject") && !className.equals("ExternalAPI") && !className.equals("MyCalculator")) {
                Class<?> standardClass;
                try {
                    standardClass = Class.forName("java.lang." + className);
                } catch (ClassNotFoundException e) {
                    // thử tìm nguyên gốc
                    standardClass = Class.forName(className);
                }

                for (java.lang.reflect.Method m : standardClass.getDeclaredMethods()) {
                    if (m.getName().equals(methodName) && m.getParameterCount() == argCount) {
                        return m.getReturnType();
                    }
                }
            }

            //Đọc thẳng file .java chưa biên dịch bằng AST Parser!
            String sourceFilePath = clonedDirPath + java.io.File.separator + className + ".java";
            java.io.File sourceFile = new java.io.File(sourceFilePath);

            if (sourceFile.exists()) {
                String sourceCode = new String(java.nio.file.Files.readAllBytes(sourceFile.toPath()));

                org.eclipse.jdt.core.dom.ASTParser parser = org.eclipse.jdt.core.dom.ASTParser.newParser(org.eclipse.jdt.core.dom.AST.JLS14);
                parser.setSource(sourceCode.toCharArray());
                parser.setKind(org.eclipse.jdt.core.dom.ASTParser.K_COMPILATION_UNIT);
                org.eclipse.jdt.core.dom.CompilationUnit cu = (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(null);

                final Class<?>[] foundType = {null};

                cu.accept(new org.eclipse.jdt.core.dom.ASTVisitor() {
                    @Override
                    public boolean visit(org.eclipse.jdt.core.dom.MethodDeclaration node) {
                        if (node.getName().getIdentifier().equals(methodName) && node.parameters().size() == argCount) {
                            String typeStr = node.getReturnType2() != null ? node.getReturnType2().toString() : "void";
                            foundType[0] = mapStringToClass(typeStr);
                        }
                        return super.visit(node);
                    }
                });

                if (foundType[0] != null) {
                    System.out.println(" đã kiểm tra được " + className + "." + methodName + " trả về: " + foundType[0].getSimpleName());
                    return foundType[0];
                }
            } else {
                System.out.println("Không tìm thấy file gốc tại: " + sourceFilePath);
            }

        } catch (Exception e) {
            System.out.println(" Lỗi khi kiểm tra class " + className + ": " + e.getMessage());
        }

        return null;
    }

    // Hàm tiện ích chuyển đổi chuỗi chữ thành Class Type
    private static Class<?> mapStringToClass(String typeStr) {
        switch (typeStr) {
            case "int":
                return int.class;
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "String":
                return String.class;
            default:
                return Object.class;
        }
    }

    /**
     * Ánh xạ kiểu Java (Class<?>) sang Sort tương ứng trong Z3,
     * theo đúng quy ước đã dùng xuyên suốt project:
     * - Số nguyên (byte/short/int/long) -> IntSort (không BitVec)
     * - float/double -> FPSort đúng chuẩn IEEE 754 (8,24) và (11,53)
     * - boolean -> BoolSort
     * - char -> CharSort (KHÔNG phải BitVec, khớp với CharacterMethodNode)
     * - String -> SeqSort<CharSort>
     * - Mảng (int[], String[], ...) -> ArraySort(IntSort, sort của phần tử), đệ quy cho mảng nhiều chiều
     * - Kiểu không xác định -> Uninterpreted Sort theo tên class (để không đánh mất thông tin)
     */
    public static Sort getZ3Sort(Class<?> type, com.microsoft.z3.Context ctx) {
        if (type == null) {
            // Không xác định được kiểu -> fallback an toàn nhất
            return ctx.mkUninterpretedSort("Unknown");
        }

        // ---- Mảng: đệ quy lấy sort phần tử, bọc trong ArraySort ----
        if (type.isArray()) {
            Class<?> elementType = type.getComponentType();
            Sort elementSort = getZ3Sort(elementType, ctx);
            // Index của mảng Java luôn là int -> dùng IntSort làm domain
            return ctx.mkArraySort(ctx.mkIntSort(), elementSort);
        }

        // ---- Số nguyên: dùng IntSort thay vì BitVec ----
        if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == byte.class || type == Byte.class
                || type == short.class || type == Short.class) {
            return ctx.mkIntSort();
        }

        // ---- boolean ----
        if (type == boolean.class || type == Boolean.class) {
            return ctx.mkBoolSort();
        }

        // ---- char: CharSort, không phải BitVec ----
        if (type == char.class || type == Character.class) {
            return ctx.mkCharSort();
        }

        // ---- float / double: FPSort đúng chuẩn IEEE 754 ----
        if (type == float.class || type == Float.class) {
            return ctx.mkFPSort(8, 24);
        }
        if (type == double.class || type == Double.class) {
            return ctx.mkFPSort(11, 53);
        }

        // ---- String: chuỗi ký tự Z3 (SeqSort<CharSort>) ----
        if (type == String.class) {
            return ctx.mkStringSort();
        }

        // ---- Kiểu không xác định (object tự định nghĩa, chưa hỗ trợ symbolic) ----
        // Thay vì mặc định về mkBitVecSort(32) (dễ gây sai ngữ nghĩa âm thầm),
        // trả về Uninterpreted Sort để lỗi lộ ra rõ ràng thay vì bị che giấu.
        return ctx.mkUninterpretedSort(type.getName());
    }

    /**
     * Suy luận kiểu trả về mong đợi từ NGỮ CẢNH nơi methodInvocation được dùng,
     * thay vì resolve trực tiếp trên method (vì method có thể không có binding).
     */
    public static Class<?> getReturnType(org.eclipse.jdt.core.dom.MethodInvocation methodInvocation) {
        org.eclipse.jdt.core.dom.ASTNode parent = methodInvocation.getParent();

        // Case 1: int[] result = obj.buildSuffixArray(s);
        if (parent instanceof org.eclipse.jdt.core.dom.VariableDeclarationFragment) {
            org.eclipse.jdt.core.dom.VariableDeclarationFragment frag =
                    (org.eclipse.jdt.core.dom.VariableDeclarationFragment) parent;
            org.eclipse.jdt.core.dom.ASTNode declParent = frag.getParent();

            org.eclipse.jdt.core.dom.Type declaredType = null;
            if (declParent instanceof org.eclipse.jdt.core.dom.VariableDeclarationStatement) {
                declaredType = ((org.eclipse.jdt.core.dom.VariableDeclarationStatement) declParent).getType();
            } else if (declParent instanceof org.eclipse.jdt.core.dom.VariableDeclarationExpression) {
                declaredType = ((org.eclipse.jdt.core.dom.VariableDeclarationExpression) declParent).getType();
            } else if (declParent instanceof org.eclipse.jdt.core.dom.FieldDeclaration) {
                declaredType = ((org.eclipse.jdt.core.dom.FieldDeclaration) declParent).getType();
            }

            if (declaredType != null) {
                org.eclipse.jdt.core.dom.ITypeBinding tb = declaredType.resolveBinding();
                if (tb != null) {
                    return mapQualifiedNameToClass(tb.getQualifiedName());
                }
                // fallback nếu binding vẫn null: dùng chuỗi cú pháp
                return mapStringToClass(declaredType.toString());
            }
        }

        // Case 2: result = obj.buildSuffixArray(s);  (gán lại, không khai báo mới)
        if (parent instanceof org.eclipse.jdt.core.dom.Assignment) {
            org.eclipse.jdt.core.dom.Assignment assign = (org.eclipse.jdt.core.dom.Assignment) parent;
            org.eclipse.jdt.core.dom.ITypeBinding tb = assign.getLeftHandSide().resolveTypeBinding();
            if (tb != null) {
                return mapQualifiedNameToClass(tb.getQualifiedName());
            }
        }

        // Case 3: return obj.buildSuffixArray(s);
        if (parent instanceof org.eclipse.jdt.core.dom.ReturnStatement) {
            org.eclipse.jdt.core.dom.MethodDeclaration enclosingMethod = null;
            org.eclipse.jdt.core.dom.ASTNode cur = parent;
            while (cur != null && !(cur instanceof org.eclipse.jdt.core.dom.MethodDeclaration)) {
                cur = cur.getParent();
            }
            if (cur != null) {
                enclosingMethod = (org.eclipse.jdt.core.dom.MethodDeclaration) cur;
                org.eclipse.jdt.core.dom.Type retType = enclosingMethod.getReturnType2();
                if (retType != null) {
                    org.eclipse.jdt.core.dom.ITypeBinding tb = retType.resolveBinding();
                    if (tb != null) return mapQualifiedNameToClass(tb.getQualifiedName());
                    return mapStringToClass(retType.toString());
                }
            }
        }

        // Case 4: (int[]) obj.buildSuffixArray(s);  -- ép kiểu tường minh
        if (parent instanceof org.eclipse.jdt.core.dom.CastExpression) {
            org.eclipse.jdt.core.dom.CastExpression cast = (org.eclipse.jdt.core.dom.CastExpression) parent;
            org.eclipse.jdt.core.dom.ITypeBinding tb = cast.getType().resolveBinding();
            if (tb != null) return mapQualifiedNameToClass(tb.getQualifiedName());
            return mapStringToClass(cast.getType().toString());
        }

        // Case 5: obj.buildSuffixArray(s) là argument của lời gọi method khác
        if (parent instanceof org.eclipse.jdt.core.dom.MethodInvocation) {
            org.eclipse.jdt.core.dom.MethodInvocation outer = (org.eclipse.jdt.core.dom.MethodInvocation) parent;
            int argIndex = outer.arguments().indexOf(methodInvocation);
            org.eclipse.jdt.core.dom.IMethodBinding outerBinding = outer.resolveMethodBinding();
            if (argIndex >= 0 && outerBinding != null) {
                org.eclipse.jdt.core.dom.ITypeBinding[] paramTypes = outerBinding.getParameterTypes();
                if (argIndex < paramTypes.length) {
                    return mapQualifiedNameToClass(paramTypes[argIndex].getQualifiedName());
                }
            }
        }

        // Không xác định được ngữ cảnh -> trả null, để chỗ gọi fallback sang cách khác
        // (ví dụ đọc source file của className như bạn đã làm trước đó)
        return null;
    }

    /**
     * Map tên kiểu (từ ITypeBinding.getQualifiedName() hoặc Type.toString() fallback)
     * sang Class<?> tương ứng. Hỗ trợ: primitive, mảng nhiều chiều, generic (bỏ type param),
     * và các class thông dụng java.lang / java.util.
     */
    private static Class<?> mapQualifiedNameToClass(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        name = name.trim();

        // 1. Bỏ phần generic: List<Integer> -> List, Map<String, Integer> -> Map
        int genericIdx = name.indexOf('<');
        if (genericIdx != -1) {
            name = name.substring(0, genericIdx).trim();
        }

        // 2. Đếm số chiều mảng: int[][] -> baseName="int", arrayDepth=2
        int arrayDepth = 0;
        while (name.endsWith("[]")) {
            arrayDepth++;
            name = name.substring(0, name.length() - 2).trim();
        }

        Class<?> baseClass = resolveBaseClass(name);
        if (baseClass == null) {
            return null;
        }
        if (arrayDepth == 0) {
            return baseClass;
        }

        // 3. Ráp lại thành Class mảng đúng số chiều, kể cả mảng primitive (int[][])
        int[] dims = new int[arrayDepth];
        return java.lang.reflect.Array.newInstance(baseClass, dims).getClass();
    }

    /**
     * Resolve kiểu cơ bản (đã bỏ [] và generic).
     * Hỗ trợ cả tên đầy đủ (java.lang.String) lẫn tên rút gọn (String) —
     * vì fallback từ Type.toString() (cú pháp) thường KHÔNG có package.
     */
    private static Class<?> resolveBaseClass(String name) {
        switch (name) {
            case "void":    return void.class;
            case "boolean": return boolean.class;
            case "byte":    return byte.class;
            case "short":   return short.class;
            case "char":    return char.class;
            case "int":     return int.class;
            case "long":    return long.class;
            case "float":   return float.class;
            case "double":  return double.class;

            case "String":
            case "java.lang.String":   return String.class;
            case "Object":
            case "java.lang.Object":   return Object.class;
            case "Integer":
            case "java.lang.Integer":  return Integer.class;
            case "Long":
            case "java.lang.Long":     return Long.class;
            case "Double":
            case "java.lang.Double":   return Double.class;
            case "Float":
            case "java.lang.Float":    return Float.class;
            case "Boolean":
            case "java.lang.Boolean":  return Boolean.class;
            case "Character":
            case "java.lang.Character": return Character.class;

            case "List":
            case "java.util.List":      return java.util.List.class;
            case "ArrayList":
            case "java.util.ArrayList": return java.util.ArrayList.class;
            case "Map":
            case "java.util.Map":       return java.util.Map.class;
            case "HashMap":
            case "java.util.HashMap":   return java.util.HashMap.class;
            case "Set":
            case "java.util.Set":       return java.util.Set.class;

            default:
                // Thử full qualified name trước (trường hợp có binding -> đã full)
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    // Thử coi là class trong package java.lang (tên rút gọn)
                    try {
                        return Class.forName("java.lang." + name);
                    } catch (ClassNotFoundException e2) {
                        return null; // để chỗ gọi tự fallback (vd: đọc source file user-defined class)
                    }
                }
        }
    }

}