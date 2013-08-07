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
 * Contributor(s): Sean L. Mooney, Lorand Szakacs
 */
package org.paninij.comp;

import java.util.ArrayList;
import java.util.HashMap;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssociate;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArray;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArrayCall;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCCapsuleWiring;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCTopology;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCWireall;
import com.sun.tools.javac.tree.JCTree.JCProcInvocation;
import com.sun.tools.javac.tree.JCTree.JCRing;
import com.sun.tools.javac.tree.JCTree.JCStar;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCDesignBlock;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;

/**
 * Visit the system and interpret java computations to their values.
 * 
 * @author Sean L. Mooney, Lorand Szakacs
 * 
 */
public class DesignDeclRewriter extends TreeTranslator {

    InterpEnv<Name, JCTree> valueEnv;

    /**
     * this is a hack required for keeping track of array sizes.
     */
    InterpEnv<Name, JCVariableDecl> varDefToAstNodeEnv;
    final Log log;
    final TreeMaker make;
    final ArithTreeInterp atInterp;

    final TreeCopier<Void> copy;

    private ClassSymbol parentCapsule;


    public JCDesignBlock rewrite(JCDesignBlock tree) {
        JCDesignBlock translated = super.translate(tree);
        translated.body.stats = unrollStatementsFromBodyStats(translated.body.stats);
        return translated;
    }

    /**
     * TODO: Refactor to removethis at some points in the future.
     * 
     * This method flattens the statements. Unrolled topology statements are
     * implemented as JCStatements nodes that contain a list of statements.
     * 
     * @param stats
     * @return
     */
    private List<JCStatement> unrollStatementsFromBodyStats(
            List<JCStatement> stats) {

        ListBuffer<JCStatement> newStats = new ListBuffer<JCStatement>();

        for (JCStatement statement : stats) {
            if (statement.getKind() == Kind.EXPRESSION_STATEMENT) {
                JCExpressionStatement exprStatement = ((JCExpressionStatement) statement);
                final Kind exprKind = exprStatement.expr.getKind();
                // TOPOLOGY OPERATORS
                if (exprKind == Kind.WIREALL || exprKind == Kind.RING
                        || exprKind == Kind.STAR_TOP
                        || exprKind == Kind.ASSOCIATE) {
                    JCTopology wireall = (JCTopology) exprStatement.expr;
                    for (JCStatement unrolledStatement : wireall.unrolled) {
                        newStats.add(unrolledStatement);
                    }
                } else
                    newStats.add(statement);
            } else if (statement.getKind() == null) {
                JCUnrolledStatement unrolledStmts = (JCUnrolledStatement) (JCTree) statement;
                for (JCStatement unrolledStatement : unrolledStmts.unrolled) {
                    newStats.add(unrolledStatement);
                }

            } else
                newStats.add(statement);
        }
        List<JCStatement> result = newStats.toList();
        return result;
    }

    public DesignDeclRewriter(TreeMaker treeMaker, Log log, ClassSymbol parentCapsule) {
        this.log = log;
        this.make = treeMaker;
        this.parentCapsule = parentCapsule;
        this.copy = new TreeCopier<Void>(make);
        this.atInterp = new ArithTreeInterp();
    }

    @Override
    public void visitIdent(JCIdent tree) {
        JCTree bound = valueEnv.lookup(tree.name);
        // Don't lose the identifier if we don't know about it!
        if (bound != null) {
            if (bound.hasTag((Tag.LITERAL))) {
                result = bound;
            } else {
                result = tree;
            }
        } else {
            result = tree;
        }
    }
    
    /* (non-Javadoc)
     * @see com.sun.tools.javac.tree.TreeTranslator#visitSelect(com.sun.tools.javac.tree.JCTree.JCFieldAccess)
     */
    @Override
    public void visitSelect(JCFieldAccess tree) {
        
        super.visitSelect(tree);
        
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        super.visitVarDef(tree);

        valueEnv.bind(tree.name, tree.init);
        // TODO-XX For Sean
        varDefToAstNodeEnv.bind(tree.name, tree);
    }

    @Override
    public void visitCapsuleArray(JCCapsuleArray tree) {
        super.visitCapsuleArray(tree);
        if (tree.sizeExpr.hasTag(Tag.LITERAL)) {
            tree.size = atInterp.asInt(((JCLiteral) tree.sizeExpr).value);
        } else {
            log.error(tree, "design.decl.cant.compute.array.size", tree.sizeExpr, this.parentCapsule.capsule_info.parentCapsule);
        }
    }

