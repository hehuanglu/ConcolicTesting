package core.ast.Expression.Method;

import com.microsoft.z3.*;
import com.microsoft.z3.enumerations.Z3_sort_kind;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.BooleanLiteralNode;
import core.ast.Expression.Literal.CharacterLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.Literal.StringLiteralNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class IntegerMethodNode extends MethodInvocationNode {
    public String ownerName;
    public String methodName;
    public List<AstNode> arguments;
    public AstNode target;

    public static AstNode executeIntegerMethod(MethodInvocation methodInvocation,
                                               MemoryModel memoryModel) {
        IntegerMethodNode integerMethodNode = new IntegerMethodNode();

        integerMethodNode.methodName = methodInvocation.getName().toString();

        List<AstNode> arguments = new ArrayList<>();
        for (int i = 0; i < methodInvocation.arguments().size(); i++) {
            AstNode argNode = ExpressionNode.executeExpression(
                    (Expression) methodInvocation.arguments().get(i),
                    memoryModel
            );
            arguments.add(argNode);
        }
        integerMethodNode.arguments = arguments;

        Expression expression = methodInvocation.getExpression();

        if (expression != null) {
            integerMethodNode.ownerName = expression.toString();

            // Integer.parseInt(s), Integer.compare(a, b), Integer.max(a, b)
            // Không executeExpression("Integer") vì Integer không phải biến.
            if (!"Integer".equals(integerMethodNode.ownerName)) {
                integerMethodNode.target =
                        ExpressionNode.executeExpression(expression, memoryModel);
            }
        }

        ExpressionNode expressionNode = executeIntegerMethodNode(integerMethodNode, memoryModel);
        return expressionNode;
    }

    public static ExpressionNode executeIntegerMethodNode(IntegerMethodNode integerMethodNode,
                                                          MemoryModel memoryModel) {
        String methodName = integerMethodNode.methodName;
        List<AstNode> arguments = integerMethodNode.arguments;
        AstNode target = integerMethodNode.target;

        try {
            // =========================================================
            // Case 1: static method: Integer.xxx(...)
            // =========================================================
            if ("Integer".equals(integerMethodNode.ownerName)) {
                switch (methodName) {
                    case "parseInt": {
                        if (arguments.size() < 1) return integerMethodNode;

                        String value = getStringValue(arguments.get(0));
                        if (value == null) return integerMethodNode;

                        if (arguments.size() >= 2) {
                            Integer radix = getIntValue(arguments.get(1));
                            if (radix == null) return integerMethodNode;

                            return new IntegerLiteralNode(Integer.parseInt(value, radix));
                        }

                        return new IntegerLiteralNode(Integer.parseInt(value));
                    }

                    case "valueOf": {
                        if (arguments.size() < 1) return integerMethodNode;

                        // Integer.valueOf("123")
                        String stringValue = getStringValue(arguments.get(0));
                        if (stringValue != null) {
                            if (arguments.size() >= 2) {
                                Integer radix = getIntValue(arguments.get(1));
                                if (radix == null) return integerMethodNode;

                                return new IntegerLiteralNode(Integer.valueOf(stringValue, radix));
                            }

                            return new IntegerLiteralNode(Integer.valueOf(stringValue));
                        }

                        // Integer.valueOf(123)
                        Integer intValue = getIntValue(arguments.get(0));
                        if (intValue == null) return integerMethodNode;

                        return new IntegerLiteralNode(Integer.valueOf(intValue));
                    }

                    case "toString": {
                        if (arguments.size() < 1) return integerMethodNode;

                        Integer value = getIntValue(arguments.get(0));
                        if (value == null) return integerMethodNode;

                        String resultValue;

                        if (arguments.size() >= 2) {
                            Integer radix = getIntValue(arguments.get(1));
                            if (radix == null) return integerMethodNode;

                            resultValue = Integer.toString(value, radix);
                        } else {
                            resultValue = Integer.toString(value);
                        }

                        StringLiteralNode result = new StringLiteralNode();
                        result.setStringValue(resultValue);
                        return result;
                    }

                    case "toHexString": {
                        if (arguments.size() < 1) return integerMethodNode;

                        Integer value = getIntValue(arguments.get(0));
                        if (value == null) return integerMethodNode;

                        StringLiteralNode result = new StringLiteralNode();
                        result.setStringValue(Integer.toHexString(value));
                        return result;
                    }

                    case "toOctalString": {
                        if (arguments.size() < 1) return integerMethodNode;

                        Integer value = getIntValue(arguments.get(0));
                        if (value == null) return integerMethodNode;

                        StringLiteralNode result = new StringLiteralNode();
                        result.setStringValue(Integer.toOctalString(value));
                        return result;
                    }

                    case "toBinaryString": {
                        if (arguments.size() < 1) return integerMethodNode;

                        Integer value = getIntValue(arguments.get(0));
                        if (value == null) return integerMethodNode;

                        StringLiteralNode result = new StringLiteralNode();
                        result.setStringValue(Integer.toBinaryString(value));
                        return result;
                    }

                    case "compare": {
                        if (arguments.size() < 2) return integerMethodNode;

                        Integer x = getIntValue(arguments.get(0));
                        Integer y = getIntValue(arguments.get(1));

                        if (x == null || y == null) return integerMethodNode;

                        return new IntegerLiteralNode(Integer.compare(x, y));
                    }

                    case "max": {
                        if (arguments.size() < 2) return integerMethodNode;

                        Integer x = getIntValue(arguments.get(0));
                        Integer y = getIntValue(arguments.get(1));

                        if (x == null || y == null) return integerMethodNode;

                        return new IntegerLiteralNode(Integer.max(x, y));
                    }

                    case "min": {
                        if (arguments.size() < 2) return integerMethodNode;

                        Integer x = getIntValue(arguments.get(0));
                        Integer y = getIntValue(arguments.get(1));

                        if (x == null || y == null) return integerMethodNode;

                        return new IntegerLiteralNode(Integer.min(x, y));
                    }

                    case "sum": {
                        if (arguments.size() < 2) return integerMethodNode;

                        Integer x = getIntValue(arguments.get(0));
                        Integer y = getIntValue(arguments.get(1));

                        if (x == null || y == null) return integerMethodNode;

                        return new IntegerLiteralNode(Integer.sum(x, y));
                    }

                    case "signum": {
                        if (arguments.size() < 1) return integerMethodNode;

                        Integer value = getIntValue(arguments.get(0));
                        if (value == null) return integerMethodNode;

                        return new IntegerLiteralNode(Integer.signum(value));
                    }

                    case "bitCount": {
                        if (arguments.size() < 1) return integerMethodNode;

                        Integer value = getIntValue(arguments.get(0));
                        if (value == null) return integerMethodNode;

                        return new IntegerLiteralNode(Integer.bitCount(value));
                    }

                    default:
                        throw new RuntimeException("Unsupported Integer method: " + methodName);
                }
            }

            // =========================================================
            // Case 2: instance method nếu bạn có Integer object/literal
            // Ví dụ: x.intValue(), x.compareTo(y), x.equals(y), x.toString()
            // =========================================================
            Integer targetValue = getIntValue(target);
            if (targetValue == null) {
                return integerMethodNode;
            }

            switch (methodName) {
                case "intValue": {
                    return new IntegerLiteralNode(targetValue);
                }

                case "byteValue": {
                    return new IntegerLiteralNode((byte) targetValue.intValue());
                }

                case "shortValue": {
                    return new IntegerLiteralNode((short) targetValue.intValue());
                }

                case "compareTo": {
                    if (arguments.size() < 1) return integerMethodNode;

                    Integer arg = getIntValue(arguments.get(0));
                    if (arg == null) return integerMethodNode;

                    return new IntegerLiteralNode(Integer.compare(targetValue, arg));
                }

                case "equals": {
                    if (arguments.size() < 1) return integerMethodNode;

                    Integer arg = getIntValue(arguments.get(0));
                    if (arg == null) return integerMethodNode;

                    BooleanLiteralNode result = new BooleanLiteralNode();
                    result.setValue(targetValue.equals(arg));
                    return result;
                }

                case "toString": {
                    StringLiteralNode result = new StringLiteralNode();
                    result.setStringValue(Integer.toString(targetValue));
                    return result;
                }

                default:
                    throw new RuntimeException("Unsupported Integer method: " + methodName);
            }

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Integer getIntValue(AstNode node) {
        if (node == null) return null;

        if (node instanceof NumberLiteralNode) {
            return parseJavaIntLiteral(((NumberLiteralNode) node).getTokenValue());
        }

        if (node instanceof CharacterLiteralNode) {
            return (int) ((CharacterLiteralNode) node).getCharacterValue();
        }

        return null;
    }

    private static String getStringValue(AstNode node) {
        if (node == null) return null;

        if (node instanceof StringLiteralNode) {
            return ((StringLiteralNode) node).getStringValue();
        }

        return null;
    }

    private static Integer parseJavaIntLiteral(String token) {
        if (token == null) return null;

        String value = token.trim().replace("_", "");

        if (value.endsWith("l") || value.endsWith("L")) {
            value = value.substring(0, value.length() - 1);
        }

        try {
            return Integer.decode(value);
        } catch (NumberFormatException ignored) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public static Expr createZ3Expression(IntegerMethodNode node,
                                          MemoryModel memoryModel,
                                          Context ctx,
                                          List<Z3VariableWrapper> vars) {
        switch (node.methodName) {
            case "compare":
                return handleIntegerCompare(node, memoryModel, ctx, vars);

            case "compareTo":
                return handleIntegerCompareTo(node, memoryModel, ctx, vars);

            case "equals":
                return handleIntegerEquals(node, memoryModel, ctx, vars);

            case "max":
                return handleIntegerMax(node, memoryModel, ctx, vars);

            case "min":
                return handleIntegerMin(node, memoryModel, ctx, vars);

            case "sum":
                return handleIntegerSum(node, memoryModel, ctx, vars);

            case "signum":
                return handleIntegerSignum(node, memoryModel, ctx, vars);

            case "intValue":
                return handleIntValue(node, memoryModel, ctx, vars);

            case "valueOf":
                return handleIntegerValueOf(node, memoryModel, ctx, vars);

            /*
             * parseInt(String), toString(int), toHexString, toBinaryString...
             * Nếu argument là literal thì executeIntegerMethodNode đã xử lý rồi.
             * Nếu argument là symbolic String thì không nên mô phỏng bằng Z3 IntSort
             * vì dễ sai semantic Java.
             */
            case "parseInt":
                return handleIntegerParseInt(node, memoryModel, ctx, vars);

            case "toString":
                return handleIntegerToString(node, memoryModel, ctx, vars);

            case "toHexString":
                return handleIntegerToHexString(node, memoryModel, ctx, vars);

            case "toOctalString":
                return handleIntegerToOctalString(node, memoryModel, ctx, vars);

            case "toBinaryString":
                return handleIntegerToBinaryString(node, memoryModel, ctx, vars);

            case "bitCount":
                return handleIntegerBitCount(node, memoryModel, ctx, vars);

            default:
                throw new RuntimeException("Unsupported Integer method: " + node.methodName);
        }
    }

    private static IntExpr getIntExpr(AstNode astNode,
                                      MemoryModel memoryModel,
                                      Context ctx,
                                      List<Z3VariableWrapper> vars) {
        if (astNode == null) {
            throw new RuntimeException("Integer argument is null");
        }

        Expr expr = OperationExpressionNode.createZ3Expression(
                (ExpressionNode) astNode,
                ctx,
                vars,
                memoryModel
        );

        if (expr.getSort().equals(ctx.getIntSort())) {
            return (IntExpr) expr;
        }

        /*
         * Cho phép char được promote sang int.
         * Ví dụ: Integer.compare('a', 100)
         */
        if (expr.getSort().equals(ctx.mkCharSort())) {
            return ctx.charToInt((Expr<CharSort>) expr);
        }

        throw new RuntimeException("Expected IntSort expression but got: " + expr.getSort());
    }

    private static Expr handleIntegerCompare(IntegerMethodNode node,
                                             MemoryModel memoryModel,
                                             Context ctx,
                                             List<Z3VariableWrapper> vars) {
        IntExpr x = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);
        IntExpr y = getIntExpr(node.arguments.get(1), memoryModel, ctx, vars);

        return compareIntExpr(x, y, ctx);
    }

    private static Expr handleIntegerCompareTo(IntegerMethodNode node,
                                               MemoryModel memoryModel,
                                               Context ctx,
                                               List<Z3VariableWrapper> vars) {
        IntExpr target = getIntExpr(node.target, memoryModel, ctx, vars);
        IntExpr arg = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);

        return compareIntExpr(target, arg, ctx);
    }

    private static Expr handleIntegerEquals(IntegerMethodNode node,
                                            MemoryModel memoryModel,
                                            Context ctx,
                                            List<Z3VariableWrapper> vars) {
        IntExpr target = getIntExpr(node.target, memoryModel, ctx, vars);
        IntExpr arg = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);

        return ctx.mkEq(target, arg);
    }

    private static Expr handleIntegerMax(IntegerMethodNode node,
                                         MemoryModel memoryModel,
                                         Context ctx,
                                         List<Z3VariableWrapper> vars) {
        IntExpr x = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);
        IntExpr y = getIntExpr(node.arguments.get(1), memoryModel, ctx, vars);

        return ctx.mkITE(
                ctx.mkGe(x, y),
                x,
                y
        );
    }

    private static Expr handleIntegerMin(IntegerMethodNode node,
                                         MemoryModel memoryModel,
                                         Context ctx,
                                         List<Z3VariableWrapper> vars) {
        IntExpr x = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);
        IntExpr y = getIntExpr(node.arguments.get(1), memoryModel, ctx, vars);

        return ctx.mkITE(
                ctx.mkLe(x, y),
                x,
                y
        );
    }

    private static Expr handleIntegerSum(IntegerMethodNode node,
                                         MemoryModel memoryModel,
                                         Context ctx,
                                         List<Z3VariableWrapper> vars) {
        IntExpr x = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);
        IntExpr y = getIntExpr(node.arguments.get(1), memoryModel, ctx, vars);

        return ctx.mkAdd(x, y);
    }

    private static Expr handleIntegerSignum(IntegerMethodNode node,
                                            MemoryModel memoryModel,
                                            Context ctx,
                                            List<Z3VariableWrapper> vars) {
        IntExpr value = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);

        return ctx.mkITE(
                ctx.mkGt(value, ctx.mkInt(0)),
                ctx.mkInt(1),
                ctx.mkITE(
                        ctx.mkLt(value, ctx.mkInt(0)),
                        ctx.mkInt(-1),
                        ctx.mkInt(0)
                )
        );
    }

    private static Expr handleIntValue(IntegerMethodNode node,
                                       MemoryModel memoryModel,
                                       Context ctx,
                                       List<Z3VariableWrapper> vars) {
        return getIntExpr(node.target, memoryModel, ctx, vars);
    }

    private static Expr handleIntegerValueOf(IntegerMethodNode node,
                                             MemoryModel memoryModel,
                                             Context ctx,
                                             List<Z3VariableWrapper> vars) {
        /*
         * Symbolic chỉ support Integer.valueOf(int).
         * Integer.valueOf(String) đã được xử lý nếu String là literal.
         */
        if (node.arguments == null || node.arguments.size() < 1) {
            throw new RuntimeException("Integer.valueOf requires one argument");
        }

        return getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);
    }

    private static Expr compareIntExpr(IntExpr x, IntExpr y, Context ctx) {
        return ctx.mkITE(
                ctx.mkEq(x, y),
                ctx.mkInt(0),
                ctx.mkITE(
                        ctx.mkLt(x, y),
                        ctx.mkInt(-1),
                        ctx.mkInt(1)
                )
        );
    }

    private static final int MAX_PARSE_INT_CHARS = 33;
    private static final int MAX_DECIMAL_INT_DIGITS = 10;
    private static final String DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz";

    private static SeqExpr<CharSort> getStringExpr(AstNode astNode,
                                                   MemoryModel memoryModel,
                                                   Context ctx,
                                                   List<Z3VariableWrapper> vars) {
        if (astNode == null) {
            throw new RuntimeException("String argument is null");
        }

        Expr expr = OperationExpressionNode.createZ3Expression(
                (ExpressionNode) astNode,
                ctx,
                vars,
                memoryModel
        );

        if (expr.getSort().getSortKind() == Z3_sort_kind.Z3_SEQ_SORT) {
            return (SeqExpr<CharSort>) expr;
        }

        throw new RuntimeException("Expected String/Seq expression but got: " + expr.getSort());
    }

    private static SeqExpr<CharSort> str(Context ctx, String value) {
        return (SeqExpr<CharSort>) ctx.mkString(value);
    }

    private static SeqExpr<CharSort> concat(Context ctx,
                                            SeqExpr<CharSort> left,
                                            SeqExpr<CharSort> right) {
        return (SeqExpr<CharSort>) ctx.mkConcat(left, right);
    }

    private static SeqExpr<CharSort> iteString(Context ctx,
                                               BoolExpr condition,
                                               SeqExpr<CharSort> thenExpr,
                                               SeqExpr<CharSort> elseExpr) {
        return (SeqExpr<CharSort>) ctx.mkITE(condition, thenExpr, elseExpr);
    }

    private static IntExpr iteInt(Context ctx,
                                  BoolExpr condition,
                                  Expr thenExpr,
                                  Expr elseExpr) {
        return (IntExpr) ctx.mkITE(condition, thenExpr, elseExpr);
    }

    private static Expr handleIntegerParseInt(IntegerMethodNode node,
                                              MemoryModel memoryModel,
                                              Context ctx,
                                              List<Z3VariableWrapper> vars) {
        if (node.arguments == null || node.arguments.size() < 1) {
            throw new RuntimeException("Integer.parseInt requires one argument");
        }

        SeqExpr<CharSort> s = getStringExpr(node.arguments.get(0), memoryModel, ctx, vars);

        IntExpr radix;
        if (node.arguments.size() >= 2) {
            radix = getIntExpr(node.arguments.get(1), memoryModel, ctx, vars);
        } else {
            radix = ctx.mkInt(10);
        }

        return parseIntSymbolic(s, radix, ctx);
    }

    private static IntExpr parseIntSymbolic(SeqExpr<CharSort> s,
                                            IntExpr radix,
                                            Context ctx) {
        IntExpr len = ctx.mkLength(s);

        Expr<CharSort> first = (Expr<CharSort>) ctx.mkNth(s, ctx.mkInt(0));
        IntExpr firstCode = ctx.charToInt(first);

        BoolExpr hasFirst = ctx.mkGt(len, ctx.mkInt(0));
        BoolExpr isNegative = ctx.mkAnd(
                hasFirst,
                ctx.mkEq(firstCode, ctx.mkInt((int) '-'))
        );

        BoolExpr isPositive = ctx.mkAnd(
                hasFirst,
                ctx.mkEq(firstCode, ctx.mkInt((int) '+'))
        );

        BoolExpr hasSign = ctx.mkOr(isNegative, isPositive);

        IntExpr acc = ctx.mkInt(0);

        for (int i = 0; i < MAX_PARSE_INT_CHARS; i++) {
            Expr<CharSort> ch = (Expr<CharSort>) ctx.mkNth(s, ctx.mkInt(i));

            BoolExpr insideLength = ctx.mkGt(len, ctx.mkInt(i));

            BoolExpr activeDigit;
            if (i == 0) {
                activeDigit = ctx.mkAnd(
                        insideLength,
                        ctx.mkNot(hasSign)
                );
            } else {
                activeDigit = insideLength;
            }

            IntExpr rawDigit = charDigitValue(ch, ctx);

            BoolExpr validDigit = ctx.mkAnd(
                    ctx.mkGe(rawDigit, ctx.mkInt(0)),
                    ctx.mkLt(rawDigit, radix)
            );

            IntExpr digit = iteInt(
                    ctx,
                    validDigit,
                    rawDigit,
                    ctx.mkInt(0)
            );

            IntExpr nextAcc = (IntExpr) ctx.mkAdd(
                    ctx.mkMul(acc, radix),
                    digit
            );

            acc = iteInt(
                    ctx,
                    activeDigit,
                    nextAcc,
                    acc
            );
        }

        return iteInt(
                ctx,
                isNegative,
                ctx.mkSub(ctx.mkInt(0), acc),
                acc
        );
    }

    private static IntExpr charDigitValue(Expr<CharSort> ch, Context ctx) {
        IntExpr code = ctx.charToInt(ch);

        BoolExpr isDigit = ctx.mkAnd(
                ctx.mkGe(code, ctx.mkInt((int) '0')),
                ctx.mkLe(code, ctx.mkInt((int) '9'))
        );

        BoolExpr isUpper = ctx.mkAnd(
                ctx.mkGe(code, ctx.mkInt((int) 'A')),
                ctx.mkLe(code, ctx.mkInt((int) 'Z'))
        );

        BoolExpr isLower = ctx.mkAnd(
                ctx.mkGe(code, ctx.mkInt((int) 'a')),
                ctx.mkLe(code, ctx.mkInt((int) 'z'))
        );

        IntExpr digitValue = (IntExpr) ctx.mkSub(code, ctx.mkInt((int) '0'));

        IntExpr upperValue = (IntExpr) ctx.mkAdd(
                ctx.mkSub(code, ctx.mkInt((int) 'A')),
                ctx.mkInt(10)
        );

        IntExpr lowerValue = (IntExpr) ctx.mkAdd(
                ctx.mkSub(code, ctx.mkInt((int) 'a')),
                ctx.mkInt(10)
        );

        return iteInt(
                ctx,
                isDigit,
                digitValue,
                iteInt(
                        ctx,
                        isUpper,
                        upperValue,
                        iteInt(
                                ctx,
                                isLower,
                                lowerValue,
                                ctx.mkInt(-1)
                        )
                )
        );
    }

    private static Expr handleIntegerToString(IntegerMethodNode node,
                                              MemoryModel memoryModel,
                                              Context ctx,
                                              List<Z3VariableWrapper> vars) {
        IntExpr value;

        if (node.arguments != null && node.arguments.size() > 0) {
            // Integer.toString(x)
            value = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);
        } else {
            // x.toString()
            value = getIntExpr(node.target, memoryModel, ctx, vars);
        }

        return intToDecimalString(value, ctx);
    }

    private static SeqExpr<CharSort> intToDecimalString(IntExpr value, Context ctx) {
        BoolExpr isNegative = ctx.mkLt(value, ctx.mkInt(0));

        IntExpr absValue = iteInt(
                ctx,
                isNegative,
                ctx.mkSub(ctx.mkInt(0), value),
                value
        );

        SeqExpr<CharSort> body =
                unsignedIntToDecimalString(absValue, MAX_DECIMAL_INT_DIGITS, ctx);

        return iteString(
                ctx,
                isNegative,
                concat(ctx, str(ctx, "-"), body),
                body
        );
    }

    private static SeqExpr<CharSort> unsignedIntToDecimalString(IntExpr value,
                                                                int maxDigits,
                                                                Context ctx) {
        if (maxDigits <= 1) {
            return intDigitString(value, 10, ctx);
        }

        IntExpr div = (IntExpr) ctx.mkDiv(value, ctx.mkInt(10));
        IntExpr rem = (IntExpr) ctx.mkMod(value, ctx.mkInt(10));

        SeqExpr<CharSort> remString = intDigitString(rem, 10, ctx);
        SeqExpr<CharSort> head = unsignedIntToDecimalString(div, maxDigits - 1, ctx);

        SeqExpr<CharSort> full = concat(ctx, head, remString);

        return iteString(
                ctx,
                ctx.mkEq(div, ctx.mkInt(0)),
                remString,
                full
        );
    }

    private static SeqExpr<CharSort> intDigitString(IntExpr digit,
                                                    int radix,
                                                    Context ctx) {
        SeqExpr<CharSort> result = str(ctx, "0");

        for (int i = 1; i < radix; i++) {
            result = iteString(
                    ctx,
                    ctx.mkEq(digit, ctx.mkInt(i)),
                    str(ctx, String.valueOf(DIGITS.charAt(i))),
                    result
            );
        }

        return result;
    }

    private static Expr handleIntegerToHexString(IntegerMethodNode node,
                                                 MemoryModel memoryModel,
                                                 Context ctx,
                                                 List<Z3VariableWrapper> vars) {
        IntExpr value = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);
        BitVecExpr bv = intToBV32(value, ctx);

        return unsignedBVToHexString(bv, ctx);
    }

    private static Expr handleIntegerToOctalString(IntegerMethodNode node,
                                                   MemoryModel memoryModel,
                                                   Context ctx,
                                                   List<Z3VariableWrapper> vars) {
        IntExpr value = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);
        BitVecExpr bv = intToBV32(value, ctx);

        return unsignedBVToOctalString(bv, ctx);
    }

    private static Expr handleIntegerToBinaryString(IntegerMethodNode node,
                                                    MemoryModel memoryModel,
                                                    Context ctx,
                                                    List<Z3VariableWrapper> vars) {
        IntExpr value = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);
        BitVecExpr bv = intToBV32(value, ctx);

        return unsignedBVToBinaryString(bv, ctx);
    }

    private static Expr handleIntegerBitCount(IntegerMethodNode node,
                                              MemoryModel memoryModel,
                                              Context ctx,
                                              List<Z3VariableWrapper> vars) {
        IntExpr value = getIntExpr(node.arguments.get(0), memoryModel, ctx, vars);
        BitVecExpr bv = intToBV32(value, ctx);

        ArithExpr sum = ctx.mkInt(0);

        for (int i = 0; i < 32; i++) {
            BitVecExpr bit = (BitVecExpr) ctx.mkExtract(i, i, bv);

            Expr oneIfSet = ctx.mkITE(
                    ctx.mkEq(bit, ctx.mkBV(1, 1)),
                    ctx.mkInt(1),
                    ctx.mkInt(0)
            );

            sum = ctx.mkAdd(sum, (ArithExpr) oneIfSet);
        }

        return sum;
    }

    private static BitVecExpr intToBV32(IntExpr value, Context ctx) {
        return (BitVecExpr) ctx.mkInt2BV(32, value);
    }

    private static SeqExpr<CharSort> unsignedBVToBinaryString(BitVecExpr bv,
                                                              Context ctx) {
        List<int[]> groups = new ArrayList<>();

        for (int i = 0; i < 32; i++) {
            groups.add(new int[]{i, i});
        }

        return unsignedBVToBaseString(bv, groups, 2, ctx);
    }

    private static SeqExpr<CharSort> unsignedBVToHexString(BitVecExpr bv,
                                                           Context ctx) {
        List<int[]> groups = new ArrayList<>();

        for (int low = 0; low < 32; low += 4) {
            groups.add(new int[]{low + 3, low});
        }

        return unsignedBVToBaseString(bv, groups, 16, ctx);
    }

    private static SeqExpr<CharSort> unsignedBVToOctalString(BitVecExpr bv,
                                                             Context ctx) {
        List<int[]> groups = new ArrayList<>();

        for (int low = 0; low <= 27; low += 3) {
            groups.add(new int[]{low + 2, low});
        }

        // group cao nhất chỉ có 2 bit: bit 31..30
        groups.add(new int[]{31, 30});

        return unsignedBVToBaseString(bv, groups, 8, ctx);
    }

    /*
     * groups truyền từ thấp lên cao.
     * Hàm này tự strip leading zero.
     */
    private static SeqExpr<CharSort> unsignedBVToBaseString(BitVecExpr bv,
                                                            List<int[]> groupsLowToHigh,
                                                            int radix,
                                                            Context ctx) {
        SeqExpr<CharSort> result = str(ctx, "0");
        SeqExpr<CharSort> suffix = str(ctx, "");

        for (int i = 0; i < groupsLowToHigh.size(); i++) {
            int high = groupsLowToHigh.get(i)[0];
            int low = groupsLowToHigh.get(i)[1];

            BitVecExpr digit = (BitVecExpr) ctx.mkExtract(high, low, bv);

            SeqExpr<CharSort> digitString = bvDigitString(digit, radix, ctx);
            SeqExpr<CharSort> fixedFromHere = concat(ctx, digitString, suffix);

            result = iteString(
                    ctx,
                    ctx.mkNot(isBVZero(digit, ctx)),
                    fixedFromHere,
                    result
            );

            suffix = fixedFromHere;
        }

        return result;
    }

    private static BoolExpr isBVZero(BitVecExpr bv, Context ctx) {
        int size = ((BitVecSort) bv.getSort()).getSize();
        return ctx.mkEq(bv, ctx.mkBV(0, size));
    }

    private static SeqExpr<CharSort> bvDigitString(BitVecExpr digit,
                                                   int radix,
                                                   Context ctx) {
        int size = ((BitVecSort) digit.getSort()).getSize();

        int maxValueBySize = (1 << size) - 1;
        int maxDigit = Math.min(radix - 1, maxValueBySize);

        SeqExpr<CharSort> result = str(ctx, "0");

        for (int i = 1; i <= maxDigit; i++) {
            result = iteString(
                    ctx,
                    ctx.mkEq(digit, ctx.mkBV(i, size)),
                    str(ctx, String.valueOf(DIGITS.charAt(i))),
                    result
            );
        }

        return result;
    }


}