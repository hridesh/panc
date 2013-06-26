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

import java.util.Map;

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
import com.sun.tools.javac.tree.JCTree.JCSystemDecl;
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
public class SystemParser {

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
    public SystemParser(TreeMaker F, Log log, Names names, Lexer S,
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
    private List<JCExpression> parseArgumentList() {
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

    private JCStatement parseStatement() {
        JCStatement returnVal = null;
        switch (token.kind) {
        case FOR: {
            returnVal = parseFor();
            break;
        }
        case LBRACE: {
            // TODO: add block parsing;
            break;
        }

        default: {
            if (isSameKind(token, SYSLANG_WIRE_ALL)) {
                int pos = token.pos;
                nextToken();
                List<JCExpression> args = parseArgumentList();
                returnVal = F.Exec(F.at(pos).ManyToOne(args));
            } else if (isSameKind(token, SYSLANG_ASSOCIATE)) {
                int pos = token.pos;
                nextToken();
                List<JCExpression> args = parseArgumentList();
                returnVal = F.Exec(F.at(pos).Associate(args));
            } else if (isSameKind(token, SYSLANG_STAR)) {
                int pos = token.pos;
                nextToken();
                List<JCExpression> args = parseArgumentList();
                returnVal = F.Exec(F.at(pos).Star(args));
            } else if (isSameKind(token, SYSLANG_RING)) {
                int pos = token.pos;
                nextToken();
                List<JCExpression> args = parseArgumentList();
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
            JCBlock systemBlock = systemBlock();
            return systemBlock;
        } else {
            JCExpression checkExpressionStatement = checkExpressionStatement(parseExpressionWithJavac());
            JCExpressionStatement forBody = F.at(token.pos).Exec(
                    checkExpressionStatement);
            accept(SEMI);
            return forBody;
        }
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

        List<JCVariableDecl> params = parseFormalParametersWithJavaC();
        JCBlock body = systemBlock();
        JCSystemDecl result = toP(F.at(pos).SystemDef(mod, systemName, body,
                params));
        return new SystemParserResult(result);
    }

    private List<JCVariableDecl> parseFormalParametersWithJavaC() {
        if (token.kind == LPAREN) {
            initJavaParserState();
            List<JCVariableDecl> formalParams = javaParser
                    .parseFormalParameters();
            restoreSystemParserState();
            return formalParams;
        } else if (token.kind == LBRACE)
            return List.<JCVariableDecl> nil();
        else {
            // TODO: better error message
            com.sun.tools.javac.util.Assert.error("illegal system decl");
            return null;
        }
    }

    private List<JCStatement> variableDeclarations() {
        JCModifiers mods = parseOptModifiers();
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
        JCExpression varInit = variableInitializerOptional(isInitAllowed);
        JCVariableDecl varDef = F.at(token.pos).VarDef(mods, variableName,
                varType, varInit);
        return toP(varDef);
    }

    private JCModifiers parseOptModifiers() {
        if (PaniniTokens.isConcurrencyModifier(token)) {
            JCModifiers mod = F.at(Position.NOPOS).Modifiers(
                    PaniniTokens.toModfier(token));
            nextToken();
            return mod;
        } else {
            return F.at(Position.NOPOS).Modifiers(0);
        }
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
            return parseVariableInitWithJavac();
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

    private List<JCStatement> systemStatements() {
        ListBuffer<JCStatement> stats = new ListBuffer<JCStatement>();
        while (true) {
            List<JCStatement> statement = systemStatement();
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

    private List<JCStatement> systemStatement() {
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
            List<JCStatement> variableDeclarations = variableDeclarations();
            accept(SEMI);
            return variableDeclarations;
        } else {
            JCExpression expression = parseExpressionWithJavac();
            accept(SEMI);
            return returnNullOrNonEmptyList(to(F.at(token.pos).Exec(
                    checkExpressionStatement(expression))));
        }

    }

    private List<JCStatement> returnNullOrNonEmptyList(JCStatement statement) {
        if (statement == null)
            return null;
        else
            return List.<JCStatement> of(statement);

    }

    /**
     * Identifier"["Expression"]" "(" {arguments} ")"
     */
    private JCStatement parseIndexedCapsuleWiringStatement() {
        JCIdent nameOfArray = identExpression();
        accept(LBRACKET);
        JCExpression indexExpression = parseExpressionWithJavac();
        accept(RBRACKET);
        List<JCExpression> args = parseArgumentList();
        accept(SEMI);

        JCCapsuleArrayCall indexedWiringExpression = F.at(token.pos)
                .CapsuleArrayCall(nameOfArray.getName(), indexExpression,
                        nameOfArray, args);
        return F.at(token.pos).Exec(indexedWiringExpression);
    }

    private JCStatement parseCapsuleWiringStatement() {
        JCExpression capsuleName = identExpression();
        List<JCExpression> params = parseArgumentList();
        accept(SEMI);
        JCCapsuleWiring wiringExpression = F.at(token.pos).WiringApply(
                capsuleName, params);
        return F.Exec(wiringExpression);
    }

    private boolean isCapsuleWiringStart() {
        // Identifier(...
        boolean result = (token.kind == IDENTIFIER) && peekToken(LPAREN);
        return result;
    };

    private boolean isIndexedCapsuleWiringStart() {
        // Identifier[...](..
        boolean result = (token.kind == IDENTIFIER)
                && (peekToken(LBRACKET) && (findAfter(RBRACKET) == LPAREN));
        return result;
    };

    // TODO: optimize;
    private boolean isVariableDeclStart() {
        boolean isPrimitiveDeclaration = (typetag(token.kind) > 0);

        boolean isSimpleDeclaration = (token.kind == IDENTIFIER)
                && peekToken(IDENTIFIER);

        boolean isArrayDeclaration = (token.kind == IDENTIFIER)
                && peekToken(LBRACKET) && (findAfter(RBRACKET) == IDENTIFIER);

        boolean isConcurrencyTypeModifier = isConcurrencyModifier(token);

        return isPrimitiveDeclaration || isSimpleDeclaration
                || isConcurrencyTypeModifier || isArrayDeclaration;
    }

    private boolean isStatementStartingToken(Token kind) {
        return isWiringToken(kind) || (kind.kind == FOR);
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

    public class SystemParserResult {
        public final Token token;
        public final int errorEndPos;
        public final JCSystemDecl systemDeclaration;

        protected SystemParserResult(JCSystemDecl systemDeclaration) {
            this.token = SystemParser.this.token;
            this.errorEndPos = endPosTable.errorEndPos;
            this.systemDeclaration = systemDeclaration;
        }

    }

    // TODO: add proper state init and reconstruction;
    private JCExpression parseExpressionWithJavac() {
        initJavaParserState();
        JCExpression result = javaParser.parseExpression();
        restoreSystemParserState();
        return result;
    }

    private JCExpression parseTypeWithJavac() {
        initJavaParserState();
        JCExpression result = javaParser.parseType();
        restoreSystemParserState();
        return result;
    }

    private JCExpression parseVariableInitWithJavac() {
        initJavaParserState();
        JCExpression result = javaParser.variableInitializer();
        restoreSystemParserState();
        return result;
    }

    /*-------- FOR loop helpers -------------*/

    private List<JCStatement> parseForInitWithJavaC() {
        initJavaParserState();
        List<JCStatement> init = javaParser.parseForLoopInit();
        restoreSystemParserState();
        return init;
    }

    private JCExpression parseForCondWithJavaC() {
        initJavaParserState();
        JCExpression forCond = javaParser.parseLoopCond();
        restoreSystemParserState();
        return forCond;
    }

    private List<JCExpressionStatement> parseForUpdateWithJavac() {
        initJavaParserState();
        List<JCExpressionStatement> update = javaParser.parseForLoopUpdate();
        restoreSystemParserState();
        return update;
    }

    /*-------- end FOR loop helpers -------------*/

    private void initJavaParserState() {
        javaParser.setToken(token);
    }

    private void restoreSystemParserState() {
        this.token = javaParser.getToken();
    }

    /*
     * --------------------------------------------------------------------------
     * --------------
     * ------------------------------------------------------------
     * ----------------------------
     * ----------------------------------------------
     * ------------------------------------------
     */
    //
    // public JCExpression parseExpressionOld() {
    // int prevmode = mode;
    // mode = EXPR;
    // JCExpression result = term();
    // lastmode = mode;
    // mode = prevmode;
    // return result;
    // }
    //
    // private JCExpression term() {
    // JCExpression t = term1();
    // if ((mode & EXPR) != 0 && token.kind == EQ
    // || PLUSEQ.compareTo(token.kind) <= 0
    // && token.kind.compareTo(GTGTGTEQ) <= 0)
    // return termRest(t);
    // else
    // return t;
    // }
    //
    // private JCExpression termRest(JCExpression t) {
    // switch (token.kind) {
    // case EQ: {
    // int pos = token.pos;
    // nextToken();
    // mode = EXPR;
    // JCExpression t1 = term();
    // return toP(F.at(pos).Assign(t, t1));
    // }
    // case PLUSEQ:
    // case SUBEQ:
    // case STAREQ:
    // case SLASHEQ:
    // case PERCENTEQ:
    // case AMPEQ:
    // case BAREQ:
    // case CARETEQ:
    // case LTLTEQ:
    // case GTGTEQ:
    // case GTGTGTEQ:
    // int pos = token.pos;
    // TokenKind tk = token.kind;
    // nextToken();
    // mode = EXPR;
    // JCExpression t1 = term();
    // return F.at(pos).Assignop(optag(tk), t, t1);
    // default:
    // return t;
    // }
    // }
    //
    // private JCExpression term1() {
    // JCExpression t = term2();
    // if ((mode & EXPR) != 0 && token.kind == QUES) {
    // mode = EXPR;
    // return term1Rest(t);
    // } else {
    // return t;
    // }
    // }
    //
    // private JCExpression term1Rest(JCExpression t) {
    // if (token.kind == QUES) {
    // int pos = token.pos;
    // nextToken();
    // JCExpression t1 = term();
    // accept(COLON);
    // JCExpression t2 = term1();
    // return F.at(pos).Conditional(t, t1, t2);
    // } else {
    // return t;
    // }
    // }
    //
    // private JCExpression term2() {
    // JCExpression t = term3();
    // if ((mode & EXPR) != 0 && prec(token.kind) >= TreeInfo.orPrec) {
    // mode = EXPR;
    // return term2Rest(t, TreeInfo.orPrec);
    // } else {
    // return t;
    // }
    // }
    //
    // private JCExpression term2Rest(JCExpression t, int minprec) {
    // List<JCExpression[]> savedOd = odStackSupply.elems;
    // JCExpression[] odStack = newOdStack();
    // List<Token[]> savedOp = opStackSupply.elems;
    // Token[] opStack = newOpStack();
    //
    // // optimization, was odStack = new Tree[...]; opStack = new Tree[...];
    // int top = 0;
    // odStack[0] = t;
    // int startPos = token.pos;
    // Token topOp = Tokens.DUMMY;
    // while (prec(token.kind) >= minprec) {
    // opStack[top] = topOp;
    // top++;
    // topOp = token;
    // nextToken();
    // odStack[top] = term3();
    // while (top > 0 && prec(topOp.kind) >= prec(token.kind)) {
    // odStack[top - 1] = makeOp(topOp.pos, topOp.kind,
    // odStack[top - 1], odStack[top]);
    // top--;
    // topOp = opStack[top];
    // }
    // }
    // Assert.check(top == 0);
    // t = odStack[0];
    //
    // if (t.hasTag(JCTree.Tag.PLUS)) {
    // StringBuffer buf = foldStrings(t);
    // if (buf != null) {
    // t = toP(F.at(startPos).Literal(TypeTags.CLASS, buf.toString()));
    // }
    // }
    //
    // odStackSupply.elems = savedOd; // optimization
    // opStackSupply.elems = savedOp; // optimization
    // return t;
    // }
    //
    // private JCExpression makeOp(int pos, TokenKind topOp, JCExpression od1,
    // JCExpression od2) {
    // if (topOp == INSTANCEOF) {
    // return F.at(pos).TypeTest(od1, od2);
    // } else {
    // return F.at(pos).Binary(optag(topOp), od1, od2);
    // }
    // }
    //
    // private StringBuffer foldStrings(JCTree tree) {
    // List<String> buf = List.nil();
    // while (true) {
    // if (tree.hasTag(LITERAL)) {
    // JCLiteral lit = (JCLiteral) tree;
    // if (lit.typetag == TypeTags.CLASS) {
    // StringBuffer sbuf = new StringBuffer((String) lit.value);
    // while (buf.nonEmpty()) {
    // sbuf.append(buf.head);
    // buf = buf.tail;
    // }
    // return sbuf;
    // }
    // } else if (tree.hasTag(JCTree.Tag.PLUS)) {
    // JCBinary op = (JCBinary) tree;
    // if (op.rhs.hasTag(LITERAL)) {
    // JCLiteral lit = (JCLiteral) op.rhs;
    // if (lit.typetag == TypeTags.CLASS) {
    // buf = buf.prepend((String) lit.value);
    // tree = op.lhs;
    // continue;
    // }
    // }
    // }
    // return null;
    // }
    // }
    //
    // private ListBuffer<JCExpression[]> odStackSupply = new
    // ListBuffer<JCExpression[]>();
    // private ListBuffer<Token[]> opStackSupply = new ListBuffer<Token[]>();
    //
    // private JCExpression[] newOdStack() {
    // if (odStackSupply.elems == odStackSupply.last)
    // odStackSupply.append(new JCExpression[infixPrecedenceLevels + 1]);
    // JCExpression[] odStack = odStackSupply.elems.head;
    // odStackSupply.elems = odStackSupply.elems.tail;
    // return odStack;
    // }
    //
    // private Token[] newOpStack() {
    // if (opStackSupply.elems == opStackSupply.last)
    // opStackSupply.append(new Token[infixPrecedenceLevels + 1]);
    // Token[] opStack = opStackSupply.elems.head;
    // opStackSupply.elems = opStackSupply.elems.tail;
    // return opStack;
    // }
    //
    // private JCExpression term3() {
    // int pos = token.pos;
    // JCExpression t = null;
    //
    // switch (token.kind) {
    //
    // // Unuary operators
    // case PLUSPLUS:
    // case SUBSUB:
    // case BANG:
    // case TILDE:
    // case PLUS:
    // case SUB: {
    // if ((mode & EXPR) != 0) {
    // TokenKind tk = token.kind;
    // nextToken();
    // mode = EXPR;
    // if (tk == SUB
    // && (token.kind == INTLITERAL || token.kind == LONGLITERAL)
    // && token.radix() == 10) {
    // mode = EXPR;
    // t = literal(names.hyphen, pos);
    // } else {
    // t = term3();
    // return F.at(pos).Unary(unoptag(tk), t);
    // }
    // } else
    // return illegal();
    // break;
    // }
    //
    // // literals:
    // case INTLITERAL:
    // case LONGLITERAL:
    // case FLOATLITERAL:
    // case DOUBLELITERAL:
    // case CHARLITERAL:
    // case STRINGLITERAL:
    // case TRUE:
    // case FALSE: {
    // if ((mode & EXPR) != 0) {
    // mode = EXPR;
    // t = literal(names.empty);
    // } else
    // return illegal();
    // break;
    // }
    //
    // case IDENTIFIER: {
    // if ((mode & EXPR) != 0) {
    // t = toP(F.at(token.pos).Ident(ident()));
    // if (token.kind == LBRACKET) {
    // JCIdent nameOfArray = (JCIdent) t;
    // nextToken();
    // if ((mode & EXPR) != 0) {
    // mode = EXPR;
    // JCExpression indexExpression = term();
    // t = to(F.at(pos).Indexed(t, indexExpression));
    // accept(RBRACKET);
    // t = suffixIndexedCapsuleWiringOptional(t, nameOfArray,
    // indexExpression);
    // }
    // }
    //
    // if (token.kind == LPAREN) {
    // t = suffixCapsuleWiringOptional(t);
    // }
    // }
    // break;
    // }
    //
    // // parentheses for nested operations:
    // case LPAREN: {
    // if ((mode & EXPR) != 0) {
    // nextToken();
    // t = parseExpressionOld();
    // accept(RPAREN);
    // lastmode = mode;
    // mode = EXPR;
    // // TODO: add typecasting
    // } else {
    // return illegal();
    // }
    // t = toP(F.at(pos).Parens(t));
    // break;
    // }
    //
    // default:
    // return illegal();
    // }
    //
    // return t;
    // }
    //
    // @Deprecated
    // private JCExpression suffixCapsuleWiringOptional(JCExpression t) {
    // List<JCExpression> params = parseArgumentList();
    // return F.at(token.pos).WiringApply(t, params);
    //
    // }
    //
    // @Deprecated
    // private JCExpression suffixIndexedCapsuleWiringOptional(JCExpression t,
    // JCIdent nameOfArray, JCExpression indexExpression) {
    // if ((mode & EXPR) != 0 && (token.kind == LPAREN)) {
    // List<JCExpression> args = parseArgumentList();
    // // TODO: Remove when the parser is fixed.
    // // unwrap the inner indexed if there is one.
    // if (t.getTag() == Tag.INDEXED) {
    // System.err
    // .println("Warning found an ArrayAcces expression inside a capsule array wiring expression."
    // + "\nUnwrapping the inner expression -- FIX THE PARSER");
    // t = ((JCArrayAccess) t).indexed;
    // }
    // return F.at(token.pos).CapsuleArrayCall(nameOfArray.getName(),
    // indexExpression, nameOfArray, args);
    // }
    // return t;
    // }

}
