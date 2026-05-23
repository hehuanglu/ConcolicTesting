package core.ast.Expression.Method;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.ast.Type.AnnotatableType.SimpleTypeNode;
import core.symbolicExecution.MemoryModel;
import java.util.List;

public class StringMethodNode extends MethodInvocationNode {
    private final AstNode target;
    private final String methodName;
    private final List<AstNode> arguments;

    public StringMethodNode(AstNode target, String methodName, List<AstNode> arguments) {
        this.target = target;
        this.methodName = methodName;
        this.arguments = arguments;
    }

    public static Expr createZ3Expression(StringMethodNode node, MemoryModel memoryModel,
                                          Context ctx, List<Z3VariableWrapper> vars) {
        Expr targetExpr = getTargetExpr(node.target, memoryModel, ctx, vars);
        SeqExpr targetStr = (SeqExpr) targetExpr;

        switch (node.methodName) {
            case "concat":
                return handleConcat(node, targetStr, memoryModel, ctx, vars);
            case "replace":
                return handleReplace(node, targetStr, memoryModel, ctx, vars);
            case "replaceAll":
                return handleReplaceAll(node, targetStr, memoryModel, ctx, vars);
            case "substring":
                return handleSubstring(node, targetStr, memoryModel, ctx, vars);
            case "length":
                return ctx.mkLength(targetStr);
            case "charAt":
                return handleCharAt(node, targetStr, memoryModel, ctx, vars);
            case "isBlank":
            case "isEmpty":
                return handleIsBlank(targetStr, ctx);
            case "equals":
                return handleEquals(node,targetStr, memoryModel, ctx, vars);
            case "startsWith":
                return handleStartsWith(node,targetStr, memoryModel, ctx, vars);
            case "endsWith":
                return handleEndsWith(node,targetStr, memoryModel, ctx, vars);
            case "indexOf":
                return handleIndexOf(node,targetStr, memoryModel, ctx, vars);
            case "contains":
                return handleContains(node,targetStr, memoryModel, ctx, vars);
            default:
                throw new RuntimeException("Unsupported String method: " + node.methodName);
        }
    }
    private static Expr handleContains(StringMethodNode node, SeqExpr<CharSort> target, MemoryModel memoryModel,
                                      Context ctx, List<Z3VariableWrapper> vars){
        ExpressionNode argument = (ExpressionNode) node.arguments.get(0);
        Expr argumentExpr = OperationExpressionNode.createZ3Expression(argument,ctx,vars,memoryModel);
        return ctx.mkContains(target,argumentExpr);
    }
    private static Expr handleIndexOf(StringMethodNode node, SeqExpr<CharSort> target, MemoryModel memoryModel,
                                      Context ctx, List<Z3VariableWrapper> vars){
        ExpressionNode argument = (ExpressionNode) node.arguments.get(0);
        ExpressionNode indexNode = (ExpressionNode) node.arguments.get(1);

        Expr argumentExpr = OperationExpressionNode.createZ3Expression(argument,ctx,vars,memoryModel);
        Expr indexExpr;
        if(indexNode != null){
            indexExpr  = OperationExpressionNode.createZ3Expression(indexNode,ctx,vars,memoryModel);
        }else{
            indexExpr = ctx.mkInt(0);
        }

        return ctx.mkIndexOf(target,argumentExpr,indexExpr);
    }

    private static Expr handleEndsWith(StringMethodNode node, SeqExpr<CharSort> target, MemoryModel memoryModel,
                                         Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argument = (ExpressionNode) node.arguments.get(0);
        Expr argumentExpr = OperationExpressionNode.createZ3Expression(argument,ctx,vars,memoryModel);
        return ctx.mkSuffixOf(argumentExpr,target);
    }

