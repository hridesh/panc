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
package org.paninij.system;

import java.util.ArrayList;
import java.util.HashMap;

import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArray;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArrayCall;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCManyToOne;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSystemDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;

/**
 * Visit the system and interpret java computations to their values.
 * 
 * @author Sean L. Mooney, Lorand Szakacs
 * 
 */
public class SystemDeclRewriter extends TreeTranslator {

    InterpEnv<Name, JCTree> valueEnv;

    /**
     * this is a hack required for keeping track of array sizes.
     */
    InterpEnv<Name, JCVariableDecl> varDefToAstNodeEnv;
    final Log log;
    final TreeMaker make;
    final ArithTreeInterp atInterp;

    public JCSystemDecl rewrite(JCSystemDecl tree) {
        JCSystemDecl translated = super.translate(tree);
        translated.body.stats = unrollStatementsFromBodyStats(translated.body.stats);
        return translated;
    }
    
    /**
     * TODO: Refactor to removethis at some points in the future.
     * 
     * This method flattens the statements. Unrolled topology statements
     * are implemented as JCStatements nodes that contain a list of statements.
     * 
     * @param stats
     * @return
     */
    private List<JCStatement> unrollStatementsFromBodyStats(
            List<JCStatement> stats) {
        
        ArrayList<JCStatement> newStats = new ArrayList<JCStatement>(
                stats.size());
        
        for (JCStatement statement : stats) {
            if (statement.getKind() == Kind.EXPRESSION_STATEMENT) {
                JCExpressionStatement exprStatement = ((JCExpressionStatement)statement);
                if(exprStatement.expr.getKind() == Kind.MANY_TO_ONE) {
                    JCManyToOne m2one = (JCManyToOne)exprStatement.expr;
                    for(JCStatement unrolledStatement: m2one.unrolled){
                        newStats.add(unrolledStatement);
                    }
                } else newStats.add(statement);
             } else newStats.add(statement);
        }
        List<JCStatement> result = List.<JCStatement>from(newStats.toArray(new JCStatement[1]));
        return result;
    }
    
    public SystemDeclRewriter(TreeMaker treeMaker, Log log) {
        this.log = log;
        this.make = treeMaker;
        this.atInterp = new ArithTreeInterp();
    }

    @Override
    public void visitIdent(JCIdent tree) {
        System.out.println("Visiting identifier: " + tree.name.toString());
        JCTree bound = valueEnv.lookup(tree.name);

        // Don't lose the identifier if we don't know about it!
        if (bound != null) {
            result = bound;
        } else {
            result = tree;
        }
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        System.out.println("Visiting var definition: " + tree.toString());
        if (tree.init != null) {
            tree.init = translate(tree.init);
        }
        valueEnv.bind(tree.name, tree.init);
        varDefToAstNodeEnv.bind(tree.name, tree);

        result = tree;
    }

    @Override
    public void visitAssign(JCAssign tree) {

        final JCExpression translatedRHS = this.translate(tree.rhs);
        if (translatedRHS != null) {
            tree.rhs = translatedRHS;
        }

        if (tree.lhs instanceof JCIdent) {
            Name assignTo = ((JCIdent) tree.lhs).name;
            valueEnv.bind(assignTo, tree.rhs);
        } else {
            // FIXME: Non-raw error message.
            log.rawError(tree.pos, tree
                    + " does not have an identifier on the left hand side!");
        }

        System.out.println("New assn: " + tree);
        result = tree;
    }

    @Override
    public void visitBinary(final JCBinary tree) {
        JCTree lhs = translate(tree.lhs);
        JCTree rhs = translate(tree.rhs);

        if (lhs instanceof JCLiteral && rhs instanceof JCLiteral) {
            JCLiteral lhsLit = (JCLiteral) lhs;
            JCLiteral rhsLit = (JCLiteral) rhs;
            result = atInterp.interp(tree, lhsLit, rhsLit);
        } else {
            result = tree;
            log.rawError(tree.pos, "Trying to interpret " + tree
                    + " but one side is not a literal!");
        }

    }

    /**
     * This function will replace the JCManyToOne node with a JCManyToOneUnrolled
     */
    @Override
    public void visitManyToOne(JCManyToOne tree) {
        System.out.println("Visiting many to one: " + tree.toString());
        int capsuleArraySize = getCapsuleArraySize(tree);

        JCStatement statements[] = new JCStatement[capsuleArraySize];
        for (int i = 0; i < capsuleArraySize; i++) {
            Name capsuleArrayName = getCapsuleArrayName(tree);
            statements[i] = make.CapsuleArrayCall(capsuleArrayName,
                    make.Literal(i), tree.many, tree.args);
        }

        System.out.println("Rewritten m2one statement: " + List.from(statements).toString());
        tree.unrolled = List.from(statements);
        result = tree;
    }

    /**
     * @param arrayName
     * @return
     */
    private int getCapsuleArraySize(JCManyToOne array) {
        Name arrayName = getCapsuleArrayName(array);
        ;
        JCVariableDecl arrayAST = varDefToAstNodeEnv.lookup(arrayName);
        assert (arrayAST.vartype instanceof JCCapsuleArray) : "m2one expects capsule arrays as the first element";
        int capsuleArraySize = ((JCCapsuleArray) arrayAST.vartype).amount;
        assert (capsuleArraySize > 0) : "capsule array sizes should always be > 0; something went wrong";
        return capsuleArraySize;
    }

    /**
     * @param tree
     * @return
     */
    private Name getCapsuleArrayName(JCManyToOne tree) {
        assert (tree.many instanceof JCIdent) : "Many2One arrays should always be referenced through identifiers";
        Name arrayName = ((JCIdent) tree.many).name;
        return arrayName;
    }

    // TODO: probably redundant method.
    @Override
    public void visitCapsuleDef(JCCapsuleDecl tree) {
        System.out.println("Visiting capsule def " + tree);
        super.visitCapsuleDef(tree);
        result = tree;
    }

    @Override
    public void visitSystemDef(JCSystemDecl tree) {
        System.out.println("Visiting a system decl " + tree.name.toString());
        valueEnv = new InterpEnv<Name, JCTree>();
        varDefToAstNodeEnv = new InterpEnv<Name, JCVariableDecl>();

        tree.body = translate(tree.body);
        result = tree;
    }

    @Override
    public void visitCapsuleArrayCall(JCCapsuleArrayCall tree) {
        System.out.println("Visiting capsule array call: " + tree.toString());
        result = tree;
    }

    /**
     * Helper to interpret arithmetic expression trees.
     * 
     * TODO: Deal with something besides ints.
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
            // TODO: Other cases?
            default:
                result = tree;
            }

            // get rid of the reference when we are done interpretting it.
            // prevents stale tree type errors.
            this.tree = null;
            return result;
        }

        final JCTree interpPlus(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Integer.valueOf(vLHS + vRHS));
                default:
                    return tree;
                }
            }
        }

        final JCTree interpMinus(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Integer.valueOf(vLHS - vRHS));
                default:
                    return tree;
                }
            }
        }

        final JCTree interpMul(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Integer.valueOf(vLHS * vRHS));
                default:
                    return tree;
                }
            }
        }

        final JCTree interpDiv(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Integer.valueOf(vLHS / vRHS));
                default:
                    return tree;
                }
            }
        }

        final JCTree interpMod(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Integer.valueOf(vLHS % vRHS));
                default:
                    return tree;
                }
            }
        }

        final int asInt(Object obj) {
            // FIXME Possible class cast exception.
            return (Integer) obj;
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
}
