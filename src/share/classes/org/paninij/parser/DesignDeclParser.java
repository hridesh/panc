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

import static com.sun.tools.javac.parser.Tokens.TokenKind.*;
import static org.paninij.parser.PaniniTokens.*;

import java.util.Map;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.parser.EndPosTable;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.Lexer;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArrayCall;
import com.sun.tools.javac.tree.JCTree.JCCapsuleWiring;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCDesignBlock;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
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
public class DesignDeclParser {

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

    /** The name table. */
    private Names names;

    /** End position mappings container */
    private final AbstractEndPosTable endPosTable;

    private final JavacParser javaParser;

    /**
     * Construct a parser from a given scanner, tree factory and log.
     *
     * @param initialToken
     * @param lastmode
     * @param mode
     */
    public DesignDeclParser(TreeMaker F, Log log, Names names, Lexer S,
            Map<JCTree, Integer> endPosTable, Token initialToken, int mode,
            int lastmode, JavacParser javaParser) {
        this.S = S;
        this.F = F;
        this.log = log;
        this.names = names;
        this.javaParser = javaParser;

        // recreate state:
        this.token = initialToken;
        this.endPosTable = newEndPosTable(endPosTable);
    }

    private AbstractEndPosTable newEndPosTable(
            Map<JCTree, Integer> keepEndPositions) {
        return keepEndPositions != null ? new SimpleEndPosTable(
                keepEndPositions) : new EmptyEndPosTable();
    }

    /* ---------- token management -------------- */

    private Token token;

    private void nextToken() {
        S.nextToken();
        token = S.token();
    }

    private TokenKind findAfter(TokenKind tk) {
        int i = 0;
        while (true) {
            if (S.token(i).kind == EOF)
                return EOF;

            if (S.token(i).kind == tk) {
                return S.token(i + 1).kind;
            } else
                i++;
        }
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
                        || token.name().toString().equals("design")
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
        return syntaxError(pos, "illegal.start.of.expr");
    }

