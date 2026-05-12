package core.ast.Expression.Literal;

import core.ast.AstNode;
import core.ast.Expression.ExpressionNode;
import core.ast.Expression.OperationExpression.InfixExpressionNode;
import core.ast.Expression.Literal.NumberLiteral.DoubleLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.IntegerLiteralNode;
import core.ast.Expression.Literal.NumberLiteral.NumberLiteralNode;
import core.ast.Expression.Literal.NullLiteralNode;
import core.ast.Expression.OperationExpression.PrefixExpressionNode;
import core.symbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

public abstract class LiteralNode extends ExpressionNode {

    public static LiteralNode executeLiteral(Expression expression) {
        if (expression instanceof NumberLiteral) {
            return NumberLiteralNode.executeNumberLiteral((NumberLiteral) expression);
        } else if (expression instanceof CharacterLiteral) {
            return CharacterLiteralNode.executeCharacterLiteral((CharacterLiteral) expression);
        } else if (expression instanceof BooleanLiteral) {
            return BooleanLiteralNode.executeBooleanLiteral((BooleanLiteral) expression);
        } else if (expression instanceof StringLiteral) {
            return StringLiteralNode.executeStringLiteral((StringLiteral) expression);
        } else if (expression instanceof NullLiteral) {
            /*???*/
            return NullLiteralNode.executeNullLiteral();
        } else if (expression instanceof TypeLiteral) {
            /*???*/
            return null;
        } else {
            throw new RuntimeException(expression.getClass() + " is not a Literal!!!");
        }
    }

    /*For Prefix Expression Node*/
    public static LiteralNode analyzeOnePrefixLiteral(PrefixExpression.Operator operator, LiteralNode literal) {
        if (LiteralNode.isBitwiseLiteral(literal) && PrefixExpressionNode.isBitwiseOperator(operator)) {
            return calculateOnePrefixBitwiseLiteral(literal, operator);
        } else if (literal.isCharacterLiteralNode()) {
            return calculateOnePrefixCharacterLiteral((CharacterLiteralNode) literal, operator);
        } else if (literal.isNumberLiteralNode()) {
            return calculateOnePrefixNumberLiteral((NumberLiteralNode) literal, operator);
        } else if (literal.isBooleanLiteralNode()) {
            return calculateOnePrefixBooleanLiteral((BooleanLiteralNode) literal, operator);
        } else {
            throw new RuntimeException("Invalid literal to analyze!!!");
        }
    }

    /*For Postfix Expression Node (ONLY USE FOR RE-ASSIGNMENT ACT. ex: i++ (use for re-assign i))*/
    public static LiteralNode analyzeOnePostfixLiteral(LiteralNode literal, PostfixExpression.Operator operator) {
        if (literal.isNumberLiteralNode()) {
            return calculateOnePostfixNumberLiteral((NumberLiteralNode) literal, operator);
        } else if (literal.isCharacterLiteralNode()) {
            return calculateOnePostfixCharacterLiteral((CharacterLiteralNode) literal, operator);
        } else {
            throw new RuntimeException("Invalid literal to analyze!!!");
        }
    }

    /*For Infix Expression Node*/
    public static LiteralNode analyzeTwoInfixLiteral(LiteralNode literal1, InfixExpression.Operator operator,
                                                     LiteralNode literal2) {
        if (isComputableAndComparableLiteral(literal1) && isComputableAndComparableLiteral(literal2)) {
            return calculateTwoInfixComputableAndComparableLiteral(literal1, operator, literal2);
        } else if (literal1.isBooleanLiteralNode() && literal2.isBooleanLiteralNode()) {
            return calculateTwoInfixBooleanLiteral((BooleanLiteralNode) literal1, operator,
                    (BooleanLiteralNode) literal2);
        } else if (literal1.isStringLiteralNode() && literal2.isStringLiteralNode()) {
            return calculateTwoInfixStringLiteral((StringLiteralNode) literal1, operator,
                    (StringLiteralNode) literal2);
        } else if (literal1.isStringLiteralNode() || literal2.isStringLiteralNode()) {
            return calculateTwoInfixConcatenableLiteral(literal1, operator, literal2);
        } else {
            throw new RuntimeException("Invalid literals to analyze!!!");
        }
    }

