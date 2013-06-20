/*
 * This file is part of the Panini project at Iowa State University.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * For more details and the latest version of this code please see
 * http://paninij.org
 *
 * Contributor(s):
 */
package org.paninij.parser;

import static com.sun.tools.javac.parser.Tokens.TokenKind.COLON;
import static com.sun.tools.javac.parser.Tokens.TokenKind.COMMA;
import static com.sun.tools.javac.parser.Tokens.TokenKind.DOT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.EOF;
import static com.sun.tools.javac.parser.Tokens.TokenKind.EQ;
import static com.sun.tools.javac.parser.Tokens.TokenKind.GTGTGTEQ;
import static com.sun.tools.javac.parser.Tokens.TokenKind.IDENTIFIER;
import static com.sun.tools.javac.parser.Tokens.TokenKind.INSTANCEOF;
import static com.sun.tools.javac.parser.Tokens.TokenKind.INTLITERAL;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LBRACE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LBRACKET;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LONGLITERAL;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LPAREN;
import static com.sun.tools.javac.parser.Tokens.TokenKind.PLUSEQ;
import static com.sun.tools.javac.parser.Tokens.TokenKind.QUES;
import static com.sun.tools.javac.parser.Tokens.TokenKind.RBRACE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.RBRACKET;
import static com.sun.tools.javac.parser.Tokens.TokenKind.RPAREN;
import static com.sun.tools.javac.parser.Tokens.TokenKind.SEMI;
import static com.sun.tools.javac.parser.Tokens.TokenKind.SUB;
import static com.sun.tools.javac.parser.Tokens.TokenKind.TRUE;
import static com.sun.tools.javac.tree.JCTree.Tag.AND;
import static com.sun.tools.javac.tree.JCTree.Tag.BITAND;
import static com.sun.tools.javac.tree.JCTree.Tag.BITAND_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.BITOR;
import static com.sun.tools.javac.tree.JCTree.Tag.BITOR_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.BITXOR;
import static com.sun.tools.javac.tree.JCTree.Tag.BITXOR_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.COMPL;
import static com.sun.tools.javac.tree.JCTree.Tag.DIV;
import static com.sun.tools.javac.tree.JCTree.Tag.DIV_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.GE;
import static com.sun.tools.javac.tree.JCTree.Tag.LE;
import static com.sun.tools.javac.tree.JCTree.Tag.LITERAL;
import static com.sun.tools.javac.tree.JCTree.Tag.MINUS;
import static com.sun.tools.javac.tree.JCTree.Tag.MINUS_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.MOD;
import static com.sun.tools.javac.tree.JCTree.Tag.MOD_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.MUL;
import static com.sun.tools.javac.tree.JCTree.Tag.MUL_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.NE;
import static com.sun.tools.javac.tree.JCTree.Tag.NEG;
import static com.sun.tools.javac.tree.JCTree.Tag.NOT;
import static com.sun.tools.javac.tree.JCTree.Tag.NO_TAG;
import static com.sun.tools.javac.tree.JCTree.Tag.OR;
import static com.sun.tools.javac.tree.JCTree.Tag.PLUS_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.POS;
import static com.sun.tools.javac.tree.JCTree.Tag.PREDEC;
import static com.sun.tools.javac.tree.JCTree.Tag.PREINC;
import static com.sun.tools.javac.tree.JCTree.Tag.SL;
import static com.sun.tools.javac.tree.JCTree.Tag.SL_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.SR;
import static com.sun.tools.javac.tree.JCTree.Tag.SR_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.TYPETEST;
import static com.sun.tools.javac.tree.JCTree.Tag.USR;
import static com.sun.tools.javac.tree.JCTree.Tag.USR_ASG;

import static org.paninij.parser.PaniniTokens.*;

import java.util.HashMap;
import java.util.Map;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.parser.EndPosTable;
import com.sun.tools.javac.parser.Lexer;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSystemDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Convert;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;

/**
 * @author lorand
 * @since panini-0.9.2
 */
public class SystemParser {
    /**
     * The number of precedence levels of infix operators.
     */
    private static final int infixPrecedenceLevels = 10;

    /**
     * The scanner used for lexical analysis.
     */
    private Lexer S;

    /**
     * The factory to be used for abstract syntax tree construction.
     */
    private TreeMaker F;

    /**
     * The log to be used for error diagnostics.
     */
    private Log log;

    /** The Source language setting. */
    private Source source;

    /** The name table. */
    private Names names;

    /** End position mappings container */
    private final AbstractEndPosTable endPosTable;

    /**
     * Construct a parser from a given scanner, tree factory and log.
     * 
     * @param initialToken
     * @param lastmode
     * @param mode
     */
    public SystemParser(TreeMaker F, Log log, Names names, Source source,
            Lexer S, boolean keepDocComments, boolean keepLineMap,
            Map<JCTree, Integer> endPosTable, Token initialToken, int mode,
            int lastmode) {
        this.S = S;
        this.F = F;
        this.log = log;
        this.names = names;
        this.source = source;
        this.allowGenerics = source.allowGenerics();
        this.allowVarargs = source.allowVarargs();
        this.allowAsserts = source.allowAsserts();
        this.allowEnums = source.allowEnums();
        this.allowForeach = source.allowForeach();
        this.allowStaticImport = source.allowStaticImport();
        this.allowAnnotations = source.allowAnnotations();
        this.allowTWR = source.allowTryWithResources();
        this.allowDiamond = source.allowDiamond();
        this.allowMulticatch = source.allowMulticatch();
        this.keepDocComments = keepDocComments;
        docComments = keepDocComments ? new HashMap<JCTree, String>() : null;
        this.keepLineMap = keepLineMap;
        this.errorTree = F.Erroneous();

        // recreate state:
        this.token = initialToken;
        this.mode = mode;
        this.lastmode = lastmode;
        this.endPosTable = newEndPosTable(endPosTable);
    }

    private AbstractEndPosTable newEndPosTable(
            Map<JCTree, Integer> keepEndPositions) {
        return keepEndPositions != null ? new SimpleEndPosTable(
                keepEndPositions) : new EmptyEndPosTable();
    }

