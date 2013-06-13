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
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArray;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArrayCall;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCCapsuleWiring;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCProcInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSystemDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

/**
 * Visit the system and interpret java computations to their values.
 * 
 * @author Sean L. Mooney, Lorand Szakacs
 * 
 */
public class SystemDeclRewriter extends TreeTranslator {

    final Names names;

    /**
     * this is a hack required for keeping track of array sizes.
     */
    final Log log;
    final TreeMaker make;

    final TreeCopier<Void> copy;

    public JCSystemDecl rewrite(JCSystemDecl tree) {
        JCSystemDecl translated = super.translate(tree);
        //translated.body.stats = unrollStatementsFromBodyStats(translated.body.stats);
        return translated;
    }



    public SystemDeclRewriter(TreeMaker treeMaker, Log log, Names names) {
        this.log = log;
        this.make = treeMaker;
        this.names = names;
        this.copy = new TreeCopier<Void>(make);
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        /* Create a new VariableDef. Needed to properly attribute
         * the main method that will get written. If not copied, the
         * symbol gets aliased, which causes the scope resolution logic
         * to think the name is defined in an inner class.
         */
        tree = make.VarDef(
                tree.getModifiers(),
                tree.name,
                tree.vartype,
                tree.init
                );


        if (tree.init != null) {
            tree.init = translate(tree.init);
        }

        result = tree;
    }

    @Override
    public void visitSystemDef(JCSystemDecl tree) {
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

    @Override
    public void visitCapsuleWiring(JCCapsuleWiring tree) {
        //TODO: full capusule wiring.
        //currently switches to ProcInvocation. Should be the 'actual'
        //statements used to wire a capsule. See the second half of
        //org.paninij.comp.Attr#visitSystemDef() for the current wiring
        //strategy

        JCIdent capID = ((JCIdent)tree.capsule);
        Name n = names.fromString(capID.name.toString());
        capID = make.Ident(n);

        JCProcInvocation pi = make.at(tree.pos)
                .ProcApply(List.<JCExpression>nil(), capID, tree.args);

        pi.switchToMethod();
        //Visit it for whatever rewriting is required.
        pi.accept(this);
    }

    //TODO:remove because it does exactly what super does.
    @Override
    public void visitIndexedCapsuleWiring(JCCapsuleArrayCall tree) {
        tree.index = translate(tree.index);
        tree.indexed = translate(tree.indexed);
        tree.arguments = translate(tree.arguments);
        result = tree;
    }
}