    private static NumberLiteralNode calculateOnePostfixNumberLiteral(NumberLiteralNode literal, PostfixExpression.Operator operator) {
        DoubleLiteralNode tmpRes = computeOnePostfixNumberLiteral(literal, operator);
        if (literal.isDoubleLiteralNode()) {
            return tmpRes;
        } else {
            IntegerLiteralNode newRes = new IntegerLiteralNode();
            newRes.setTokenValue(tmpRes.getDoubleValue());
            return newRes;
        }
    }

    private static CharacterLiteralNode calculateOnePostfixCharacterLiteral(CharacterLiteralNode literal, PostfixExpression.Operator operator) {
        return computeOnePostfixCharacterLiteral(literal, operator);
    }

    private static NumberLiteralNode calculateOnePrefixNumberLiteral(NumberLiteralNode literal,
                                                                     PrefixExpression.Operator operator) {
        DoubleLiteralNode tmpRes = computeOnePrefixNumberLiteral(literal, operator);
        if (literal.isDoubleLiteralNode()) {
            return tmpRes;
        } else {
            IntegerLiteralNode newRes = new IntegerLiteralNode();
            newRes.setTokenValue(tmpRes.getDoubleValue());
            return newRes;
        }
    }

    private static IntegerLiteralNode calculateOnePrefixBitwiseLiteral(LiteralNode literal, PrefixExpression.Operator operator) {
        int intValue = changeLiteralNodeToInteger(literal);

        IntegerLiteralNode result = new IntegerLiteralNode();

        if (operator.equals(PrefixExpression.Operator.COMPLEMENT)) {
            result.setTokenValue(~intValue);
        } else {
            throw new RuntimeException(operator + " is Invalid bitwise operator");
        }

        return result;
    }

    private static LiteralNode calculateOnePrefixCharacterLiteral(CharacterLiteralNode literal,
                                                                  PrefixExpression.Operator operator) {
        return computeOnePrefixCharacterLiteral(literal, operator);
    }

    private static BooleanLiteralNode calculateOnePrefixBooleanLiteral(BooleanLiteralNode literal,
                                                                       PrefixExpression.Operator operator) {
        return computeOnePrefixBooleanLiteral(literal, operator);
    }

    private static StringLiteralNode calculateTwoInfixConcatenableLiteral(LiteralNode literal1,
                                                                          InfixExpression.Operator operator,
                                                                          LiteralNode literal2) {

        StringLiteralNode stringLiteralNode1 = changeToStringLiteral(literal1);
        StringLiteralNode stringLiteralNode2 = changeToStringLiteral(literal2);

        if (InfixExpressionNode.isStringConcatenationOperator(operator)) {
            return concatenateTwoInfixStringLiteral(stringLiteralNode1, operator, stringLiteralNode2);
        } else {
            throw new RuntimeException("Invalid infix concatenate operator!!!");
        }
    }

    private static LiteralNode calculateTwoInfixStringLiteral(StringLiteralNode literal1,
                                                              InfixExpression.Operator operator,
                                                              StringLiteralNode literal2) {
        if (InfixExpressionNode.isStringConcatenationOperator(operator)) {
            return concatenateTwoInfixStringLiteral(literal1, operator, literal2);
        } else if (InfixExpressionNode.isStringComparisonOperator(operator)) {
            return compareTwoInfixStringLiteral(literal1, operator, literal2);
        } else {
            throw new RuntimeException("Invalid infix operator for applying to String type!!!");
        }
    }

    private static LiteralNode calculateTwoInfixComputableAndComparableLiteral(LiteralNode literal1,
                                                                               InfixExpression.Operator operator,
                                                                               LiteralNode literal2) {
        if (InfixExpressionNode.isArithmeticOperator(operator)) {
            DoubleLiteralNode tmpRes = computeTwoInfixLiteral(literal1, operator, literal2);
            if (literal1 instanceof DoubleLiteralNode || literal2 instanceof DoubleLiteralNode) {
                return tmpRes;
            } else {
                IntegerLiteralNode newRes = new IntegerLiteralNode();
                newRes.setTokenValue(tmpRes.getDoubleValue());
                return newRes;
            }
        } else if (InfixExpressionNode.isBitwiseOperator(operator)) {
            return computeTwoInfixBitwiseLiteral(literal1, operator, literal2);
        } else if (InfixExpressionNode.isComparisonOperator(operator)) {
            return compareTwoInfixLiteral(literal1, operator, literal2);
        } else {
            throw new RuntimeException("Operator given is neither infix comparison nor math operator");
        }
    }

