package core.ast.Expression.Literal;

import core.ast.Expression.ExpressionNode;
import org.eclipse.jdt.core.dom.CharacterLiteral;

public class CharacterLiteralNode extends LiteralNode {

    private char characterValue = 'X';

    public static CharacterLiteralNode executeCharacterLiteral(CharacterLiteral characterLiteral) {
        CharacterLiteralNode characterLiteralNode = new CharacterLiteralNode();
        characterLiteralNode.setCharacterValue(characterLiteral.charValue());
        return characterLiteralNode;
    }

    public static CharacterLiteralNode createCharacterLiteral(char value) {
        CharacterLiteralNode literalNode = new CharacterLiteralNode();
        literalNode.setCharacterValue(value);
        return literalNode;
    }

    public static CharacterLiteralNode[] createCharacterLiteralInitializationArray(int capacity) {
        CharacterLiteralNode[] array = new CharacterLiteralNode[capacity];
        for(int i = 0; i < capacity; i++) {
            array[i] = new CharacterLiteralNode();
        }
        return array;
    }

    public String getEscapedValue() {
        return ("'" + this.characterValue + "'");
    }

    public char getCharacterValue() {
        return characterValue;
    }

    public int getIntegerValue() {
        return (int) characterValue;
    }

    public void setCharacterValue(char characterValue) {
        this.characterValue = characterValue;
    }

    @Override
    public String toString() {
        return getEscapedValue();
    }

    @Override
    public ExpressionNode copy() {
        return createCharacterLiteral(this.getCharacterValue());
    }
}