    /**
     * Switch: Should generics be recognized?
     */
    @Deprecated
    private boolean allowGenerics;

    /**
     * Switch: Should diamond operator be recognized?
     */
    @Deprecated
    private boolean allowDiamond;

    /**
     * Switch: Should multicatch clause be accepted?
     */
    @Deprecated
    private boolean allowMulticatch;

    /**
     * Switch: Should varargs be recognized?
     */
    @Deprecated
    private boolean allowVarargs;

    /**
     * Switch: should we recognize assert statements, or just give a warning?
     */
    private boolean allowAsserts;

    /**
     * Switch: should we recognize enums, or just give a warning?
     */
    @Deprecated
    private boolean allowEnums;

    /**
     * Switch: should we recognize foreach?
     */
    @Deprecated
    private boolean allowForeach;

    /**
     * Switch: should we recognize foreach?
     */
    @Deprecated
    private boolean allowStaticImport;

    /**
     * Switch: should we recognize annotations?
     */
    @Deprecated
    private boolean allowAnnotations;

    /**
     * Switch: should we recognize try-with-resources?
     */
    @Deprecated
    private boolean allowTWR;

    /**
     * Switch: should we fold strings?
     */
    @Deprecated
    private boolean allowStringFolding;

    /**
     * Switch: should we recognize lambda expressions?
     */
    @Deprecated
    private boolean allowLambda;

    /**
     * Switch: should we allow method/constructor references?
     */
    @Deprecated
    private boolean allowMethodReferences;

    /**
     * Switch: should we keep docComments?
     */
    private boolean keepDocComments;

    /**
     * Switch: should we keep line table?
     */
    private boolean keepLineMap;

    /**
     * When terms are parsed, the mode determines which is expected: mode = EXPR
     * : an expression mode = TYPE : a type mode = NOPARAMS : no parameters
     * allowed for type mode = TYPEARG : type argument
     */
    static final int EXPR = 0x1;
    static final int TYPE = 0x2;
    static final int NOPARAMS = 0x4;
    static final int TYPEARG = 0x8;
    static final int DIAMOND = 0x10;

    /**
     * The current mode.
     */
    private int mode = 0;

    /**
     * The mode of the term that was parsed last.
     */
    private int lastmode = 0;

    /* ---------- token management -------------- */

    private Token token;

    private void nextToken() {
        S.nextToken();
        token = S.token();
    }

    private boolean peekToken(TokenKind tk) {
        return S.token(1).kind == tk;
    }

    private boolean peekToken(TokenKind tk1, TokenKind tk2) {
        TokenKind t1 = S.token(1).kind;
        TokenKind t2 = S.token(2).kind;
        return t1 == tk1 && t2 == tk2;
    }

    private boolean peekToken(TokenKind tk1, TokenKind tk2, TokenKind tk3) {
        return S.token(1).kind == tk1 && S.token(2).kind == tk2
                && S.token(3).kind == tk3;
    }

    private boolean peekToken(TokenKind... kinds) {
        for (int lookahead = 0; lookahead < kinds.length; lookahead++) {
            if (S.token(lookahead + 1).kind != kinds[lookahead]) {
                return false;
            }
        }
        return true;
    }

    /* ---------- error recovery -------------- */

    private JCErroneous errorTree;

    /**
     * Skip forward until a suitable stop token is found.
     */
    @Deprecated
    private void skip(boolean stopAtImport, boolean stopAtMemberDecl,
            boolean stopAtIdentifier, boolean stopAtStatement) {
        while (true) {
            switch (token.kind) {
            case SEMI:
                nextToken();
                return;
            case PUBLIC:
            case FINAL:
            case ABSTRACT:
            case MONKEYS_AT:
            case EOF:
            case CLASS:
            case INTERFACE:
            case ENUM:
                return;
            case IMPORT:
                if (stopAtImport)
                    return;
                break;
            case LBRACE:
            case RBRACE:
            case PRIVATE:
            case PROTECTED:
            case STATIC:
            case TRANSIENT:
            case NATIVE:
            case VOLATILE:
            case SYNCHRONIZED:
            case STRICTFP:
            case LT:
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
            case VOID:
                if (stopAtMemberDecl)
                    return;
                break;
            case IDENTIFIER:
                // Panini code
                // TODO: FIXME: remove these;
                if (token.name().toString().equals("library")
                        || token.name().toString().equals("system")
                        || token.name().toString().equals("capsule")
                        || token.name().toString().equals("signature"))
                    return;
                // end Panini code
                if (stopAtIdentifier)
                    return;
                break;
            case CASE:
            case DEFAULT:
            case IF:
            case FOR:
            case WHILE:
            case DO:
            case TRY:
            case SWITCH:
            case RETURN:
            case THROW:
            case BREAK:
            case CONTINUE:
            case ELSE:
            case FINALLY:
            case CATCH:
                if (stopAtStatement)
                    return;
                break;
            }
            nextToken();
        }
    }

    private JCErroneous syntaxError(int pos, String key, TokenKind... args) {
        return syntaxError(pos, List.<JCTree> nil(), key, args);
    }

    private JCErroneous syntaxError(int pos, List<JCTree> errs, String key,
            TokenKind... args) {
        setErrorEndPos(pos);
        JCErroneous err = F.at(pos).Erroneous(errs);
        reportSyntaxError(err, key, (Object[]) args);
        if (errs != null) {
            JCTree last = errs.last();
            if (last != null)
                storeEnd(last, pos);
        }
        return toP(err);
    }

    private int errorPos = Position.NOPOS;

    /**
     * Report a syntax using the given the position parameter and arguments,
     * unless one was already reported at the same position.
     */
    private void reportSyntaxError(int pos, String key, Object... args) {
        JCDiagnostic.DiagnosticPosition diag = new JCDiagnostic.SimpleDiagnosticPosition(
                pos);
        reportSyntaxError(diag, key, args);
    }

