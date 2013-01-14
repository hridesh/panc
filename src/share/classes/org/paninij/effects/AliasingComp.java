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


public class AliasingComp extends JCTree.Visitor {
    private LinkedList<ASTChainNode> nodesToProcess;
    private HashMap<ASTChainNode, EffectSet> effectsSoFar;
    private HeapRepresentation visitResult;

    public void fillInAliasingInfo(CFG chain) {
        nodesToProcess = new LinkedList<ASTChainNode>(chain.nodesInOrder);

        HeapRepresentation result = new HeapRepresentation();

        while (!nodesToProcess.isEmpty()) {
            ASTChainNode node = nodesToProcess.poll();

            HeapRepresentation newNodeHR = new HeapRepresentation();
            for (ASTChainNode prev : node.previous) { 
                newNodeHR = newNodeHR.union(prev.heapRepresentation);
            }
            
            newNodeHR = newNodeHR.union(computeHeapRepresentationForTree(node.tree));

            if (!newNodeHR.equals(node.heapRepresentation)) {
                nodesToProcess.addAll(node.next);
            }
            node.heapRepresentation = newNodeHR;
            
            result = result.union(newNodeHR);
        }

        chain.endHeapRepresentation = result;
    }

    public HeapRepresentation computeHeapRepresentationForTree(JCTree tree) {
        visitResult = new HeapRepresentation();
        tree.accept(this);
        return visitResult;
    }

    public void visitReturn(JCReturn tree)               { visitTree(tree); }
    public void visitApply(JCMethodInvocation tree) { 
        for (JCExpression arg : tree.args) {
            if (arg instanceof JCIdent) {
                // if a local variable is passed to a method, we stop assuming we know anything about its aliasing
                visitResult.add(((JCIdent)arg).sym, UnknownHeapLocation.instance); 
            }
        }
    }
    public void visitAssign(JCAssign tree) { 
        if (tree.lhs instanceof JCIdent) {
            handleLocalAssignment(((JCIdent)tree.lhs).sym, tree.rhs);
        }
    }
    public void visitAssignop(JCAssignOp tree)           { visitTree(tree); }
    public void visitUnary(JCUnary tree)                 { visitTree(tree); }
    public void visitBinary(JCBinary tree)               { visitTree(tree); }
    public void visitTypeCast(JCTypeCast tree)           { visitTree(tree); }
    public void visitTypeTest(JCInstanceOf tree)         { visitTree(tree); }
    public void visitIndexed(JCArrayAccess tree)         { visitTree(tree); }
    public void visitSelect(JCFieldAccess tree)          { visitTree(tree); }
    public void visitIdent(JCIdent tree)                 { visitTree(tree); }
    public void visitProcApply(JCProcInvocation tree)    { visitApply(tree); }
    public void visitFree(JCFree tree)	                 { visitTree(tree); }
    public void visitVarDef(JCVariableDecl tree) {
        handleLocalAssignment(tree.sym, tree.init);
    }
    public void visitTree(JCTree tree)                   {}

    public void handleLocalAssignment(Symbol s, JCExpression rhs) {
        if (rhs instanceof JCNewClass) {
            visitResult.add(s, LocalHeapLocation.instance);
        } else if (rhs instanceof JCIdent) {
            Symbol rhsSym = ((JCIdent)rhs).sym;
            if (rhsSym.name.toString().equals("this")) {
                visitResult.add(s, ThisHeapLocation.instance);
            } else {
                visitResult.add(s, visitResult.locationForSymbol(rhsSym));
            }
        }
    }
}