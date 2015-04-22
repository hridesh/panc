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
 * Contributor(s): Yuheng Long
 */

package org.paninij.effects;

import java.util.*;

import javax.lang.model.element.ElementKind;

import org.paninij.analysis.AnalysisUtil;
import org.paninij.effects.LoopSynchronize.CollectedCall;
import org.paninij.path.*;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;

public class EffectIntra {
	// detect whether the method always return newly created object.
	public boolean returnNewObject = true;

	public final ArrayList<JCTree> order;
	public final EffectInter inter;
	public final HashMap<JCTree, AliasingGraph> aliasing;
	public final JCMethodDecl curr_meth;

	public HashMap<JCTree, EffectSet> effectBeforeFlow =
	    new HashMap<JCTree, EffectSet>();
	public HashMap<JCTree, EffectSet> effectAfterFlow =
	    new HashMap<JCTree, EffectSet>();

	// pair of the capsule calls that have no synchronization in between
	public HashSet<BiCall> direct = new HashSet<BiCall>();
	// pair of the capsule calls that have synchronization in between
	public HashSet<BiCall> indirect = new HashSet<BiCall>();

	public EffectIntra(EffectInter inter,
			JCMethodDecl curr_meth, ArrayList<JCTree> order,
			HashMap<JCTree, AliasingGraph> aliasing) {
		this.inter = inter;
		this.order = order;
		this.aliasing = aliasing;
		this.curr_meth = curr_meth;
	}

	private final void flowThrough(JCTree tree, AliasingGraph aliasing,
							   EffectSet inValue, EffectSet out, MethodSymbol sym) {
		out.init(inValue);

		// if (out.isBottom) { return; }

		if (tree instanceof JCMethodInvocation) { /////////// Calls
			inter.intraProcessMethodCall((JCMethodInvocation)tree, aliasing,
					out, sym, this);
		} else if (tree instanceof JCAssign) { // lhs =
			abstractCommandAssign(tree, ((JCAssign)tree).lhs, aliasing, out);
		} else if (tree instanceof JCAssignOp) { // lhs X=
			abstractCommandAssign(tree, ((JCAssignOp)tree).lhs, aliasing, out);
		} else if (tree instanceof JCUnary) { // sth++
			JCUnary jcu = (JCUnary)tree;
			Tag opcode = jcu.getTag();
			if (opcode == JCTree.Tag.PREINC || opcode == JCTree.Tag.POSTINC || 
				opcode == JCTree.Tag.POSTDEC || opcode == JCTree.Tag.PREDEC) {
				abstractCommandAssign(tree, jcu.arg, aliasing, out);
			} else if (opcode == JCTree.Tag.POS || opcode == JCTree.Tag.NEG ||
					opcode == JCTree.Tag.NOT || opcode == JCTree.Tag.COMPL) {
				// -sth, +sth, !sth or ~sth, ignore
			} else throw new Error("opcode match error = " + opcode);
		} else if(tree instanceof JCFieldAccess) { // field read o.f
			addFieldAccessEffect((JCFieldAccess)tree, aliasing, out, 0);
		} else if (tree instanceof JCIdent) {      // field read f, omitted this
			addIdentEffect((JCIdent)tree, out, 0);
		} else if (tree instanceof JCArrayAccess) { // array read a[i]
			JCArrayAccess arr = (JCArrayAccess) tree;
			addArrayAccessEffect(arr.indexed, arr.index, aliasing, 0, out);
		} else if (tree instanceof JCForeach) {
			inter.intraForeach((JCForeach)tree, aliasing, this, out);
		} else if (tree instanceof JCReturn) {
			JCExpression expr = ((JCReturn) tree).expr;
			if (expr != null &&
					!(expr instanceof JCNewClass) &&
					!(expr instanceof JCNewArray) &&
					!aliasing.isReceiverNew(expr, true)) {
				returnNewObject = false;
			}
		} else if (tree instanceof JCCatch || tree instanceof JCBinary ||
				tree instanceof JCInstanceOf || tree instanceof JCTypeCast ||
				tree instanceof JCVariableDecl || tree instanceof JCMethodDecl
				|| tree instanceof JCModifiers ||
				tree instanceof JCTypeParameter || tree instanceof TypeBoundKind
				//the followings are JCExpression 
				|| tree instanceof JCAnnotation ||
				tree instanceof JCArrayTypeTree || tree instanceof JCConditional
				|| tree instanceof JCLiteral || tree instanceof JCNewArray ||
				tree instanceof JCNewClass || tree instanceof JCParens ||
				tree instanceof JCPrimitiveTypeTree ||
				tree instanceof JCTypeApply || tree instanceof JCTypeUnion ||
				tree instanceof JCWildcard || //the followings are JCStatement
				tree instanceof JCAssert || tree instanceof JCBlock ||
				tree instanceof JCBreak || tree instanceof JCCase ||
				tree instanceof JCClassDecl || tree instanceof JCContinue ||
				tree instanceof JCDoWhileLoop || tree instanceof JCIf||
				tree instanceof JCEnhancedForLoop ||
				tree instanceof JCExpressionStatement ||
				tree instanceof JCLabeledStatement || tree instanceof JCSkip || 
				tree instanceof JCSwitch || tree instanceof JCSynchronized ||
				tree instanceof JCThrow || tree instanceof JCTry ||
				tree instanceof JCWhileLoop) {
			// ignored do nothing...
		} else if (tree instanceof JCCompilationUnit || tree instanceof JCImport
				|| tree instanceof JCMethodDecl || tree instanceof JCErroneous
				|| tree instanceof LetExpr) {
		} else if (tree instanceof JCForLoop){
		} else throw new Error("JCTree match faliure " + tree);
	}