    /**
     * Report a syntax error using the given DiagnosticPosition object and
     * arguments, unless one was already reported at the same position.
     */
    private void reportSyntaxError(JCDiagnostic.DiagnosticPosition diagPos,
            String key, Object... args) {
        int pos = diagPos.getPreferredPosition();
        if (pos > S.errPos() || pos == Position.NOPOS) {
            if (token.kind == EOF) {
                error(diagPos, "premature.eof");
            } else {
                error(diagPos, key, args);
            }
        }
        S.errPos(pos);
        if (token.pos == errorPos)
            nextToken(); // guarantee progress
        errorPos = token.pos;
    }

    /**
     * Generate a syntax error at current position unless one was already
     * reported at the same position.
     */
    private JCErroneous syntaxError(String key) {
        return syntaxError(token.pos, key);
    }

    /**
     * Generate a syntax error at current position unless one was already
     * reported at the same position.
     */
    private JCErroneous syntaxError(String key, TokenKind arg) {
        return syntaxError(token.pos, key, arg);
    }

    /**
     * If next input token matches given token, skip it, otherwise report an
     * error.
     */
    private void accept(TokenKind tk) {
        if (token.kind == tk) {
            nextToken();
        } else {
            setErrorEndPos(token.pos);
            reportSyntaxError(S.prevToken().endPos, "expected", tk);
        }
    }

    /**
     * Report an illegal start of expression/type error at given position.
     */
    private JCExpression illegal(int pos) {
        setErrorEndPos(pos);
        if ((mode & EXPR) != 0)
            return syntaxError(pos, "illegal.start.of.expr");
        return syntaxError(pos, "illegal.start.of.type");
    }

    /**
     * Report an illegal start of expression/type error at current position.
     */
    private JCExpression illegal() {
        return illegal(token.pos);
    }

    /** Diagnose a modifier flag from the set, if any. */
    private void checkNoMods(long mods) {
        if (mods != 0) {
            long lowestMod = mods & -mods;
            error(token.pos, "mod.not.allowed.here", Flags.asFlagSet(lowestMod));
        }
    }

    /* ---------- doc comments --------- */

    /**
     * A hashtable to store all documentation comments indexed by the tree nodes
     * they refer to. defined only if option flag keepDocComment is set.
     */
    private final Map<JCTree, String> docComments;

    /**
     * Make an entry into docComments hashtable, provided flag keepDocComments
     * is set and given doc comment is non-null.
     * 
     * @param tree
     *            The tree to be used as index in the hashtable
     * @param dc
     *            The doc comment to associate with the tree, or null.
     */
    private void attach(JCTree tree, String dc) {
        if (keepDocComments && dc != null) {
            // System.out.println("doc comment = ");System.out.println(dc);//DEBUG
            docComments.put(tree, dc);
        }
    }

    /* -------- source positions ------- */

    private void setErrorEndPos(int errPos) {
        endPosTable.setErrorEndPos(errPos);
    }

    private void storeEnd(JCTree tree, int endpos) {
        endPosTable.storeEnd(tree, endpos);
    }

    private <T extends JCTree> T to(T t) {
        return endPosTable.to(t);
    }

    private <T extends JCTree> T toP(T t) {
        return endPosTable.toP(t);
    }

    /**
     * Get the start position for a tree node. The start position is defined to
     * be the position of the first character of the first token of the node's
     * source text.
     * 
     * @param tree
     *            The tree node
     */
    public int getStartPos(JCTree tree) {
        return TreeInfo.getStartPos(tree);
    }

    /**
     * Get the end position for a tree node. The end position is defined to be
     * the position of the last character of the last token of the node's source
     * text. Returns Position.NOPOS if end positions are not generated or the
     * position is otherwise not found.
     * 
     * @param tree
     *            The tree node
     */
    public int getEndPos(JCTree tree) {
        return endPosTable.getEndPos(tree);
    }

    /* ---------- parsing -------------- */

    /**
     * Ident = IDENTIFIER
     */
    private Name ident() {
        if (token.kind == IDENTIFIER) {
            Name name = token.name();
            nextToken();
            return name;
        } else {
            accept(IDENTIFIER);
            return names.error;
        }
    }

    /**
     * Qualident = Ident { DOT Ident }
     */
    private JCExpression qualident() {
        JCExpression t = toP(F.at(token.pos).Ident(ident()));
        while (token.kind == DOT) {
            int pos = token.pos;
            nextToken();
            t = toP(F.at(pos).Select(t, ident()));
        }
        return t;
    }

    private JCExpression literal(Name prefix) {
        return literal(prefix, token.pos);
    }

    /**
     * Literal = INTLITERAL | LONGLITERAL | FLOATLITERAL | DOUBLELITERAL |
     * CHARLITERAL | STRINGLITERAL | TRUE | FALSE | NULL
     */
    private JCExpression literal(Name prefix, int pos) {
        JCExpression t = errorTree;
        switch (token.kind) {
        case INTLITERAL:
            try {
                t = F.at(pos).Literal(TypeTags.INT,
                        Convert.string2int(strval(prefix), token.radix()));
            } catch (NumberFormatException ex) {
                error(token.pos, "int.number.too.large", strval(prefix));
            }
            break;
        case LONGLITERAL:
            try {
                t = F.at(pos).Literal(
                        TypeTags.LONG,
                        new Long(Convert.string2long(strval(prefix),
                                token.radix())));
            } catch (NumberFormatException ex) {
                error(token.pos, "int.number.too.large", strval(prefix));
            }
            break;
        case FLOATLITERAL: {
            String proper = token.radix() == 16 ? ("0x" + token.stringVal())
                    : token.stringVal();
            Float n;
            try {
                n = Float.valueOf(proper);
            } catch (NumberFormatException ex) {
                // error already reported in scanner
                n = Float.NaN;
            }
            if (n.floatValue() == 0.0f && !isZero(proper))
                error(token.pos, "fp.number.too.small");
            else if (n.floatValue() == Float.POSITIVE_INFINITY)
                error(token.pos, "fp.number.too.large");
            else
                t = F.at(pos).Literal(TypeTags.FLOAT, n);
            break;
        }
        case DOUBLELITERAL: {
            String proper = token.radix() == 16 ? ("0x" + token.stringVal())
                    : token.stringVal();
            Double n;
            try {
                n = Double.valueOf(proper);
            } catch (NumberFormatException ex) {
                // error already reported in scanner
                n = Double.NaN;
            }
            if (n.doubleValue() == 0.0d && !isZero(proper))
                error(token.pos, "fp.number.too.small");
            else if (n.doubleValue() == Double.POSITIVE_INFINITY)
                error(token.pos, "fp.number.too.large");
            else
                t = F.at(pos).Literal(TypeTags.DOUBLE, n);
            break;
        }
        case CHARLITERAL:
            t = F.at(pos).Literal(TypeTags.CHAR,
                    token.stringVal().charAt(0) + 0);
            break;
        case STRINGLITERAL:
            t = F.at(pos).Literal(TypeTags.CLASS, token.stringVal());
            break;
        case TRUE:
        case FALSE:
            t = F.at(pos).Literal(TypeTags.BOOLEAN,
                    (token.kind == TRUE ? 1 : 0));
            break;
        // case NULL:
        // t = F.at(pos).Literal(
        // TypeTags.BOT,
        // null);
        // break;
        default:
            Assert.error();
        }
        if (t == errorTree)
            t = F.at(pos).Erroneous();
        storeEnd(t, token.endPos);
        nextToken();
        return t;
    }

