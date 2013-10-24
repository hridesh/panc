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
 * Contributor(s): Rex Fernando, Yuheng Long and Ganesha Upadhyaya
 */

package org.paninij.analysis;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;

import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;

/* Trying to detect the following example violation. The capsule reference is
 * leaked via a inner class.
  
interface Escape {
	public void method();
}

capsule B {
	void escape(Escape fun){
		fun.method();
	}
}

capsule A {
	design{
		B b;
		A escapee;
	}

	void run() {
		b.escape(new Escape(){
			public void method(){
				// a reference to a capsule of type A leaked to B
				escapee.nothing();
			}
		});
	}

	void nothing(){};
}
*/

public class InnerClassCapsuleAliasDetector extends TreeScanner {
	private ClassSymbol cap_sym = null;
	private Log log;

	public InnerClassCapsuleAliasDetector(Log log) {
		this.log = log;
	}

	public void visitTopLevel(JCCompilationUnit tree) { Assert.error(); }
	public void visitImport(JCImport tree) { Assert.error(); }
	public void visitLetExpr(LetExpr tree) { Assert.error(); }
	
	public void visitErroneous(JCErroneous tree) { Assert.error(); }

	/* The default behavior, visit all the subexpressions of an expression. */
	/*
	public void visitAnnotation(JCAnnotation tree) {}
	public void visitModifiers(JCModifiers tree) {}
	public void visitTypeApply(JCTypeApply tree) {}
	public void visitTypeUnion(JCTypeUnion tree) {}
	public void visitTypeParameter(JCTypeParameter tree) {}
	public void visitWildcard(JCWildcard tree) {}
	public void visitTypeBoundKind(TypeBoundKind tree) {}
	public void visitTypeIdent(JCPrimitiveTypeTree tree) {}
	public void visitLiteral(JCLiteral tree) {}
	// URL[].class
	public void visitTypeArray(JCArrayTypeTree tree) { singleton(tree); }
	// while (i != 0);
	public void visitSkip(JCSkip tree) { singleton(tree); }
	public void visitLabelled(JCLabeledStatement tree) {}
	public void visitAssert(JCAssert tree) {}
	public void visitMethodDef(JCMethodDecl tree) {}
	public void visitVarDef(JCVariableDecl tree) {}
	public void visitBlock(JCBlock tree) {}
	public void visitDoLoop(JCDoWhileLoop tree) {}
	public void visitWhileLoop(JCWhileLoop tree) {}
	public void visitForLoop(JCForLoop tree) {}
	public void visitForeachLoop(JCEnhancedForLoop tree) {}
	public void visitSwitch(JCSwitch tree) {}
	public void visitCase(JCCase tree) {}
	public void visitSynchronized(JCSynchronized tree) {}
	public void visitTry(JCTry tree) {}
	public void visitCatch(JCCatch tree) {}
	public void visitConditional(JCConditional tree) {}
	public void visitIf(JCIf tree) {}
	public void visitExec(JCExpressionStatement tree) {}
	public void visitBreak(JCBreak tree) {}
	public void visitContinue(JCContinue tree) {}
	public void visitReturn(JCReturn tree) {}
	public void visitThrow(JCThrow tree) {}
	public void visitApply(JCMethodInvocation tree) {}
	public void visitNewClass(JCNewClass tree) {}
	public void visitNewArray(JCNewArray tree) {}
	public void visitParens(JCParens tree) {}
	public void visitAssign(JCAssign tree) {}
	public void visitAssignop(JCAssignOp tree) {}
	public void visitUnary(JCUnary tree) {}
	public void visitBinary(JCBinary tree) {}
	public void visitTypeCast(JCTypeCast tree) {}
	public void visitTypeTest(JCInstanceOf tree) {}
	public void visitIndexed(JCArrayAccess tree) {}
	public void visitSelect(JCFieldAccess tree) {}
	private void singleton(JCTree tree) {} */

	public void visitClassDef(JCClassDecl tree) {
		ClassSymbol temp = cap_sym;
		cap_sym = tree.sym;

		for (JCTree def : tree.defs) {
			def.accept(this);
		}

		cap_sym = temp;
	}

	public void visitIdent(JCIdent tree) {
		if (tree.type.tsym.isCapsule() && cap_sym != null) {
			Symbol sym = tree.sym;
			log.useSource (sym.outermostClass().sourcefile);
			log.warning(tree.pos(), "capsule.escape.inner.class",
					sym, AnalysisUtil.rmDollar(cap_sym.toString()));
		}
	}
}