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

package org.paninij.analysis;

import javax.lang.model.element.ElementKind;

import com.sun.tools.javac.util.*;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;

/* This class detects the capsule fields that leaks. */
public class ViolationDetector extends TreeScanner {
	public void visitTopLevel(JCCompilationUnit tree) { Assert.error(); }
	public void visitImport(JCImport tree) { Assert.error(); }

	public void visitLetExpr(LetExpr tree) { /* do nothing */ }
	public void visitAssert(JCAssert tree) { /* do nothing */ }
	public void visitAnnotation(JCAnnotation tree) { /* do nothing */ }
	public void visitModifiers(JCModifiers tree) { /* do nothing */ }
	public void visitErroneous(JCErroneous tree) { /* do nothing */ }
	public void visitTypeIdent(JCPrimitiveTypeTree tree) { /* do nothing */ }
	public void visitTypeArray(JCArrayTypeTree tree) { /* do nothing */ }
	public void visitTypeApply(JCTypeApply tree) { /* do nothing */ }
	public void visitTypeUnion(JCTypeUnion tree) { /* do nothing */ }
	public void visitTypeParameter(JCTypeParameter tree) { /* do nothing */ }
	public void visitWildcard(JCWildcard tree) { /* do nothing */ }
	public void visitTypeBoundKind(TypeBoundKind tree) { /* do nothing */ }

	public void visitSkip(JCSkip tree) { /* do nothing */ }
	public void visitLabelled(JCLabeledStatement tree) { /* do nothing */ }

	public final List<JCTree> defs;
	public final JCCapsuleDecl capsule;
	public final JCMethodDecl m;

    Log log;
	public ViolationDetector(Log log, List<JCTree> defs, JCCapsuleDecl capsule,
			JCMethodDecl m) {
        this.log = log;
		this.defs = defs;
		this.capsule = capsule;
		this.m = m;
	}

	public void visitVarDef(JCVariableDecl that) {
		warningCandidates(that.init);

		super.visitVarDef(that);
	}

	public void visitReturn(JCReturn that) {
		warningCandidates(that.expr);

		super.visitReturn(that);
	}

	public void visitApply(JCMethodInvocation that) {
		JCExpression meth = that.meth;

		boolean innerCall = false;

		// if the method is not intra capsule method call
		if (meth instanceof JCIdent) {
			innerCall = true;
		} else if (meth instanceof JCFieldAccess) {
			JCFieldAccess jcfa = (JCFieldAccess)meth;
			JCExpression receiver = getEssentialExpr(jcfa.selected);

			if (receiver instanceof JCIdent) {
				JCIdent jci = (JCIdent)receiver;
				if (isInnerField(jci.sym)) {
					innerCall = true;
				}
			} else if (receiver instanceof JCFieldAccess) {
				JCFieldAccess field = (JCFieldAccess)receiver;
				if (isInnerField(field.sym)) {
					innerCall = true;
				}
			}
		}

		if (!innerCall) {
			warningList(that.args);
		}

		super.visitApply(that);
	}

	public void visitAssign(JCAssign that) {
		JCExpression rhs = that.rhs;

		if (!(getEssentialExpr(rhs) instanceof JCNewClass)) {
			warningCandidates(that.lhs);
		}

		warningCandidates(that.rhs);
		

		super.visitAssign(that);
	}

	public void visitAssignop(JCAssignOp that) {
		warningCandidates(that.lhs);
		warningCandidates(that.rhs);

		super.visitAssignop(that);
	}

	public void visitNewClass(JCNewClass that) {
		warningList(that.args);

		super.visitNewClass(that);
	}

	public void visitNewArray(JCNewArray that) {
		warningList(that.elems);

		super.visitNewArray(that);
	}

	private void warningList(List<? extends JCTree> listsOfTrees) {
		if (listsOfTrees != null) {
			for (JCTree jct : listsOfTrees) {
				warningCandidates(jct);
			}
		}
	}

	private void warningCandidates(JCTree tree) {
		if (tree != null) {
			if (tree instanceof JCExpression) {
				JCExpression jce = (JCExpression)tree;
				tree = getEssentialExpr(jce);
			}

			if (tree instanceof JCIdent) {
				warning((JCIdent) tree);
			} else if (tree instanceof JCFieldAccess) {
				JCFieldAccess jcfa = (JCFieldAccess)tree;
				JCExpression selected = jcfa.selected;
				if (isVarThis(selected)) {
					if (isInnerField(jcfa.sym) && !jcfa.type.isPrimitive()) {
						log.useSource(((JCFieldAccess) tree).sym.outermostClass().sourcefile);
						log.error(tree.pos(), "confinement.violation", jcfa.sym,
								capsule.sym, m.sym);
					}
				}
			}
		}
	}

	private static JCExpression getEssentialExpr(JCExpression original) {
		JCExpression rightOp = original;
		while (rightOp instanceof JCTypeCast || rightOp instanceof JCParens) {
			if (rightOp instanceof JCTypeCast) {
				rightOp = ((JCTypeCast)rightOp).expr;
			} else if (rightOp instanceof JCParens) {
				rightOp = ((JCParens)rightOp).expr;
			}
		}
		return rightOp;
	}

	private void warning(JCIdent tree) {
		Symbol sym = tree.sym;
		if (sym != null) {
			ElementKind symKind = sym.getKind();
			if (symKind == ElementKind.FIELD && isInnerField(sym) &&
					!sym.type.isPrimitive()) {
				log.useSource (tree.sym.outermostClass().sourcefile);
				log.error(tree.pos(), "confinement.violation", sym, capsule.sym, m.sym);
			}
		}
	}

	private static boolean isVarThis(JCTree that) {
		if (that instanceof JCIdent) {
			JCIdent tree = (JCIdent)that;
			Symbol sym = tree.sym;
			if (sym != null) {
				ElementKind symKind = sym.getKind();
				if (symKind == ElementKind.FIELD) {
					if (sym.name.toString().compareTo("this")==0) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isInnerField(Symbol s) {
		for (JCTree def : defs) {
			if (def instanceof JCVariableDecl) {
				JCVariableDecl field = (JCVariableDecl)def;
				if (field.sym == s &&
						((field.mods.flags & Flags.PRIVATE) != 0)) {
					return true;
				}
			}
		}
		return false;
	}
}