    private static Expr handleStartsWith(StringMethodNode node, SeqExpr<CharSort> target, MemoryModel memoryModel,
                                         Context ctx, List<Z3VariableWrapper> vars) {

        // 1. Lấy tham số đầu tiên (chuỗi argument cần kiểm tra)
        ExpressionNode argument = (ExpressionNode) node.arguments.get(0);
        Expr argExprRaw = OperationExpressionNode.createZ3Expression(argument, ctx, vars, memoryModel);
        // Bắt buộc ép kiểu sang SeqExpr để dùng cho mkPrefixOf
        SeqExpr<CharSort> argumentExpr = (SeqExpr<CharSort>) argExprRaw;

        // 2. Xử lý tham số index (tofset) một cách an toàn
        IntExpr indexExpr;
        if (node.arguments.size() > 1 && node.arguments.get(1) != null) {
            ExpressionNode indexNode = (ExpressionNode) node.arguments.get(1);
            Expr parsedIndex = OperationExpressionNode.createZ3Expression(indexNode, ctx, vars, memoryModel);
            // Bắt buộc ép kiểu sang IntExpr
            indexExpr = (IntExpr) parsedIndex;
        } else {
            // Nếu không có tham số thứ 2, mặc định index là 0
            indexExpr = ctx.mkInt(0);
        }

        // --- Bắt đầu Ánh xạ Z3 ---

        // remainingLen = target.length() - indexExpr
        IntExpr remainingLen = (IntExpr) ctx.mkSub(ctx.mkLength(target), indexExpr);

        // lấy substring từ vị trí index với độ dài remainingLen
        SeqExpr<CharSort> subStr = ctx.mkExtract(target, indexExpr, remainingLen);

        // so sánh chuỗi con và chuỗi argument (Lưu ý: Tiền tố argumentExpr đứng TRƯỚC)
        BoolExpr startsWithAtIndex = ctx.mkPrefixOf(argumentExpr, subStr);

        BoolExpr indexGTEZero = ctx.mkGe(indexExpr, ctx.mkInt(0)); // index >= 0
        BoolExpr indexLteLen = ctx.mkLe(indexExpr, ctx.mkLength(target)); // index <= target.length()

        // Kết hợp các điều kiện lại
        return ctx.mkAnd(indexGTEZero, indexLteLen, startsWithAtIndex);
    }