	private static void abstractCommandAssign(JCTree tree, JCExpression leftOp, 
			AliasingGraph aliasing, EffectSet out) {
		leftOp = AnalysisUtil.getEssentialExpr(leftOp);

		if (leftOp instanceof JCIdent) { /////////// v=...
			addIdentEffect((JCIdent)leftOp, out, 1);
		} else if (leftOp instanceof JCFieldAccess) {  // X.f = ..., ///////////
			addFieldAccessEffect((JCFieldAccess)leftOp, aliasing, out, 1);
		} else if (leftOp instanceof JCArrayAccess) {  ////////////// v[i] = ...
			JCArrayAccess arr = (JCArrayAccess) leftOp;
			addArrayAccessEffect(arr.indexed, arr.index, aliasing, 1, out);
		} else if (leftOp instanceof JCAnnotation ||
				leftOp instanceof JCArrayTypeTree || leftOp instanceof JCAssign
				|| leftOp instanceof JCAssignOp || leftOp instanceof JCBinary ||
				leftOp instanceof JCConditional || leftOp instanceof JCErroneous
				|| leftOp instanceof JCInstanceOf || leftOp instanceof JCLiteral
				|| leftOp instanceof JCMethodInvocation ||
				leftOp instanceof JCNewArray || leftOp instanceof JCNewClass ||
				leftOp instanceof JCPrimitiveTypeTree ||
				leftOp instanceof JCTypeApply || leftOp instanceof JCTypeUnion
				|| leftOp instanceof JCUnary || leftOp instanceof JCWildcard ||
				leftOp instanceof LetExpr) {
			throw new Error("Array match failure = " + tree + " type = " +
					leftOp.getClass());
		} else throw new Error("JCAssign match failure = " + tree + " type = " +
				leftOp.getClass());
	}

	public static void addArrayAccessEffect(JCExpression indexed,
			JCExpression index, AliasingGraph aliasing, int readOrWrite,
	        EffectSet result) {
		if (!aliasing.isReceiverNew(indexed, true)) {
			Path p = aliasing.createPathForExp(indexed);

			Type type = indexed.type;
			if (readOrWrite == 0) {
				result.read.add(new ArrayEffect(p, type));
			} else {
				result.write.add(new ArrayEffect(p, type));
			}
		}
	}

	public static void addFieldAccessEffect(JCFieldAccess jcf,
			AliasingGraph aliasing, EffectSet result, int readOrWrite) {
		Symbol sym = jcf.sym;
		if ((sym.flags_field & Flags.STATIC) == 0) {
			if (sym.getKind() == ElementKind.FIELD) {
				JCExpression selected = jcf.selected;
				selected = AnalysisUtil.getEssentialExpr(selected);

				if (!aliasing.isReceiverNew(selected, true)) {
					Path p = aliasing.createPathForExp(selected);

					if (readOrWrite == 0) {
						result.read.add(new FieldEffect(p, sym));
					} else {
						result.assignField(sym);
						result.write.add(new FieldEffect(p, sym));
					}
				}
			} else if (sym.getKind() == ElementKind.METHOD ||
					sym.getKind() == ElementKind.PACKAGE ||
					sym.getKind() == ElementKind.CLASS ||
					sym.getKind() == ElementKind.CONSTRUCTOR ||
					sym.getKind() == ElementKind.ENUM ||
					sym.getKind() == ElementKind.INTERFACE) { // ignore
			} else throw new Error("should be a field = " + jcf + "\t" +
					sym.getKind());
		}
	}

