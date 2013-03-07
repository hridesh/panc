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
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;

import javax.lang.model.element.ElementKind;

import org.paninij.analysis.CFG;
import org.paninij.analysis.CFGNodeImpl;

import java.util.LinkedList;

import org.paninij.effects.EffectSet.*;

public class MethodEffectsComp extends JCTree.Visitor {
    private LinkedList<JCTree> nodesToProcess;
    private EffectSet visitResult;
    private JCTree currentNode;
    private JCMethodDecl method;
    private Symbol capsuleSym;

    public EffectSet computeEffectsForMethod(JCMethodDecl method, Symbol capsuleSym) {
        this.method = method;
        this.capsuleSym = capsuleSym;
        nodesToProcess = new LinkedList<JCTree>(method.nodesInOrder);

        while (!nodesToProcess.isEmpty()) {
            JCTree node = nodesToProcess.poll();

            EffectSet newNodeEffects = new EffectSet();
            for (JCTree prev : node.predecessors) { 
                newNodeEffects.addAll(prev.effects);
            }
            
            newNodeEffects.addAll(computeEffectsForNode(node));

            if (!newNodeEffects.equals(node.effects)) {
                nodesToProcess.addAll(node.successors);
            }
            node.effects = newNodeEffects;
        }

        EffectSet union = new EffectSet();
        for (JCTree node : method.nodesInOrder) {
            union.addAll(node.effects);
        }
        return union;
    }

    public EffectSet computeEffectsForNode(JCTree node) {
        visitResult = new EffectSet();
        currentNode = node;
        node.accept(this);
        return visitResult;
    }

    public void visitReturn(JCReturn tree)               { visitTree(tree); }
    public void visitApply(JCMethodInvocation tree) { 
        MethodSymbol sym = (MethodSymbol)TreeInfo.symbol(tree.meth);
        if (capsuleSym != null) { // otherwise this is in a library
            if (sym.owner.isCapsule && sym.owner != capsuleSym) {
                visitResult.add(new OpenEffect(sym));
            } else if (sym.ownerCapsule() != capsuleSym) {
//                System.out.println("LIBRARY CALL: " + tree);
                visitResult.add(new LibMethodEffect(sym));
            } else {
                visitResult.add(new MethodEffect(sym));
            }
        } else {
            visitResult.add(new MethodEffect(sym));
        }

    }
    public void visitAssign(JCAssign tree)               { visitTree(tree); }
    public void visitAssignop(JCAssignOp tree)           { visitTree(tree); }
    public void visitUnary(JCUnary tree)                 { visitTree(tree); }
    public void visitBinary(JCBinary tree)               { visitTree(tree); }
    public void visitTypeCast(JCTypeCast tree)           { visitTree(tree); }
    public void visitTypeTest(JCInstanceOf tree)         { visitTree(tree); }
    public void visitIndexed(JCArrayAccess tree)         { visitTree(tree); }
    public void visitSelect(JCFieldAccess tree) { 
        if (!(method.endHeapRepresentation.locationForSymbol(TreeInfo.symbol(tree.selected))
              instanceof LocalHeapLocation)) {
            if (tree.sym != null) {
                if (!(tree.sym instanceof MethodSymbol)) {
                    if (currentNode.isLHS()) {
                        visitResult.add(new FieldWriteEffect(tree.sym));
                    } else {
                        visitResult.add(new FieldReadEffect(tree.sym));             
                    }
                }

            }
        }
    }
    public void visitIdent(JCIdent tree) {
        if (tree.sym.getKind() == ElementKind.FIELD) {
            if (!tree.sym.name.toString().equals("this")) {
                if (!(method.endHeapRepresentation.locationForSymbol(tree.sym)
                      instanceof LocalHeapLocation)) {
                    if (currentNode.isLHS()) {
                        visitResult.add(new FieldWriteEffect(tree.sym));
                    } else {
                        visitResult.add(new FieldReadEffect(tree.sym));
                    }
                }
            }
        }
    }
    public void visitProcApply(JCProcInvocation tree) { 
        Assert.error();
    }
    public void visitFree(JCFree tree)	                 { visitTree(tree); }
    public void visitTree(JCTree tree)                   {}
}