package core.ast.VariableDeclaration;

import com.microsoft.z3.*;
import core.Z3Vars.Z3VariableWrapper;
import core.ast.AstNode;
import core.ast.Expression.Array.ArrayCreationNode;
import core.ast.Expression.Array.ArrayCreationWithNewKeyWord;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.OperationExpression.OperationExpressionNode;
import core.ast.Type.AnnotatableType.PrimitiveTypeNode;
import core.symbolicExecution.MemoryModel;
import core.symbolicExecution.SymbolicExecutionRewrite;
import core.testGeneration.TestGeneration;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.*;

import java.util.List;
import java.util.Map;

@Slf4j
public class VariableDeclarationFragmentNode extends VariableDeclarationNode {

    public static void executeVariableDeclarationFragment(VariableDeclarationFragment fragment,
                                                          Type baseType,
                                                          MemoryModel memoryModel) {
        String name = fragment.getName().getIdentifier();
        Expression initializer = fragment.getInitializer();

        if(initializer != null) {
            if(baseType instanceof PrimitiveType) {
                PrimitiveType type = (PrimitiveType) baseType;
                memoryModel.declarePrimitiveTypeVariable(type, name, ExpressionNode.executeExpression(initializer, memoryModel));
            }
            else if (baseType instanceof SimpleType) {
                SimpleType type = (SimpleType) baseType;
                memoryModel.declareSimpleTypeVariable(type, name, ExpressionNode.executeExpression(initializer, memoryModel));
            }
            else if (baseType instanceof ArrayType) {
                ArrayType type = (ArrayType) baseType;

                AstNode initNode = null;
                if (initializer instanceof ArrayCreation) {
                    ArrayCreation arrayCreation = (ArrayCreation) initializer;
                    ArrayCreationWithNewKeyWord strategy = new ArrayCreationWithNewKeyWord();
                    initNode = ArrayCreationNode.executeArrayCreation(arrayCreation, memoryModel, strategy);
                } else {
                    initNode = ExpressionNode.executeExpression(initializer, memoryModel);
                }

                memoryModel.declareArrayTypeVariable(type, name, type.getDimensions(), initNode);

                try {
                    if (initializer instanceof ArrayCreation) {
                        ArrayCreation arrayCreation = (ArrayCreation) initializer;
                        List<ASTNode> dimensions = arrayCreation.dimensions();

                        if (!dimensions.isEmpty()) {
                            Expression firstDimension = (Expression) dimensions.get(0);

                            // Móc context Z3 ra
                            Context ctx = SymbolicExecutionRewrite.globalCtx.get();
                            Map<String, Expr> stateMap = SymbolicExecutionRewrite.z3ArrayStateMap.get();
                            List<Z3VariableWrapper> vars = SymbolicExecutionRewrite.globalZ3Vars.get();

                            if (ctx != null && stateMap != null && vars != null) {

                                // Tạo mảng rỗng toàn số 0 trong Z3
                                Sort domainSort = ctx.mkBitVecSort(32);
                                String eleType = type.getElementType().toString();
                                Sort rangeSort = ctx.mkBitVecSort(32); // Mặc định là BitVec32
                                Expr defaultValue = ctx.mkBV(0, 32);

                                if (eleType.equals("long")) {
                                    rangeSort = ctx.mkBitVecSort(64);
                                    defaultValue = ctx.mkBV(0, 64);
                                } else if (eleType.equals("float")) {
                                    rangeSort = ctx.mkFPSortSingle();
                                    defaultValue = ctx.mkFP(0.0f, (FPSort) rangeSort);
                                } else if (eleType.equals("double")) {
                                    rangeSort = ctx.mkFPSortDouble();
                                    defaultValue = ctx.mkFP(0.0, (FPSort) rangeSort);
                                }

                                ArrayExpr z3NewArray = ctx.mkConstArray(domainSort, defaultValue);
                                stateMap.put(name, z3NewArray);

                                // Dịch biểu thức kích thước sang Z3
                                ExpressionNode dimExprNode = (ExpressionNode) ExpressionNode.executeExpression(firstDimension, memoryModel);
                                Expr z3SizeExpr = OperationExpressionNode.createZ3Expression(dimExprNode, ctx, vars, memoryModel);

                                Expr z3LengthVar = ctx.mkBVConst(name + ".length", 32);
                                BoolExpr lengthConstraint = ctx.mkEq(z3LengthVar, z3SizeExpr);

                                SymbolicExecutionRewrite.arrayLengthConstraints.add(lengthConstraint);
                                log.info("Đã chích Z3 cho mảng cục bộ: {} length = {}" ,name, firstDimension.toString());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi đồng bộ Z3 cho ArrayCreation: {}", e.getMessage());
                }
            } else {
                throw new RuntimeException("Chưa hỗ trợ khởi tạo cho kiểu: " + baseType.getClass());
            }
        } else {
            // Declaration without initialization
            if(baseType instanceof PrimitiveType) {
                PrimitiveType type = (PrimitiveType) baseType;
                memoryModel.declarePrimitiveTypeVariable(type, name, PrimitiveTypeNode.changePrimitiveTypeToLiteralInitialization(type));
            } else if (baseType instanceof SimpleType) {
                // Không khởi tạo gì thêm, kệ nó
            } else {
                throw new RuntimeException("Chưa hỗ trợ khai báo rỗng cho kiểu này!");
            }
        }
    }

    public static void replaceMethodInvocationWithStub(VariableDeclarationFragment originVariableDeclarationFragment,  MethodInvocation originMethodInvocation, ASTNode replacement) {
        Expression initializer = originVariableDeclarationFragment.getInitializer();
        if (initializer == originMethodInvocation) {
            originVariableDeclarationFragment.setInitializer((Expression) replacement);
        }
    }

}
