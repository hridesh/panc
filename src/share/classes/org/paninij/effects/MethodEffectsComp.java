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
 * Contributor(s): Rex Fernando
 */

package org.paninij.effects;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.code.Type.*;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.HashMap;
import java.util.LinkedList;


public class MethodEffectsComp extends JCTree.Visitor {
    private LinkedList<ASTChainNode> nodesToProcess;
    private EffectSet visitResult;

    public EffectSet computeEffectsForMethod(JCMethodDecl m) {
        System.out.println(m);
        ASTChain chain = ASTChainBuilder.buildChain(m);
        new ASTChainPrinter().printChain(chain);
        nodesToProcess = new LinkedList<ASTChainNode>(chain.nodesInOrder);

        while (!nodesToProcess.isEmpty()) {
            ASTChainNode node = nodesToProcess.poll();

            EffectSet newNodeEffects = new EffectSet();
            for (ASTChainNode prev : node.previous) { 
                newNodeEffects.addAll(prev.effects);
            }
            
            newNodeEffects.addAll(computeEffectsForTree(node.tree));

            if (!newNodeEffects.equals(node.effects)) {
                nodesToProcess.addAll(node.next);
            }
            node.effects = newNodeEffects;
        }

        EffectSet union = new EffectSet();
        for (ASTChainNode node : chain.nodesInOrder) {
            union.addAll(node.effects);
        }
        return union;
    }

    public EffectSet computeEffectsForTree(JCTree tree) {
        visitResult = new EffectSet();
        tree.accept(this);
        return visitResult;
    }

    public void visitReturn(JCReturn tree)               { visitTree(tree); }
    public void visitApply(JCMethodInvocation tree)      { visitTree(tree); }
    public void visitAssign(JCAssign tree)               { visitTree(tree); }
    public void visitAssignop(JCAssignOp tree)           { visitTree(tree); }
    public void visitUnary(JCUnary tree)                 { visitTree(tree); }
    public void visitBinary(JCBinary tree)               { visitTree(tree); }
    public void visitTypeCast(JCTypeCast tree)           { visitTree(tree); }
    public void visitTypeTest(JCInstanceOf tree)         { visitTree(tree); }
    public void visitIndexed(JCArrayAccess tree)         { visitTree(tree); }
    public void visitSelect(JCFieldAccess tree)          { System.out.println(tree); }
    public void visitIdent(JCIdent tree)                 { System.out.println(tree); }
    public void visitProcApply(JCProcInvocation tree)    { visitTree(tree); }
    public void visitFree(JCFree tree)	                 { visitTree(tree); }
    public void visitTree(JCTree tree)                   {}
}