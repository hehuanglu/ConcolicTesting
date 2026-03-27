package core.symbolicExecution;

import com.microsoft.z3.Sort;

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
                    // thêm java.lang
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

    public static Sort getZ3Sort(Class<?> type, com.microsoft.z3.Context ctx) {
        if (type == int.class || type == Integer.class) {
            return ctx.mkBitVecSort(32);
        } else if (type == boolean.class || type == Boolean.class) {
            return ctx.mkBoolSort();
        } else if (type == byte.class || type == Byte.class) {
            return ctx.mkBitVecSort(8);
        } else if (type == short.class || type == Short.class) {
            return ctx.mkBitVecSort(16);
        } else if (type == long.class || type == Long.class) {
            return ctx.mkBitVecSort(64);
        } else if (type == char.class || type == Character.class) {
            return ctx.mkBitVecSort(16);
        } else if (type == float.class || type == Float.class) {
            return ctx.mkFPSort(8, 24);
        } else if (type == double.class || type == Double.class) {
            return ctx.mkFPSort(11, 53);
        } else {
            return ctx.mkBitVecSort(32);
        }
    }
}