    /**
     * Report an illegal start of expression/type error at current position.
     */
    private JCExpression illegal() {
        return illegal(token.pos);
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

    private JCIdent identExpression() {
        return toP(F.at(token.pos).Ident(ident()));
    }

    // TODO: replace with delegated call to javaC parser;
    private List<JCExpression> parseParameterList() {
        ListBuffer<JCExpression> lb = new ListBuffer<JCExpression>();
        accept(LPAREN);

        if (token.kind == RPAREN) {
            accept(RPAREN);
            return List.<JCExpression> nil();
        }

        while (true) {
            JCExpression param = parseExpressionWithJavac();
            lb.add(param);
            if ((token.kind == RPAREN))
                break;
            else
                accept(COMMA);
        }
        accept(RPAREN);
        return lb.toList();
    }

    private JCExpression parseCapsuleArrayTypeOptional(JCExpression vartype) {
        if (token.kind == LBRACKET) {
            nextToken();
            JCExpression sizeExpression = parseExpressionWithJavac();
            accept(RBRACKET);
            return toP(F.at(token.pos).CapsuleArray(vartype, sizeExpression));
        }
        return vartype;
    }

    /**
     * <pre>
     * Statement =
     *       For
     *     | TopologyOperator
     *
     * TopologyOperator =
     *       Wireall
     *     | Associate
     *     | Ring
     *     | Star
     * </pre>
     */
    private JCStatement parseStatement() {
        JCStatement returnVal = null;
        switch (token.kind) {
        case FOR: {
            returnVal = parseFor();
            break;
        }
        default: {
            if (isSameKind(token, SYSLANG_WIRE_ALL)) {
                int pos = token.pos;
                nextToken();
                List<JCExpression> args = parseParameterList();
                returnVal = F.Exec(F.at(pos).ManyToOne(args));
            } else if (isSameKind(token, SYSLANG_ASSOCIATE)) {
                int pos = token.pos;
                nextToken();
                List<JCExpression> args = parseParameterList();
                returnVal = F.Exec(F.at(pos).Associate(args));
            } else if (isSameKind(token, SYSLANG_STAR)) {
                int pos = token.pos;
                nextToken();
                List<JCExpression> args = parseParameterList();
                returnVal = F.Exec(F.at(pos).Star(args));
            } else if (isSameKind(token, SYSLANG_RING)) {
                int pos = token.pos;
                nextToken();
                List<JCExpression> args = parseParameterList();
                returnVal = F.Exec(F.at(pos).Ring(args));
            }
            accept(SEMI);
        }
        }

        // we return null if there isn't any statement to parse;
        // this is used to determine whether or not we reached the end of
        // a block during parsing;
        return returnVal;
    }

    /**
     * <pre>
     * For =
     *    JavacForHeader DesignBlock | DesignStatement
     * </pre>
     */
    private JCStatement parseFor() {
        int pot = token.pos;
        accept(FOR);
        List<JCStatement> forInit = parseForInitWithJavaC();
        JCExpression cond = parseForCondWithJavaC();
        List<JCExpressionStatement> forUpdate = parseForUpdateWithJavac();
        JCStatement body = parseForBody();
        return F.at(pot).ForLoop(forInit, cond, forUpdate, body);
    }

    private JCStatement parseForBody() {
        if (token.kind == LBRACE) {
            JCBlock designBlock = designBlock();
            return designBlock;
        } else {
            JCStatement forBody = designStatement().head;
            return forBody;
        }
    }

    /**
     * <pre>
     *    DesignDecl =
     *       "design" Block
     * </pre>
     *
     *
     * @return
     */
    public DesignDeclResult parseDesignDecl(JCModifiers mods) {
        //Design decls cannot have any flags.
        if (mods.flags != 0) {
            syntaxError(mods.pos, "mods.for.design");
        }

        nextToken();
        int pos = token.pos;
        JCDesignBlock result;
        JCBlock body = null;
        if (token.kind == LBRACE) {
            pos = token.pos;
            body = designBlock();
        }else {
            reportSyntaxError(token.pos, "expected", LBRACE);
            //syntaxError(token.pos, "expected", LBRACE);
            // error recovery
            skip(false, true, false, false);
            if (token.kind == LBRACE) {
                body = designBlock();
            }
        }
        result = toP(F.at(pos).WiringBlock(mods, body));
        return new DesignDeclResult(result);
    }

    /**
     * VariableDeclarations = OptModifiers JavacType VariableDeclaration
     * {,VariableDeclaration}*
     */
    private List<JCStatement> variableDeclarations(JCModifiers mods) {
        JCExpression varType = parseTypeWithJavac();
        ListBuffer<JCStatement> variableDecls = new ListBuffer<JCStatement>();
        variableDecls.add(variableDeclaration(mods, varType));
        while (token.kind == COMMA) {
            accept(COMMA);
            JCVariableDecl newDecl = variableDeclaration(mods, varType);
            variableDecls.add(newDecl);
        }
        return variableDecls.toList();
    }

    /**
     *
     * @param mods
     *            Any modifiers
     * @param varType
     *            the base type of this variable declaration, will be further
     *            interpreted to see if it is a capsule Array Type;
     */
    private JCVariableDecl variableDeclaration(JCModifiers mods,
            JCExpression varType) {
        Name variableName = ident();
        JCExpression previousVarType = varType;
        // FIXME-XXX: do better disambiguation between capsule arrays and normal
        // arrays;
        varType = parseCapsuleArrayTypeOptional(varType);
        // if the variable type didn't changed after we've parsed the optional
        // capsule arrayType then we can't initialize
        boolean isInitAllowed = (previousVarType == varType);
        JCExpression varInit = variableInitializerOptional(isInitAllowed,
                variableName);
        JCVariableDecl varDef = F.at(token.pos).VarDef(mods, variableName,
                varType, varInit);
        return toP(varDef);
    }

    /**
     * <pre>
     * Modifier =
     *      {@link  org.paninij.parser.PaniniTokens#CAP_KIND_TASK "task"}
     *    | {@link  org.paninij.parser.PaniniTokens#CAP_KIND_SEQUENTIAL "sequential"}
     *    | {@link  org.paninij.parser.PaniniTokens#CAP_KIND_MONITOR  "monitor"}
     * </pre>
     */
    private JCModifiers parseOptModifiers() {
        if (PaniniTokens.isCapsuleKindModifier(token)) {
            JCModifiers mod = F.at(Position.NOPOS).Modifiers(
                    PaniniTokens.toModfier(token));
            nextToken();
            return mod;
        } else {
            return F.at(Position.NOPOS).Modifiers(0);
        }
    }

    /**
     * <pre>
     *   VariableInitializer = "=" JavacVariableInit
     * </pre>
     *
     * @param isInitAllowed
     *            indicated whether or not a variable initializer is allowed;
     */
    private JCExpression variableInitializerOptional(boolean isInitAllowed,
            Name name) {
        if (token.kind == EQ) {
            if (!isInitAllowed) {
                error(token.endPos, "design.cannot.init.variable", name);
            }
            nextToken();
            return parseVariableInitWithJavac();
        }
        return null;
    }

    /**
     * <pre>
     *   DesignBlock = "{" DesignStatements "}"
     * </pre>
     */
    private JCBlock designBlock() {
        accept(LBRACE);
        List<JCStatement> stats = designStatements();
        JCBlock t = F.at(token.pos).Block(0, stats);
        t.endpos = token.pos;
        accept(RBRACE);
        return toP(t);
    }

    /**
     * <pre>
     *  DesignStatements = DesignStatement*
     * </pre>
     */
    private List<JCStatement> designStatements() {
        ListBuffer<JCStatement> stats = new ListBuffer<JCStatement>();
        while (true) {
            List<JCStatement> statement = designStatement();
            if (statement == null) {
                return stats.toList();
            } else {
                if (token.pos <= endPosTable.errorEndPos) {
                    skip(false, true, true, true);
                }
                stats.addAll(statement);
            }
        }
    }

    /**
     * <pre>
     *  DesignStatement =
     *       Statement
     *     | VariableDecl
     *     | CapsuleWiring
     *     | CapsuleIndexedWiring
     *     | JavacExpression ";"
     * </pre>
     */
    private List<JCStatement> designStatement() {
        if (token.kind == EOF) {
            error(token.pos, "premature.eof");
        }
        if (token.kind == RBRACE) {
            return null;
        }
        if (isStatementStartingToken(token)) {
            JCStatement statement = parseStatement();
            return returnNullOrNonEmptyList(statement);
        } else if (isCapsuleWiringStart()) {
            return returnNullOrNonEmptyList(parseCapsuleWiringStatement());
        } else if (isIndexedCapsuleWiringStart()) {
            return returnNullOrNonEmptyList(parseIndexedCapsuleWiringStatement());
        } else if (isVariableDeclStart()) {
            List<JCStatement> variableDeclarations = variableDeclarations(parseOptModifiers());
            accept(SEMI);
            return variableDeclarations;
        } else {
            JCExpression expression = parseExpressionWithJavac();
            accept(SEMI);
            return returnNullOrNonEmptyList(to(F.at(token.pos).Exec(
                    checkExpressionStatement(expression))));
        }

    }

    /**
     * Method used for disambiguate between DesignStatements;
     */
    private boolean isCapsuleWiringStart() {
        // Identifier(...
        boolean result = (token.kind == IDENTIFIER) && peekToken(LPAREN);
        return result;
    };

    /**
     * Method used for disambiguate between DesignStatements;
     */
    private boolean isIndexedCapsuleWiringStart() {
        // Identifier[...](..
        boolean lBrace = peekToken(LBRACKET);
        TokenKind afterRBracket = findAfter(RBRACKET);
        boolean result = (token.kind == IDENTIFIER)
                && (lBrace && (afterRBracket == LPAREN));
        return result;
    };

    /**
     * Method used for disambiguate between DesignStatements;
     */
    private boolean isVariableDeclStart() {
        boolean isPrimitiveDeclaration = (typetag(token.kind) > 0);

        boolean isSimpleDeclaration = (token.kind == IDENTIFIER)
                && peekToken(IDENTIFIER);

        boolean isArrayDeclaration = (token.kind == IDENTIFIER)
                && peekToken(LBRACKET) && (findAfter(RBRACKET) == IDENTIFIER);

        boolean isConcurrencyTypeModifier = isCapsuleKindModifier(token);

        return isPrimitiveDeclaration || isSimpleDeclaration
                || isConcurrencyTypeModifier || isArrayDeclaration;
    }

    /**
     * Method used for disambiguate between DesignStatements;
     */
    private boolean isStatementStartingToken(Token kind) {
        return isWiringToken(kind) || (kind.kind == FOR);
    }

    /**
     * Method used for disambiguate between DesignStatements;
     */
    private List<JCStatement> returnNullOrNonEmptyList(JCStatement statement) {
        if (statement == null)
            return null;
        else
            return List.<JCStatement> of(statement);
    }

    /**
     * <pre>
     *   Identifier"["JavacExpression"]" ParameterList
     * </pre>
     */
    private JCStatement parseIndexedCapsuleWiringStatement() {
        JCIdent nameOfArray = identExpression();
        accept(LBRACKET);
        JCExpression indexExpression = parseExpressionWithJavac();
        accept(RBRACKET);
        List<JCExpression> args = parseParameterList();
        accept(SEMI);

        JCCapsuleArrayCall indexedWiringExpression = F.at(token.pos)
                .CapsuleArrayCall(nameOfArray.getName(), indexExpression,
                        nameOfArray, args);
        return F.at(token.pos).Exec(indexedWiringExpression);
    }

    /**
     * <pre>
     *   CapsuleWiring = Ident ParameterList
     * </pre>
     */
    private JCStatement parseCapsuleWiringStatement() {
        int pos = token.pos;
        JCExpression capsuleName = identExpression();
        List<JCExpression> params = parseParameterList();
        accept(SEMI);
        JCCapsuleWiring wiringExpression = F.at(pos).WiringApply(
                capsuleName, params);
        return F.Exec(wiringExpression);
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

    /**
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

    /**
     * This class is a copy of inner class from JavacParser. It was added in to
     * avoid the
     *
     * @author lorand
     * @since panini-0.9.2
     */
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

    public class DesignDeclResult {
        public final Token token;
        public final int errorEndPos;
        public final JCDesignBlock designDeclaration;

        protected DesignDeclResult(JCDesignBlock designDeclaration) {
            this.token = DesignDeclParser.this.token;
            this.errorEndPos = endPosTable.errorEndPos;
            this.designDeclaration = designDeclaration;
        }
    }

    private JCExpression parseExpressionWithJavac() {
        initJavaParserState();
        JCExpression result = javaParser.parseExpression();
        restoreDesignParserState();
        return result;
    }

    private JCExpression parseTypeWithJavac() {
        initJavaParserState();
        JCExpression result = javaParser.parseType();
        restoreDesignParserState();
        return result;
    }

    private JCExpression parseVariableInitWithJavac() {
        initJavaParserState();
        JCExpression result = javaParser.variableInitializer();
        restoreDesignParserState();
        return result;
    }

    private List<JCVariableDecl> parseFormalParametersWithJavaC() {
        initJavaParserState();
        List<JCVariableDecl> formalParams = javaParser.parseFormalParameters();
        restoreDesignParserState();
        return formalParams;
    }

    /*-------- FOR loop helpers -------------*/

    private List<JCStatement> parseForInitWithJavaC() {
        initJavaParserState();
        List<JCStatement> init = javaParser.parseForLoopInit();
        restoreDesignParserState();
        return init;
    }

    private JCExpression parseForCondWithJavaC() {
        initJavaParserState();
        JCExpression forCond = javaParser.parseLoopCond();
        restoreDesignParserState();
        return forCond;
    }

    private List<JCExpressionStatement> parseForUpdateWithJavac() {
        initJavaParserState();
        List<JCExpressionStatement> update = javaParser.parseForLoopUpdate();
        restoreDesignParserState();
        return update;
    }

    /*-------- end FOR loop helpers -------------*/

    /**
     * errorPosTable is shared between the JavacParser and the DesignParser no
     * need to initialize it;
     */
    private void initJavaParserState() {
        javaParser.setToken(token);
    }

    private void restoreDesignParserState() {
        this.token = javaParser.getToken();
    }
}
