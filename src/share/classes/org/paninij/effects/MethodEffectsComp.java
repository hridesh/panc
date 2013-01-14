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
import javax.lang.model.element.ElementKind;
import java.util.HashMap;
import java.util.LinkedList;


public class MethodEffectsComp extends JCTree.Visitor {
    private LinkedList<CFGNode> nodesToProcess;
    private EffectSet visitResult;
    private CFGNode currentNode;
    private CFG chain;
    private Symbol moduleSym;

    public EffectSet computeEffectsForMethod(CFG chain, Symbol moduleSym) {
        this.chain = chain;
        this.moduleSym = moduleSym;
        nodesToProcess = new LinkedList<CFGNode>(chain.nodesInOrder);

        while (!nodesToProcess.isEmpty()) {
            CFGNode node = nodesToProcess.poll();

            EffectSet newNodeEffects = new EffectSet();
            for (CFGNode prev : node.previous) { 
                newNodeEffects.addAll(prev.effects);
            }
            
            newNodeEffects.addAll(computeEffectsForNode(node));

            if (!newNodeEffects.equals(node.effects)) {
                nodesToProcess.addAll(node.next);
            }
            node.effects = newNodeEffects;
        }

        EffectSet union = new EffectSet();
        for (CFGNode node : chain.nodesInOrder) {
            union.addAll(node.effects);
        }
        return union;
    }

    public EffectSet computeEffectsForNode(CFGNode node) {
        visitResult = new EffectSet();
        currentNode = node;
        node.tree.accept(this);
        return visitResult;
    }

    public void visitReturn(JCReturn tree)               { visitTree(tree); }
    public void visitApply(JCMethodInvocation tree) { 
        MethodSymbol sym = (MethodSymbol)TreeInfo.symbol(tree.meth);
        if (moduleSym != null) { // otherwise this is in a library
            if (sym.owner.isModule && sym.owner != moduleSym) {
                visitResult.add(new OpenEffect(sym));
            } else if (sym.ownerModule() != moduleSym) {
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
        if (!(chain.endHeapRepresentation.locationForSymbol(TreeInfo.symbol(tree.selected))
              instanceof LocalHeapLocation)) {
            if (tree.sym != null) {
                if (!(tree.sym instanceof MethodSymbol)) {
                    if (currentNode.lhs) {
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
                if (!(chain.endHeapRepresentation.locationForSymbol(tree.sym)
                      instanceof LocalHeapLocation)) {
                    if (currentNode.lhs) {
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