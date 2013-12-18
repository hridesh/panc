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
import javax.tools.JavaFileObject;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;

import org.paninij.analysis.AnalysisUtil;
import org.paninij.effects.LoopAlias.LoopCapsuleTarget;

public class AliasingIntra {
	public final ClassSymbol cap;
	public final JCMethodDecl curr_meth;
	public HashMap<JCTree, AliasingGraph> graphBeforeFlow =
		new HashMap<JCTree, AliasingGraph>();
	public HashMap<JCTree, AliasingGraph> graphAfterFlow =
		new HashMap<JCTree, AliasingGraph>();
	public final TreeMaker make;

	public AliasingIntra(ClassSymbol cap, JCMethodDecl current_meth,
			TreeMaker make) {
		this.curr_meth = current_meth;
		this.cap = cap;
		this.make = make;
	}

	private void flowThrough(AliasingGraph in, JCTree tree, AliasingGraph out) {
		out.init(in);

		if (tree instanceof JCMethodInvocation) { // Calls
			if (!EffectInter.isCapsuleCall((JCMethodInvocation)tree, out)) {
				out.processUnalyzableAffectedPahts();
			}
		} else if (tree instanceof JCAssign) { // assignment
			JCExpression leftOp = ((JCAssign)tree).lhs;
			JCExpression rightOp = ((JCAssign)tree).rhs;
			if (leftOp instanceof JCIdent) { // v = ...           
				JCIdent left = (JCIdent)leftOp;
				localAssignOp(left.sym, rightOp, tree, out);
			} else if (leftOp instanceof JCFieldAccess) { // X.f = ...
				fieldAssignmentOperation((JCFieldAccess)leftOp, rightOp, tree,
						out);
			} else if (leftOp instanceof JCArrayAccess) { // v[i] = ...
				JCArrayAccess jcaa = (JCArrayAccess)leftOp;
				JCExpression indexed = jcaa.indexed;
				if (indexed instanceof JCIdent) {
					JCIdent jci = (JCIdent)indexed;
					Symbol jcisym = jci.sym;
					ElementKind kind = jcisym.getKind();
					if (kind == ElementKind.LOCAL_VARIABLE ||
							kind == ElementKind.PARAMETER) {
						// if (!jci.type.isPrimitive()) { // reference types
							rightOp = AnalysisUtil.getEssentialExpr(rightOp);

							if (rightOp instanceof JCMethodInvocation) {
								JCMethodInvocation jcmi =
									(JCMethodInvocation)rightOp;
								if (!EffectInter.isCapsuleCall(jcmi, out)) {
									out.removeLocal(jcisym);
								} else {
									out.assignCapsuleCallToLocal(jcisym,
											EffectInter.capsuleCall(jcmi, out,
													cap));
								}
							} /* else {
								out.removeLocal(jcisym);
							} */
						// }
					}
				}
			} else if (leftOp instanceof JCAnnotation ||
					leftOp instanceof JCArrayTypeTree ||
					leftOp instanceof JCAssign || leftOp instanceof JCAssignOp
					|| leftOp instanceof JCBinary ||
					leftOp instanceof JCConditional ||
					leftOp instanceof JCErroneous ||
					leftOp instanceof JCInstanceOf ||
					leftOp instanceof JCLiteral ||
					leftOp instanceof JCMethodInvocation ||
					leftOp instanceof JCNewArray || leftOp instanceof JCNewClass
					|| leftOp instanceof JCPrimitiveTypeTree ||
					leftOp instanceof JCTypeApply ||
					leftOp instanceof JCTypeUnion || leftOp instanceof JCUnary
					|| leftOp instanceof JCWildcard ||
					leftOp instanceof LetExpr) {
					throw new Error("Array match failure = " + tree + " type = "
							+ leftOp.getClass());
			} else throw new Error("JCAssign match failure = " + tree +
					" type = " + leftOp.getClass());
		} else if (tree instanceof JCVariableDecl) {
			JCVariableDecl jcvd = (JCVariableDecl)tree;
	        JCExpression init = jcvd.init;
	        VarSymbol sym = jcvd.sym;
			localAssignOp(sym, init, tree, out);
			out.writtenLocals.remove(sym);
		} else if (tree instanceof JCEnhancedForLoop) {
			JCEnhancedForLoop jcefl = (JCEnhancedForLoop)tree;
			VarSymbol sym = jcefl.var.sym;
			localAssignOp(sym, jcefl.expr, tree, out);
		} else if (tree instanceof JCCatch || tree instanceof JCAssignOp ||
				tree instanceof JCBinary || tree instanceof JCInstanceOf ||
				tree instanceof JCTypeCast || tree instanceof JCReturn ||
				tree instanceof JCMethodDecl || tree instanceof JCModifiers ||
				tree instanceof JCTypeParameter || tree instanceof TypeBoundKind
				|| // the followings are JCExpression 
				tree instanceof JCAnnotation || tree instanceof JCArrayAccess ||
				tree instanceof JCArrayTypeTree || tree instanceof JCConditional
				|| tree instanceof JCFieldAccess || tree instanceof JCIdent ||
				tree instanceof JCLiteral || tree instanceof JCNewArray ||
				tree instanceof JCNewClass || tree instanceof JCParens ||
				tree instanceof JCPrimitiveTypeTree ||
				tree instanceof JCTypeApply || tree instanceof JCTypeUnion ||
				tree instanceof JCUnary || tree instanceof JCWildcard ||
				// the followings are JCStatement
				tree instanceof JCAssert || tree instanceof JCBlock ||
				tree instanceof JCBreak || tree instanceof JCCase ||
				tree instanceof JCClassDecl || tree instanceof JCContinue ||
				tree instanceof JCDoWhileLoop || 
				tree instanceof JCEnhancedForLoop ||
				tree instanceof JCExpressionStatement || 
				tree instanceof JCForLoop || tree instanceof JCIf ||
				tree instanceof JCLabeledStatement || tree instanceof JCSkip || 
				tree instanceof JCSwitch || tree instanceof JCSynchronized ||
				tree instanceof JCThrow || tree instanceof JCTry ||
				tree instanceof JCWhileLoop) { // ignored do nothing...
		} else if (tree instanceof JCCompilationUnit || tree instanceof JCImport
				|| tree instanceof JCMethodDecl || tree instanceof JCErroneous
				|| tree instanceof LetExpr) {
		} else throw new Error("JCTree match faliure " + tree);
	}