    private static LiteralNode calculateTwoInfixBooleanLiteral(BooleanLiteralNode literal1, InfixExpression.Operator operator,
                                                               BooleanLiteralNode literal2) {
        if (InfixExpressionNode.isConditionalOperator(operator) ||
                InfixExpressionNode.isBooleanComparisonOperator(operator) ||
                InfixExpressionNode.isBooleanBitwiseOperator(operator)) {
            return compareTwoInfixBooleanLiteral(literal1, operator, literal2);
        } else {
            throw new RuntimeException("Invalid infix operator for applying to boolean type");
        }
    }

    private static BooleanLiteralNode computeOnePrefixBooleanLiteral(BooleanLiteralNode literal, PrefixExpression.Operator operator) {
        boolean booleanValue = literal.getValue();

        BooleanLiteralNode result = new BooleanLiteralNode();

        if (operator.equals(PrefixExpression.Operator.NOT)) {
            result.setValue(!booleanValue);
        } else {
            throw new RuntimeException("Invalid prefix operator for boolean literal");
        }

        return result;
    }

    private static LiteralNode computeOnePrefixCharacterLiteral(CharacterLiteralNode literal, PrefixExpression.Operator operator) {
        char characterValue = literal.getCharacterValue();

        CharacterLiteralNode result = new CharacterLiteralNode();

        if (operator.equals(PrefixExpression.Operator.INCREMENT)) {
            result.setCharacterValue(++characterValue);
        } else if (operator.equals(PrefixExpression.Operator.DECREMENT)) {
            result.setCharacterValue(--characterValue);
        } else if (operator.equals(PrefixExpression.Operator.PLUS)) {
            IntegerLiteralNode newRes = new IntegerLiteralNode();
            newRes.setTokenValue(+characterValue);
            return newRes;
        } else if (operator.equals(PrefixExpression.Operator.MINUS)) {
            IntegerLiteralNode newRes = new IntegerLiteralNode();
            newRes.setTokenValue(-characterValue);
            return newRes;
        } else {
            throw new RuntimeException("Invalid prefix operator for character literal");
        }

        return result;
    }

    private static CharacterLiteralNode computeOnePostfixCharacterLiteral(CharacterLiteralNode literal, PostfixExpression.Operator operator) {
        char characterValue = literal.getCharacterValue();

        CharacterLiteralNode result = new CharacterLiteralNode();

        if (operator.equals(PostfixExpression.Operator.INCREMENT)) {
            result.setCharacterValue((char) (characterValue + 1));
        } else if (operator.equals(PostfixExpression.Operator.DECREMENT)) {
            result.setCharacterValue((char) (characterValue - 1));
        } else {
            throw new RuntimeException("Invalid postfix operator for character literal");
        }

        return result;
    }

    private static DoubleLiteralNode computeOnePostfixNumberLiteral(NumberLiteralNode literal, PostfixExpression.Operator operator) {
        double doubleValue = changeToDoubleLiteral(literal).getDoubleValue();

        DoubleLiteralNode result = new DoubleLiteralNode();

        if (operator.equals(PostfixExpression.Operator.INCREMENT)) {
            result.setTokenValue(doubleValue + 1);
        } else if (operator.equals(PostfixExpression.Operator.DECREMENT)) {
            result.setTokenValue(doubleValue - 1);
        } else {
            throw new RuntimeException("Invalid postfix operator for number literal");
        }

        return result;
    }

