package org.paninij.effects;

import java.util.*;

import javax.lang.model.element.ElementKind;

import org.paninij.analysis.AnalysisUtil;
import org.paninij.path.Path_Var;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;

public class AliasingIntra {
	public final ClassSymbol cap;
	public final JCMethodDecl current_analyzing_meth;
	public HashMap<JCTree, AliasingGraph> graphBeforeFlow =
		new HashMap<JCTree, AliasingGraph>();
	public HashMap<JCTree, AliasingGraph> graphAfterFlow =
		new HashMap<JCTree, AliasingGraph>();

	public AliasingIntra(ClassSymbol cap, JCMethodDecl current_meth) {
		this.current_analyzing_meth = current_meth;
		this.cap = cap;
	}

	private void flowThrough(AliasingGraph in, JCTree unit, AliasingGraph out) {
		out.init(in);

		if (unit instanceof JCMethodInvocation) { // Calls
			if (!EffectInter.isCapsuleCall((JCMethodInvocation)unit,
					out)) {
				out.processUnalyzableAffectedPahts();
			}
		} else if (unit instanceof JCAssign) { // assignment
			JCExpression leftOp = ((JCAssign)unit).lhs;
			JCExpression rightOp = ((JCAssign)unit).rhs;
			if (leftOp instanceof JCIdent) { // v = ...           
				JCIdent left = (JCIdent)leftOp;
				localAssignOp(left.sym, rightOp, unit, out);
			} else if (leftOp instanceof JCFieldAccess) { // X.f = ...
				fieldAssignmentOperation((JCFieldAccess)leftOp, rightOp, unit,
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
						if (!jci.type.isPrimitive()) { // reference types
							rightOp = AnalysisUtil.getEssentialExpr(rightOp);

							if (rightOp instanceof JCMethodInvocation) {
								if (!EffectInter.isCapsuleCall(
										(JCMethodInvocation)rightOp, out)) {
									out.removeLocal(jcisym);
								} else {
									out.pathsToNewNode.put(
											new Path_Var(jcisym, true),
											AliasingGraph.unknownType);
								}
							} else {
								out.removeLocal(jcisym);
							}
						}
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
					throw new Error("Array match failure = " + unit + " type = "
							+ leftOp.getClass());
			} else throw new Error("JCAssign match failure = " + unit +
					" type = " + leftOp.getClass());
		} else if (unit instanceof JCVariableDecl) {
			JCVariableDecl jcvd = (JCVariableDecl)unit;
	        JCExpression init = jcvd.init;
	        VarSymbol sym = jcvd.sym;
			localAssignOp(sym, init, unit, out);
			out.writtenLocals.remove(sym);
		} else if (unit instanceof JCEnhancedForLoop) {
			JCEnhancedForLoop jcefl = (JCEnhancedForLoop)unit;
			VarSymbol sym = jcefl.var.sym;
			localAssignOp(sym, jcefl.expr, unit, out);
		} else if (unit instanceof JCCatch || unit instanceof JCAssignOp ||
				unit instanceof JCBinary || unit instanceof JCInstanceOf ||
				unit instanceof JCTypeCast || unit instanceof JCReturn ||
				unit instanceof JCMethodDecl || unit instanceof JCModifiers ||
				unit instanceof JCTypeParameter || unit instanceof TypeBoundKind
				|| // the followings are JCExpression 
				unit instanceof JCAnnotation || unit instanceof JCArrayAccess ||
				unit instanceof JCArrayTypeTree || unit instanceof JCConditional
				|| unit instanceof JCFieldAccess || unit instanceof JCIdent ||
				unit instanceof JCLiteral || unit instanceof JCNewArray ||
				unit instanceof JCNewClass || unit instanceof JCParens ||
				unit instanceof JCPrimitiveTypeTree ||
				unit instanceof JCTypeApply || unit instanceof JCTypeUnion ||
				unit instanceof JCUnary || unit instanceof JCWildcard ||
				// the followings are JCStatement
				unit instanceof JCAssert || unit instanceof JCBlock ||
				unit instanceof JCBreak || unit instanceof JCCase ||
				unit instanceof JCClassDecl || unit instanceof JCContinue ||
				unit instanceof JCDoWhileLoop || 
				unit instanceof JCEnhancedForLoop ||
				unit instanceof JCExpressionStatement || 
				unit instanceof JCForLoop || unit instanceof JCIf ||
				unit instanceof JCLabeledStatement || unit instanceof JCSkip || 
				unit instanceof JCSwitch || unit instanceof JCSynchronized ||
				unit instanceof JCThrow || unit instanceof JCTry ||
				unit instanceof JCWhileLoop) { // ignored do nothing...
		} else if (unit instanceof JCCompilationUnit || unit instanceof JCImport
				|| unit instanceof JCMethodDecl || unit instanceof JCErroneous
				|| unit instanceof LetExpr) {
		} else throw new Error("JCTree match faliure " + unit);
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
					outValue.writeField(left);
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
		if (!left.type.isPrimitive()) {		 ///////// reference types
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
					outValue.writePath(left);
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

	private static final void mergeInto(AliasingGraph inout, AliasingGraph in) {
		AliasingGraph tmp = new AliasingGraph();
		tmp = new AliasingGraph(inout);
		tmp.union(in);

		inout.init(tmp);
	}

	public final void analyze(ArrayList<JCTree> order, HashSet<JCTree> exists) {
		JCTree head = order.get(0);

		Collection<JCTree> changedUnits = AnalysisUtil.constructWorklist(order);

		for (JCTree node : order) {
			changedUnits.add(node);
			graphBeforeFlow.put(node, newInitialFlow());
			graphAfterFlow.put(node, newInitialFlow());
		}
		if (head != null) {
			graphBeforeFlow.put(head, entryInitialFlow());
			graphAfterFlow.put(head, newInitialFlow());
		}

		// Perform fixed point flow analysis
		while (!changedUnits.isEmpty()) {
			AliasingGraph previousAfterFlow = newInitialFlow();

			AliasingGraph beforeFlow;
			AliasingGraph afterFlow;

			//get the first object
			JCTree s = changedUnits.iterator().next();
			changedUnits.remove(s);
			previousAfterFlow.init(graphAfterFlow.get(s));

			List<JCTree> preds = s.predecessors;

			beforeFlow = graphBeforeFlow.get(s);

			if (preds.size() > 0) { // copy
				for (JCTree sPred : preds) {
					AliasingGraph otherBranchFlow = graphAfterFlow.get(sPred);
					mergeInto(beforeFlow, otherBranchFlow);
				}
			}

			// Compute afterFlow and store it.
			afterFlow = graphAfterFlow.get(s);
			// set aliasingInfo before calling
			flowThrough(beforeFlow, s, afterFlow);
			if (!afterFlow.equals(previousAfterFlow)) {
				changedUnits.addAll(s.successors);
			}
		}
	}

	private AliasingGraph newInitialFlow() { return new AliasingGraph(); }

	private AliasingGraph entryInitialFlow() {
		AliasingGraph entry = new AliasingGraph(true);
		int i = 1;
		for (JCVariableDecl jcv : current_analyzing_meth.params) {
			VarSymbol sym = jcv.sym;
			entry.assignParamToLocal(sym, i);
			i++;
		}
		return entry;
	}
}