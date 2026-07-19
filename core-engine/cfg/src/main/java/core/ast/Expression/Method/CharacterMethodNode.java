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

    // =========================================================
    // Unicode range tables (BMP 0x0000-0xFFFF)
    // Generated from Java Character class semantics.
    // Each row: {lo, hi} inclusive codepoint range.
    // =========================================================

    /** Character.isDigit — Unicode Nd category */
    private static final int[][] DIGIT_RANGES = {
            {0x0030,0x0039},{0x0660,0x0669},{0x06f0,0x06f9},{0x07c0,0x07c9},
            {0x0966,0x096f},{0x09e6,0x09ef},{0x0a66,0x0a6f},{0x0ae6,0x0aef},
            {0x0b66,0x0b6f},{0x0be6,0x0bef},{0x0c66,0x0c6f},{0x0ce6,0x0cef},
            {0x0d66,0x0d6f},{0x0de6,0x0def},{0x0e50,0x0e59},{0x0ed0,0x0ed9},
            {0x0f20,0x0f29},{0x1040,0x1049},{0x1090,0x1099},{0x17e0,0x17e9},
            {0x1810,0x1819},{0x1946,0x194f},{0x19d0,0x19d9},{0x1a80,0x1a89},
            {0x1a90,0x1a99},{0x1b50,0x1b59},{0x1bb0,0x1bb9},{0x1c40,0x1c49},
            {0x1c50,0x1c59},{0xa620,0xa629},{0xa8d0,0xa8d9},{0xa900,0xa909},
            {0xa9d0,0xa9d9},{0xa9f0,0xa9f9},{0xaa50,0xaa59},{0xabf0,0xabf9},
            {0xff10,0xff19}
    };

    /**
     * Character.isUpperCase — Unicode Lu category (BMP key ranges).
     * Includes Greek: Α(0391)..Ρ(03A1), Σ(03A3)..Ω(03AB)
     * Includes Cyrillic: А(0410)..Я(042F)
     */
    private static final int[][] UPPER_RANGES = {
            // Latin Basic + Latin-1
            {0x0041,0x005a},{0x00c0,0x00d6},{0x00d8,0x00de},
            // Latin Extended A/B (alternating pairs — odd=lower, even=upper)
            {0x0100,0x0100},{0x0102,0x0102},{0x0104,0x0104},{0x0106,0x0106},
            {0x0108,0x0108},{0x010a,0x010a},{0x010c,0x010c},{0x010e,0x010e},
            {0x0110,0x0110},{0x0112,0x0112},{0x0114,0x0114},{0x0116,0x0116},
            {0x0118,0x0118},{0x011a,0x011a},{0x011c,0x011c},{0x011e,0x011e},
            {0x0120,0x0120},{0x0122,0x0122},{0x0124,0x0124},{0x0126,0x0126},
            {0x0128,0x0128},{0x012a,0x012a},{0x012c,0x012c},{0x012e,0x012e},
            {0x0130,0x0130},{0x0132,0x0132},{0x0134,0x0134},{0x0136,0x0136},
            {0x0139,0x0139},{0x013b,0x013b},{0x013d,0x013d},{0x013f,0x013f},
            {0x0141,0x0141},{0x0143,0x0143},{0x0145,0x0145},{0x0147,0x0147},
            {0x014a,0x014a},{0x014c,0x014c},{0x014e,0x014e},{0x0150,0x0150},
            {0x0152,0x0152},{0x0154,0x0154},{0x0156,0x0156},{0x0158,0x0158},
            {0x015a,0x015a},{0x015c,0x015c},{0x015e,0x015e},{0x0160,0x0160},
            {0x0162,0x0162},{0x0164,0x0164},{0x0166,0x0166},{0x0168,0x0168},
            {0x016a,0x016a},{0x016c,0x016c},{0x016e,0x016e},{0x0170,0x0170},
            {0x0172,0x0172},{0x0174,0x0174},{0x0176,0x0176},{0x0178,0x0179},
            {0x017b,0x017b},{0x017d,0x017d},
            // IPA/Greek letters with uppercase
            {0x0181,0x0182},{0x0184,0x0184},{0x0186,0x0187},{0x0189,0x018b},
            {0x018e,0x0191},{0x0193,0x0194},{0x0196,0x0198},{0x019c,0x019d},
            {0x019f,0x01a0},{0x01a2,0x01a2},{0x01a4,0x01a4},{0x01a6,0x01a7},
            {0x01a9,0x01a9},{0x01ac,0x01ac},{0x01ae,0x01af},{0x01b1,0x01b3},
            {0x01b5,0x01b5},{0x01b7,0x01b8},{0x01bc,0x01bc},
            // Greek uppercase block: Α..Ρ, Σ..Ω (covers Σ=03A3, Ω=03A9, Ξ=039E etc.)
            {0x0370,0x0370},{0x0372,0x0372},{0x0376,0x0376},{0x037f,0x037f},
            {0x0386,0x0386},{0x0388,0x038a},{0x038c,0x038c},{0x038e,0x038f},
            {0x0391,0x03a1},{0x03a3,0x03ab},
            {0x03cf,0x03cf},{0x03d2,0x03d4},{0x03d8,0x03d8},{0x03da,0x03da},
            {0x03dc,0x03dc},{0x03de,0x03de},{0x03e0,0x03e0},{0x03e2,0x03e2},
            {0x03e4,0x03e4},{0x03e6,0x03e6},{0x03e8,0x03e8},{0x03ea,0x03ea},
            {0x03ec,0x03ec},{0x03ee,0x03ee},{0x03f4,0x03f4},{0x03f7,0x03f7},
            {0x03f9,0x03fa},{0x03fd,0x042f},
            // Cyrillic uppercase А(0410)..Я(042F) + extended
            {0x0460,0x0460},{0x0462,0x0462},{0x0464,0x0464},{0x0466,0x0466},
            {0x0468,0x0468},{0x046a,0x046a},{0x046c,0x046c},{0x046e,0x046e},
            {0x0470,0x0470},{0x0472,0x0472},{0x0474,0x0474},{0x0476,0x0476},
            {0x0478,0x0478},{0x047a,0x047a},{0x047c,0x047c},{0x047e,0x047e},
            {0x0480,0x0480},{0x048a,0x048a},{0x048c,0x048c},{0x048e,0x048e},
            {0x0490,0x0490},{0x0492,0x0492},{0x0494,0x0494},{0x0496,0x0496},
            {0x0498,0x0498},{0x049a,0x049a},{0x049c,0x049c},{0x049e,0x049e},
            {0x04a0,0x04a0},{0x04a2,0x04a2},{0x04a4,0x04a4},{0x04a6,0x04a6},
            {0x04a8,0x04a8},{0x04aa,0x04aa},{0x04ac,0x04ac},{0x04ae,0x04ae},
            {0x04b0,0x04b0},{0x04b2,0x04b2},{0x04b4,0x04b4},{0x04b6,0x04b6},
            {0x04b8,0x04b8},{0x04ba,0x04ba},{0x04bc,0x04bc},{0x04be,0x04be},
            {0x04c0,0x04c1},{0x04c3,0x04c3},{0x04c5,0x04c5},{0x04c7,0x04c7},
            {0x04c9,0x04c9},{0x04cb,0x04cb},{0x04cd,0x04cd},
            {0x04d0,0x04d0},{0x04d2,0x04d2},{0x04d4,0x04d4},{0x04d6,0x04d6},
            {0x04d8,0x04d8},{0x04da,0x04da},{0x04dc,0x04dc},{0x04de,0x04de},
            {0x04e0,0x04e0},{0x04e2,0x04e2},{0x04e4,0x04e4},{0x04e6,0x04e6},
            {0x04e8,0x04e8},{0x04ea,0x04ea},{0x04ec,0x04ec},{0x04ee,0x04ee},
            {0x04f0,0x04f0},{0x04f2,0x04f2},{0x04f4,0x04f4},{0x04f6,0x04f6},
            {0x04f8,0x04f8},{0x04fa,0x04fa},{0x04fc,0x04fc},{0x04fe,0x04fe},
            // Armenian, Georgian uppercase
            {0x0531,0x0556},{0x10a0,0x10c5},{0x10c7,0x10c7},{0x10cd,0x10cd},
            {0x13a0,0x13f5},{0x1c90,0x1cba},{0x1cbd,0x1cbf},
            // Fullwidth Latin uppercase
            {0xff21,0xff3a}
    };

    /**
     * Character.isLowerCase — Unicode Ll category (BMP key ranges).
     * Includes Greek: α(03B1)..ω(03C9) and lowercase variants
     * Includes Cyrillic: а(0430)..я(044F)
     */
    private static final int[][] LOWER_RANGES = {
            // Latin Basic + Latin-1
            {0x0061,0x007a},{0x00b5,0x00b5},{0x00df,0x00f6},{0x00f8,0x00ff},
            // Latin Extended A/B (odd codepoints are lowercase)
            {0x0101,0x0101},{0x0103,0x0103},{0x0105,0x0105},{0x0107,0x0107},
            {0x0109,0x0109},{0x010b,0x010b},{0x010d,0x010d},{0x010f,0x010f},
            {0x0111,0x0111},{0x0113,0x0113},{0x0115,0x0115},{0x0117,0x0117},
            {0x0119,0x0119},{0x011b,0x011b},{0x011d,0x011d},{0x011f,0x011f},
            {0x0121,0x0121},{0x0123,0x0123},{0x0125,0x0125},{0x0127,0x0127},
            {0x0129,0x0129},{0x012b,0x012b},{0x012d,0x012d},{0x012f,0x012f},
            {0x0131,0x0131},{0x0133,0x0133},{0x0135,0x0135},{0x0137,0x0138},
            {0x013a,0x013a},{0x013c,0x013c},{0x013e,0x013e},{0x0140,0x0140},
            {0x0142,0x0142},{0x0144,0x0144},{0x0146,0x0146},{0x0148,0x0149},
            {0x014b,0x014b},{0x014d,0x014d},{0x014f,0x014f},{0x0151,0x0151},
            {0x0153,0x0153},{0x0155,0x0155},{0x0157,0x0157},{0x0159,0x0159},
            {0x015b,0x015b},{0x015d,0x015d},{0x015f,0x015f},{0x0161,0x0161},
            {0x0163,0x0163},{0x0165,0x0165},{0x0167,0x0167},{0x0169,0x0169},
            {0x016b,0x016b},{0x016d,0x016d},{0x016f,0x016f},{0x0171,0x0171},
            {0x0173,0x0173},{0x0175,0x0175},{0x0177,0x0177},{0x017a,0x017a},
            {0x017c,0x017c},{0x017e,0x0180},
            // Greek lowercase block: ά(03AC)..ώ(03CE), including α..ω (03B1..03C9)
            {0x0390,0x0390},{0x03ac,0x03ce},
            {0x03d0,0x03d1},{0x03d5,0x03d7},{0x03d9,0x03d9},{0x03db,0x03db},
            {0x03dd,0x03dd},{0x03df,0x03df},{0x03e1,0x03e1},{0x03e3,0x03e3},
            {0x03e5,0x03e5},{0x03e7,0x03e7},{0x03e9,0x03e9},{0x03eb,0x03eb},
            {0x03ed,0x03ed},{0x03ef,0x03f3},{0x03f5,0x03f5},{0x03f8,0x03f8},
            {0x03fb,0x03fc},
            // Cyrillic lowercase а(0430)..я(044F)
            {0x0430,0x045f},
            {0x0461,0x0461},{0x0463,0x0463},{0x0465,0x0465},{0x0467,0x0467},
            {0x0469,0x0469},{0x046b,0x046b},{0x046d,0x046d},{0x046f,0x046f},
            {0x0471,0x0471},{0x0473,0x0473},{0x0475,0x0475},{0x0477,0x0477},
            {0x0479,0x0479},{0x047b,0x047b},{0x047d,0x047d},{0x047f,0x047f},
            {0x0481,0x0481},
            // Armenian, Georgian lowercase
            {0x0560,0x0588},{0x10d0,0x10fa},{0x10fd,0x10ff},{0x13f8,0x13fd},
            {0x1c80,0x1c88},
            // Fullwidth Latin lowercase
            {0xff41,0xff5a}
    };

    /**
     * Character.isLetter — Unicode Lu+Ll+Lt+Lm+Lo categories.
     * Key BMP ranges covering Latin, Greek, Cyrillic, CJK, Hangul, Arabic, etc.
     */
    private static final int[][] LETTER_RANGES = {
            {0x0041,0x005a},{0x0061,0x007a},
            {0x00aa,0x00aa},{0x00b5,0x00b5},{0x00ba,0x00ba},
            {0x00c0,0x00d6},{0x00d8,0x00f6},{0x00f8,0x02c1},
            {0x02c6,0x02d1},{0x02e0,0x02e4},{0x02ec,0x02ec},{0x02ee,0x02ee},
            // Full Greek block (covers Α,Β,Γ..Ω and α,β,γ..ω and extended)
            {0x0370,0x0374},{0x0376,0x0377},{0x037a,0x037d},{0x037f,0x037f},
            {0x0386,0x0386},{0x0388,0x038a},{0x038c,0x038c},{0x038e,0x03a1},
            {0x03a3,0x03f5},{0x03f7,0x0481},{0x048a,0x052f},
            // Armenian
            {0x0531,0x0556},{0x0559,0x0559},{0x0560,0x0588},
            // Hebrew
            {0x05d0,0x05ea},{0x05ef,0x05f2},
            // Arabic
            {0x0620,0x064a},{0x066e,0x066f},{0x0671,0x06d3},{0x06d5,0x06d5},
            {0x06e5,0x06e6},{0x06ee,0x06ef},{0x06fa,0x06fc},{0x06ff,0x06ff},
            // Devanagari
            {0x0904,0x0939},{0x093d,0x093d},{0x0950,0x0950},{0x0958,0x0961},{0x0971,0x0980},
            // CJK Unified Ideographs Extension A + B
            {0x3400,0x4dbf},{0x4e00,0xa48c},
            {0xa4d0,0xa4fd},{0xa500,0xa60c},{0xa610,0xa61f},{0xa62a,0xa62b},
            // Hangul Syllables
            {0xac00,0xd7a3},{0xd7b0,0xd7c6},{0xd7cb,0xd7fb},
            // CJK Compatibility Ideographs
            {0xf900,0xfa6d},{0xfa70,0xfad9},
            // Alphabetic Presentation Forms
            {0xfb00,0xfb06},{0xfb13,0xfb17},
            // Fullwidth Latin
            {0xff21,0xff3a},{0xff41,0xff5a},{0xff66,0xffbe},
            {0xffc2,0xffc7},{0xffca,0xffcf},{0xffd2,0xffd7},{0xffda,0xffdc}
    };

    /** Character.isSpaceChar — Unicode Zs+Zl+Zp categories */
    private static final int[][] SPACE_CHAR_RANGES = {
            {0x0020,0x0020},{0x00a0,0x00a0},{0x1680,0x1680},
            {0x2000,0x200a},{0x2028,0x2029},{0x202f,0x202f},{0x205f,0x205f},
            {0x3000,0x3000}
    };

    /**
     * Character.isWhitespace — Java-specific definition:
     * ISO control whitespace + most Zs (excluding NBSP 0xA0, 0x2007, 0x202F).
     */
    private static final int[][] WHITESPACE_RANGES = {
            {0x0009,0x000d},{0x001c,0x0020},{0x1680,0x1680},
            {0x2000,0x2006},{0x2008,0x200a},{0x205f,0x205f},{0x3000,0x3000}
    };

    // =========================================================
    // Entry points
    // =========================================================

    public static AstNode executeCharacterMethod(MethodInvocation methodInvocation,
                                                 MemoryModel memoryModel) {
        CharacterMethodNode node = new CharacterMethodNode();
        node.methodName = methodInvocation.getName().toString();

        List<AstNode> arguments = new ArrayList<>();
        for (int i = 0; i < methodInvocation.arguments().size(); i++) {
            AstNode argNode = ExpressionNode.executeExpression(
                    (Expression) methodInvocation.arguments().get(i), memoryModel);
            arguments.add(argNode);
        }
        node.arguments = arguments;

        Expression expression = methodInvocation.getExpression();
        if (expression != null) {
            node.ownerName = expression.toString();
            if (!"Character".equals(node.ownerName)) {
                node.target = ExpressionNode.executeExpression(expression, memoryModel);
            }
        }

        return executeCharacterMethodNode(node, memoryModel);
    }

    public static ExpressionNode executeCharacterMethodNode(CharacterMethodNode node,
                                                            MemoryModel memoryModel) {
        String methodName = node.methodName;
        List<AstNode> arguments = node.arguments;
        AstNode target = node.target;

        try {
            if ("Character".equals(node.ownerName)) {
                switch (methodName) {
                    case "isDigit":         { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return boolNode(Character.isDigit(ch)); }
                    case "isLetter":        { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return boolNode(Character.isLetter(ch)); }
                    case "isLetterOrDigit": { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return boolNode(Character.isLetterOrDigit(ch)); }
                    case "isUpperCase":     { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return boolNode(Character.isUpperCase(ch)); }
                    case "isLowerCase":     { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return boolNode(Character.isLowerCase(ch)); }
                    case "isWhitespace":    { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return boolNode(Character.isWhitespace(ch)); }
                    case "isSpaceChar":     { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return boolNode(Character.isSpaceChar(ch)); }
                    case "toUpperCase":     { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return charNode(Character.toUpperCase(ch)); }
                    case "toLowerCase":     { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return charNode(Character.toLowerCase(ch)); }
                    case "toTitleCase":     { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return charNode(Character.toTitleCase(ch)); }
                    case "compare": {
                        if (arguments.size() < 2) return node;
                        Character ch1 = getCharValue(arguments.get(0));
                        Character ch2 = getCharValue(arguments.get(1));
                        if (ch1 == null || ch2 == null) return node;
                        return new IntegerLiteralNode(Character.compare(ch1, ch2));
                    }
                    case "getNumericValue": { Character ch = requireCharArg(arguments, 0); if (ch == null) return node; return new IntegerLiteralNode(Character.getNumericValue(ch)); }
                    case "digit": {
                        if (arguments.size() < 2) return node;
                        Character ch = getCharValue(arguments.get(0));
                        Integer radix = getIntValue(arguments.get(1));
                        if (ch == null || radix == null) return node;
                        return new IntegerLiteralNode(Character.digit(ch, radix));
                    }
                    case "toString": {
                        Character ch = requireCharArg(arguments, 0);
                        if (ch == null) return node;
                        StringLiteralNode r = new StringLiteralNode();
                        r.setStringValue(Character.toString(ch));
                        return r;
                    }
                    default: throw new RuntimeException("Unsupported static Character method: " + methodName);
                }
            }

            // Instance methods
            Character targetValue = getCharValue(target);
            if (targetValue == null) return node;

            switch (methodName) {
                case "charValue": return charNode(targetValue);
                case "toString": {
                    StringLiteralNode r = new StringLiteralNode();
                    r.setStringValue(Character.toString(targetValue));
                    return r;
                }
                case "compareTo": {
                    Character arg = requireCharArg(arguments, 0);
                    if (arg == null) return node;
                    return new IntegerLiteralNode(Character.compare(targetValue, arg));
                }
                case "equals": {
                    Character arg = requireCharArg(arguments, 0);
                    if (arg == null) return node;
                    return boolNode(targetValue.equals(arg));
                }
                default: throw new RuntimeException("Unsupported instance Character method: " + methodName);
            }

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // =========================================================
    // Z3 symbolic execution
    // =========================================================

    public static Expr createZ3Expression(CharacterMethodNode node,
                                          MemoryModel memoryModel,
                                          Context ctx,
                                          List<Z3VariableWrapper> vars) {
        switch (node.methodName) {
            case "isDigit":          return handleIsDigit(node, memoryModel, ctx, vars);
            case "isLetter":         return handleIsLetter(node, memoryModel, ctx, vars);
            case "isLetterOrDigit":  return handleIsLetterOrDigit(node, memoryModel, ctx, vars);
            case "isUpperCase":      return handleIsUpperCase(node, memoryModel, ctx, vars);
            case "isLowerCase":      return handleIsLowerCase(node, memoryModel, ctx, vars);
            case "isWhitespace":     return handleIsWhitespace(node, memoryModel, ctx, vars);
            case "isSpaceChar":      return handleIsSpaceChar(node, memoryModel, ctx, vars);
            case "toUpperCase":      return handleToUpperCase(node, memoryModel, ctx, vars);
            case "toLowerCase":      return handleToLowerCase(node, memoryModel, ctx, vars);
            case "compare":          return handleCompare(node, memoryModel, ctx, vars);
            case "compareTo":        return handleCompareTo(node, memoryModel, ctx, vars);
            case "equals":           return handleEquals(node, memoryModel, ctx, vars);
            case "getNumericValue":  return handleGetNumericValue(node, memoryModel, ctx, vars);
            case "digit":            return handleDigit(node, memoryModel, ctx, vars);
            case "toString":         return handleToString(node, memoryModel, ctx, vars);
            case "charValue":        return handleCharValue(node, memoryModel, ctx, vars);
            default: throw new RuntimeException("Unsupported Character method in Z3: " + node.methodName);
        }
    }

    // =========================================================
    // Z3 handlers — all delegate to inUnicodeRanges / unicodeTo*
    // =========================================================

    private static Expr handleIsDigit(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return inUnicodeRanges(resolveCharExpr(n, mm, ctx, vars), DIGIT_RANGES, ctx);
    }
    private static Expr handleIsLetter(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return inUnicodeRanges(resolveCharExpr(n, mm, ctx, vars), LETTER_RANGES, ctx);
    }
    private static Expr handleIsLetterOrDigit(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = resolveCharExpr(n, mm, ctx, vars);
        return ctx.mkOr(inUnicodeRanges(ch, LETTER_RANGES, ctx), inUnicodeRanges(ch, DIGIT_RANGES, ctx));
    }
    private static Expr handleIsUpperCase(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return inUnicodeRanges(resolveCharExpr(n, mm, ctx, vars), UPPER_RANGES, ctx);
    }
    private static Expr handleIsLowerCase(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return inUnicodeRanges(resolveCharExpr(n, mm, ctx, vars), LOWER_RANGES, ctx);
    }
    private static Expr handleIsWhitespace(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return inUnicodeRanges(resolveCharExpr(n, mm, ctx, vars), WHITESPACE_RANGES, ctx);
    }
    private static Expr handleIsSpaceChar(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return inUnicodeRanges(resolveCharExpr(n, mm, ctx, vars), SPACE_CHAR_RANGES, ctx);
    }
    private static Expr handleToUpperCase(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return unicodeToUpperCase(resolveCharExpr(n, mm, ctx, vars), ctx);
    }
    private static Expr handleToLowerCase(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return unicodeToLowerCase(resolveCharExpr(n, mm, ctx, vars), ctx);
    }
    private static Expr handleCompare(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return ctx.mkSub(charCode(getCharExpr(n.arguments.get(0), mm, ctx, vars), ctx),
                charCode(getCharExpr(n.arguments.get(1), mm, ctx, vars), ctx));
    }
    private static Expr handleCompareTo(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return ctx.mkSub(charCode(getCharExpr(n.target, mm, ctx, vars), ctx),
                charCode(getCharExpr(n.arguments.get(0), mm, ctx, vars), ctx));
    }
    private static Expr handleEquals(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return ctx.mkEq(getCharExpr(n.target, mm, ctx, vars),
                getCharExpr(n.arguments.get(0), mm, ctx, vars));
    }
    private static Expr handleGetNumericValue(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return numericValueExpr(resolveCharExpr(n, mm, ctx, vars), ctx);
    }
    private static Expr handleDigit(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        Expr<CharSort> ch = getCharExpr(n.arguments.get(0), mm, ctx, vars);
        IntExpr radix = (IntExpr) OperationExpressionNode.createZ3Expression(
                (ExpressionNode) n.arguments.get(1), ctx, vars, mm);
        IntExpr numericValue = (IntExpr) numericValueExpr(ch, ctx);
        BoolExpr validRadix = ctx.mkAnd(ctx.mkGe(radix, ctx.mkInt(2)), ctx.mkLe(radix, ctx.mkInt(36)));
        BoolExpr validDigit = ctx.mkAnd(ctx.mkGe(numericValue, ctx.mkInt(0)), ctx.mkLt(numericValue, radix));
        return ctx.mkITE(ctx.mkAnd(validRadix, validDigit), numericValue, ctx.mkInt(-1));
    }
    private static Expr handleToString(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return ctx.mkUnit(resolveCharExpr(n, mm, ctx, vars));
    }
    private static Expr handleCharValue(CharacterMethodNode n, MemoryModel mm, Context ctx, List<Z3VariableWrapper> vars) {
        return getCharExpr(n.target, mm, ctx, vars);
    }

    // =========================================================
    // Core Z3 Unicode predicates
    // =========================================================

    /**
     * Returns BoolExpr: charCode(ch) is in ANY of the given Unicode ranges.
     * This replaces all isAsciiXxx helpers — works for any Unicode block.
     */
    private static BoolExpr inUnicodeRanges(Expr<CharSort> ch, int[][] ranges, Context ctx) {
        IntExpr code = charCode(ch, ctx);
        BoolExpr[] clauses = new BoolExpr[ranges.length];
        for (int i = 0; i < ranges.length; i++) {
            if (ranges[i][0] == ranges[i][1]) {
                clauses[i] = ctx.mkEq(code, ctx.mkInt(ranges[i][0]));
            } else {
                clauses[i] = ctx.mkAnd(
                        ctx.mkGe(code, ctx.mkInt(ranges[i][0])),
                        ctx.mkLe(code, ctx.mkInt(ranges[i][1]))
                );
            }
        }
        return clauses.length == 1 ? clauses[0] : ctx.mkOr(clauses);
    }

    /**
     * Unicode-aware toUpperCase via Z3 ITE chain.
     *
     * For each cased block, if the char is in the lowercase range,
     * apply the known offset to get the uppercase form.
     * Characters outside known blocks are returned unchanged.
     *
     * Covers: Latin Basic, Latin-1 Supplement, Greek (α..ω → Α..Ω),
     *         Cyrillic (а..я → А..Я), Fullwidth Latin.
     */
    public static Expr<CharSort> unicodeToUpperCase(Expr<CharSort> ch, Context ctx) {
        IntExpr code = charCode(ch, ctx);

        // Latin Basic: a(61)..z(7A) → offset -32
        BoolExpr isLatinLower    = rangeCheck(code, 0x0061, 0x007a, ctx);
        // Latin-1: à(E0)..þ(FE) excl. ÷(F7) → offset -32
        BoolExpr isLatin1Lower   = ctx.mkAnd(rangeCheck(code, 0x00e0, 0x00fe, ctx),
                ctx.mkNot(ctx.mkEq(code, ctx.mkInt(0x00f7))));
        // Greek: α(03B1)..ω(03C9) → Α(0391)..Ω(03A9), offset -32
        BoolExpr isGreekLower    = rangeCheck(code, 0x03b1, 0x03c9, ctx);
        // Cyrillic: а(0430)..я(044F) → А(0410)..Я(042F), offset -32
        BoolExpr isCyrillicLower = rangeCheck(code, 0x0430, 0x044f, ctx);
        // Fullwidth: ａ(FF41)..ｚ(FF5A) → Ａ(FF21)..Ｚ(FF3A), offset -32
        BoolExpr isFwLower       = rangeCheck(code, 0xff41, 0xff5a, ctx);

        Expr<CharSort> sub32 = charFromOffset(ch, ctx, -32);

        return (Expr<CharSort>) ctx.mkITE(isLatinLower,    sub32,
                ctx.mkITE(isLatin1Lower,   sub32,
                        ctx.mkITE(isGreekLower,    sub32,
                                ctx.mkITE(isCyrillicLower, sub32,
                                        ctx.mkITE(isFwLower,       sub32,
                                                ch)))));
    }

    /**
     * Unicode-aware toLowerCase. Mirror of unicodeToUpperCase.
     */
    public static Expr<CharSort> unicodeToLowerCase(Expr<CharSort> ch, Context ctx) {
        IntExpr code = charCode(ch, ctx);

        // Latin Basic: A(41)..Z(5A) → offset +32
        BoolExpr isLatinUpper    = rangeCheck(code, 0x0041, 0x005a, ctx);
        // Latin-1: À(C0)..Þ(DE) excl. ×(D7) → offset +32
        BoolExpr isLatin1Upper   = ctx.mkAnd(rangeCheck(code, 0x00c0, 0x00de, ctx),
                ctx.mkNot(ctx.mkEq(code, ctx.mkInt(0x00d7))));
        // Greek: Α(0391)..Ω(03A9) → α(03B1)..ω(03C9), offset +32
        BoolExpr isGreekUpper    = rangeCheck(code, 0x0391, 0x03a9, ctx);
        // Cyrillic: А(0410)..Я(042F) → а(0430)..я(044F), offset +32
        BoolExpr isCyrillicUpper = rangeCheck(code, 0x0410, 0x042f, ctx);
        // Fullwidth: Ａ(FF21)..Ｚ(FF3A) → ａ(FF41)..ｚ(FF5A), offset +32
        BoolExpr isFwUpper       = rangeCheck(code, 0xff21, 0xff3a, ctx);

        Expr<CharSort> add32 = charFromOffset(ch, ctx, +32);

        return (Expr<CharSort>) ctx.mkITE(isLatinUpper,    add32,
                ctx.mkITE(isLatin1Upper,   add32,
                        ctx.mkITE(isGreekUpper,    add32,
                                ctx.mkITE(isCyrillicUpper, add32,
                                        ctx.mkITE(isFwUpper,       add32,
                                                ch)))));
    }

    /** Inline range check: lo <= code <= hi */
    private static BoolExpr rangeCheck(IntExpr code, int lo, int hi, Context ctx) {
        if (lo == hi) return ctx.mkEq(code, ctx.mkInt(lo));
        return ctx.mkAnd(ctx.mkGe(code, ctx.mkInt(lo)), ctx.mkLe(code, ctx.mkInt(hi)));
    }

    /**
     * Apply a codepoint offset via BitVec arithmetic:
     * charFromBv(charToBv(ch) ± |offset|)
     */
    private static Expr<CharSort> charFromOffset(Expr<CharSort> ch, Context ctx, int offset) {
        BitVecExpr bv = ctx.charToBv(ch);
        int sz = ((BitVecSort) bv.getSort()).getSize();
        return offset >= 0
                ? ctx.charFromBv(ctx.mkBVAdd(bv, ctx.mkBV(offset, sz)))
                : ctx.charFromBv(ctx.mkBVSub(bv, ctx.mkBV(-offset, sz)));
    }

    /**
     * numericValueExpr: models Character.getNumericValue.
     * Only ASCII digits/letters have numeric values (0-35); everything else → -1.
     */
    private static Expr numericValueExpr(Expr<CharSort> ch, Context ctx) {
        IntExpr code = charCode(ch, ctx);
        BoolExpr isDigit = rangeCheck(code, '0', '9', ctx);
        BoolExpr isUpper = rangeCheck(code, 'A', 'Z', ctx);
        BoolExpr isLower = rangeCheck(code, 'a', 'z', ctx);
        IntExpr digitVal = (IntExpr) ctx.mkSub(code, ctx.mkInt('0'));
        IntExpr upperVal = (IntExpr) ctx.mkAdd(ctx.mkSub(code, ctx.mkInt('A')), ctx.mkInt(10));
        IntExpr lowerVal = (IntExpr) ctx.mkAdd(ctx.mkSub(code, ctx.mkInt('a')), ctx.mkInt(10));
        return ctx.mkITE(isDigit, digitVal, ctx.mkITE(isUpper, upperVal, ctx.mkITE(isLower, lowerVal, ctx.mkInt(-1))));
    }

    private static IntExpr charCode(Expr<CharSort> ch, Context ctx) {
        return ctx.charToInt(ch);
    }

    // =========================================================
    // resolveCharExpr + getCharExpr
    // =========================================================

    /**
     * Pick the primary operand based on call type:
     *   static (ownerName == "Character") → arguments[0]
     *   instance                          → target
     */
    private static Expr<CharSort> resolveCharExpr(CharacterMethodNode node,
                                                  MemoryModel memoryModel,
                                                  Context ctx,
                                                  List<Z3VariableWrapper> vars) {
        AstNode source = "Character".equals(node.ownerName)
                ? node.arguments.get(0)
                : node.target;
        return getCharExpr(source, memoryModel, ctx, vars);
    }

    /**
     * Convert any AstNode to a Z3 CharSort expression.
     *   CharSort   → direct
     *   SeqSort    → mkNth(seq, 0)   (literal or symbolic string of length 1)
     *   IntSort    → intToChar
     *   BitVecSort → charFromBv
     */
    private static Expr<CharSort> getCharExpr(AstNode astNode,
                                              MemoryModel memoryModel,
                                              Context ctx,
                                              List<Z3VariableWrapper> vars) {
        if (astNode == null) throw new RuntimeException("Character argument is null");

        Expr expr = OperationExpressionNode.createZ3Expression(
                (ExpressionNode) astNode, ctx, vars, memoryModel);

        Sort sort = expr.getSort();

        if (sort.equals(ctx.mkCharSort()))   return (Expr<CharSort>) expr;
        if (sort instanceof SeqSort)         return (Expr<CharSort>) ctx.mkNth((SeqExpr<CharSort>) expr, ctx.mkInt(0));
        if (sort instanceof IntSort)         return ctx.intToString((SeqExpr) expr);
        if (sort instanceof BitVecSort)      return ctx.charFromBv((BitVecExpr) expr);

        throw new RuntimeException(
                "Cannot convert to CharSort: node=" + astNode.getClass().getSimpleName() + ", sort=" + sort);
    }

    // =========================================================
    // Concrete helpers
    // =========================================================

    private static Character requireCharArg(List<AstNode> args, int index) {
        if (args == null || args.size() <= index) return null;
        return getCharValue(args.get(index));
    }

    private static Character getCharValue(AstNode node) {
        if (node == null) return null;
        if (node instanceof CharacterLiteralNode) return ((CharacterLiteralNode) node).getCharacterValue();
        if (node instanceof StringLiteralNode) {
            String v = ((StringLiteralNode) node).getStringValue();
            return (v != null && v.length() == 1) ? v.charAt(0) : null;
        }
        if (node instanceof NumberLiteralNode) {
            try {
                int v = (int) Double.parseDouble(((NumberLiteralNode) node).getTokenValue());
                if (v >= Character.MIN_VALUE && v <= Character.MAX_VALUE) return (char) v;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static Integer getIntValue(AstNode node) {
        if (node == null) return null;
        if (node instanceof NumberLiteralNode) {
            try { return (int) Double.parseDouble(((NumberLiteralNode) node).getTokenValue()); }
            catch (NumberFormatException ignored) { return null; }
        }
        if (node instanceof CharacterLiteralNode) return (int) ((CharacterLiteralNode) node).getCharacterValue();
        if (node instanceof StringLiteralNode) {
            String v = ((StringLiteralNode) node).getStringValue();
            if (v != null && v.length() == 1) return (int) v.charAt(0);
        }
        return null;
    }

    // =========================================================
    // Factory helpers
    // =========================================================

    private static BooleanLiteralNode boolNode(boolean value) {
        BooleanLiteralNode n = new BooleanLiteralNode(); n.setValue(value); return n;
    }

    private static CharacterLiteralNode charNode(char value) {
        CharacterLiteralNode n = new CharacterLiteralNode(); n.setCharacterValue(value); return n;
    }
}