    private static DoubleLiteralNode computeOnePrefixNumberLiteral(NumberLiteralNode literal, PrefixExpression.Operator operator) {
        double doubleValue = changeToDoubleLiteral(literal).getDoubleValue();

        DoubleLiteralNode result = new DoubleLiteralNode();

        if (operator.equals(PrefixExpression.Operator.INCREMENT)) {
            result.setTokenValue(++doubleValue);
        } else if (operator.equals(PrefixExpression.Operator.DECREMENT)) {
            result.setTokenValue(--doubleValue);
        } else if (operator.equals(PrefixExpression.Operator.PLUS)) {
            result.setTokenValue(+doubleValue);
        } else if (operator.equals(PrefixExpression.Operator.MINUS)) {
            result.setTokenValue(-doubleValue);
        } else {
            throw new RuntimeException("Invalid prefix operator for number literal");
        }

        return result;
    }

    private static BooleanLiteralNode compareTwoInfixBooleanLiteral(BooleanLiteralNode literal1, InfixExpression.Operator operator,
                                                                    BooleanLiteralNode literal2) {
        BooleanLiteralNode result = new BooleanLiteralNode();

        boolean booleanValue1 = literal1.getValue();
        boolean booleanValue2 = literal2.getValue();

        if (operator.equals(InfixExpression.Operator.CONDITIONAL_OR)) {
            result.setValue(booleanValue1 || booleanValue2);
        } else if (operator.equals(InfixExpression.Operator.CONDITIONAL_AND)) {
            result.setValue(booleanValue1 && booleanValue2);
        } else if (operator.equals(InfixExpression.Operator.EQUALS)) {
            result.setValue(booleanValue1 == booleanValue2);
        } else if (operator.equals(InfixExpression.Operator.NOT_EQUALS)) {
            result.setValue(booleanValue1 != booleanValue2);
        } else if (operator.equals(InfixExpression.Operator.AND)) {
            result.setValue(booleanValue1 & booleanValue2);
        } else if (operator.equals(InfixExpression.Operator.OR)) {
            result.setValue(booleanValue1 | booleanValue2);
        } else if (operator.equals(InfixExpression.Operator.XOR)) {
            result.setValue(booleanValue1 ^ booleanValue2);
        } else {
            throw new RuntimeException("Invalid infix operator for applying to boolean type");
        }

        return result;
    }

    private static BooleanLiteralNode compareTwoInfixStringLiteral(StringLiteralNode literal1,
                                                                   InfixExpression.Operator operator,
                                                                   StringLiteralNode literal2) {
        BooleanLiteralNode result = new BooleanLiteralNode();

        String stringValue1 = literal1.getStringValue();
        String stringValue2 = literal2.getStringValue();

        if (operator.equals(InfixExpression.Operator.EQUALS)) {
            result.setValue(stringValue1 == stringValue2);
        } else if (operator.equals(InfixExpression.Operator.NOT_EQUALS)) {
            result.setValue(stringValue1 != stringValue2);
        } else {
            throw new RuntimeException("Invalid infix operator for applying to String type");
        }

        return result;
    }

    private static StringLiteralNode concatenateTwoInfixStringLiteral(StringLiteralNode literal1,
                                                                      InfixExpression.Operator operator,
                                                                      StringLiteralNode literal2) {
        StringLiteralNode result = new StringLiteralNode();

        String stringValue1 = literal1.getStringValue();
        String stringValue2 = literal2.getStringValue();

        if (operator.equals(InfixExpression.Operator.PLUS)) {
            result.setStringValue(stringValue1 + stringValue2);
        } else {
            throw new RuntimeException("Invalid infix concat operator!!!");
        }

        return result;
    }

    private static IntegerLiteralNode computeTwoInfixBitwiseLiteral(LiteralNode literal1, InfixExpression.Operator operator, LiteralNode literal2) {
        if (!(isBitwiseLiteral(literal1) &&
                isBitwiseLiteral(literal2))) {
            throw new RuntimeException("Literals given are not bitwise literals!!!");
        }

        int intValue1 = changeLiteralNodeToInteger(literal1);
        int intValue2 = changeLiteralNodeToInteger(literal2);

        IntegerLiteralNode result = new IntegerLiteralNode();

        if (operator.equals(InfixExpression.Operator.LEFT_SHIFT)) {
            result.setTokenValue(intValue1 << intValue2);
        } else if (operator.equals(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED)) {
            result.setTokenValue(intValue1 >>> intValue2);
        } else if (operator.equals(InfixExpression.Operator.RIGHT_SHIFT_SIGNED)) {
            result.setTokenValue(intValue1 >> intValue2);
        } else if (operator.equals(InfixExpression.Operator.OR)) {
            result.setTokenValue(intValue1 | intValue2);
        } else if (operator.equals(InfixExpression.Operator.XOR)) {
            result.setTokenValue(intValue1 ^ intValue2);
        } else if (operator.equals(InfixExpression.Operator.AND)) {
            result.setTokenValue(intValue1 & intValue2);
        } else {
            throw new RuntimeException("Invalid infix bitwise operator!!!");
        }

        return result;
    }

