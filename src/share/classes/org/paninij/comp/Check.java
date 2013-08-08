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
 * Contributor(s): Sean L. Mooney
 */
package org.paninij.comp;

import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.Flags.StandardFlags;
import static com.sun.tools.javac.code.Flags.asFlagSet;

import java.util.HashSet;
import java.util.Set;

import org.paninij.systemgraph.SystemGraph;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Scope.Entry;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Names;

/**Type checking helper class for the attribution phase of panini code.
 * Companion/parallel to {@link com.sun.tools.javac.comp.Check}
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public class Check {

    protected static final Context.Key<Check> checkKey =
            new Context.Key<Check>();

    private final Log log;
    private final Names names;
    private final com.sun.tools.javac.comp.MemberEnter jMemberEnter;

    private final HashSet<Symbol> checkedForCycles;

    public static Check instance(Context context) {
        Check instance = context.get(checkKey);
        if (instance == null)
            instance = new Check(context);
        return instance;
    }

    protected Check(Context context) {
        context.put(checkKey, this);

        log = Log.instance(context);
        names = Names.instance(context);
        jMemberEnter = com.sun.tools.javac.comp.MemberEnter.instance(context);

        checkedForCycles = new HashSet<Symbol>();
    }

    /**
     * Mark a capsule as having been checked for cycles in
     * the design decl.
     *
     * This symbol should be a symbol to the 'origin' capsule
     * type and not to one of the capsule kinds. Work around
     * to prevent checking/reporting cyclic warnings in multiple
     * capsule kinds.
     *
     * @param sym
     */
    private void markedCheckedForCycles(Symbol sym) {
        //Must be a symbol
        Assert.check(sym.isCapsule());
        //parentCapsule == sym => This is the 'origin' capsule symbol
        // and not one of translated capsule kinds.
        Assert.check( ((ClassSymbol)sym).capsule_info.parentCapsule == sym);
        checkedForCycles.add(sym);
    }

    /** Check that given modifiers are legal for given symbol and
     *  return modifiers together with any implicit modififiers for that symbol.
     *  Warning: we can't use flags() here since this method
     *  is called during class enter, when flags() would cause a premature
     *  completion.
     *  <p>
     *  Valid symbols that can be check are:
     *  <ul>
     *  <li><code>Kinds.MTH</code> if the name is
     *  {@link org.paninij.util.Names.InterCapsuleWiring} </li>
     *  </ul>
     *
     *  <p>
     *  Follows the default behavior of
     *  {@link com.sun.tools.javac.comp.Check#checkFlags}, which is
     *  throw an unchecked {@link AssertionError} if the symbol is
     *  something the method doesn't know what flags it should use.
     *  Make sure the symbol is method before checking its flags.
     *
     *
     *  @param pos           Position to be used for error reporting.
     *  @param flags         The set of modifiers given in a definition.
     *  @param sym           The defined symbol.
     */
    long checkFlags(DiagnosticPosition pos, long flags, Symbol sym) {
        long mask = 0;
        long implicit = 0;
        switch (sym.kind) {
        case Kinds.MTH:
            if(sym.name == names.panini.InternalCapsuleWiring) {
                implicit = Flags.WIRING_BLOCK_FLAGS;
                mask = Flags.WIRING_BLOCK_FLAGS;
            } else {
                throw new AssertionError();
            }

        break;
        default:
            throw new AssertionError();
        }

        long illegal = flags & StandardFlags & ~mask;
        if (illegal != 0) {
            if ((illegal & INTERFACE) != 0) {
                log.error(pos, "intf.not.allowed.here");
                mask |= INTERFACE;
            }
            else {
                log.error(pos,
                          "mod.not.allowed.here", asFlagSet(illegal));
            }
        }

        return flags & (mask | ~StandardFlags) | implicit;
    }

    /** Check the flags for capasule parameters
     * @param pos
     * @param flags
     * @return 0. No flags needed.
     */
    public long checkCapsuleParamFlags(DiagnosticPosition pos, long flags) {
        //Make sure there aren't any visibility modifiers on param.
        long illegal = flags & StandardFlags;
        if( illegal != 0) {
            log.error(pos,
                    "mod.not.allowed.here", asFlagSet(illegal));
        }

        return 0;
    }

    void checkStateInit(final ClassSymbol sym, final Env<AttrContext> env) {
        final List<JCVariableDecl>  stateToInit = sym.capsule_info.stateToInit;
        final List<JCMethodDecl> initMethods = sym.capsule_info.initMethods;

        class InitScanner extends TreeScanner {
            Env<AttrContext> env;
            static final int STATE_KIND = 1 << 24; //stick this on a high bit.
            final Set<Symbol> states;

            public InitScanner(Env<AttrContext> env) {
                this.env = env;
                states = new HashSet<Symbol>();
                enterStateNames(stateToInit);
            }

            final void enterStateNames(final List<JCVariableDecl>  stateToInit) {
                for(List<JCVariableDecl> l = stateToInit; l.nonEmpty(); l = l.tail) {
                    jMemberEnter.memberEnter(l.head, env);
                    l.head.sym.kind = STATE_KIND;
                    l.head.sym.tree = l.head;
                    if(l.head.init == null) {;
                        states.add(l.head.sym);
                    }
                }
            }

            public final void scan(JCTree tree) {
                if (tree != null )
                    tree.accept(this);
            }

            @Override
            public final void visitAssign(JCAssign tree) {
                if(tree.lhs != null) {
                    tree.lhs.accept(this);
                }
            }

            @Override
            public final void visitMethodDef(JCMethodDecl tree) {
                Env<AttrContext> oldEnv = env;
                //Create a basic symbol for the tree
                tree.sym = new MethodSymbol(0, tree.name, null, sym);
                Env<AttrContext> localenv = jMemberEnter.methodEnv(tree, env);
                env = localenv;
                super.visitMethodDef(tree);
                env = oldEnv;
            }

            @Override
            public final void visitVarDef(JCVariableDecl tree) {
                jMemberEnter.memberEnter(tree, env);
            }

            @Override
            public final void visitIdent(JCIdent tree) {
                Entry decl = env.info.getScope().lookup(tree.name);
                //lookup ident in scope
                if(decl.sym != null) {
                    if(decl.sym.kind == STATE_KIND) {
                        if(states.contains(decl.sym)) {
                            states.remove(decl.sym);
                        } else {
                            log.warning(tree.pos, "state.already.initialized", tree.name);
                        }
                    }
                }
            }

            public final void checkAllStateInit() {
                if(!states.isEmpty()) {
                    for(List<JCVariableDecl>  l = stateToInit; l.nonEmpty(); l = l.tail) {
                        checkStateInit(l.head, l.head.sym);
                    }
                }
            }

            private final void checkStateInit(DiagnosticPosition pos, Symbol sym) {
                if(states.contains(sym)
                        // Don't report unitialized capsule types.
                        // If there is a capsule type here a
                        // different error will be reported. Reporting
                        // not initialized is spurious.
                        && !sym.type.tsym.isCapsule()
                        ) {
                    log.warning(pos, "state.not.initialized");
                }
            }
        }

        InitScanner s = new InitScanner(env.dup(sym.tree));
        for (List<JCMethodDecl> l = initMethods; l.nonEmpty(); l = l.tail) {
            l.head.accept(s);
        }
        s.checkAllStateInit();
    }

    /**
     * @param location
     * @param fst
     * @param snd
     */
    public void checkCycleRepeat(SystemGraph sysGraph, Name startPoint, Env<AttrContext> env) {
        Symbol location = env.enclMethod.sym.location();
        //Don't recheck. It's been done for some other capsule kind
        if (checkedForCycles.contains(location)) {
            return;
        }

        for (List<Pair<Name, Name>> cycles = sysGraph.detectCyclicReferences(startPoint);
                cycles.nonEmpty(); cycles = cycles.tail) {
            log.warning("cyclic.references.exists", cycles.head.fst,
                    cycles.head.snd, location);
        }

        markedCheckedForCycles(location);
    }
}
