package core.ast.Expression.Method;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.BooleanLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.Literal.StringLiteralNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.ast.Type.AnnotatableType.SimpleTypeNode;
import core.symbolicExecution.MemoryModel;
import core.symbolicExecution.UnsatPathException;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class StringMethodNode extends MethodInvocationNode {
    private AstNode target; // phần trước method s
    private String methodName; // concat
    private List<AstNode> arguments; // "abc" / a / b
    public final static int BOUND_LENGTH = 4;

    public static AstNode executeStringMethod(MethodInvocation methodInvocation, MemoryModel memoryModel) {
        StringMethodNode stringMethodNode = new StringMethodNode();

        stringMethodNode. methodName = methodInvocation.getName().toString(); // tên method
        List<AstNode> arguments = new ArrayList<>();
        for (int i = 0; i < methodInvocation.arguments().size(); i++) {
            AstNode argNode = ExpressionNode.executeExpression((Expression) methodInvocation.arguments().get(i), memoryModel);
            arguments.add(argNode);
        }
        stringMethodNode.arguments = arguments; // tham số
        stringMethodNode.target = ExpressionNode.executeExpression(methodInvocation.getExpression(), memoryModel); // phần trước method

        // giải nghĩa node với những node là method của StringLiteralNode
        ExpressionNode expressionNode = executeStringMethodNode(stringMethodNode, memoryModel);
        return expressionNode;
    }

    public static ExpressionNode executeStringMethodNode(StringMethodNode stringMethodNode, MemoryModel memoryModel) {
        AstNode target = stringMethodNode.target;
        String methodName = stringMethodNode.methodName;
        List<AstNode> arguments = stringMethodNode.arguments;

        if (!(target instanceof StringLiteralNode)) {
            return stringMethodNode;
        }

        String targetValue = ((StringLiteralNode) target).getStringValue();

        try {
            switch (methodName) {
                case "length": {
                    return new IntegerLiteralNode(targetValue.length());
                }
                case "isEmpty": {
                    BooleanLiteralNode result = new BooleanLiteralNode();
                    result.setValue(targetValue.isEmpty());
                    return result;
                }
                case "isBlank": {
                    BooleanLiteralNode result = new BooleanLiteralNode();
                    result.setValue(targetValue.isBlank());
                    return result;
                }
                case "contains": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof StringLiteralNode))
                        return stringMethodNode;
                    String arg = ((StringLiteralNode) arguments.get(0)).getStringValue();
                    BooleanLiteralNode result = new BooleanLiteralNode();
                    result.setValue(targetValue.contains(arg));
                    return result;
                }
                case "equals": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof StringLiteralNode))
                        return stringMethodNode;
                    String arg = ((StringLiteralNode) arguments.get(0)).getStringValue();
                    BooleanLiteralNode result = new BooleanLiteralNode();
                    result.setValue(targetValue.equals(arg));
                    return result;
                }
                case "equalsIgnoreCase": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof StringLiteralNode))
                        return stringMethodNode;
                    String arg = ((StringLiteralNode) arguments.get(0)).getStringValue();
                    BooleanLiteralNode result = new BooleanLiteralNode();
                    result.setValue(targetValue.equalsIgnoreCase(arg));
                    return result;
                }
                case "concat": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof StringLiteralNode))
                        return stringMethodNode;
                    String arg = ((StringLiteralNode) arguments.get(0)).getStringValue();
                    StringLiteralNode result = new StringLiteralNode();
                    result.setStringValue(targetValue.concat(arg));
                    return result;
                }
                case "substring": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof NumberLiteralNode))
                        return stringMethodNode;
                    int start = Integer.parseInt(((NumberLiteralNode) arguments.get(0)).getTokenValue());
                    if (arguments.size() == 2) {
                        if (!(arguments.get(1) instanceof NumberLiteralNode))
                            return stringMethodNode;
                        int end = Integer.parseInt(((NumberLiteralNode) arguments.get(1)).getTokenValue());
                        StringLiteralNode result = new StringLiteralNode();
                        result.setStringValue(targetValue.substring(start, end));
                        return result;
                    }
                    StringLiteralNode result = new StringLiteralNode();
                    result.setStringValue(targetValue.substring(start));
                    return result;
                }
                case "indexOf": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof StringLiteralNode))
                        return stringMethodNode;

                    String arg = ((StringLiteralNode) arguments.get(0)).getStringValue();

                    if (arguments.size() >= 2) {
                        if (!(arguments.get(1) instanceof NumberLiteralNode))
                            return stringMethodNode;

                        int fromIndex = Integer.parseInt(((NumberLiteralNode) arguments.get(1)).getTokenValue());
                        return new IntegerLiteralNode(targetValue.indexOf(arg, fromIndex));
                    }

                    return new IntegerLiteralNode(targetValue.indexOf(arg));
                }
                case "lastIndexOf": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof StringLiteralNode))
                        return stringMethodNode;

                    String arg = ((StringLiteralNode) arguments.get(0)).getStringValue();

                    if (arguments.size() >= 2) {
                        if (!(arguments.get(1) instanceof NumberLiteralNode))
                            return stringMethodNode;

                        int fromIndex = Integer.parseInt(((NumberLiteralNode) arguments.get(1)).getTokenValue());
                        return new IntegerLiteralNode(targetValue.lastIndexOf(arg, fromIndex));
                    }

                    return new IntegerLiteralNode(targetValue.lastIndexOf(arg));
                }
                case "replace": {
                    if (arguments.size() < 2
                            || !(arguments.get(0) instanceof StringLiteralNode)
                            || !(arguments.get(1) instanceof StringLiteralNode))
                        return stringMethodNode;
                    String oldChar = ((StringLiteralNode) arguments.get(0)).getStringValue();
                    String newChar = ((StringLiteralNode) arguments.get(1)).getStringValue();
                    StringLiteralNode result = new StringLiteralNode();
                    result.setStringValue(targetValue.replace(oldChar, newChar));
                    return result;
                }
                case "replaceAll": {
                    if (arguments.size() < 2
                            || !(arguments.get(0) instanceof StringLiteralNode)
                            || !(arguments.get(1) instanceof StringLiteralNode))
                        return stringMethodNode;
                    String regex = ((StringLiteralNode) arguments.get(0)).getStringValue();
                    String replacement = ((StringLiteralNode) arguments.get(1)).getStringValue();
                    StringLiteralNode result = new StringLiteralNode();
                    result.setStringValue(targetValue.replaceAll(regex, replacement));
                    return result;
                }
                case "toUpperCase": {
                    StringLiteralNode result = new StringLiteralNode();
                    result.setStringValue(targetValue.toUpperCase());
                    return result;
                }
                case "toLowerCase": {
                    StringLiteralNode result = new StringLiteralNode();
                    result.setStringValue(targetValue.toLowerCase());
                    return result;
                }
                case "trim": {
                    StringLiteralNode result = new StringLiteralNode();
                    result.setStringValue(targetValue.trim());
                    return result;
                }
                case "startsWith": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof StringLiteralNode))
                        return stringMethodNode;

                    String prefix = ((StringLiteralNode) arguments.get(0)).getStringValue();

                    BooleanLiteralNode result = new BooleanLiteralNode();

                    if (arguments.size() >= 2) {
                        if (!(arguments.get(1) instanceof NumberLiteralNode))
                            return stringMethodNode;

                        int offset = Integer.parseInt(((NumberLiteralNode) arguments.get(1)).getTokenValue());
                        result.setValue(targetValue.startsWith(prefix, offset));
                    } else {
                        result.setValue(targetValue.startsWith(prefix));
                    }

                    return result;
                }
                case "endsWith": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof StringLiteralNode))
                        return stringMethodNode;
                    String arg = ((StringLiteralNode) arguments.get(0)).getStringValue();
                    BooleanLiteralNode result = new BooleanLiteralNode();
                    result.setValue(targetValue.endsWith(arg));
                    return result;
                }
                case "charAt": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof NumberLiteralNode))
                        return stringMethodNode;
                    int index = Integer.parseInt(((NumberLiteralNode) arguments.get(0)).getTokenValue());
                    StringLiteralNode result = new StringLiteralNode();
                    if (index < 0 || index >= targetValue.length()) {
                        throw new UnsatPathException("index out of bounds");
                    }
                    result.setStringValue(String.valueOf(targetValue.charAt(index)));
                    return result;
                }
                case "compareTo": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof StringLiteralNode))
                        return stringMethodNode;

                    String arg = ((StringLiteralNode) arguments.get(0)).getStringValue();

                    return new IntegerLiteralNode(targetValue.compareTo(arg));
                }

                case "compareToIgnoreCase": {
                    if (arguments.size() < 1 || !(arguments.get(0) instanceof StringLiteralNode))
                        return stringMethodNode;

                    String arg = ((StringLiteralNode) arguments.get(0)).getStringValue();

                    return new IntegerLiteralNode(targetValue.compareToIgnoreCase(arg));
                }
                default:
                    throw new RuntimeException("Unsupported String method: " + methodName);
            }
        } catch (Exception e) {
            //throw new UnsatPathException("Expression is UNSATISFIABLE");
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Expr createZ3Expression(StringMethodNode node, MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        SeqExpr<CharSort> targetExpr = (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression((ExpressionNode) node.target, ctx, vars, memoryModel);

        switch (node.methodName) {
            case "length":
                return ctx.mkLength(targetExpr);
            case "isEmpty":
                return ctx.mkEq(ctx.mkLength(targetExpr), ctx.mkInt(0));
            case "isBlank":
                return handleIsBlank(targetExpr, ctx);
            case "concat":
                return handleConcat(node, targetExpr, memoryModel, ctx, vars);
            case "contains":
                return handleContains(node, targetExpr, memoryModel, ctx, vars);
            case "startsWith":
                return handleStartsWith(node, targetExpr, memoryModel, ctx, vars);
            case "endsWith":
                return handleEndsWith(node, targetExpr, memoryModel, ctx, vars);
            case "equals":
                return handleEquals(node, targetExpr, memoryModel, ctx, vars);
            case "equalsIgnoreCase":
                return handleEqualsIgnoreCase(node, targetExpr, memoryModel, ctx, vars);
            case "indexOf":
                return handleIndexOf(node, targetExpr, memoryModel, ctx, vars);
            case "lastIndexOf":
                return handleLastIndexOf(node, targetExpr, memoryModel, ctx, vars);
            case "substring":
                return handleSubstring(node, targetExpr, memoryModel, ctx, vars);
            case "charAt":
                return handleCharAt(node, targetExpr, memoryModel, ctx, vars);
            case "replace":
                return handleReplace(node, targetExpr, memoryModel, ctx, vars);
            case "replaceAll":
                return handleReplaceAll(node, targetExpr, memoryModel, ctx, vars);
            case "toUpperCase":
                return handleUpperCase(targetExpr, ctx);
            case "toLowerCase":
                return handleLowerCase(targetExpr, ctx);
            case "trim":
                return handleTrim(targetExpr, ctx);
            case "compareTo":
                return handleCompareTo(node, targetExpr, memoryModel, ctx, vars);
            case "compareToIgnoreCase":
                return handleCompareToIgnoreCase(node, targetExpr, memoryModel, ctx, vars);
            default:
                throw new RuntimeException("Unsupported String method: " + node.methodName);
        }
    }

// ========== HANDLE METHODS ==========

    private static Expr handleConcat(StringMethodNode node, SeqExpr<CharSort> target,
                                     MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) node.arguments.get(0);
        SeqExpr<CharSort> argExpr = (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);
        return ctx.mkConcat(target, argExpr);
    }

    private static Expr handleContains(StringMethodNode node, SeqExpr<CharSort> target,
                                       MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) node.arguments.get(0);
        SeqExpr<CharSort> argExpr = (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);
        return ctx.mkContains(target, argExpr);
    }

    private static Expr handleStartsWith(StringMethodNode node, SeqExpr<CharSort> target,
                                         MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode prefixNode = (ExpressionNode) node.arguments.get(0);
        SeqExpr<CharSort> prefixExpr =
                (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(prefixNode, ctx, vars, memoryModel);

        if (node.arguments.size() == 1) {
            return ctx.mkPrefixOf(prefixExpr, target);
        }

        ExpressionNode offsetNode = (ExpressionNode) node.arguments.get(1);
        IntExpr offsetExpr =
                (IntExpr) OperationExpressionNode.createZ3Expression(offsetNode, ctx, vars, memoryModel);

        IntExpr len = ctx.mkLength(target);
        IntExpr prefixLen = ctx.mkLength(prefixExpr);

        BoolExpr validOffset = ctx.mkAnd(
                ctx.mkGe(offsetExpr, ctx.mkInt(0)),
                ctx.mkLe(offsetExpr, len),
                ctx.mkLe(ctx.mkAdd(offsetExpr, prefixLen), len)
        );

        SeqExpr<CharSort> subTarget =
                ctx.mkExtract(target, offsetExpr, ctx.mkSub(len, offsetExpr));

        return ctx.mkITE(
                validOffset,
                ctx.mkPrefixOf(prefixExpr, subTarget),
                ctx.mkFalse()
        );
    }

    private static Expr handleEndsWith(StringMethodNode node, SeqExpr<CharSort> target,
                                       MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode suffixNode = (ExpressionNode) node.arguments.get(0);
        SeqExpr<CharSort> suffixExpr = (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(suffixNode, ctx, vars, memoryModel);
        // mkSuffixOf(suffix, target)
        return ctx.mkSuffixOf(suffixExpr, target);
    }

    // s.equals(string)
    // method : equals
    // target s
    // argument: "abc"
    private static Expr handleEquals(StringMethodNode node, SeqExpr<CharSort> target,
                                     MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) node.arguments.get(0);
        SeqExpr<CharSort> argExpr = (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);
        return ctx.mkEq(target, argExpr);
    }

    private static Expr handleEqualsIgnoreCase(StringMethodNode node, SeqExpr<CharSort> target,
                                               MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) node.arguments.get(0);
        SeqExpr<CharSort> argExpr = (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);

        return ctx.mkEq(handleLowerCase(target, ctx), handleLowerCase(argExpr, ctx));
    }

    private static Expr handleIndexOf(StringMethodNode node, SeqExpr<CharSort> target,
                                      MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode patternNode = (ExpressionNode) node.arguments.get(0);
        SeqExpr<CharSort> patternExpr = (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(patternNode, ctx, vars, memoryModel);

        IntExpr rawFromIndex;
        if (node.arguments.size() >= 2 && node.arguments.get(1) != null) {
            ExpressionNode fromNode = (ExpressionNode) node.arguments.get(1);
            rawFromIndex = (IntExpr) OperationExpressionNode.createZ3Expression(fromNode, ctx, vars, memoryModel);
        } else {
            rawFromIndex = ctx.mkInt(0);
        }

        IntExpr len = ctx.mkLength(target);
        IntExpr fromIndex = (IntExpr) ctx.mkITE(ctx.mkLt(rawFromIndex, ctx.mkInt(0)), ctx.mkInt(0), rawFromIndex);

        BoolExpr emptyPattern = ctx.mkEq(patternExpr, ctx.mkString(""));
        Expr emptyResult = ctx.mkITE(ctx.mkGt(rawFromIndex, len), len, fromIndex);
        Expr normalResult = ctx.mkITE(ctx.mkGt(fromIndex, len), ctx.mkInt(-1), ctx.mkIndexOf(target, patternExpr, fromIndex));

        return ctx.mkITE(emptyPattern, emptyResult, normalResult);
    }

    private static IntExpr minInt(Context ctx, IntExpr a, IntExpr b) {
        return (IntExpr) ctx.mkITE(ctx.mkLe(a, b), a, b);
    }

    private static IntExpr maxInt(Context ctx, IntExpr a, IntExpr b) {
        return (IntExpr) ctx.mkITE(ctx.mkGe(a, b), a, b);
    }

    private static Expr handleLastIndexOf(StringMethodNode node, SeqExpr<CharSort> target,
                                          MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode patternNode = (ExpressionNode) node.arguments.get(0);
        SeqExpr<CharSort> patternExpr =
                (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(patternNode, ctx, vars, memoryModel);

        IntExpr len = ctx.mkLength(target);
        BoolExpr emptyPattern = ctx.mkEq(patternExpr, ctx.mkString(""));

        if (node.arguments.size() < 2 || node.arguments.get(1) == null) {
            return ctx.mkITE(
                    emptyPattern,
                    len,
                    ctx.mkLastIndexOf(target, patternExpr)
            );
        }

        ExpressionNode fromNode = (ExpressionNode) node.arguments.get(1);
        IntExpr rawFromIndex =
                (IntExpr) OperationExpressionNode.createZ3Expression(fromNode, ctx, vars, memoryModel);

        BoolExpr negativeFrom = ctx.mkLt(rawFromIndex, ctx.mkInt(0));

        IntExpr nonNegFrom = maxInt(ctx, rawFromIndex, ctx.mkInt(0));
        IntExpr cappedFrom = minInt(ctx, nonNegFrom, len);

        IntExpr patternLen = ctx.mkLength(patternExpr);

        IntExpr searchLen = minInt(
                ctx,
                len,
                (IntExpr) ctx.mkAdd(cappedFrom, patternLen)
        );

        SeqExpr<CharSort> searchRegion =
                ctx.mkExtract(target, ctx.mkInt(0), searchLen);

        IntExpr normalResult = ctx.mkLastIndexOf(searchRegion, patternExpr);

        IntExpr emptyResult = (IntExpr) ctx.mkITE(
                negativeFrom,
                ctx.mkInt(-1),
                cappedFrom
        );

        return ctx.mkITE(
                emptyPattern,
                emptyResult,
                ctx.mkITE(negativeFrom, ctx.mkInt(-1), normalResult)
        );
    }

    private static Expr handleSubstring(StringMethodNode node, SeqExpr<CharSort> target,
                                        MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode beginNode = (ExpressionNode) node.arguments.get(0);
        IntExpr beginExpr = (IntExpr) OperationExpressionNode.createZ3Expression(beginNode, ctx, vars, memoryModel);

        IntExpr lengthExpr;
        if (node.arguments.size() == 1) {
            // substring(begin) → extract từ begin đến hết
            lengthExpr = (IntExpr) ctx.mkSub(ctx.mkLength(target), beginExpr);
        } else {
            // substring(begin, end) → length = end - begin
            ExpressionNode endNode = (ExpressionNode) node.arguments.get(1);
            IntExpr endExpr = (IntExpr) OperationExpressionNode.createZ3Expression(endNode, ctx, vars, memoryModel);
            lengthExpr = (IntExpr) ctx.mkSub(endExpr, beginExpr);
        }

        return ctx.mkExtract(target, beginExpr, lengthExpr);
    }

    private static Expr<CharSort> handleCharAt(StringMethodNode node, SeqExpr<CharSort> target, MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode indexNode = (ExpressionNode) node.arguments.get(0);

        IntExpr indexExpr = (IntExpr) OperationExpressionNode.createZ3Expression(indexNode, ctx, vars, memoryModel);

        return (Expr<CharSort>) ctx.mkNth(target, indexExpr);
    }

    private static Expr handleReplace(StringMethodNode node, SeqExpr<CharSort> target,
                                      MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode oldNode = (ExpressionNode) node.arguments.get(0);
        ExpressionNode newNode = (ExpressionNode) node.arguments.get(1);

        SeqExpr<CharSort> oldExpr =
                (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(oldNode, ctx, vars, memoryModel);
        SeqExpr<CharSort> newExpr =
                (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(newNode, ctx, vars, memoryModel);

        return ctx.mkReplaceAll(target, oldExpr, newExpr);
    }

    private static Expr handleReplaceAll(StringMethodNode node, SeqExpr<CharSort> target,
                                         MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        // replaceAll dùng regex → model bằng uninterpreted function
        ExpressionNode oldNode = (ExpressionNode) node.arguments.get(0);
        ExpressionNode newNode = (ExpressionNode) node.arguments.get(1);
        SeqExpr<CharSort> oldExpr = (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(oldNode, ctx, vars, memoryModel);
        SeqExpr<CharSort> newExpr = (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(newNode, ctx, vars, memoryModel);

        FuncDecl replaceAllFunc = ctx.mkFuncDecl(
                "replaceAll",
                new Sort[]{ctx.getStringSort(), ctx.getStringSort(), ctx.getStringSort()},
                ctx.getStringSort()
        );
        return ctx.mkApp(replaceAllFunc, target, oldExpr, newExpr);
    }

    private static SeqExpr<CharSort> handleLowerCase(SeqExpr<CharSort> text, Context ctx) {
        Expr<CharSort> ch = (Expr<CharSort>) ctx.mkConst("lower_ch", ctx.mkCharSort());
        Lambda<CharSort> lowerFn = ctx.mkLambda(new Expr<?>[]{ch}, lowerAsciiChar(ch, ctx));
        return ctx.mkSeqMap((Expr<?>) lowerFn, text);
    }

    private static Expr<CharSort> lowerAsciiChar(Expr<CharSort> ch, Context ctx) {
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

    private static SeqExpr<CharSort> handleUpperCase(SeqExpr<CharSort> text, Context ctx) {
        Expr<CharSort> ch = (Expr<CharSort>) ctx.mkConst("upper_ch", ctx.mkCharSort());
        Lambda<CharSort> upperFn = ctx.mkLambda(new Expr<?>[]{ch}, upperAsciiChar(ch, ctx));
        return ctx.mkSeqMap((Expr<?>) upperFn, text);
    }

    private static Expr<CharSort> upperAsciiChar(Expr<CharSort> ch, Context ctx) {
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

    private static Expr handleTrim(SeqExpr<CharSort> target, Context ctx) {
        // Z3 không có trim native → uninterpreted function
        FuncDecl trimFunc = ctx.mkFuncDecl(
                "trim",
                new Sort[]{ctx.getStringSort()},
                ctx.getStringSort()
        );
        return ctx.mkApp(trimFunc, target);
    }

    private static BoolExpr handleIsBlank(SeqExpr<CharSort> target, Context ctx) {
        ReExpr<SeqSort<CharSort>> blankChar = ctx.mkUnion(
                ctx.mkToRe(ctx.mkString(" ")),
                ctx.mkToRe(ctx.mkString("\t")),
                ctx.mkToRe(ctx.mkString("\n")),
                ctx.mkToRe(ctx.mkString("\r")),
                ctx.mkToRe(ctx.mkString("\f")),
                ctx.mkToRe(ctx.mkString("\u000B")),
                ctx.mkToRe(ctx.mkString("\u001C")),
                ctx.mkToRe(ctx.mkString("\u001D")),
                ctx.mkToRe(ctx.mkString("\u001E")),
                ctx.mkToRe(ctx.mkString("\u001F"))
        );

        return ctx.mkInRe(target, ctx.mkStar(blankChar));
    }

    private static IntExpr compareToSign(SeqExpr<CharSort> a, SeqExpr<CharSort> b, Context ctx) {
        return (IntExpr) ctx.mkITE(
                ctx.mkEq(a, b),
                ctx.mkInt(0),
                ctx.mkITE(
                        ctx.MkStringLt(a, b),
                        ctx.mkInt(-1),
                        ctx.mkInt(1)
                )
        );
    }

    private static Expr handleCompareTo(StringMethodNode node, SeqExpr<CharSort> target,
                                        MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) node.arguments.get(0);
        SeqExpr<CharSort> argExpr =
                (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);

        return compareToSign(target, argExpr, ctx);
    }

    private static Expr handleCompareToIgnoreCase(StringMethodNode node, SeqExpr<CharSort> target,
                                                  MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) node.arguments.get(0);

        SeqExpr<CharSort> argExpr =
                (SeqExpr<CharSort>) OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);

        SeqExpr<CharSort> lowerTarget = handleLowerCase(target, ctx);
        SeqExpr<CharSort> lowerArg = handleLowerCase(argExpr, ctx);

        return compareToSign(lowerTarget, lowerArg, ctx);
    }
}