    private static DoubleLiteralNode computeTwoInfixLiteral(LiteralNode literal1, InfixExpression.Operator operator, LiteralNode literal2) {
        if (!(isComputableAndComparableLiteral(literal1) &&
                isComputableAndComparableLiteral(literal2))) {
            throw new RuntimeException("Literals given are not computable!!!");
        }

        double doubleValue1 = changeToDoubleLiteral(literal1).getDoubleValue();
        double doubleValue2 = changeToDoubleLiteral(literal2).getDoubleValue();

        DoubleLiteralNode result = new DoubleLiteralNode();

        if (operator.equals(InfixExpression.Operator.PLUS)) {
            result.setTokenValue(doubleValue1 + doubleValue2);
        } else if (operator.equals(InfixExpression.Operator.MINUS)) {
            result.setTokenValue(doubleValue1 - doubleValue2);
        } else if (operator.equals(InfixExpression.Operator.TIMES)) {
            result.setTokenValue(doubleValue1 * doubleValue2);
        } else if (operator.equals(InfixExpression.Operator.DIVIDE)) {
            result.setTokenValue(doubleValue1 / doubleValue2);
        } else if (operator.equals(InfixExpression.Operator.REMAINDER)) {
            result.setTokenValue(doubleValue1 % doubleValue2);
        } else {
            throw new RuntimeException("Invalid infix math operator!!!");
        }

        return result;
    }

    private static BooleanLiteralNode compareTwoInfixLiteral(LiteralNode literal1, InfixExpression.Operator operator, LiteralNode literal2) {
        if (!(isComputableAndComparableLiteral(literal1) &&
                isComputableAndComparableLiteral(literal2))) {
            throw new RuntimeException("Literals given are not comparable!!!");
        }

        double doubleValue1 = changeToDoubleLiteral(literal1).getDoubleValue();
        double doubleValue2 = changeToDoubleLiteral(literal2).getDoubleValue();

        BooleanLiteralNode result = new BooleanLiteralNode();

        if (operator.equals(InfixExpression.Operator.GREATER)) {
            result.setValue(doubleValue1 > doubleValue2);
        } else if (operator.equals(InfixExpression.Operator.LESS)) {
            result.setValue(doubleValue1 < doubleValue2);
        } else if (operator.equals(InfixExpression.Operator.GREATER_EQUALS)) {
            result.setValue(doubleValue1 >= doubleValue2);
        } else if (operator.equals(InfixExpression.Operator.LESS_EQUALS)) {
            result.setValue(doubleValue1 <= doubleValue2);
        } else if (operator.equals(InfixExpression.Operator.EQUALS)) {
            result.setValue(doubleValue1 == doubleValue2);
        } else if (operator.equals(InfixExpression.Operator.NOT_EQUALS)) {
            result.setValue(doubleValue1 != doubleValue2);
        } else {
            throw new RuntimeException("Invalid infix comparison operator!!!");
        }

        return result;
    }

    /*Change char, int, double to double*/
    private static DoubleLiteralNode changeToDoubleLiteral(LiteralNode literal) {
        DoubleLiteralNode doubleLiteralNode = new DoubleLiteralNode();

        if (literal instanceof IntegerLiteralNode) {
            doubleLiteralNode.setTokenValue(Double.parseDouble(((IntegerLiteralNode) literal).getTokenValue()));
            return doubleLiteralNode;
        } else if (literal instanceof CharacterLiteralNode) {
            doubleLiteralNode.setTokenValue(((CharacterLiteralNode) literal).getIntegerValue());
            return doubleLiteralNode;
        } else if (literal instanceof DoubleLiteralNode) {
            return (DoubleLiteralNode) literal;
        } else {
            throw new RuntimeException("You only can change IntegerLiteral, CharacterLiteral or DoubleLiteral to DoubleLiteral!!!");
        }
    }