	public void localAssignOp(Symbol left, JCExpression rightOp, JCTree unit,
			AliasingGraph outValue) {
		ElementKind kind = left.getKind();
		if (kind == ElementKind.LOCAL_VARIABLE ||
				kind == ElementKind.PARAMETER) {
			if (!left.type.isPrimitive()) { // reference types
				rightOp = AnalysisUtil.getEssentialExpr(rightOp);

				outValue.removeLocal(left);
				if (rightOp instanceof JCIdent) { // v = v
					JCIdent jr = (JCIdent)rightOp;
					Symbol sr = jr.sym;
					ElementKind rightkind = sr.getKind();
					if (rightkind == ElementKind.LOCAL_VARIABLE ||
							rightkind == ElementKind.PARAMETER) {
						outValue.localAssignment(left, jr.sym);
					} else if (rightkind == ElementKind.FIELD) { // this.f = ...
						if (sr.name.toString().compareTo("this") == 0) {
							// this.f = this
							outValue.localThisAssignment(left);
						} else { // this.f = sr
							outValue.assignFieldToLocal(left, sr);
						}
					} else if (rightkind == ElementKind.EXCEPTION_PARAMETER) { 
						throw new Error("not implemented yet.");
					} else throw new Error("assignment match failure");
				} else if (rightOp instanceof JCFieldAccess) { // v = v.f
					outValue.assignFieldToLocal(left,
							(JCFieldAccess)rightOp);
				} else if (rightOp instanceof JCArrayAccess) { // v = v[]
					outValue.removeLocal(left);
				} else if (rightOp instanceof JCAssign) { // v = (v = ...)
					outValue.assignJCAssignToLocal(left, (JCAssign)rightOp);
				} else if (rightOp instanceof JCNewArray) { ///// v = new C[];
					outValue.assignNewArrayToLocal(left);
				} else if (rightOp instanceof JCNewClass) { ///// v = new C();
					outValue.processUnalyzableAffectedPahts();
					JCNewClass jcn = (JCNewClass)rightOp;
					outValue.assignNewObjectToLocal(left, jcn);
				} else if (rightOp instanceof JCMethodInvocation) {
					JCMethodInvocation jcmi = (JCMethodInvocation)rightOp;
					if (EffectInter.isCapsuleCall((JCMethodInvocation)rightOp,
							outValue)) {
						outValue.assignCapsuleCallToLocal(left,
								EffectInter.capsuleCall(jcmi, outValue, cap));
					} else if (EffectInter.isCallReturnNew(jcmi, outValue)) {
						outValue.assignCapsuleCallToLocal(left, null);
					} else if (AnalysisUtil.isNewExpression(rightOp)) {
						outValue.assignNewObjectToLocal(left, rightOp.type);
					} else {
						outValue.removeLocal(left);
					}
				} else if (rightOp instanceof JCAssignOp ||
						rightOp instanceof JCBinary ||
						rightOp instanceof JCConditional ||
						rightOp instanceof JCErroneous ||
						rightOp instanceof JCInstanceOf ||
						rightOp instanceof JCLiteral ||
						rightOp instanceof JCParens ||
						rightOp instanceof JCTypeCast) {
					outValue.localIsUnknown(left);
				} else if (rightOp instanceof JCAnnotation ||
						rightOp instanceof JCArrayTypeTree || 
						rightOp instanceof JCPrimitiveTypeTree ||
						rightOp instanceof JCTypeApply || 
						rightOp instanceof JCTypeUnion ||
						rightOp instanceof JCUnary || 
						rightOp instanceof JCWildcard ||
						rightOp instanceof LetExpr) {
					throw new Error("JCAssign match failure="+unit);
				}
			}
		} else if (kind == ElementKind.FIELD) { // f = ...
			if (!left.type.isPrimitive()) {		 ///////// reference types
				rightOp = AnalysisUtil.getEssentialExpr(rightOp);

				if (rightOp instanceof JCIdent) { // v = v
					JCIdent jr = (JCIdent)rightOp;
					Symbol sr = jr.sym;
					ElementKind rightkind = sr.getKind();
					if (rightkind == ElementKind.LOCAL_VARIABLE ||
							rightkind == ElementKind.PARAMETER) {
						outValue.assignLocalToThisField(left, sr);
					} else if (rightkind == ElementKind.FIELD) { // f = ...
						outValue.assignThisFieldToThisField(left, jr);
					} else if (rightkind == ElementKind.EXCEPTION_PARAMETER) {
						outValue.writeField(left);
					} else throw new Error("assignment match failure");
				} else if (rightOp instanceof JCFieldAccess) { // v = v.f
					outValue.assignPathToThisField(left, (JCFieldAccess)rightOp);
				} else if (rightOp instanceof JCArrayAccess) { // v = v[]
					outValue.writeField(left);
				} else if (rightOp instanceof JCAssign) { // v = (v = ...)
					outValue.writeField(left);
				} else if (rightOp instanceof JCNewArray) { // v = new C[];
					outValue.assignNewArrayToThisField(left);
				} else if (rightOp instanceof JCNewClass) { // v = new C();
					outValue.processUnalyzableAffectedPahts();
					JCNewClass jcn = (JCNewClass)rightOp;
					outValue.assignNewToThisField(left, jcn);
				} else if (rightOp instanceof JCMethodInvocation) {
					if (AnalysisUtil.isNewExpression(rightOp)) {
						outValue.assignNewToThisField(left, rightOp.type);
					} else {
						outValue.writeField(left);
					}
				} else if (rightOp instanceof JCAssignOp ||
						rightOp instanceof JCBinary ||
						rightOp instanceof JCConditional || 
						rightOp instanceof JCErroneous ||
						rightOp instanceof JCInstanceOf ||
						rightOp instanceof JCLiteral ||
						rightOp instanceof JCParens ||
						rightOp instanceof JCTypeCast) {
					outValue.writeField(left);
				} else if (rightOp instanceof JCAnnotation ||
						rightOp instanceof JCArrayTypeTree || 
						rightOp instanceof JCPrimitiveTypeTree ||
						rightOp instanceof JCTypeApply || 
						rightOp instanceof JCTypeUnion ||
						rightOp instanceof JCUnary || 
						rightOp instanceof JCWildcard ||
						rightOp instanceof LetExpr) {
					throw new Error("JCAssign match failure="+unit);
				}
			}
		} else if (kind == ElementKind.EXCEPTION_PARAMETER) { // Exception e
		} else throw new Error("assignment match failure = " + kind);
	}
	