    @Override
    public void visitAssign(JCAssign tree) {

        final JCExpression translatedRHS = this.translate(tree.rhs);
        // in case of uninitialized variables
        // FIXME: might want to consider failing; artifact of not having
        // attribution
        if (translatedRHS != null) {
            tree.rhs = translatedRHS;
        }

        // TODO: explore cases like x = y = 42;
        if (tree.lhs instanceof JCIdent) {
            Name assignTo = ((JCIdent) tree.lhs).name;
            valueEnv.bind(assignTo, tree.rhs);
        } else {
            log.error(tree.lhs.pos(), "rewrite.assign.lhs.not.identifier",
                    tree.lhs);
        }

        // TODO return translatedRHS instead of tree;
        result = tree;
    }

    @Override
    public void visitBinary(final JCBinary tree) {
        JCExpression lhs = translate(tree.lhs);
        JCExpression rhs = translate(tree.rhs);

        if (lhs instanceof JCLiteral && rhs instanceof JCLiteral) {
            JCLiteral lhsLit = (JCLiteral) lhs;
            JCLiteral rhsLit = (JCLiteral) rhs;
            result = atInterp.interp(tree, lhsLit, rhsLit);
        } else {
            tree.lhs = lhs;
            tree.rhs = rhs;
            result = tree;
            // log.rawError(tree.pos, "Trying to interpret " + tree
            // + " but one side is not a literal!");
        }

    }

    @Override
    public void visitUnary(JCUnary tree) {
        super.visitUnary(tree);

        switch (tree.getTag()) {
        case NEG: {
            // TODO: Turn unary neg into a negative literal?
        }
            break;
        case NOT: {
            if (tree.arg.type.isTrue()) {
                result = make.Literal(Boolean.FALSE);
            } else if (tree.arg.type.isFalse()) {
                result = make.Literal(Boolean.TRUE);
            }
        }
            break;
        default:
            // Nothing to be done. Result was set in super call
        }
    }

    @Override
    public void visitWireall(JCWireall tree) {
        super.visitWireall(tree);
        int capsuleArraySize = getCapsuleArraySize(tree.many);

        ListBuffer<JCStatement> unrolledStats = new ListBuffer<JCStatement>();

        for (int i = 0; i < capsuleArraySize; i++) {
            unrolledStats.add(make.Exec( // others -> center
                    createIndexedCapsuleWiring(tree.many, i, tree.args)));
        }

        tree.unrolled = unrolledStats.toList();
        result = tree;
    }

    @Override
    public void visitStar(JCStar tree) {
        super.visitStar(tree);
        JCExpression center = tree.center;
        JCExpression others = tree.others;
        List<JCExpression> args = tree.args;
        int capsuleArraySize = getCapsuleArraySize(others);
        ListBuffer<JCStatement> unrolledStats = new ListBuffer<JCStatement>();

        unrolledStats.add(make.Exec(make.WiringApply(center,
                args.prepend(others))));
        tree.unrolled = unrolledStats.toList();
        result = tree;
    }

    private JCExpression createIndexedCapsuleWiring(JCExpression indexed,
            int index, List<JCExpression> args) {
        return make.CapsuleArrayCall(getIdentName(indexed),
                make.Literal(index), indexed, args);
    }

    /**
     * @param others
     * @return
     */
    private Name getIdentName(JCExpression others) {
        if (others.hasTag(Tag.IDENT)) {
            return ((JCIdent) others).getName();
        } else {
            Assert.error(others + " should be an Identifier.");
            return null; // this will die on the above unchecked exception.
        }
    }

    @Override
    public void visitRing(JCRing tree) {
        super.visitRing(tree);
        JCExpression capsules = tree.capsules;
        List<JCExpression> args = tree.args;
        int capsuleArraySize = getCapsuleArraySize(capsules);
        ListBuffer<JCStatement> unrolledStats = new ListBuffer<JCStatement>();
        for (int i = 0; i < capsuleArraySize - 1; i++) {
            unrolledStats
                    .add(make.Exec(createIndexedCapsuleWiring(
                            capsules,
                            i,
                            args.prepend(make.Indexed(capsules,
                                    make.Literal(i + 1))))));
        }
        unrolledStats.add(make.Exec(createIndexedCapsuleWiring(capsules,
                capsuleArraySize - 1,
                args.prepend(make.Indexed(capsules, make.Literal(0))))));
        tree.unrolled = unrolledStats.toList();
        result = tree;
    }