    private boolean isZero(String s) {
        char[] cs = s.toCharArray();
        int base = ((cs.length > 1 && Character.toLowerCase(cs[1]) == 'x') ? 16
                : 10);
        int i = ((base == 16) ? 2 : 0);
        while (i < cs.length && (cs[i] == '0' || cs[i] == '.'))
            i++;
        return !(i < cs.length && (Character.digit(cs[i], base) > 0));
    }

    private String strval(Name prefix) {
        String s = token.stringVal();
        return prefix.isEmpty() ? s : prefix + s;
    }

    public JCExpression parseExpression() {
        int prevmode = mode;
        mode = EXPR;
        JCExpression result = term();
        lastmode = mode;
        mode = prevmode;
        return result;
    }

    /**
     * <pre>
     *  Expression = Expression1 [ExpressionRest]
     *  ExpressionRest = [AssignmentOperator Expression1]
     *  AssignmentOperator = "=" | "+=" | "-=" | "*=" | "/=" |
     *                       "&=" | "|=" | "^=" |
     *                       "%=" | "<<=" | ">>=" | ">>>="
     *  Type = Type1
     *  TypeNoParams = TypeNoParams1
     *  StatementExpression = Expression
     *  ConstantExpression = Expression
     * </pre>
     */
    private JCExpression term() {
        JCExpression t = term1();
        if ((mode & EXPR) != 0 && token.kind == EQ
                || PLUSEQ.compareTo(token.kind) <= 0
                && token.kind.compareTo(GTGTGTEQ) <= 0)
            return termRest(t);
        else
            return t;
    }

    private JCExpression termRest(JCExpression t) {
        switch (token.kind) {
        case EQ: {
            int pos = token.pos;
            nextToken();
            mode = EXPR;
            JCExpression t1 = term();
            return toP(F.at(pos).Assign(t, t1));
        }
        case PLUSEQ:
        case SUBEQ:
        case STAREQ:
        case SLASHEQ:
        case PERCENTEQ:
        case AMPEQ:
        case BAREQ:
        case CARETEQ:
        case LTLTEQ:
        case GTGTEQ:
        case GTGTGTEQ:
            int pos = token.pos;
            TokenKind tk = token.kind;
            nextToken();
            mode = EXPR;
            JCExpression t1 = term();
            return F.at(pos).Assignop(optag(tk), t, t1);
        default:
            return t;
        }
    }

    /**
     * <pre>
     *  Expression1   = Expression2 [Expression1Rest]
     *  Type1         = Type2
     *  TypeNoParams1 = TypeNoParams2
     * </pre>
     */
    private JCExpression term1() {
        JCExpression t = term2();
        if ((mode & EXPR) != 0 && token.kind == QUES) {
            mode = EXPR;
            return term1Rest(t);
        } else {
            return t;
        }
    }

    /**
     * Expression1Rest = ["?" Expression ":" Expression1]
     */
    private JCExpression term1Rest(JCExpression t) {
        if (token.kind == QUES) {
            int pos = token.pos;
            nextToken();
            JCExpression t1 = term();
            accept(COLON);
            JCExpression t2 = term1();
            return F.at(pos).Conditional(t, t1, t2);
        } else {
            return t;
        }
    }

    /**
     * Expression2 = Expression3 [Expression2Rest] Type2 = Type3 TypeNoParams2 =
     * TypeNoParams3
     */
    private JCExpression term2() {
        JCExpression t = term3();
        if ((mode & EXPR) != 0 && prec(token.kind) >= TreeInfo.orPrec) {
            mode = EXPR;
            return term2Rest(t, TreeInfo.orPrec);
        } else {
            return t;
        }
    }

    /**
     * <pre>
     *    Expression2Rest = {infixop Expression3}
     * FIXME:remove this | Expression3 instanceof Type
     *   infixop         = "||"
     *                   | "&&"
     *                   | "|"
     *                   | "^"
     *                   | "&"
     *                   | "==" | "!="
     *                   | "<" | ">" | "<=" | ">="
     *                   | "<<" | ">>" | ">>>"
     *                   | "+" | "-"
     *                   | "*" | "/" | "%"
     * </pre>
     */
    private JCExpression term2Rest(JCExpression t, int minprec) {
        List<JCExpression[]> savedOd = odStackSupply.elems;
        JCExpression[] odStack = newOdStack();
        List<Token[]> savedOp = opStackSupply.elems;
        Token[] opStack = newOpStack();

        // optimization, was odStack = new Tree[...]; opStack = new Tree[...];
        int top = 0;
        odStack[0] = t;
        int startPos = token.pos;
        Token topOp = Tokens.DUMMY;
        while (prec(token.kind) >= minprec) {
            opStack[top] = topOp;
            top++;
            topOp = token;
            nextToken();
            odStack[top] = term3();
            while (top > 0 && prec(topOp.kind) >= prec(token.kind)) {
                odStack[top - 1] = makeOp(topOp.pos, topOp.kind,
                        odStack[top - 1], odStack[top]);
                top--;
                topOp = opStack[top];
            }
        }
        Assert.check(top == 0);
        t = odStack[0];

        if (t.hasTag(JCTree.Tag.PLUS)) {
            StringBuffer buf = foldStrings(t);
            if (buf != null) {
                t = toP(F.at(startPos).Literal(TypeTags.CLASS, buf.toString()));
            }
        }

        odStackSupply.elems = savedOd; // optimization
        opStackSupply.elems = savedOp; // optimization
        return t;
    }