	private void fieldAssignmentOperation(JCFieldAccess left,
			JCExpression rightOp, JCTree unit, AliasingGraph outValue) {
		if (!left.type.isPrimitive()) {	// reference types
			rightOp = AnalysisUtil.getEssentialExpr(rightOp);

			if (rightOp instanceof JCIdent) { //////////////////// v = v
				JCIdent jr = (JCIdent)rightOp;
				Symbol sr = jr.sym;
				ElementKind rightkind = sr.getKind();
				if (rightkind == ElementKind.LOCAL_VARIABLE ||
						rightkind==ElementKind.PARAMETER) {
					outValue.assignLocalToField(left, sr);
				} else if (rightkind == ElementKind.FIELD) { //  X.f = this.f
					outValue.assignThisFieldToField(left, sr);
				} else if (rightkind == ElementKind.EXCEPTION_PARAMETER) { 
					throw new Error("not implemented yet.");
				} else throw new Error("assignment match failure");
			} else if (rightOp instanceof JCFieldAccess) { // X.f = v.f
				outValue.assignFieldToField(left, (JCFieldAccess)rightOp);
			} else if (rightOp instanceof JCArrayAccess) { // v = v[]
				outValue.writePath(left);
			} else if (rightOp instanceof JCAssign) { ///////// v = (v = ...)
				outValue.writePath(left);
			} else if (rightOp instanceof JCNewArray) { ///////// v = new C[];
				outValue.assignNewArrayToField(left);
			} else if (rightOp instanceof JCNewClass) { ///////// v = new C();
				outValue.processUnalyzableAffectedPahts();
				outValue.assignNewToField(left, (JCNewClass)rightOp);
			} else if (rightOp instanceof JCMethodInvocation) {
				if (!EffectInter.isCapsuleCall((JCMethodInvocation)rightOp,
						outValue)) {
					if (AnalysisUtil.isNewExpression(rightOp)) {
						outValue.assignNewToField(left, rightOp.type);
					} else {
						outValue.writePath(left);
					}
				} else {
					outValue.assignCapsuleCallToField(left);
				}
			} else if (rightOp instanceof JCAssignOp ||
					rightOp instanceof JCBinary ||
					rightOp instanceof JCConditional ||
					rightOp instanceof JCErroneous ||
					rightOp instanceof JCInstanceOf ||
					rightOp instanceof JCLiteral || rightOp instanceof JCParens
					|| rightOp instanceof JCTypeCast) {
				outValue.writePath(left);
			} else if (rightOp instanceof JCAnnotation ||
					rightOp instanceof JCArrayTypeTree || 
					rightOp instanceof JCPrimitiveTypeTree ||
					rightOp instanceof JCTypeApply || 
					rightOp instanceof JCTypeUnion || rightOp instanceof JCUnary
					|| rightOp instanceof JCWildcard ||
					rightOp instanceof LetExpr) {
				throw new Error("JCAssign match failure = " + unit);
			}
		}
	}