    @Override
    public void visitAssociate(JCAssociate tree) {
        super.visitAssociate(tree);
        JCExpression src = tree.src;
        JCExpression dest = tree.dest;
        List<JCExpression> args = tree.args;

        // Don't worry about the casts. Typing checking assures the
        // index/len elems are integer types.If during interpretation
        // they are not evaluated to literals then it means that they depend
        // on runtime values.
        if (!tree.srcPos.hasTag(Tag.LITERAL)) {
            Assert.error("The source position parameter of a assoc operator should always evaluate to a literal: "
                    + tree.srcPos);
        }
        if (!tree.destPos.hasTag(Tag.LITERAL)) {
            Assert.error("The destination position parameter of a assoc operator should always evaluate to a literal: "
                    + tree.srcPos);
        }
        if (!tree.len.hasTag(Tag.LITERAL)) {
            Assert.error("The length parameter of a assoc operator should always evaluate to a literal: "
                    + tree.srcPos);
        }
        int srcPos = atInterp.asInt(((JCLiteral) tree.srcPos).value);
        int destPos = atInterp.asInt(((JCLiteral) tree.destPos).value);
        int len = atInterp.asInt(((JCLiteral) tree.len).value);
        ListBuffer<JCStatement> unrolledStats = new ListBuffer<JCStatement>();
        for (int i = 0; i < len; i++) {
            unrolledStats
                    .add(make.Exec(createIndexedCapsuleWiring(
                            src,
                            srcPos + i,
                            args.prepend(make.Indexed(dest,
                                    make.Literal(destPos + i))))));
        }
        tree.unrolled = unrolledStats.toList();

        result = tree;
    }

    @Override
    public void visitForLoop(JCForLoop tree) {
        List<JCStatement> unrolledLoop = executeForLoop(tree);
        result = new JCUnrolledStatement(unrolledLoop);
    }

    /**
     * @param tree
     * @return
     */
    private List<JCStatement> executeForLoop(JCForLoop tree) {
        valueEnv = valueEnv.extend();

        // put the symbols in the env;
        for (JCStatement c : tree.init)
            translate(c);

        ListBuffer<JCStatement> buffer = new ListBuffer<JCStatement>();
        boolean cond = atInterp.asBoolean(((JCLiteral) translate(copy
                .copy(tree.cond))).value);
        while (cond) {
            if (tree.body.getKind() == Kind.BLOCK) {
                JCBlock b = (JCBlock) tree.body;
                for (JCStatement s : b.stats) {
                    executeForStatement(s, buffer);
                }
            } else {
                executeForStatement(tree.body, buffer);
            }

            List<JCExpressionStatement> treeStep = copy.copy(tree.step);
            translate(treeStep);
            cond = atInterp.asBoolean(((JCLiteral) translate(copy
                    .copy(tree.cond))).value);
        }

        valueEnv = valueEnv.pop();
        return buffer.toList();
    }

    private void executeForStatement(JCStatement statement,
            ListBuffer<JCStatement> unrolledLoopBuffer) {
        JCStatement copyOfS = translate(copy.copy(statement));
        // int b = 4 * 2
        // a[i](b)
        // TODO: treat case of wiring regular capsules.
        Kind kindOfS = unboxKindOfExpressionStatement(copyOfS);
        if (kindOfS == Kind.CAPSULE_ARRAY_CALL) {
            unrolledLoopBuffer.add(copyOfS);
        }
    }

    private Kind unboxKindOfExpressionStatement(JCStatement statement) {
        if (statement.getKind() == Kind.EXPRESSION_STATEMENT) {
            return ((JCExpressionStatement) statement).expr.getKind();
        } else
            return statement.getKind();
    }

    private int getCapsuleArraySize(JCExpression array) {
        Name arrayName = getIdentifierName(array);
        JCVariableDecl arrayAST = varDefToAstNodeEnv.lookup(arrayName);
        assert (arrayAST.vartype instanceof JCCapsuleArray) : "wireall expects capsule arrays as the first element";
        int capsuleArraySize = ((JCCapsuleArray) arrayAST.vartype).size;
        assert (capsuleArraySize > 0) : "capsule array sizes should always be > 0; something went wrong";
        return capsuleArraySize;
    }