    /**
     * Construct a binary or type test node.
     */
    private JCExpression makeOp(int pos, TokenKind topOp, JCExpression od1,
            JCExpression od2) {
        if (topOp == INSTANCEOF) {
            return F.at(pos).TypeTest(od1, od2);
        } else {
            return F.at(pos).Binary(optag(topOp), od1, od2);
        }
    }

    /**
     * If tree is a concatenation of string literals, replace it by a single
     * literal representing the concatenated string.
     */
    private StringBuffer foldStrings(JCTree tree) {
        if (!allowStringFolding)
            return null;
        List<String> buf = List.nil();
        while (true) {
            if (tree.hasTag(LITERAL)) {
                JCLiteral lit = (JCLiteral) tree;
                if (lit.typetag == TypeTags.CLASS) {
                    StringBuffer sbuf = new StringBuffer((String) lit.value);
                    while (buf.nonEmpty()) {
                        sbuf.append(buf.head);
                        buf = buf.tail;
                    }
                    return sbuf;
                }
            } else if (tree.hasTag(JCTree.Tag.PLUS)) {
                JCBinary op = (JCBinary) tree;
                if (op.rhs.hasTag(LITERAL)) {
                    JCLiteral lit = (JCLiteral) op.rhs;
                    if (lit.typetag == TypeTags.CLASS) {
                        buf = buf.prepend((String) lit.value);
                        tree = op.lhs;
                        continue;
                    }
                }
            }
            return null;
        }
    }

    /**
     * optimization: To save allocating a new operand/operator stack for every
     * binary operation, we use supplys.
     */
    private ListBuffer<JCExpression[]> odStackSupply = new ListBuffer<JCExpression[]>();
    private ListBuffer<Token[]> opStackSupply = new ListBuffer<Token[]>();

    private JCExpression[] newOdStack() {
        if (odStackSupply.elems == odStackSupply.last)
            odStackSupply.append(new JCExpression[infixPrecedenceLevels + 1]);
        JCExpression[] odStack = odStackSupply.elems.head;
        odStackSupply.elems = odStackSupply.elems.tail;
        return odStack;
    }

    private Token[] newOpStack() {
        if (opStackSupply.elems == opStackSupply.last)
            opStackSupply.append(new Token[infixPrecedenceLevels + 1]);
        Token[] opStack = opStackSupply.elems.head;
        opStackSupply.elems = opStackSupply.elems.tail;
        return opStack;
    }

    /**
     * <pre>
     *  Expression3    = PrefixOp Expression3
     *                 | "(" Expr | TypeNoParams ")" Expression3
     *                 | Primary {Selector} {PostfixOp}
     *  Primary        = "(" Expression ")"
     *                 | Literal
     *                 | [TypeArguments] THIS [Arguments]
     *                 | [TypeArguments] SUPER SuperSuffix
     *                 | NEW [TypeArguments] Creator
     *                 | "(" Arguments ")" "->" ( Expression | Block )
     *                 | Ident "->" ( Expression | Block )
     *                 | Ident { "." Ident }
     *                 | Expression3 MemberReferenceSuffix
     *                   [ "[" ( "]" BracketsOpt "." CLASS | Expression "]" )
     *                   | Arguments
     *                   | "." ( CLASS | THIS | [TypeArguments] SUPER Arguments | NEW [TypeArguments] InnerCreator )
     *                   ]
     *                 | BasicType BracketsOpt "." CLASS
     *  PrefixOp       = "++" | "--" | "!" | "~" | "+" | "-"
     *  PostfixOp      = "++" | "--"
     *  Type3          = Ident { "." Ident } [TypeArguments] {TypeSelector} BracketsOpt
     *                 | BasicType
     *  TypeNoParams3  = Ident { "." Ident } BracketsOpt
     *  Selector       = "." [TypeArguments] Ident [Arguments]
     *                 | "." THIS
     *                 | "." [TypeArguments] SUPER SuperSuffix
     *                 | "." NEW [TypeArguments] InnerCreator
     *                 | "[" Expression "]"
     *  TypeSelector   = "." Ident [TypeArguments]
     *  SuperSuffix    = Arguments | "." Ident [Arguments]
     * </pre>
     */
    private JCExpression term3() {
        int pos = token.pos;
        JCExpression t = null;

        switch (token.kind) {

        // Unuary operators
        case PLUSPLUS:
        case SUBSUB:
        case BANG:
        case TILDE:
        case PLUS:
        case SUB: {
            if ((mode & EXPR) != 0) {
                TokenKind tk = token.kind;
                nextToken();
                mode = EXPR;
                if (tk == SUB
                        && (token.kind == INTLITERAL || token.kind == LONGLITERAL)
                        && token.radix() == 10) {
                    mode = EXPR;
                    t = literal(names.hyphen, pos);
                } else {
                    t = term3();
                    return F.at(pos).Unary(unoptag(tk), t);
                }
            } else
                return illegal();
            break;
        }

        // literals:
        case INTLITERAL:
        case LONGLITERAL:
        case FLOATLITERAL:
        case DOUBLELITERAL:
        case CHARLITERAL:
        case STRINGLITERAL:
        case TRUE:
        case FALSE: {
            if ((mode & EXPR) != 0) {
                mode = EXPR;
                t = literal(names.empty);
            } else
                return illegal();
            break;
        }

        case IDENTIFIER: {
            if ((mode & EXPR) != 0) {
                t = toP(F.at(token.pos).Ident(ident()));
                if (token.kind == LBRACKET) {
                    JCIdent nameOfArray = (JCIdent) t;
                    nextToken();
                    if ((mode & EXPR) != 0) {
                        mode = EXPR;
                        JCExpression indexExpression = term();
                        t = to(F.at(pos).Indexed(t, indexExpression));
                        accept(RBRACKET);
                        t = suffixIndexedCapsuleWiringOptional(t, nameOfArray,
                                indexExpression);
                    }
                }

                if (token.kind == LPAREN) {
                    t = suffixCapsuleWiringOptional(t);
                }
            }
            break;
        }

        // parentheses for nested operations:
        case LPAREN: {
            if ((mode & EXPR) != 0) {
                nextToken();
                t = parseExpression();
                accept(RPAREN);
                lastmode = mode;
                mode = EXPR;
                // TODO: add typecasting
            } else {
                return illegal();
            }
            t = toP(F.at(pos).Parens(t));
            break;
        }

        default:
            return illegal();
        }

        return t;
    }

