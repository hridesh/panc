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

import java.util.HashMap;

import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCSystemDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;

/**
 * Visit the system and interpret java computations to their values.
 *
 * @author Sean L. Mooney, Lorand Szakacs
 *
 */
public class SystemDeclRewriter extends TreeTranslator {

    InterpEnv<Name, JCTree> env;
    Log log;
    final TreeMaker make;

    public SystemDeclRewriter(TreeMaker treeMaker, Log log) {
        this.log = log;
        this.make = treeMaker;
    }

    @Override
    public void visitIdent(JCIdent tree) {
        System.out.println("Visiting " + tree.name.toString());
        JCTree bound = env.lookup(tree.name);

        // Don't loose the identifier if we don't know about it!
        if (bound != null) {
            result = bound;
        } else {
            result = tree;
        }
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        if (tree.init != null) {
            tree.init = translate(tree.init);
        }
        env.bind(tree.name, tree.init);

        result = tree;
    }

    @Override
    public void visitAssign(JCAssign tree) {

        JCExpression translate = this.translate(tree.rhs);
        if (translate != null) {
            tree.rhs = translate;
        }

        if (tree.lhs instanceof JCIdent) {
            Name assignTo = ((JCIdent) tree.lhs).name;
            env.bind(assignTo, tree.rhs);
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

        // TODO: Deal with something besides ints.
        class hell {
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
                return (Integer) obj;
            }
        }

        hell hell = new hell();

        JCTree lhs = translate(tree.lhs);
        JCTree rhs = translate(tree.rhs);

        if (lhs instanceof JCLiteral && rhs instanceof JCLiteral) {
            JCLiteral lhsLit = (JCLiteral) lhs;
            JCLiteral rhsLit = (JCLiteral) rhs;
            switch (tree.getTag()) {
            case PLUS:
                result = hell.interpPlus(lhsLit, rhsLit);
                break;
            case MINUS:
                result = hell.interpMinus(lhsLit, rhsLit);
                break;
            case MUL:
                result = hell.interpMul(lhsLit, rhsLit);
                break;
            case DIV:
                result = hell.interpDiv(lhsLit, rhsLit);
                break;
            case MOD:
                result = hell.interpMod(lhsLit, rhsLit);
                break;
            // TODO: Other cases?
            default:
                result = tree;
            }
        } else {
            result = tree;
            log.rawError(tree.pos, "Trying to interpret " + tree
                    + " but one side is not a literal!");
        }

    }

    @Override
    public void visitCapsuleDef(JCCapsuleDecl tree) {
        System.out.println("Visiting capsule def " + tree);
        super.visitCapsuleDef(tree);
    }

    @Override
    public void visitSystemDef(JCSystemDecl tree) {
        System.out.println("Visiting a system decl " + tree.name.toString());
        env = new InterpEnv<Name, JCTree>();

        tree.body = translate(tree.body);
        result = tree;
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
