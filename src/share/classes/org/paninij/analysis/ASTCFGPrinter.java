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

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.*;

public class ASTCFGPrinter extends TreeScanner {
	public void visitTopLevel(JCCompilationUnit tree)    { /* do nothing */ }
	public void visitImport(JCImport tree)               { /* do nothing */ }
	public void visitMethodDef(JCMethodDecl tree)        { /* do nothing */ }
	public void visitLetExpr(LetExpr tree)               { /* do nothing */ }
	public void visitAnnotation(JCAnnotation tree)       { /* do nothing */ }
	public void visitModifiers(JCModifiers tree)         { /* do nothing */ }
	public void visitErroneous(JCErroneous tree)         { /* do nothing */ }

	public void visitTypeIdent(JCPrimitiveTypeTree tree) { /* do nothing */ }
	public void visitTypeArray(JCArrayTypeTree tree)     { /* do nothing */ }
	public void visitTypeApply(JCTypeApply tree)         { /* do nothing */ }
	public void visitTypeUnion(JCTypeUnion tree)         { /* do nothing */ }
	public void visitTypeParameter(JCTypeParameter tree) { /* do nothing */ }
	public void visitWildcard(JCWildcard tree)           { /* do nothing */ }
	public void visitTypeBoundKind(TypeBoundKind tree)   { /* do nothing */ }

	public void visitSkip(JCSkip tree)                   { /* do nothing */ }
	
	public void visitClassDef(JCClassDecl tree)          { printCurrent(tree); }
	public void visitIdent(JCIdent tree)                 { printCurrent(tree); }
	public void visitLiteral(JCLiteral tree)             { printCurrent(tree); }

	public void visitAssert(JCAssert tree)               {
		printCurrent(tree);
		super.visitAssert(tree);
	}

	public void visitLabelled(JCLabeledStatement tree)   {
		printCurrent(tree);
		super.visitLabelled(tree);
	}

	public void visitVarDef(JCVariableDecl tree) {
		printCurrent(tree);

		super.visitVarDef(tree);
	}

	public void visitBlock(JCBlock tree) {
		printCurrent(tree);

		super.visitBlock(tree);
	}

	public void visitDoLoop(JCDoWhileLoop tree) {
		printCurrent(tree);

		super.visitDoLoop(tree);
	}

	public void visitWhileLoop(JCWhileLoop tree) {
		printCurrent(tree);

		super.visitWhileLoop(tree);
	}

	public void visitForLoop(JCForLoop tree) {
		printCurrent(tree);

		super.visitForLoop(tree);
	}

	public void visitForeachLoop(JCEnhancedForLoop tree) {
		printCurrent(tree);

		super.visitForeachLoop(tree);
	}

	public void visitSwitch(JCSwitch tree) {
		printCurrent(tree);

		super.visitSwitch(tree);
	}

	public void visitCase(JCCase tree) {
		printCurrent(tree);

		super.visitCase(tree);
	}

	public void visitSynchronized(JCSynchronized tree) {
		printCurrent(tree);

		super.visitSynchronized(tree);
	}

	public void visitTry(JCTry tree) {
		printCurrent(tree);

		super.visitTry(tree);
	}

	public void visitCatch(JCCatch tree) {
		printCurrent(tree);

		super.visitCatch(tree);
	}

	public void visitConditional(JCConditional tree) {
		printCurrent(tree);

		super.visitConditional(tree);
	}

	public void visitIf(JCIf tree) {
		printCurrent(tree);

		super.visitIf(tree);
	}

	public void visitExec(JCExpressionStatement tree) {
		printCurrent(tree);

		super.visitExec(tree);
	}

	public void visitBreak(JCBreak tree) {
		printCurrent(tree);

		super.visitBreak(tree);
	}

	public void visitContinue(JCContinue tree) {
		printCurrent(tree);

		super.visitContinue(tree);
	}

	public void visitReturn(JCReturn tree) {
		printCurrent(tree);

		super.visitReturn(tree);
	}

	public void visitThrow(JCThrow tree) {
		printCurrent(tree);

		super.visitThrow(tree);
	}

	public void visitApply(JCMethodInvocation tree) {
		printCurrent(tree);

		super.visitApply(tree);
	}

	public void visitNewClass(JCNewClass tree) {
		printCurrent(tree);

		// super.visitNewClass(tree);
	}

	public void visitNewArray(JCNewArray tree) {
		printCurrent(tree);

		super.visitNewArray(tree);
	}

	public void visitParens(JCParens tree) {
		printCurrent(tree);

		super.visitParens(tree);
	}

	public void visitAssign(JCAssign tree) {
		printCurrent(tree);

		super.visitAssign(tree);
	}

	public void visitAssignop(JCAssignOp tree) {
		printCurrent(tree);

		super.visitAssignop(tree);
	}

	public void visitUnary(JCUnary tree) {
		printCurrent(tree);

		super.visitUnary(tree);
	}

	public void visitBinary(JCBinary tree) {
		printCurrent(tree);

		super.visitBinary(tree);
	}

	public void visitTypeCast(JCTypeCast tree) {
		printCurrent(tree);

		super.visitTypeCast(tree);
	}

	public void visitTypeTest(JCInstanceOf tree) {
		printCurrent(tree);

		super.visitTypeTest(tree);
	}

	public void visitIndexed(JCArrayAccess tree) {
		printCurrent(tree);

		super.visitIndexed(tree);
	}

	public void visitSelect(JCFieldAccess tree) {
		printCurrent(tree);

		super.visitSelect(tree);
	}

	private static void printCurrent(JCTree tree) {
		if (tree.predecessors != null) {
			for (JCTree next : tree.predecessors) {
				System.out.println(nodeText(next) + " -> " + nodeText(tree));
			}
		}
	}

	private static String nodeText(JCTree tree) {
		return "\"" + tree.id + " " + tree.toString().replace("\"", "\\\"") +
				"\"";
	}
}