    private JCExpression suffixCapsuleWiringOptional(JCExpression t) {
        List<JCExpression> params = parseArgumentList();
        return F.at(token.pos).WiringApply(t, params);

    }

    private JCExpression suffixIndexedCapsuleWiringOptional(JCExpression t,
            JCIdent nameOfArray, JCExpression indexExpression) {
        if ((mode & EXPR) != 0 && (token.kind == LPAREN)) {
            List<JCExpression> args = parseArgumentList();
            return F.at(token.pos).CapsuleArrayCall(nameOfArray.getName(),
                    indexExpression, t, args);
        }
        return t;
    }

    /**
     * @return
     */
    private List<JCExpression> parseArgumentList() {
        ListBuffer<JCExpression> lb = new ListBuffer<JCExpression>();
        accept(LPAREN);
        while (true) {
            JCExpression param = parseExpression();
            lb.add(param);
            if ((token.kind == RPAREN))
                break;
            else
                accept(COMMA);
        }
        accept(RPAREN);
        return lb.toList();
    }

    public JCExpression parseType() {
        JCExpression vartype;

        if (token.kind == IDENTIFIER) {
            Name typeName = ident();
            vartype = F.at(token.pos).Ident(typeName);
            if (isAcceptableTypeForNormalArray(typeName)) {
                vartype = parseArrayTypeOptional(vartype);
            } else {
                vartype = parseCapsuleArrayTypeOptional(vartype);
            }
        } else {
            vartype = basicType();
            vartype = parseArrayTypeOptional(vartype);
        }
        return vartype;
    }

    /**
     * We have to differentiate capsule arrays and other arrays.
     */
    private boolean isAcceptableTypeForNormalArray(Name typeName) {
        ListBuffer<String> acc = new ListBuffer<String>();
        acc.add("String");
        return acc.contains(typeName.toString());
    }

    private JCExpression parseCapsuleArrayTypeOptional(JCExpression vartype) {
        if (token.kind == LBRACKET) {
            nextToken();
            JCExpression sizeExpression = parseExpression();
            accept(RBRACKET);
            return toP(F.at(token.pos).CapsuleArray(vartype, sizeExpression));
        }
        return vartype;
    }

    private JCExpression parseArrayTypeOptional(JCExpression vartype) {
        if (token.kind == LBRACKET) {
            nextToken();
            accept(RBRACKET);
            vartype = toP(F.at(token.pos).TypeArray(vartype));
        }
        return vartype;
    }

    /**
     * BasicType = BYTE | SHORT | CHAR | INT | LONG | FLOAT | DOUBLE | BOOLEAN
     */
    private JCPrimitiveTypeTree basicType() {
        JCPrimitiveTypeTree t = to(F.at(token.pos).TypeIdent(
                typetag(token.kind)));
        nextToken();
        return t;
    }

    private JCStatement parseStatement() {
        if (isSameKind(token, SYSLANG_WIRE_ALL)) {
            int pos = token.pos;
            nextToken();
            List<JCExpression> args = parseArgumentList();
            return F.Exec(F.at(pos).ManyToOne(args));
        }

        // we have to return null if there isn't any statement to parse;
        // this is used to determine whether or not we reached the end of
        // a block during parsing;
        return null;
    }

    /**
     * 
     * <pre>
     *    "system" Identifier [VariableDeclaration] Block
     * </pre>
     * 
     * @param mods
     * @param dc
     * @return
     */
    public SystemParserResult parseSystemDecl(JCModifiers mod, String dc) {
        accept(IDENTIFIER);
        int pos = token.pos;
        Name systemName = ident();

        List<JCVariableDecl> params = systemParametersOptional();

        JCBlock body = systemBlock();
        JCSystemDecl result = toP(F.at(pos).SystemDef(mod, systemName, body,
                params));
        attach(result, dc);
        return new SystemParserResult(result);
    }

    private List<JCVariableDecl> systemParametersOptional() {
        List<JCVariableDecl> params = List.<JCVariableDecl> nil();

        if (token.kind == LPAREN) {
            accept(LPAREN);
            // TODO: parse more and then type check;
            params.head = variableDeclaration(false);
            accept(RPAREN);
        }
        // TODO: remove this, it should be done during type checking;
        if (params.length() > 1
                || (params.length() == 1 && !params.get(0).getType().toString()
                        .equals("String[]")))
            log.error(token.pos, "system.argument.illegal");

        return params;
    }

    /**
     * <pre>
     * Identifier
     * </pre>
     * 
     */
    private JCVariableDecl variableDeclaration(boolean isInitAllowed) {
        JCExpression vartype = parseType();
        Name variableName = ident();

        return toP(F.at(token.pos).VarDef(F.at(token.pos).Modifiers(0),
                variableName, vartype,
                variableInitializerOptional(isInitAllowed)));
    }

    /**
     * @param isInitAllowed
     * @return
     */
    private JCExpression variableInitializerOptional(boolean isInitAllowed) {
        if (token.kind == EQ) {
            if (!isInitAllowed) {
                rawError("Cannot initialize this variable");
            }
            nextToken();
            return parseExpression();
        }
        return null;
    }

    private JCBlock systemBlock() {
        accept(LBRACE);
        List<JCStatement> stats = systemStatements();
        JCBlock t = F.at(token.pos).Block(0, stats);
        t.endpos = token.pos;
        accept(RBRACE);
        return toP(t);
    }