    private  static Expr handleEquals(StringMethodNode node,SeqExpr target, MemoryModel memoryModel,
                                      Context ctx, List<Z3VariableWrapper> vars){

        ExpressionNode argNode = (ExpressionNode) node.arguments.get(0);
        Expr argExpr = OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);
        return ctx.mkEq(target,argExpr);
    }

    private static Expr getTargetExpr(AstNode target, MemoryModel memoryModel,
                                      Context ctx, List<Z3VariableWrapper> vars) {
        if (target instanceof SimpleTypeNode)
            return SimpleTypeNode.createZ3Expression((SimpleTypeNode) target, memoryModel, ctx, vars);
        if (target instanceof StringMethodNode)
            return createZ3Expression((StringMethodNode) target, memoryModel, ctx, vars);
        throw new RuntimeException("Invalid target type");
    }

    private static Expr handleConcat(StringMethodNode node, SeqExpr targetStr,
                                     MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {
        ExpressionNode argNode = (ExpressionNode) node.arguments.get(0);
        Expr argExpr = OperationExpressionNode.createZ3Expression(argNode, ctx, vars, memoryModel);
        if (!argExpr.getSort().equals(ctx.getStringSort()))
            throw new RuntimeException("concat argument not string");
        return ctx.mkConcat(targetStr, (SeqExpr) argExpr);
    }

    private static Expr handleReplace(StringMethodNode node, SeqExpr targetStr,
                                      MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {

        try {
            ExpressionNode oldNode = (ExpressionNode) node.arguments.get(0);
            ExpressionNode newNode = (ExpressionNode) node.arguments.get(1);
            Expr oldExpr = OperationExpressionNode.createZ3Expression(oldNode, ctx, vars, memoryModel);
            Expr newExpr = OperationExpressionNode.createZ3Expression(newNode, ctx, vars, memoryModel);
            if (!oldExpr.getSort().equals(ctx.getStringSort()) || !newExpr.getSort().equals(ctx.getStringSort()))
                throw new RuntimeException("replace arguments must be strings");
            return ctx.mkReplace(targetStr, (SeqExpr) oldExpr, (SeqExpr) newExpr);
        } catch (Exception e) {
            throw new RuntimeException("mkReplace not available in this Z3 version. Please upgrade or use stub.", e);
        }
    }

    private static Expr handleSubstring(StringMethodNode node, SeqExpr targetStr,
                                        MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {

        ExpressionNode beginNode = (ExpressionNode) node.arguments.get(0);
        Expr beginExpr = OperationExpressionNode.createZ3Expression(beginNode, ctx, vars, memoryModel);

        if (!(beginExpr instanceof IntExpr)) {
            throw new RuntimeException("substring begin index must be int");
        }
        IntExpr beginInt = (IntExpr) beginExpr;

        if (node.arguments.size() == 1) {
            IntExpr len = ctx.mkLength(targetStr);

            // mkSub -> ArithExpr → cast về IntExpr
            ArithExpr tmpLength = ctx.mkSub(len, beginInt);
            IntExpr length = (IntExpr) tmpLength;

            return ctx.mkExtract(targetStr, beginInt, length);
        } else {
            ExpressionNode endNode = (ExpressionNode) node.arguments.get(1);
            Expr endExpr = OperationExpressionNode.createZ3Expression(endNode, ctx, vars, memoryModel);

            if (!(endExpr instanceof IntExpr)) {
                throw new RuntimeException("substring end index must be int");
            }
            IntExpr endInt = (IntExpr) endExpr;

            ArithExpr tmpLength = ctx.mkSub(endInt, beginInt);
            IntExpr length = (IntExpr) tmpLength;

            return ctx.mkExtract(targetStr, beginInt, length);
        }
    }

    private static Expr handleCharAt(StringMethodNode node, SeqExpr targetStr,
                                     MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {

        ExpressionNode indexNode = (ExpressionNode) node.arguments.get(0);
        Expr indexExpr = OperationExpressionNode.createZ3Expression(indexNode, ctx, vars, memoryModel);

        // 1. CHUẨN HÓA KIỂU DỮ LIỆU
        // Dùng BitVecExpr để bao trùm cả hằng số (BitVecNum) lẫn biến số (BitVec)
        if (indexExpr instanceof BitVecExpr) {
            indexExpr = ctx.mkBV2Int((BitVecExpr) indexExpr, true);
        } else if (!(indexExpr instanceof IntExpr)) {
            throw new RuntimeException("charAt index must be evaluable to an Int or BitVec");
        }

        IntExpr indexInt = (IntExpr) indexExpr;
        IntExpr one = ctx.mkInt(1);

        // 2. THỰC THI PHÉP TOÁN TRÊN STRING
        // Dùng mkExtract nạp chồng cho SeqExpr để lấy ra 1 ký tự
        return ctx.mkExtract(targetStr, indexInt, one);
    }

    private static BoolExpr handleIsBlank(SeqExpr s, Context ctx) {
        IntExpr len = ctx.mkLength(s);
        IntExpr i = ctx.mkIntConst("i_" + s.hashCode());

        SeqExpr space = ctx.mkString(" ");
        BoolExpr range = ctx.mkAnd(
                ctx.mkGe(i, ctx.mkInt(0)),
                ctx.mkLt(i, len)
        );

        BoolExpr isSpace = ctx.mkEq(
                ctx.mkExtract(s, i, ctx.mkInt(1)),
                space
        );

        BoolExpr forallSpaces = ctx.mkForall(
                new Expr[]{i},
                ctx.mkImplies(range, isSpace),
                1, null, null, null, null
        );

        return forallSpaces;
    }

    private static Expr handleReplaceAll(StringMethodNode node, SeqExpr targetStr,
                                         MemoryModel memoryModel, Context ctx, List<Z3VariableWrapper> vars) {

        ExpressionNode oldNode = (ExpressionNode) node.arguments.get(0);
        ExpressionNode newNode = (ExpressionNode) node.arguments.get(1);

        Expr oldExpr = OperationExpressionNode.createZ3Expression(oldNode, ctx, vars, memoryModel);
        Expr newExpr = OperationExpressionNode.createZ3Expression(newNode, ctx, vars, memoryModel);

        if (!oldExpr.getSort().equals(ctx.getStringSort()) ||
                !newExpr.getSort().equals(ctx.getStringSort())) {
            throw new RuntimeException("replaceAll arguments must be strings");
        }

        // Tạo (hoặc reuse) uninterpreted function: replaceAll(s, old, new)
        FuncDecl replaceAllFunc = ctx.mkFuncDecl(
                "replaceAll",
                new Sort[]{ctx.getStringSort(), ctx.getStringSort(), ctx.getStringSort()},
                ctx.getStringSort()
        );

        return ctx.mkApp(replaceAllFunc, targetStr, oldExpr, newExpr);
    }
}