    /**
     * @param tree
     * @return
     */
    private Name getIdentifierName(JCExpression tree) {
        if (tree instanceof JCIdent) {
            Name arrayName = ((JCIdent) tree).name;
            return arrayName;
        } else {
            // TODO log the error properly:
            // we expected an identifier, but got something else
            log.error("unexpected.type", "identifier", tree.getKind()
                    .toString());
            return null;
        }
    }

    @Override
    public void visitDesignBlock(JCDesignBlock tree) {
        valueEnv = new InterpEnv<Name, JCTree>();
        varDefToAstNodeEnv = new InterpEnv<Name, JCVariableDecl>();

        // translate all individual statements from the system block. This is
        // necessary because we want all subsequent blocks to enclose for
        // statements.
        ListBuffer<JCStatement> statsBuff = new ListBuffer<JCStatement>();
        for (List<JCStatement> l = tree.body.stats; l.nonEmpty(); l = l.tail) {
            statsBuff.add(translate(l.head));
        }
        tree.body.stats = statsBuff.toList();
        result = tree;
    }

    /**
     * Helper to interpret arithmetic expression trees.
     * 
     * TODO: Deal with something besides ints. rename because now it supports
     * booleans
     * 
     * @author sean
     * 
     */
    private class ArithTreeInterp {

        /**
         * A reference to the origin tree, for diagnostic purposes.
         */
        JCTree tree;

        public JCTree interp(final JCTree tree, final JCLiteral lhs,
                final JCLiteral rhs) {
            this.tree = tree; // bind the tree we are current working on;
            final JCTree result;

            switch (tree.getTag()) {
            case PLUS:
                result = interpPlus(lhs, rhs);
                break;
            case MINUS:
                result = interpMinus(lhs, rhs);
                break;
            case MUL:
                result = interpMul(lhs, rhs);
                break;
            case DIV:
                result = interpDiv(lhs, rhs);
                break;
            case MOD:
                result = interpMod(lhs, rhs);
                break;
            case LT:
                result = interpretLT(lhs, rhs);
                break;
            case LE:
                result = interpretLE(lhs, rhs);
                break;
            case GT:
                result = interpretGT(lhs, rhs);
                break;
            case GE:
                result = interpretGE(lhs, rhs);
                break;
            case EQ:
                result = interpretEQ(lhs, rhs);
                break;
            case NE:
                result = interpretNE(lhs, rhs);
                break;
            case AND:
                result = interpretAND(lhs, rhs);
                break;
            case OR:
                result = interpretOR(lhs, rhs);
                break;

            // TODO: Other cases?
            default:
                Assert.error("Unsupported operator during interpretation: "
                        + tree);
                result = tree;
            }

            // get rid of the reference when we are done interpretting it.
            // prevents stale tree type errors.
            this.tree = null;
            return result;
        }

        /**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretOR(JCLiteral lhs, JCLiteral rhs) {
            make.pos = tree.pos;
            return make.Literal(TypeTags.BOOLEAN, (Integer) lhs.value
                    | (Integer) rhs.value);
        }

        /**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretAND(JCLiteral lhs, JCLiteral rhs) {
            make.pos = tree.pos;
            return make.Literal(TypeTags.BOOLEAN, (Integer) lhs.value
                    & (Integer) rhs.value);
        }

        /**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretNE(JCLiteral lhs, JCLiteral rhs) {
            make.pos = tree.pos;
            return make.Literal(TypeTags.BOOLEAN,
                    ((Number) lhs.value != (Number) rhs.value) ? 1 : 0);
        }

        /**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretEQ(JCLiteral lhs, JCLiteral rhs) {
            make.pos = tree.pos;
            return make.Literal(TypeTags.BOOLEAN,
                    ((Number) lhs.value == (Number) rhs.value) ? 1 : 0);
        }

        /**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretGE(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != TypeTags.INT || rhs.typetag != TypeTags.INT) {
                return reportNonInt();
            }
            make.pos = tree.pos;
            return make.Literal(TypeTags.BOOLEAN, (((Number) lhs.value)
                    .intValue() >= ((Number) rhs.value).intValue()) ? 1 : 0);
        }

        /**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretGT(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != TypeTags.INT || rhs.typetag != TypeTags.INT) {
                return reportNonInt();
            }
            make.pos = tree.pos;
            return make.Literal(TypeTags.BOOLEAN, (((Number) lhs.value)
                    .intValue() > ((Number) rhs.value).intValue()) ? 1 : 0);
        }

        /**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretLE(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != TypeTags.INT || rhs.typetag != TypeTags.INT) {
                return reportNonInt();
            }
            make.pos = tree.pos;
            return make.Literal(TypeTags.BOOLEAN, (((Number) lhs.value)
                    .intValue() <= ((Number) rhs.value).intValue()) ? 1 : 0);
        }

        /**
         * @return
         */
        private JCTree interpretLT(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != TypeTags.INT || rhs.typetag != TypeTags.INT) {
                return reportNonInt();
            }
            make.pos = tree.pos;
            return make.Literal(TypeTags.BOOLEAN, (((Number) lhs.value)
                    .intValue() < ((Number) rhs.value).intValue()) ? 1 : 0);
        }