    /**
     * @return
     */
    private List<JCStatement> systemStatements() {
        ListBuffer<JCStatement> stats = new ListBuffer<JCStatement>();
        while (true) {
            JCStatement statement = systemStatement();
            if (statement == null) {
                return stats.toList();
            } else {
                if (token.pos <= endPosTable.errorEndPos) {
                    skip(false, true, true, true);
                }
                stats.add(statement);
            }
        }
    }

    /**
     * @return
     */
    private JCStatement systemStatement() {
        if (token.kind == RBRACE && peekToken(EOF)) {
            return null;
        }
        if (isStatementStartingToken(token)) {
            return parseStatement();
        } else {
            boolean isVariableDeclStart = isVariableDeclStart();
            if (isVariableDeclStart) {
                JCVariableDecl variableDeclaration = variableDeclaration(true);
                accept(SEMI);
                return variableDeclaration;
            } else {
                JCExpression expression = parseExpression();
                accept(SEMI);
                return to(F.at(token.pos).Exec(
                        checkExpressionStatement(expression)));
            }
        }

    }

    // TODO: repair, seriously broken
    private boolean isVariableDeclStart() {
        boolean isSimpleDeclaration = (token.kind == IDENTIFIER)
                && peekToken(IDENTIFIER);

        boolean isArrayDeclaration = (token.kind == IDENTIFIER)
                && peekToken(LBRACKET, TokenKind.INTLITERAL, RBRACKET,
                        IDENTIFIER);

        boolean isArrayDeclarationWithIdentifier = (token.kind == IDENTIFIER)
                && peekToken(LBRACKET, IDENTIFIER, RBRACKET, IDENTIFIER);

        return (typetag(token.kind) > 0) || isSimpleDeclaration
                || isArrayDeclaration || isArrayDeclarationWithIdentifier;
    }

    private boolean isStatementStartingToken(Token kind) {
        return isWiringToken(kind);
    }

    /* ---------- auxiliary methods -------------- */
    // TODO: replace all of these
    private void rawError(String msg) {
        log.rawError(token.pos, msg);
    }

    private void rawError(int pos, String msg) {
        log.rawError(pos, msg);
    }

    private void error(int pos, String key, Object... args) {
        log.error(DiagnosticFlag.SYNTAX, pos, key, args);
    }

    private void error(DiagnosticPosition pos, String key, Object... args) {
        log.error(DiagnosticFlag.SYNTAX, pos, key, args);
    }

    private void warning(int pos, String key, Object... args) {
        log.warning(pos, key, args);
    }

    /**
     * Check that given tree is a legal expression statement.
     */
    private JCExpression checkExpressionStatement(JCExpression t) {
        switch (t.getTag()) {
        case PREINC:
        case PREDEC:
        case POSTINC:
        case POSTDEC:
        case ASSIGN:
        case BITOR_ASG:
        case BITXOR_ASG:
        case BITAND_ASG:
        case SL_ASG:
        case SR_ASG:
        case USR_ASG:
        case PLUS_ASG:
        case MINUS_ASG:
        case MUL_ASG:
        case DIV_ASG:
        case MOD_ASG:
        case APPLY:
        case NEWCLASS:
            // TODO: rename?
        case MAAPPLY:
        case CAPSULE_WIRING:
        case ERRONEOUS:
            return t;
        default:
            JCExpression ret = F.at(t.pos).Erroneous(List.<JCTree> of(t));
            error(ret, "not.stmt");
            return ret;
        }
    }

    /**
     * Return precedence of operator represented by token, -1 if token is not a
     * binary operator. @see TreeInfo.opPrec
     */
    private static int prec(TokenKind token) {
        JCTree.Tag oc = optag(token);
        return (oc != NO_TAG) ? TreeInfo.opPrec(oc) : -1;
    }

    /**
     * Return the lesser of two positions, making allowance for either one being
     * unset.
     */
    private static int earlier(int pos1, int pos2) {
        if (pos1 == Position.NOPOS)
            return pos2;
        if (pos2 == Position.NOPOS)
            return pos1;
        return (pos1 < pos2 ? pos1 : pos2);
    }

    /**
     * Return operation tag of binary operator represented by token, No_TAG if
     * token is not a binary operator.
     */
    private static JCTree.Tag optag(TokenKind token) {
        switch (token) {
        case BARBAR:
            return OR;
        case AMPAMP:
            return AND;
        case BAR:
            return BITOR;
        case BAREQ:
            return BITOR_ASG;
        case CARET:
            return BITXOR;
        case CARETEQ:
            return BITXOR_ASG;
        case AMP:
            return BITAND;
        case AMPEQ:
            return BITAND_ASG;
        case EQEQ:
            return JCTree.Tag.EQ;
        case BANGEQ:
            return NE;
        case LT:
            return JCTree.Tag.LT;
        case GT:
            return JCTree.Tag.GT;
        case LTEQ:
            return LE;
        case GTEQ:
            return GE;
        case LTLT:
            return SL;
        case LTLTEQ:
            return SL_ASG;
        case GTGT:
            return SR;
        case GTGTEQ:
            return SR_ASG;
        case GTGTGT:
            return USR;
        case GTGTGTEQ:
            return USR_ASG;
        case PLUS:
            return JCTree.Tag.PLUS;
        case PLUSEQ:
            return PLUS_ASG;
        case SUB:
            return MINUS;
        case SUBEQ:
            return MINUS_ASG;
        case STAR:
            return MUL;
        case STAREQ:
            return MUL_ASG;
        case SLASH:
            return DIV;
        case SLASHEQ:
            return DIV_ASG;
        case PERCENT:
            return MOD;
        case PERCENTEQ:
            return MOD_ASG;
        case INSTANCEOF:
            return TYPETEST;
        default:
            return NO_TAG;
        }
    }

    /**
     * Return operation tag of unary operator represented by token, No_TAG if
     * token is not a binary operator.
     */
    private static JCTree.Tag unoptag(TokenKind token) {
        switch (token) {
        case PLUS:
            return POS;
        case SUB:
            return NEG;
        case BANG:
            return NOT;
        case TILDE:
            return COMPL;
        case PLUSPLUS:
            return PREINC;
        case SUBSUB:
            return PREDEC;
        default:
            return NO_TAG;
        }
    }