	public final void analyze(ArrayList<JCTree> order, HashSet<JCTree> exists,
			JavaFileObject sourcefile) {
		JCTree head = order.get(0);

		TreeSet<JCTree> changedUnits = AnalysisUtil.constructWorklist(order);

		for (JCTree node : order) {
			changedUnits.add(node);
			graphBeforeFlow.put(node, new AliasingGraph());
			graphAfterFlow.put(node, new AliasingGraph());
		}
		if (head != null) {
			graphBeforeFlow.put(head, entryInitialFlow());
			graphAfterFlow.put(head, new AliasingGraph());
		}

		LoopAlias la = new LoopAlias(sourcefile);
		curr_meth.body.accept(la);

		HashMap<JCTree, HashSet<LoopCapsuleTarget>> loop_capsules =
			la.loop_capsules;

		// Perform fixed point flow analysis
		while (!changedUnits.isEmpty()) {
			//get the first object
			JCTree s = changedUnits.iterator().next();
			changedUnits.remove(s);

			AliasingGraph previousAfterFlow =
				new AliasingGraph(graphAfterFlow.get(s));

			List<JCTree> preds = s.predecessors;
			AliasingGraph beforeFlow = graphBeforeFlow.get(s);

			if (preds.size() > 0) { // copy
				for (JCTree sPred : preds) {
					AliasingGraph otherBranchFlow = graphAfterFlow.get(sPred);
					beforeFlow.union(otherBranchFlow);
				}
			}

			for (JCTree forloop : loop_capsules.keySet()) {
				JCForLoop jcf = (JCForLoop)forloop;
				if (AnalysisUtil.forloopsuccessors(jcf).contains(s)) {
					HashSet<ForallAliasing> hs = beforeFlow.forall_alias;

					for (LoopCapsuleTarget lct : loop_capsules.get(forloop)) {
						hs.add(new ForallAliasing(make.Literal(TypeTags.INT, 0),
								jcf.cond, make.Literal(TypeTags.INT, 1),
								lct.target.indexed, lct.capsule_instance,
								lct.capsule_meth, lct.pos, lct.line));
					}
				}
			}

			if (!beforeFlow.isInit) { continue; }

			// Compute afterFlow and store it.
			AliasingGraph afterFlow = graphAfterFlow.get(s);
			// set aliasingInfo before calling
			flowThrough(beforeFlow, s, afterFlow);
			if (!afterFlow.equals(previousAfterFlow)) {
				changedUnits.addAll(s.successors);
			}
		}
	}

	private AliasingGraph entryInitialFlow() {
		AliasingGraph entry = new AliasingGraph(true);
		int i = 1;
		for (JCVariableDecl jcv : curr_meth.params) {
			VarSymbol sym = jcv.sym;
			entry.initParam(sym, i);
			i++;
		}
		return entry;
	}
}