    /*Change char, number, boolean to double*/
    private static StringLiteralNode changeToStringLiteral(LiteralNode literal) {
        StringLiteralNode stringLiteralNode = new StringLiteralNode();

        if (literal.isStringLiteralNode()) {
            return (StringLiteralNode) literal;
        } else if (literal.isNumberLiteralNode()) {
            stringLiteralNode.setStringValue(((NumberLiteralNode) literal).getTokenValue());
            return stringLiteralNode;
        } else if (literal.isCharacterLiteralNode()) {
            stringLiteralNode.setStringValue(String.valueOf(((CharacterLiteralNode) literal).getCharacterValue()));
            return stringLiteralNode;
        } else if (literal.isBooleanLiteralNode()) {
            stringLiteralNode.setStringValue(String.valueOf(((BooleanLiteralNode) literal).getValue()));
            return stringLiteralNode;
        } else {
            throw new RuntimeException("You only can change NumberLiteral, CharacterLiteral or BooleanLiteral to StringLiteral!!!");
        }
    }

    /*Change Number literal, Character literal to int type*/
    public static int changeLiteralNodeToInteger(LiteralNode literalNode) {
        if (literalNode instanceof NumberLiteralNode) {
            NumberLiteralNode numberLiteralNode = (NumberLiteralNode) literalNode;
            if (NumberLiteralNode.isIntegerValue((numberLiteralNode.getTokenValue()))) {
                return Integer.parseInt(numberLiteralNode.getTokenValue());
            } else {
                throw new RuntimeException("Can't change double to integer");
            }
        } else if (literalNode instanceof CharacterLiteralNode) {
            CharacterLiteralNode characterLiteralNode = (CharacterLiteralNode) literalNode;
            return (int) characterLiteralNode.getIntegerValue();
        } else {
            throw new RuntimeException("Invalid literal to change to integer");
        }
    }

    private static boolean isComputableAndComparableLiteral(LiteralNode literalNode) {
        return (literalNode.isCharacterLiteralNode() ||
                literalNode.isNumberLiteralNode());
    }

    /*Character is considered as integer*/
    private static boolean isBitwiseLiteral(LiteralNode literalNode) {
        return (literalNode instanceof IntegerLiteralNode ||
                literalNode instanceof CharacterLiteralNode);
    }

    public final boolean isNumberLiteralNode() {
        return this instanceof NumberLiteralNode;
    }

    public final boolean isCharacterLiteralNode() {
        return this instanceof CharacterLiteralNode;
    }

    public final boolean isBooleanLiteralNode() {
        return this instanceof BooleanLiteralNode;
    }

    public final boolean isStringLiteralNode() {
        return this instanceof StringLiteralNode;
    }

    public final boolean isNullLiteralNode() {
        return this instanceof NullLiteralNode;
    }

    public final boolean isTypeLiteralNode() {
        return this instanceof TypeLiteralNode;
    }

    public static boolean isLiteral(ASTNode astNode) {
        return (astNode instanceof NumberLiteral ||
                astNode instanceof CharacterLiteral ||
                astNode instanceof StringLiteral ||
                astNode instanceof BooleanLiteral ||
                astNode instanceof NullLiteral ||
                astNode instanceof TypeLiteral);
    }

    public static Class<?> getLiteralClass(ASTNode literal) {
        if (literal instanceof NumberLiteral) {
            NumberLiteral numberLiteral = (NumberLiteral) literal;
            if (NumberLiteralNode.isIntegerValue(numberLiteral.getToken())) {
                return int.class;
            } else {
                return double.class;
            }
        } else if (literal instanceof CharacterLiteral) {
            return char.class;
        } else if (literal instanceof BooleanLiteral) {
            return boolean.class;
        } else if (literal instanceof StringLiteral) {
            return String.class;
        } else {
            throw new RuntimeException("Unsupported Literal: " + literal.getClass());
        }
    }

    abstract public ExpressionNode copy();
}