    /**
     * Return type tag of basic type represented by token, -1 if token is not a
     * basic type identifier.
     */
    private static int typetag(TokenKind token) {
        switch (token) {
        case BYTE:
            return TypeTags.BYTE;
        case CHAR:
            return TypeTags.CHAR;
        case SHORT:
            return TypeTags.SHORT;
        case INT:
            return TypeTags.INT;
        case LONG:
            return TypeTags.LONG;
        case FLOAT:
            return TypeTags.FLOAT;
        case DOUBLE:
            return TypeTags.DOUBLE;
        case BOOLEAN:
            return TypeTags.BOOLEAN;
        default:
            return -1;
        }
    }

    private void checkGenerics() {
        if (!allowGenerics) {
            error(token.pos, "generics.not.supported.in.source", source.name);
            allowGenerics = true;
        }
    }

    private void checkVarargs() {
        if (!allowVarargs) {
            error(token.pos, "varargs.not.supported.in.source", source.name);
            allowVarargs = true;
        }
    }

    private void checkForeach() {
        if (!allowForeach) {
            error(token.pos, "foreach.not.supported.in.source", source.name);
            allowForeach = true;
        }
    }

    private void checkStaticImports() {
        if (!allowStaticImport) {
            error(token.pos, "static.import.not.supported.in.source",
                    source.name);
            allowStaticImport = true;
        }
    }

    private void checkAnnotations() {
        if (!allowAnnotations) {
            error(token.pos, "annotations.not.supported.in.source", source.name);
            allowAnnotations = true;
        }
    }

    private void checkDiamond() {
        if (!allowDiamond) {
            error(token.pos, "diamond.not.supported.in.source", source.name);
            allowDiamond = true;
        }
    }

    private void checkMulticatch() {
        if (!allowMulticatch) {
            error(token.pos, "multicatch.not.supported.in.source", source.name);
            allowMulticatch = true;
        }
    }

    private void checkTryWithResources() {
        if (!allowTWR) {
            error(token.pos, "try.with.resources.not.supported.in.source",
                    source.name);
            allowTWR = true;
        }
    }

    private void checkLambda() {
        if (!allowLambda) {
            log.error(token.pos, "lambda.not.supported.in.source", source.name);
            allowLambda = true;
        }
    }

    private void checkMethodReferences() {
        if (!allowMethodReferences) {
            log.error(token.pos, "method.references.not.supported.in.source",
                    source.name);
            allowMethodReferences = true;
        }
    }

    /*
     * a functional source tree and end position mappings
     */
    private class SimpleEndPosTable extends AbstractEndPosTable {

        private final Map<JCTree, Integer> endPosMap;

        // FIXME: might consider a better solution for this. This data type is
        // duplicated
        // from JavacParser.
        SimpleEndPosTable(Map<JCTree, Integer> initialTable) {
            endPosMap = initialTable;
        }

        protected void storeEnd(JCTree tree, int endpos) {
            endPosMap.put(tree, errorEndPos > endpos ? errorEndPos : endpos);
        }

        protected <T extends JCTree> T to(T t) {
            storeEnd(t, token.endPos);
            return t;
        }

        protected <T extends JCTree> T toP(T t) {
            storeEnd(t, S.prevToken().endPos);
            return t;
        }

        public int getEndPos(JCTree tree) {
            Integer value = endPosMap.get(tree);
            return (value == null) ? Position.NOPOS : value;
        }

        public int replaceTree(JCTree oldTree, JCTree newTree) {
            Integer pos = endPosMap.remove(oldTree);
            if (pos != null) {
                endPosMap.put(newTree, pos);
                return pos;
            }
            return Position.NOPOS;
        }

    }

    /*
     * a default skeletal implementation without any mapping overhead.
     */
    private class EmptyEndPosTable extends AbstractEndPosTable {

        protected void storeEnd(JCTree tree, int endpos) { /* empty */
        }

        protected <T extends JCTree> T to(T t) {
            return t;
        }

        protected <T extends JCTree> T toP(T t) {
            return t;
        }

        public int getEndPos(JCTree tree) {
            return Position.NOPOS;
        }

        public int replaceTree(JCTree oldTree, JCTree newTree) {
            return Position.NOPOS;
        }

    }

    private abstract class AbstractEndPosTable implements EndPosTable {

        /**
         * Store the last error position.
         */
        protected int errorEndPos;

        /**
         * Store ending position for a tree, the value of which is the greater
         * of last error position and the given ending position.
         * 
         * @param tree
         *            The tree.
         * @param endpos
         *            The ending position to associate with the tree.
         */
        protected abstract void storeEnd(JCTree tree, int endpos);

        /**
         * Store current token's ending position for a tree, the value of which
         * will be the greater of last error position and the ending position of
         * the current token.
         * 
         * @param t
         *            The tree.
         */
        protected abstract <T extends JCTree> T to(T t);

        /**
         * Store current token's ending position for a tree, the value of which
         * will be the greater of last error position and the ending position of
         * the previous token.
         * 
         * @param t
         *            The tree.
         */
        protected abstract <T extends JCTree> T toP(T t);

        /**
         * Set the error position during the parsing phases, the value of which
         * will be set only if it is greater than the last stored error
         * position.
         * 
         * @param errPos
         *            The error position
         */
        protected void setErrorEndPos(int errPos) {
            if (errPos > errorEndPos) {
                errorEndPos = errPos;
            }
        }
    }

    // Panini code
    public class SystemParserResult {
        public final Token token;
        public final int mode;
        public final int lastMode;
        public final int errorEndPos;
        public final JCSystemDecl systemDeclaration;

        protected SystemParserResult(JCSystemDecl systemDeclaration) {
            this.token = SystemParser.this.token;
            this.mode = SystemParser.this.mode;
            this.lastMode = SystemParser.this.lastmode;
            this.errorEndPos = endPosTable.errorEndPos;
            this.systemDeclaration = systemDeclaration;
        }

    }
    // end Panini code

}
