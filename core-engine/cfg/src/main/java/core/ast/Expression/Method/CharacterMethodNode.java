package core.ast.Expression.Method;

import com.microsoft.z3.*;
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

public class CharacterMethodNode extends MethodInvocationNode {
    public String ownerName;
    public String methodName;
    public List<AstNode> arguments;
    public AstNode target;

    public static AstNode executeCharacterMethod(MethodInvocation methodInvocation,
                                                 MemoryModel memoryModel) {
        CharacterMethodNode characterMethodNode = new CharacterMethodNode();

        characterMethodNode.methodName = methodInvocation.getName().toString();

        List<AstNode> arguments = new ArrayList<>();
        for (int i = 0; i < methodInvocation.arguments().size(); i++) {
            AstNode argNode = ExpressionNode.executeExpression(
                    (Expression) methodInvocation.arguments().get(i),
                    memoryModel
            );
            arguments.add(argNode);
        }
        characterMethodNode.arguments = arguments;

        Expression expression = methodInvocation.getExpression();

        if (expression != null) {
            characterMethodNode.ownerName = expression.toString();

            // Character.isDigit(c), Character.toUpperCase(c)
            // Không nên executeExpression("Character") vì Character không phải biến.
            if (!"Character".equals(characterMethodNode.ownerName)) {
                characterMethodNode.target =
                        ExpressionNode.executeExpression(expression, memoryModel);
            }
        }

        ExpressionNode expressionNode = executeCharacterMethodNode(characterMethodNode, memoryModel);

        return expressionNode;
    }

    public static ExpressionNode executeCharacterMethodNode(CharacterMethodNode characterMethodNode,
                                                            MemoryModel memoryModel) {
        String methodName = characterMethodNode.methodName;
        List<AstNode> arguments = characterMethodNode.arguments;
        AstNode target = characterMethodNode.target;

        try {
            // =========================================================
            // Case 1: static method: Character.isDigit(c)
            // =========================================================
            if ("Character".equals(characterMethodNode.ownerName)) {
                switch (methodName) {
                    case "isDigit": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        BooleanLiteralNode result = new BooleanLiteralNode();
                        result.setValue(Character.isDigit(ch));
                        return result;
                    }

                    case "isLetter": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        BooleanLiteralNode result = new BooleanLiteralNode();
                        result.setValue(Character.isLetter(ch));
                        return result;
                    }

                    case "isLetterOrDigit": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        BooleanLiteralNode result = new BooleanLiteralNode();
                        result.setValue(Character.isLetterOrDigit(ch));
                        return result;
                    }

                    case "isUpperCase": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        BooleanLiteralNode result = new BooleanLiteralNode();
                        result.setValue(Character.isUpperCase(ch));
                        return result;
                    }