	public static void addIdentEffect(JCIdent left, EffectSet fcg,
			int readOrWrite) {
		Symbol sym = left.sym;
		if ((sym.flags_field & Flags.STATIC) == 0) {
			ElementKind kind = sym.getKind();
			if (kind == ElementKind.FIELD) {
				if (sym.name.toString().compareTo("this") != 0) {
					// ignore this = x
					if (readOrWrite == 0) {
						fcg.read.add(new FieldEffect(
								new Path_Parameter(null, 0), sym));
					} else {
						fcg.assignField(sym);
						fcg.write.add(new FieldEffect(
										new Path_Parameter(null, 0), sym));
					}
				}
			} else if (kind == ElementKind.LOCAL_VARIABLE ||
					kind == ElementKind.PARAMETER ||
					kind == ElementKind.EXCEPTION_PARAMETER) {
				if (readOrWrite == 1 && !sym.type.isPrimitive()) {
					fcg.assignVar(sym);
				}
			} else if (kind == ElementKind.CLASS ||
					kind == ElementKind.INTERFACE || kind == ElementKind.METHOD
					|| kind == ElementKind.CONSTRUCTOR ||
					kind == ElementKind.PACKAGE ||
					kind == ElementKind.ENUM) {
			} else throw new Error("Match failure = " + kind + "\t" + left);
		}
	}

