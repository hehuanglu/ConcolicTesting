package core.ast.Expression.OperationExpression;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.Literal.BooleanLiteralNode;
import core.ast.Expression.Literal.CharacterLiteralNode;
import core.ast.Expression.Literal.LiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.MethodInvocationNode;
import core.ast.Expression.Name.NameNode;
import core.symbolicExecution.MemoryModel;
import core.variable.Variable;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public abstract class OperationExpressionNode extends ExpressionNode {

//    public static Expr createZ3Expression(OperationExpressionNode operationExpressionNode, Context ctx, List<Expr> vars, MemoryModel memoryModel) {
//        if (operationExpressionNode instanceof InfixExpressionNode) {
//            return InfixExpressionNode.createZ3Expression((InfixExpressionNode) operationExpressionNode, ctx, vars, memoryModel);
//        } else if (operationExpressionNode instanceof PrefixExpressionNode) {
//            return PrefixExpressionNode.createZ3Expression((PrefixExpressionNode) operationExpressionNode, ctx, vars, memoryModel);
//        } else if (operationExpressionNode instanceof PostfixExpressionNode) {
//            return PostfixExpressionNode.createZ3Expression((PostfixExpressionNode) operationExpressionNode, ctx, vars, memoryModel);
//        } else if (operationExpressionNode instanceof ParenthesizedExpressionNode) {
//            return ParenthesizedExpressionNode.createZ3Expression((ParenthesizedExpressionNode) operationExpressionNode, ctx, vars, memoryModel);
//        } else {
//            throw new RuntimeException(operationExpressionNode.getClass() + " is not an OperationExpression!!!");
//        }
//    }

    public static Expr createZ3Expression(ExpressionNode operand, Context ctx, List<Z3VariableWrapper> vars, MemoryModel memoryModel) {
        if (operand instanceof InfixExpressionNode) {
            return InfixExpressionNode.createZ3Expression((InfixExpressionNode) operand, ctx, vars, memoryModel);
        } else if (operand instanceof PostfixExpressionNode) {
            return PostfixExpressionNode.createZ3Expression((PostfixExpressionNode) operand, ctx, vars, memoryModel);
        } else if (operand instanceof PrefixExpressionNode) {
            return PrefixExpressionNode.createZ3Expression((PrefixExpressionNode) operand, ctx, vars, memoryModel);
        } else if (operand instanceof ParenthesizedExpressionNode) {
            return ParenthesizedExpressionNode.createZ3Expression((ParenthesizedExpressionNode) operand, ctx, vars, memoryModel);
        } else if (operand instanceof NameNode) {
            NameNode n = (NameNode) operand;
            String name = NameNode.getStringNameNode(n);

            if (operand.isFake()) {
                return ctx.mkIntConst(name);   // bypass memory + không gọi toString()
            }
            return createZ3Variable(n, ctx, vars, memoryModel);
        } else if (operand instanceof LiteralNode) {
            if (operand instanceof NumberLiteralNode) {
                String tokenVal = ((NumberLiteralNode) operand).getTokenValue();

                if (operand instanceof IntegerLiteralNode) {
                    boolean isLong = tokenVal.toUpperCase().endsWith("L");
                    String numStr = isLong ? tokenVal.substring(0, tokenVal.length() - 1) : tokenVal;
                    numStr = numStr.replace("_", ""); // Remove underscores
                    boolean isHex = numStr.toLowerCase().startsWith("0x");
                    boolean isBinary = numStr.toLowerCase().startsWith("0b");
                    boolean isOctal = numStr.startsWith("0") && !isHex && !isBinary && numStr.length() > 1;
                    long val;
                    if (isHex) {
                        val = Long.parseLong(numStr.substring(2), 16);
                    } else if (isBinary) {
                        val = Long.parseLong(numStr.substring(2), 2);
                    } else if (isOctal) {
                        val = Long.parseLong(numStr, 8);
                    } else {
                        val = Long.parseLong(numStr, 10);
                    }
                    if (isLong) {
                        return ctx.mkBV(val, 64);
                    } else {
                        return ctx.mkBV(val, 32);
                    }
                } else {
                    double val = Double.parseDouble(tokenVal.replace("_", ""));
                    boolean isFloat = tokenVal.toUpperCase().endsWith("F");
                    if (isFloat) {
                        return ctx.mkFP(val, ctx.mkFPSort32());
                    } else {
                        return ctx.mkFP(val, ctx.mkFPSort64());
                    }
                }
            } else if (operand instanceof BooleanLiteralNode) {
                return ctx.mkBool(((BooleanLiteralNode) operand).getValue());
            } else if (operand instanceof CharacterLiteralNode) {
                return ctx.mkBV(((CharacterLiteralNode) operand).getCharacterValue(), 16);
            } else {
                throw new RuntimeException("Invalid Literal");
            }
        } else if (operand instanceof MethodInvocationNode) {
            MethodInvocationNode methodInvocationNode = (MethodInvocationNode) operand;
            String methodName = methodInvocationNode.getMethodName();
            String className = methodInvocationNode.getClassName();
            List<AstNode> args = methodInvocationNode.getArgument();

            if ("Math".equals(className)) {
                if ("abs".equals(methodName)) {
                    ExpressionNode argNode = (ExpressionNode) args.get(0);
                    Expr argZ3 = createZ3Expression(argNode, ctx, vars, memoryModel);
                    if (argZ3 instanceof BitVecExpr) {
                        BitVecExpr x_arg = (BitVecExpr) argZ3;
                        BoolExpr isNegative = ctx.mkBVSLT(x_arg, ctx.mkBV(0, x_arg.getSortSize()));
                        BitVecExpr negativeX = ctx.mkBVNeg(x_arg);
                        System.out.println("Đã dịch Math.abs sang Z3");
                        return ctx.mkITE(isNegative, negativeX, x_arg);
                    }
                } else if ("max".equals(methodName)) {
                    ExpressionNode arg1Node = (ExpressionNode) args.get(0);
                    ExpressionNode arg2Node = (ExpressionNode) args.get(1);

                    Expr arg1Z3 = createZ3Expression(arg1Node, ctx, vars, memoryModel);
                    Expr arg2Z3 = createZ3Expression(arg2Node, ctx, vars, memoryModel);

                    if (arg1Z3 instanceof BitVecExpr && arg2Z3 instanceof BitVecExpr) {
                        BitVecExpr x_arg1 = (BitVecExpr) arg1Z3;
                        BitVecExpr x_arg2 = (BitVecExpr) arg2Z3;

                        BoolExpr a_gt_b = ctx.mkBVSGT(x_arg1, x_arg2);

                        return ctx.mkITE(a_gt_b, x_arg1, x_arg2);
                    }
                } else if ("min".equals(methodName)) {
                    ExpressionNode arg1Node = (ExpressionNode) args.get(0);
                    ExpressionNode arg2Node = (ExpressionNode) args.get(1);

                    Expr z3Arg1 = createZ3Expression(arg1Node, ctx, vars, memoryModel);
                    Expr z3Arg2 = createZ3Expression(arg2Node, ctx, vars, memoryModel);

                    if (z3Arg1 instanceof BitVecExpr && z3Arg2 instanceof BitVecExpr) {
                        BitVecExpr a = (BitVecExpr) z3Arg1;
                        BitVecExpr b = (BitVecExpr) z3Arg2;

                        BoolExpr a_lt_b = ctx.mkBVSLT(a, b);

                        return ctx.mkITE(a_lt_b, a, b);
                    }
                }
            } else {
                throw new RuntimeException("Chưa hỗ trợ hàm này");
            }
        } else if (operand instanceof CastExpressionNode) {
            CastExpressionNode castNode = (CastExpressionNode) operand;

            String targetType = castNode.getTargetNode().toString();

            ExpressionNode innerExpr = castNode.getInnerExpression();

            Expr z3Inner = createZ3Expression(innerExpr, ctx, vars, memoryModel);

            if (z3Inner instanceof BitVecExpr) {
                BitVecExpr arg = (BitVecExpr) z3Inner;

                int currentSize = arg.getSortSize();

                if ("long".equals(targetType) && currentSize == 32) {
                    System.out.println("Đã ép kiểu int thành long cho Z3");
                    return ctx.mkSignExt(32, arg);
                } else if ("int".equals(targetType) && currentSize == 64) {
                    System.out.println(" Đã ép kiểu long thành int cho Z3");
                    return ctx.mkExtract(31, 0, arg);
                } else if ("short".equals(targetType) && currentSize == 32) {
                    System.out.println(" Đã ép kiểu int thành short cho Z3");
                    return ctx.mkExtract(15, 0, arg);
                } else if ("int".equals(targetType) && currentSize == 8) {
                    return ctx.mkSignExt(24, arg);
                }
            }

            return z3Inner;
        } else {
            throw new RuntimeException(operand.getClass() + " is not an Expression");
        }
        return null;
    }

    private static Expr createZ3Variable(NameNode variableName, Context ctx, List<Z3VariableWrapper> vars, MemoryModel memoryModel) {
        String stringName = NameNode.getStringNameNode(variableName);
        Expr variable = Variable.createZ3Variable(memoryModel.getVariable(stringName), ctx);
        Z3VariableWrapper z3VariableWrapper = new Z3VariableWrapper(variable);
        //Check duplicate and add to vars
        int variableIndex = getDuplicateVariableIndex(z3VariableWrapper, vars);
        if (variableIndex != -1) {
            vars.set(variableIndex, z3VariableWrapper);
        } else {
            vars.add(z3VariableWrapper);
        }

        return variable;
    }

    public static int getDuplicateVariableIndex(Z3VariableWrapper z3Variable, List<Z3VariableWrapper> vars) {
        for (int i = 0; i < vars.size(); i++) {
            if (z3Variable.equals(vars.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static AstNode executeOperationExpression(Expression expression, MemoryModel memoryModel) {
        if (expression instanceof InfixExpression) {
            return InfixExpressionNode.executeInfixExpression((InfixExpression) expression, memoryModel);
        } else if (expression instanceof PrefixExpression) {
            return PrefixExpressionNode.executePrefixExpression((PrefixExpression) expression, memoryModel);
        } else if (expression instanceof PostfixExpression) {
            return PostfixExpressionNode.executePostfixExpression((PostfixExpression) expression, memoryModel);
        } else if (expression instanceof ParenthesizedExpression) {
            return ParenthesizedExpressionNode.executeParenthesizedExpression((ParenthesizedExpression) expression, memoryModel);
        } else {
            throw new RuntimeException(expression.getClass() + " is not an OperationExpression!!!");
        }
    }

    public static ExpressionNode executeOperandNode(ExpressionNode operand, MemoryModel memoryModel) {
        if (operand instanceof InfixExpressionNode) {
            return InfixExpressionNode.executeInfixExpressionNode((InfixExpressionNode) operand, memoryModel);
        } else if (operand instanceof PrefixExpressionNode) {
            return PrefixExpressionNode.executePrefixExpressionNode((PrefixExpressionNode) operand, memoryModel);
        } else if (operand instanceof PostfixExpressionNode) {
            return PostfixExpressionNode.executePostfixExpressionNode((PostfixExpressionNode) operand, memoryModel);
        } else if (operand instanceof ParenthesizedExpressionNode) {
            return ParenthesizedExpressionNode.executeParenthesizedExpressionNode((ParenthesizedExpressionNode) operand, memoryModel);
        } else if (operand instanceof NameNode) {
            return NameNode.executeNameNode((NameNode) operand, memoryModel);
        } else if (operand instanceof CastExpressionNode) {
            return operand;
        } else if (operand instanceof MethodInvocationNode) {
            return operand;
        } else {
            throw new RuntimeException(operand.getClass() + " is Invalid expressionNode");
        }
    }

    public static void replaceMethodInvocationWithStub(Expression originExpression, MethodInvocation originMethodInvocation, ASTNode replacement) {
        if (originExpression instanceof InfixExpression) {
            InfixExpressionNode.replaceMethodInvocationWithStub((InfixExpression) originExpression, originMethodInvocation, replacement);
        } else if (originExpression instanceof PrefixExpression) {
            PrefixExpressionNode.replaceMethodInvocationWithStub((PrefixExpression) originExpression, originMethodInvocation, replacement);
        } else if (originExpression instanceof PostfixExpression) {
            PostfixExpressionNode.replaceMethodInvocationWithStub((PostfixExpression) originExpression, originMethodInvocation, replacement);
        } else if (originExpression instanceof ParenthesizedExpression) {
            ParenthesizedExpressionNode.replaceMethodInvocationWithStub((ParenthesizedExpression) originExpression, originMethodInvocation, replacement);
        } else {
            throw new RuntimeException(originExpression.getClass() + " is not an OperationExpression!!!");
        }
    }
}