        final JCTree interpPlus(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != TypeTags.INT || rhs.typetag != TypeTags.INT) {
                return reportNonInt();
            }
            make.pos = tree.pos;
            return make.Literal(TypeTags.INT, ((Number) lhs.value).intValue()
                    + ((Number) rhs.value).intValue());
        }

        final JCTree interpMinus(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != TypeTags.INT || rhs.typetag != TypeTags.INT) {
                return reportNonInt();
            }
            make.pos = tree.pos;
            return make.Literal(TypeTags.INT, ((Number) lhs.value).intValue()
                    - ((Number) rhs.value).intValue());
        }

        final JCTree interpMul(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != TypeTags.INT || rhs.typetag != TypeTags.INT) {
                return reportNonInt();
            }
            make.pos = tree.pos;
            return make.Literal(TypeTags.INT, ((Number) lhs.value).intValue()
                    * ((Number) rhs.value).intValue());
        }

        final JCTree interpDiv(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != TypeTags.INT || rhs.typetag != TypeTags.INT) {
                return reportNonInt();
            }
            make.pos = tree.pos;
            return make.Literal(
                    TypeTags.INT,
                    Integer.valueOf(((Number) lhs.value).intValue()
                            / ((Number) rhs.value).intValue()));
        }

        final JCTree interpMod(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != TypeTags.INT || rhs.typetag != TypeTags.INT) {
                return reportNonInt();
            }
            make.pos = tree.pos;
            return make.Literal(TypeTags.INT, ((Number) lhs.value).intValue()
                    % ((Number) rhs.value).intValue());
        }

        private JCTree reportNonInt() {
            // Only allowing integer values in concern of bit precision issues.
            // No log error at this point due to reports breaking regression
            // tests
            return tree;
        }

        final int asInt(Object obj) {
            // FIXME Possible class cast exception.
            return (Integer) obj;
        }

        final boolean asBoolean(Object obj) {
            return (Integer) obj == 1;
        }
    }

    /**
     * Standard linked environment for the interpretor
     * 
     * @author sean
     * 
     */
    private static class InterpEnv<K, V> {
        private final HashMap<K, V> table;
        private final InterpEnv<K, V> parent;

        public InterpEnv() {
            table = new HashMap<K, V>();
            parent = null;
        }

        private InterpEnv(InterpEnv<K, V> other) {
            table = new HashMap<K, V>();
            parent = other;
        }

        public void bind(K k, V v) {
            table.put(k, v);
        }

        public V lookup(K k) {
            V v = table.get(k);
            if (v == null && parent != null) {
                return parent.lookup(k);
            } else {
                return v;
            }
        }

        public InterpEnv<K, V> extend() {
            return new InterpEnv<K, V>(this);
        }

        public InterpEnv<K, V> pop() {
            return parent;
        }
    }

    // FIXME: do something to have a generic solution for both wireall and for;
    private static class JCUnrolledStatement extends JCStatement {
        public List<JCStatement> unrolled;

        /**
         * @param unrolledLoop
         */
        public JCUnrolledStatement(List<JCStatement> unrolledLoop) {
            unrolled = unrolledLoop;
        }

        @Override
        public Kind getKind() {
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.tools.javac.tree.JCTree#getTag()
         */
        @Override
        public Tag getTag() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.sun.tools.javac.tree.JCTree#accept(com.sun.tools.javac.tree.JCTree
         * .Visitor)
         */
        @Override
        public void accept(Visitor v) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.sun.tools.javac.tree.JCTree#accept(com.sun.source.tree.TreeVisitor
         * , java.lang.Object)
         */
        @Override
        public <R, D> R accept(TreeVisitor<R, D> v, D d) {
            // TODO Auto-generated method stub
            return null;
        }
    }

}