	public EffectSet doAnalysis(List<JCTree> endNodes, MethodSymbol sym) {
		JCTree head = order.get(0);
		TreeSet<JCTree> changedUnits = AnalysisUtil.constructWorklist(order);

	    // Create the order of the AST for analyzing the effect of the methods.
        // This order is used to make the algorithm converge faster.
		HashSet<JCTree> resultNodes = new HashSet<JCTree>();
		for (JCTree node : order) {
			changedUnits.add(node);
			resultNodes.add(node);
			effectBeforeFlow.put(node, new EffectSet());
			effectAfterFlow.put(node, new EffectSet());
		}

		if (head != null) {
			effectBeforeFlow.put(head, new EffectSet(true));
			effectAfterFlow.put(head, new EffectSet());
		}

		LoopSynchronize la = new LoopSynchronize(aliasing);
		curr_meth.body.accept(la);
		HashMap<JCTree, HashSet<CollectedCall>> loop_collect = la.loop_collect;

		LoopCapCall lcc = new LoopCapCall();
		curr_meth.body.accept(lcc);
		HashMap<JCTree, HashSet<Integer>> loop_call = lcc.non_recursive;

		HashMap<JCTree, LoopEffect> le = new HashMap<JCTree, LoopEffect>();

		// Perform fixed point flow analysis
		while (!changedUnits.isEmpty()) {
			//get the first object
			JCTree s = changedUnits.iterator().next();
			changedUnits.remove(s);

			EffectSet previousAfterFlow = new EffectSet(effectAfterFlow.get(s));

			// Compute and store beforeFlow
			List<JCTree> preds = s.predecessors;
			EffectSet beforeFlow = effectBeforeFlow.get(s);

			if (preds.size() > 0) { // copy
				for (JCTree sPred : preds) {
					EffectSet otherBranchFlow = effectAfterFlow.get(sPred);
					beforeFlow.union(otherBranchFlow);
				}
			}

			for (JCTree forloop : loop_collect.keySet()) {
				JCForLoop jcf = (JCForLoop)forloop;
				if (AnalysisUtil.forloopsuccessors(jcf).contains(s)) {
					// call effects that are still alive
					HashSet<CallEffect> alive = beforeFlow.alive;
					// call effects that are collected
					HashSet<CallEffect> collected = beforeFlow.collected;

					for (CollectedCall lct : loop_collect.get(jcf)) {
						HashSet<CallEffect> toberemoved =
							new HashSet<CallEffect>();
						for (CallEffect ce : alive) {
							if (ce.pos() == lct.pos) {
								toberemoved.add(ce);
							}
						}

						collected.addAll(toberemoved);
						alive.removeAll(toberemoved);
					}
				}
			}

			for (JCTree forloop : loop_call.keySet()) {
				JCForLoop jcf = (JCForLoop)forloop;
				if (jcf.startNodes.contains(s)) {
					le.put(jcf, new LoopEffect(beforeFlow.alive,
							beforeFlow.collected, direct, indirect));
				}
			}

			if (!beforeFlow.isInit) { continue; }
			// Compute afterFlow and store it.
			EffectSet afterFlow = effectAfterFlow.get(s);

			flowThrough(s, aliasing.get(s), beforeFlow, afterFlow, sym);

			for (JCTree forloop : loop_call.keySet()) {
				JCForLoop jcf = (JCForLoop)forloop;
				if (AnalysisUtil.forloopsuccessors(jcf).contains(s)) {
					HashSet<Integer> calls = loop_call.get(jcf);
					LoopEffect loopeffect = le.get(jcf);

					HashSet<BiCall> tbr = new HashSet<BiCall>();
					HashSet<BiCall> tba = new HashSet<BiCall>();
					for (BiCall bc : direct) {
						CallEffect ce1 = bc.ce1;
						CallEffect ce2 = bc.ce2;

						if (ce1.equals(ce2) && calls.contains(ce1.pos())) {
							if (!loopeffect.alive.contains(ce1)) {
								tbr.add(bc);
								BiCall bc1 = new BiCall(bc.ce1, bc.ce2);
								bc1.notsameindex = true;
								tba.add(bc1);
							}
						}
					}
					direct.removeAll(tbr);
					direct.addAll(tba);
					tbr.clear();
					tba.clear();
					for (BiCall bc : indirect) {
						CallEffect ce1 = bc.ce1;
						CallEffect ce2 = bc.ce2;

						if (ce1.equals(ce2) && calls.contains(ce1.pos())) {
							if (!loopeffect.collected.contains(ce1)) {
								tbr.add(bc);
								BiCall bc1 = new BiCall(bc.ce1, bc.ce2);
								bc1.notsameindex = true;
								tba.add(bc1);
							}
						}
					}
					indirect.removeAll(tbr);
					indirect.addAll(tba);
				}
			}

			afterFlow.compress();

			if (!afterFlow.continue_Analyzing(previousAfterFlow) /*&&
					!afterFlow.continue_Analyzing(beforeFlow)*/) {
				/*if (afterFlow.continue_Analyzing(beforeFlow)) {*/
					changedUnits.addAll(s.successors);
				/*} else  {
					for (JCTree successor : s.successors) {
						if (successor.predecessors.size() > 1) {
							changedUnits.add(successor);
						}
					}
				}*/
			}
		}

		EffectSet resultEffect = new EffectSet();
		for (JCTree astc : resultNodes) {
		// for (JCTree astc : endNodes) {
			EffectSet otherBranchFlow = effectAfterFlow.get(astc);
			// mergeInto(resultEffect, otherBranchFlow);

			if (endNodes.contains(astc)) {
				resultEffect.union(otherBranchFlow);
			} else {
				resultEffect.unionHeapEffect(otherBranchFlow);
			}
		}
		resultEffect.compress();
		resultEffect.returnNewObject = returnNewObject;

		for (JCTree forloop : loop_call.keySet()) {
			JCForLoop jcf = (JCForLoop)forloop;
			if (AnalysisUtil.forloopsuccessors(jcf).isEmpty()) {
				HashSet<Integer> calls = loop_call.get(jcf);
				LoopEffect loopeffect = le.get(jcf);

				for (BiCall bc : direct) {
					CallEffect ce1 = bc.ce1;
					CallEffect ce2 = bc.ce2;

					if (ce1.equals(ce2) && calls.contains(ce1.pos())) {
						if (!loopeffect.alive.contains(ce1)) {
							bc.notsameindex = true;
						}
					}
				}
				for (BiCall bc : indirect) {
					CallEffect ce1 = bc.ce1;
					CallEffect ce2 = bc.ce2;

					if (ce1.equals(ce2) && calls.contains(ce1.pos())) {
						if (!loopeffect.collected.contains(ce1)) {
							bc.notsameindex = true;
						}
					}
				}
			}
		}

		resultEffect.direct = direct;
		resultEffect.indirect = indirect;

		// stateless capsules do not have any visible heap effect.
		JCTree capDecl = curr_meth.sym.ownerCapsule().tree;
		if (AnalysisUtil.statelessCapsule(capDecl)) {
			resultEffect.clearReadWrite();
		} else if (AnalysisUtil.primitiveStateCapsule(capDecl)) {
			resultEffect.clearNonPrimitiveEffect();
		}

		return resultEffect;
	}

	public static class ValueComparator implements Comparator<JCTree> {
	    Map<JCTree, Double> base;
	    public ValueComparator(Map<JCTree, Double> base) {
	        this.base = base;
	    }

	    // Note: this comparator imposes orderings that are inconsistent with
	    // equals.    
	    public int compare(JCTree a, JCTree b) {
	        if (base.get(a) >= base.get(b)) {
	            return -1;
	        } else {
	            return 1;
	        }
	    }
	}

	public static class LoopEffect {
		// Record call effects that are still alive
		public final HashSet<CallEffect> alive;
		// Record call effects that are collected
		public final HashSet<CallEffect> collected;
		// Record the direct before the loop.
		public final HashSet<BiCall> direct;
		// Record the indirect before the loop.
		public final HashSet<BiCall> indirect;

		public LoopEffect(HashSet<CallEffect> alive,
				HashSet<CallEffect> collected, HashSet<BiCall> direct,
				HashSet<BiCall> indirect) {
			this.alive = new HashSet<CallEffect>(alive);
			this.collected = new HashSet<CallEffect>(collected);
			this.direct = new HashSet<BiCall>(direct);
			this.indirect = new HashSet<BiCall>(indirect);
		}
	}
}