                    case "isLowerCase": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        BooleanLiteralNode result = new BooleanLiteralNode();
                        result.setValue(Character.isLowerCase(ch));
                        return result;
                    }

                    case "isWhitespace": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        BooleanLiteralNode result = new BooleanLiteralNode();
                        result.setValue(Character.isWhitespace(ch));
                        return result;
                    }

                    case "isSpaceChar": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        BooleanLiteralNode result = new BooleanLiteralNode();
                        result.setValue(Character.isSpaceChar(ch));
                        return result;
                    }

                    case "toUpperCase": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        return new CharacterLiteralNode(Character.toUpperCase(ch));
                    }

                    case "toLowerCase": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        return new CharacterLiteralNode(Character.toLowerCase(ch));
                    }

                    case "toTitleCase": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        return newCharacterLiteralNode(Character.toTitleCase(ch));
                    }

                    case "compare": {
                        if (arguments.size() < 2) return characterMethodNode;

                        Character ch1 = getCharValue(arguments.get(0));
                        Character ch2 = getCharValue(arguments.get(1));

                        if (ch1 == null || ch2 == null) return characterMethodNode;

                        return new IntegerLiteralNode(Character.compare(ch1, ch2));
                    }

                    case "getNumericValue": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        return new IntegerLiteralNode(Character.getNumericValue(ch));
                    }

                    case "digit": {
                        if (arguments.size() < 2) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        Integer radix = getIntValue(arguments.get(1));

                        if (ch == null || radix == null) return characterMethodNode;

                        return new IntegerLiteralNode(Character.digit(ch, radix));
                    }

                    case "toString": {
                        if (arguments.size() < 1) return characterMethodNode;

                        Character ch = getCharValue(arguments.get(0));
                        if (ch == null) return characterMethodNode;

                        StringLiteralNode result = new StringLiteralNode();
                        result.setStringValue(Character.toString(ch));
                        return result;
                    }

                    default:
                        throw new RuntimeException("Unsupported Character method: " + methodName);
                }
            }

            // =========================================================
            // Case 2: instance method nếu bạn có Character object literal
            // Ví dụ: ch.charValue(), ch.compareTo('a'), ch.toString()
            // =========================================================
            Character targetValue = getCharValue(target);
            if (targetValue == null) {
                return characterMethodNode;
            }

            switch (methodName) {
                case "charValue": {
                    return new CharacterLiteralNode(targetValue);
                }

                case "toString": {
                    StringLiteralNode result = new StringLiteralNode();
                    result.setStringValue(Character.toString(targetValue));
                    return result;
                }

                case "compareTo": {
                    if (arguments.size() < 1) return characterMethodNode;

                    Character arg = getCharValue(arguments.get(0));
                    if (arg == null) return characterMethodNode;

                    return new IntegerLiteralNode(Character.compare(targetValue, arg));
                }

                case "equals": {
                    if (arguments.size() < 1) return characterMethodNode;

                    Character arg = getCharValue(arguments.get(0));
                    if (arg == null) return characterMethodNode;

                    BooleanLiteralNode result = new BooleanLiteralNode();
                    result.setValue(targetValue.equals(arg));
                    return result;
                }

                default:
                    throw new RuntimeException("Unsupported Character method: " + methodName);
            }

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Character getCharValue(AstNode node) {
        if (node == null) return null;

        if (node instanceof CharacterLiteralNode) {
            return ((CharacterLiteralNode) node).getCharacterValue();
        }

        // Trường hợp String.charAt(...) hiện tại của bạn đang trả về StringLiteralNode độ dài 1
        if (node instanceof StringLiteralNode) {
            String value = ((StringLiteralNode) node).getStringValue();

            if (value.length() == 1) {
                return value.charAt(0);
            }

            return null;
        }

        if (node instanceof NumberLiteralNode) {
            int value = Integer.parseInt(((NumberLiteralNode) node).getTokenValue());

            if (value >= Character.MIN_VALUE && value <= Character.MAX_VALUE) {
                return (char) value;
            }
        }

        return null;
    }

    private static Integer getIntValue(AstNode node) {
        if (node == null) return null;

        if (node instanceof NumberLiteralNode) {
            return Integer.parseInt(((NumberLiteralNode) node).getTokenValue());
        }

        if (node instanceof CharacterLiteralNode) {
            return (int) ((CharacterLiteralNode) node).getCharacterValue();
        }

        if (node instanceof StringLiteralNode) {
            String value = ((StringLiteralNode) node).getStringValue();

            if (value.length() == 1) {
                return (int) value.charAt(0);
            }
        }

        return null;
    }

    private static CharacterLiteralNode newCharacterLiteralNode(char value) {
        CharacterLiteralNode result = new CharacterLiteralNode();
        result.setCharacterValue(value);
        return result;
    }

    public static Expr createZ3Expression(CharacterMethodNode node,
                                          MemoryModel memoryModel,
                                          Context ctx,
                                          List<Z3VariableWrapper> vars) {
        switch (node.methodName) {
            case "isDigit":
                return handleIsDigit(node, memoryModel, ctx, vars);

            case "isLetter":
                return handleIsLetter(node, memoryModel, ctx, vars);

            case "isLetterOrDigit":
                return handleIsLetterOrDigit(node, memoryModel, ctx, vars);

            case "isUpperCase":
                return handleIsUpperCase(node, memoryModel, ctx, vars);

            case "isLowerCase":
                return handleIsLowerCase(node, memoryModel, ctx, vars);

            case "isWhitespace":
                return handleIsWhitespace(node, memoryModel, ctx, vars);

            case "toUpperCase":
                return handleCharacterToUpperCase(node, memoryModel, ctx, vars);

            case "toLowerCase":
                return handleCharacterToLowerCase(node, memoryModel, ctx, vars);

            case "compare":
                return handleCharacterCompare(node, memoryModel, ctx, vars);

            case "compareTo":
                return handleCharacterCompareTo(node, memoryModel, ctx, vars);

            case "equals":
                return handleCharacterEquals(node, memoryModel, ctx, vars);

            case "getNumericValue":
                return handleGetNumericValue(node, memoryModel, ctx, vars);

            case "digit":
                return handleDigit(node, memoryModel, ctx, vars);

            case "toString":
                return handleCharacterToString(node, memoryModel, ctx, vars);

            case "charValue":
                return handleCharValue(node, memoryModel, ctx, vars);

            default:
                throw new RuntimeException("Unsupported Character method: " + node.methodName);
        }
    }

    private static Expr<CharSort> getCharExpr(AstNode astNode,
                                              MemoryModel memoryModel,
                                              Context ctx,
                                              List<Z3VariableWrapper> vars) {
        if (astNode == null) {
            throw new RuntimeException("Character argument is null");
        }

        Expr expr = OperationExpressionNode.createZ3Expression(
                (ExpressionNode) astNode,
                ctx,
                vars,
                memoryModel
        );

        if (expr.getSort().equals(ctx.mkCharSort())) {
            return (Expr<CharSort>) expr;
        }

        /*
         * Trường hợp tạm thời nếu char literal của bạn đang bị parse thành StringLiteralNode
         * hoặc String.charAt(...) cũ từng trả StringLiteralNode độ dài 1.
         */
        if (astNode instanceof StringLiteralNode) {
            String value = ((StringLiteralNode) astNode).getStringValue();

            if (value.length() == 1) {
                SeqExpr<CharSort> stringExpr = (SeqExpr<CharSort>) expr;
                return (Expr<CharSort>) ctx.mkNth(stringExpr, ctx.mkInt(0));
            }
        }

        throw new RuntimeException("Expected CharSort expression but got: " + expr.getSort());
    }

    private static IntExpr charCode(Expr<CharSort> ch, Context ctx) {
        return ctx.charToInt(ch);
    }

    private static BoolExpr isAsciiDigit(Expr<CharSort> ch, Context ctx) {
        IntExpr code = charCode(ch, ctx);

        return ctx.mkAnd(
                ctx.mkGe(code, ctx.mkInt((int) '0')),
                ctx.mkLe(code, ctx.mkInt((int) '9'))
        );
    }

    private static BoolExpr isAsciiUpper(Expr<CharSort> ch, Context ctx) {
        IntExpr code = charCode(ch, ctx);

        return ctx.mkAnd(
                ctx.mkGe(code, ctx.mkInt((int) 'A')),
                ctx.mkLe(code, ctx.mkInt((int) 'Z'))
        );
    }

    private static BoolExpr isAsciiLower(Expr<CharSort> ch, Context ctx) {
        IntExpr code = charCode(ch, ctx);

        return ctx.mkAnd(
                ctx.mkGe(code, ctx.mkInt((int) 'a')),
                ctx.mkLe(code, ctx.mkInt((int) 'z'))
        );
    }

    private static BoolExpr isAsciiLetter(Expr<CharSort> ch, Context ctx) {
        return ctx.mkOr(
                isAsciiUpper(ch, ctx),
                isAsciiLower(ch, ctx)
        );
    }

    private static Expr handleIsDigit(CharacterMethodNode node,
                                      MemoryModel memoryModel,
                                      Context ctx,
                                      List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);
        return isAsciiDigit(ch, ctx);
    }

    private static Expr handleIsLetter(CharacterMethodNode node,
                                       MemoryModel memoryModel,
                                       Context ctx,
                                       List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);
        return isAsciiLetter(ch, ctx);
    }

    private static Expr handleIsLetterOrDigit(CharacterMethodNode node,
                                              MemoryModel memoryModel,
                                              Context ctx,
                                              List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);

        return ctx.mkOr(
                isAsciiLetter(ch, ctx),
                isAsciiDigit(ch, ctx)
        );
    }

    private static Expr handleIsUpperCase(CharacterMethodNode node,
                                          MemoryModel memoryModel,
                                          Context ctx,
                                          List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);
        return isAsciiUpper(ch, ctx);
    }

    private static Expr handleIsLowerCase(CharacterMethodNode node,
                                          MemoryModel memoryModel,
                                          Context ctx,
                                          List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);
        return isAsciiLower(ch, ctx);
    }

    private static Expr handleIsWhitespace(CharacterMethodNode node,
                                           MemoryModel memoryModel,
                                           Context ctx,
                                           List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);
        IntExpr code = charCode(ch, ctx);

        return ctx.mkOr(
                ctx.mkEq(code, ctx.mkInt((int) ' ')),
                ctx.mkEq(code, ctx.mkInt((int) '\t')),
                ctx.mkEq(code, ctx.mkInt((int) '\n')),
                ctx.mkEq(code, ctx.mkInt((int) '\r')),
                ctx.mkEq(code, ctx.mkInt((int) '\f')),
                ctx.mkEq(code, ctx.mkInt(0x0B)) // vertical tab
        );
    }

    private static Expr handleCharacterToUpperCase(CharacterMethodNode node,
                                                   MemoryModel memoryModel,
                                                   Context ctx,
                                                   List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);
        return upperAsciiChar(ch, ctx);
    }

    private static Expr handleCharacterToLowerCase(CharacterMethodNode node,
                                                   MemoryModel memoryModel,
                                                   Context ctx,
                                                   List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);
        return lowerAsciiChar(ch, ctx);
    }

    public static Expr<CharSort> lowerAsciiChar(Expr<CharSort> ch, Context ctx) {
        IntExpr code = ctx.charToInt(ch);

        BoolExpr isUpper = ctx.mkAnd(
                ctx.mkGe(code, ctx.mkInt((int) 'A')),
                ctx.mkLe(code, ctx.mkInt((int) 'Z'))
        );

        BitVecExpr bv = ctx.charToBv(ch);
        int bvSize = ((BitVecSort) bv.getSort()).getSize();

        Expr<CharSort> lowered =
                ctx.charFromBv(ctx.mkBVAdd(bv, ctx.mkBV(32, bvSize)));

        return (Expr<CharSort>) ctx.mkITE(isUpper, lowered, ch);
    }

    public static Expr<CharSort> upperAsciiChar(Expr<CharSort> ch, Context ctx) {
        IntExpr code = ctx.charToInt(ch);

        BoolExpr isLower = ctx.mkAnd(
                ctx.mkGe(code, ctx.mkInt((int) 'a')),
                ctx.mkLe(code, ctx.mkInt((int) 'z'))
        );

        BitVecExpr bv = ctx.charToBv(ch);
        int bvSize = ((BitVecSort) bv.getSort()).getSize();

        Expr<CharSort> uppered =
                ctx.charFromBv(ctx.mkBVSub(bv, ctx.mkBV(32, bvSize)));

        return (Expr<CharSort>) ctx.mkITE(isLower, uppered, ch);
    }

    private static Expr handleCharacterCompare(CharacterMethodNode node,
                                               MemoryModel memoryModel,
                                               Context ctx,
                                               List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch1 = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);
        Expr<CharSort> ch2 = getCharExpr(node.arguments.get(1), memoryModel, ctx, vars);

        return ctx.mkSub(
                charCode(ch1, ctx),
                charCode(ch2, ctx)
        );
    }

    private static Expr handleCharacterCompareTo(CharacterMethodNode node,
                                                 MemoryModel memoryModel,
                                                 Context ctx,
                                                 List<Z3VariableWrapper> vars) {
        Expr<CharSort> target = getCharExpr(node.target, memoryModel, ctx, vars);
        Expr<CharSort> arg = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);

        return ctx.mkSub(
                charCode(target, ctx),
                charCode(arg, ctx)
        );
    }

    private static Expr handleCharacterEquals(CharacterMethodNode node,
                                              MemoryModel memoryModel,
                                              Context ctx,
                                              List<Z3VariableWrapper> vars) {
        Expr<CharSort> target = getCharExpr(node.target, memoryModel, ctx, vars);
        Expr<CharSort> arg = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);

        return ctx.mkEq(target, arg);
    }

    private static Expr handleCharValue(CharacterMethodNode node,
                                        MemoryModel memoryModel,
                                        Context ctx,
                                        List<Z3VariableWrapper> vars) {
        return getCharExpr(node.target, memoryModel, ctx, vars);
    }

    private static Expr handleGetNumericValue(CharacterMethodNode node,
                                              MemoryModel memoryModel,
                                              Context ctx,
                                              List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);
        IntExpr code = charCode(ch, ctx);

        IntExpr digitValue = (IntExpr) ctx.mkSub(code, ctx.mkInt((int) '0'));
        IntExpr upperValue = (IntExpr) ctx.mkAdd(
                ctx.mkSub(code, ctx.mkInt((int) 'A')),
                ctx.mkInt(10)
        );
        IntExpr lowerValue = (IntExpr) ctx.mkAdd(
                ctx.mkSub(code, ctx.mkInt((int) 'a')),
                ctx.mkInt(10)
        );

        return ctx.mkITE(
                isAsciiDigit(ch, ctx),
                digitValue,
                ctx.mkITE(
                        isAsciiUpper(ch, ctx),
                        upperValue,
                        ctx.mkITE(
                                isAsciiLower(ch, ctx),
                                lowerValue,
                                ctx.mkInt(-1)
                        )
                )
        );
    }

    private static Expr handleDigit(CharacterMethodNode node,
                                    MemoryModel memoryModel,
                                    Context ctx,
                                    List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);

        IntExpr radix = (IntExpr) OperationExpressionNode.createZ3Expression(
                (ExpressionNode) node.arguments.get(1),
                ctx,
                vars,
                memoryModel
        );

        IntExpr numericValue = (IntExpr) handleGetNumericValueFromChar(ch, ctx);

        BoolExpr validRadix = ctx.mkAnd(
                ctx.mkGe(radix, ctx.mkInt(2)),
                ctx.mkLe(radix, ctx.mkInt(36))
        );

        BoolExpr validDigit = ctx.mkAnd(
                ctx.mkGe(numericValue, ctx.mkInt(0)),
                ctx.mkLt(numericValue, radix)
        );

        return ctx.mkITE(
                ctx.mkAnd(validRadix, validDigit),
                numericValue,
                ctx.mkInt(-1)
        );
    }

    private static Expr handleGetNumericValueFromChar(Expr<CharSort> ch, Context ctx) {
        IntExpr code = charCode(ch, ctx);

        IntExpr digitValue = (IntExpr) ctx.mkSub(code, ctx.mkInt((int) '0'));
        IntExpr upperValue = (IntExpr) ctx.mkAdd(
                ctx.mkSub(code, ctx.mkInt((int) 'A')),
                ctx.mkInt(10)
        );
        IntExpr lowerValue = (IntExpr) ctx.mkAdd(
                ctx.mkSub(code, ctx.mkInt((int) 'a')),
                ctx.mkInt(10)
        );

        return ctx.mkITE(
                isAsciiDigit(ch, ctx),
                digitValue,
                ctx.mkITE(
                        isAsciiUpper(ch, ctx),
                        upperValue,
                        ctx.mkITE(
                                isAsciiLower(ch, ctx),
                                lowerValue,
                                ctx.mkInt(-1)
                        )
                )
        );
    }

    private static Expr handleCharacterToString(CharacterMethodNode node,
                                                MemoryModel memoryModel,
                                                Context ctx,
                                                List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch;

        if (node.arguments != null && node.arguments.size() > 0) {
            // Character.toString(ch)
            ch = getCharExpr(node.arguments.get(0), memoryModel, ctx, vars);
        } else {
            // characterObj.toString()
            ch = getCharExpr(node.target, memoryModel, ctx, vars);
        }

        return ctx.mkUnit(ch);
    }
}
