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
package org.paninij.comp;

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.CapsuleSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symbol.WiringSymbol;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;

/**
 * @author sean
 * @since panini-0.9.2
 */
public class MemberEnter extends com.sun.tools.javac.comp.MemberEnter {

    /**
     * @param context
     */
    public MemberEnter(Context context) {
        super(context);
    }

    @Override
    protected final void finishClass(JCClassDecl tree, Env<AttrContext> env) {
        super.finishClass(tree, env);

        if (tree instanceof JCCapsuleDecl) {
            // Create an unshared instance of the env. Need to know about
            // existing environment, but we don't want to modify it. Otherwise
            // the fields created from translating the CapsuleDecl will conflict
            // with the symbol names we entered here.
            Env<AttrContext> localEnv = env.dup(tree,
                    env.info.dup(env.info.getScope().dupUnshared()));

            finishCapsule((JCCapsuleDecl) tree, localEnv);
        }
    }

    protected void finishCapsule(JCCapsuleDecl tree, Env<AttrContext> env) {
        ListBuffer<Type> wts = new ListBuffer<Type>();

        for (JCVariableDecl p : tree.params) {
            wts.append(enterCapsuleParam(p, tree.sym, env));
        }

        //Create a wiring symbol from the parameter types.
        WiringSymbol wiringSym = new WiringSymbol(0, names.panini.Wiring,
                new org.paninij.code.Type.WiringType(wts.toList(), tree.sym),
                tree.sym);
        ((CapsuleSymbol) tree.sym).wiringSym = wiringSym;
    }

    protected Type enterCapsuleParam(JCVariableDecl tree, Symbol owner, Env<AttrContext> env) {
        /*
         * Adapted from the logic for entering VariableDecls.
         */
        Scope enclScope = env.info.getScope();

        Type type = attr.attribType(tree.vartype, env);

        VarSymbol v = new VarSymbol(0, tree.name, tree.vartype.type, tree.sym);
        v.owner = owner;
        v.flags_field = chk.checkFlags(tree.pos(), tree.mods.flags, v, tree);
        tree.sym = v;

        if (chk.checkUnique(tree.pos(), v, enclScope)) {
            chk.checkTransparentVar(tree.pos(), v, enclScope);
            enclScope.enter(v);
        }
        annotateLater(tree.mods.annotations, env, v);
        v.pos = tree.pos;

        return type;
    }
}
