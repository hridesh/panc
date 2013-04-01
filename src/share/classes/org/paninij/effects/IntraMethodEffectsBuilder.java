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
 * Contributor(s): Yuheng Long, Rex Fernando
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
import org.paninij.analysis.CommonMethod;
import org.paninij.effects.EffectSet;

import java.util.LinkedList;

public class IntraMethodEffectsBuilder extends TreeScanner {
	private LinkedList<CFGNodeImpl> nodesToProcess;
	private EffectSet visitResult;
	private CFGNodeImpl currentNode;
	private CFG cfg;
	private Symbol capsuleSym;

	public EffectSet computeEffectsForMethod(CFG cfg, Symbol capsuleSym) {
		this.cfg = cfg;
		this.capsuleSym = capsuleSym;
		nodesToProcess = new LinkedList<CFGNodeImpl>(cfg.nodesInOrder);

		while (!nodesToProcess.isEmpty()) {
			CFGNodeImpl node = nodesToProcess.poll();

			EffectSet newNodeEffects = new EffectSet();
			for (CFGNodeImpl prev : node.successors) { 
				newNodeEffects.addAll(prev.effects);
			}

			newNodeEffects.addAll(computeEffectsForNode(node));

			if (!newNodeEffects.equals(node.effects)) {
				nodesToProcess.addAll(node.predecessors);
			}
			node.effects = newNodeEffects;
		}

		EffectSet union = new EffectSet();
		for (CFGNodeImpl node : cfg.nodesInOrder) {
			union.addAll(node.effects);
		}
		return union;
	}

	public EffectSet computeEffectsForMethod(JCTree root, Symbol capsuleSym) {
		this.capsuleSym = capsuleSym;
		visitResult = new EffectSet();

		this.scan(root);

		return visitResult;
	}

	public EffectSet computeEffectsForNode(CFGNodeImpl node) {
		visitResult = new EffectSet();
		currentNode = node;
		//node.tree.accept(this);
		return visitResult;
	}

	public void visitApply(JCMethodInvocation tree) { 
		MethodSymbol sym = (MethodSymbol)TreeInfo.symbol(tree.meth);
		if (capsuleSym != null) { // otherwise this is in a library
			if (sym.owner instanceof CapsuleSymbol && sym.owner != capsuleSym) {
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

	public void visitSelect(JCFieldAccess tree) { 
		if (cfg == null || !(cfg.endHeapRepresentation.locationForSymbol(TreeInfo.symbol(tree.selected))
				instanceof LocalHeapLocation)) {
			if (tree.sym != null) {
				if (!(tree.sym instanceof MethodSymbol)) {
					if (currentNode != null && currentNode.lhs) {
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
				if (cfg == null || !(cfg.endHeapRepresentation.locationForSymbol(tree.sym)
						instanceof LocalHeapLocation)) {
					if (currentNode != null && currentNode.lhs) {
						visitResult.add(new FieldWriteEffect(tree.sym));
					} else {
						visitResult.add(new FieldReadEffect(tree.sym));
					}
				}
			}
		}
	}
	public void visitAssign(JCIdent tree) {
		JCExpression lhs = CommonMethod.getEssentialExpr(tree);

		if (lhs instanceof JCFieldAccess) {
			JCFieldAccess jcfa = (JCFieldAccess)lhs;
			if (!(jcfa.sym instanceof MethodSymbol)) {
				if (currentNode.lhs) {
					visitResult.add(new FieldWriteEffect(tree.sym));
				} else {
					visitResult.add(new FieldReadEffect(tree.sym));             
				}
			}
		} else if (lhs instanceof JCIdent) {
			JCIdent jci = (JCIdent)lhs;
			if (jci.sym.getKind() == ElementKind.FIELD) {
				if (!jci.sym.name.toString().equals("this")) {
					visitResult.add(new FieldWriteEffect(tree.sym));
				}
			}
		}
	}
	public void visitProcApply(JCProcInvocation tree) { 
		Assert.error();
	}
}