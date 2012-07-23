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
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.code.Type.*;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;


class BlockDivisionPointScanner extends TreeScanner {
    int point;
    boolean inLoop;
    
    public void visitProcApply(JCProcInvocation tree) {
        point++;
        visitApply(tree);
    }
    
    public void visitFree(JCFree tree){
        point++;
    	scan(tree.exp);
    }
    
    public void visitClassDef(JCClassDecl tree) {
        point++;
        scan(tree.mods);
        scan(tree.typarams);
        scan(tree.extending);
        scan(tree.implementing);
        scan(tree.defs);
    }

    public void visitMethodDef(JCMethodDecl tree) {
        point++;
        scan(tree.mods);
        scan(tree.restype);
        scan(tree.typarams);
        scan(tree.params);
        scan(tree.thrown);
        scan(tree.defaultValue);
        scan(tree.body);
    }

    public void visitVarDef(JCVariableDecl tree) {
        point++;
        scan(tree.mods);
        scan(tree.vartype);
        scan(tree.init);
    }

    public void visitSkip(JCSkip tree) {
        point++;
    }

    public void visitBlock(JCBlock tree) {
        point++;
        scan(tree.stats);
    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        boolean b = inLoop;
        inLoop = true;
        point++;
        scan(tree.body);
        scan(tree.cond);
        inLoop = b;
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        boolean b = inLoop;
        inLoop = true;
        point++;
        scan(tree.cond);
        scan(tree.body);
        inLoop = b;
    }

    public void visitForLoop(JCForLoop tree) {
        boolean b = inLoop;
        inLoop = true;
        point++;
        scan(tree.init);
        scan(tree.cond);
        scan(tree.step);
        scan(tree.body);
        inLoop = b;
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        boolean b = inLoop;
        inLoop = true;
        point++;
        scan(tree.var);
        scan(tree.expr);
        scan(tree.body);
        inLoop = b;
    }

    public void visitLabelled(JCLabeledStatement tree) {
        point++;
        scan(tree.body);
    }

    public void visitSwitch(JCSwitch tree) {
        point++;
        scan(tree.selector);
        scan(tree.cases);
    }

    public void visitCase(JCCase tree) {
        point++;
        scan(tree.pat);
        scan(tree.stats);
    }

    public void visitSynchronized(JCSynchronized tree) {
        point++;
        scan(tree.lock);
        scan(tree.body);
    }

    public void visitTry(JCTry tree) {
        point++;
        scan(tree.resources);
        scan(tree.body);
        scan(tree.catchers);
        scan(tree.finalizer);
    }

    public void visitCatch(JCCatch tree) {
        point++;
        scan(tree.param);
        scan(tree.body);
    }

    public void visitConditional(JCConditional tree) {
        point++;
        scan(tree.cond);
        scan(tree.truepart);
        scan(tree.falsepart);
    }

    public void visitIf(JCIf tree) {
        point++;
        scan(tree.cond);
        scan(tree.thenpart);
        scan(tree.elsepart);
    }

    public void visitExec(JCExpressionStatement tree) {
        point++;
        scan(tree.expr);
    }

    public void visitBreak(JCBreak tree) {
        point++;
    }

    public void visitContinue(JCContinue tree) {
        point++;
    }

    public void visitReturn(JCReturn tree) {
        point++;
        scan(tree.expr);
    }

    public void visitThrow(JCThrow tree) {
        point++;
        scan(tree.expr);
    }

    public void visitAssert(JCAssert tree) {
        point++;
        scan(tree.cond);
        scan(tree.detail);
    }

    public void visitApply(JCMethodInvocation tree) {
        point++;
        scan(tree.typeargs);
        scan(tree.meth);
        scan(tree.args);
    }

    public void visitNewClass(JCNewClass tree) {
        point++;
        scan(tree.encl);
        scan(tree.clazz);
        scan(tree.typeargs);
        scan(tree.args);
        scan(tree.def);
    }

    public void visitNewArray(JCNewArray tree) {
        point++;
        scan(tree.elemtype);
        scan(tree.dims);
        scan(tree.elems);
    }

    public void visitLambda(JCLambda tree) {
        point++;
        scan(tree.body);
        scan(tree.params);
    }

    public void visitParens(JCParens tree) {
        point++;
        scan(tree.expr);
    }

    public void visitAssign(JCAssign tree) {
        point++;
        scan(tree.lhs);
        scan(tree.rhs);
    }

    public void visitAssignop(JCAssignOp tree) {
        point++;
        scan(tree.lhs);
        scan(tree.rhs);
    }

    public void visitUnary(JCUnary tree) {
        point++;
        scan(tree.arg);
    }

    public void visitBinary(JCBinary tree) {
        point++;
        scan(tree.lhs);
        scan(tree.rhs);
    }

    public void visitTypeCast(JCTypeCast tree) {
        point++;
        scan(tree.clazz);
        scan(tree.expr);
    }

    public void visitTypeTest(JCInstanceOf tree) {
        point++;
        scan(tree.expr);
        scan(tree.clazz);
    }

    public void visitIndexed(JCArrayAccess tree) {
        point++;
        scan(tree.indexed);
        scan(tree.index);
    }

    public void visitSelect(JCFieldAccess tree) {
        point++;
        scan(tree.selected);
    }

    public void visitReference(JCMemberReference tree) {
        point++;
        scan(tree.expr);
        scan(tree.typeargs);
    }

    public void visitIdent(JCIdent tree) {
        point++;
    }

    public void visitLiteral(JCLiteral tree) {
        point++;
    }

    public void visitTypeIdent(JCPrimitiveTypeTree tree) {
        point++;
    }

    public void visitTypeArray(JCArrayTypeTree tree) {
        point++;
        scan(tree.elemtype);
    }

    public void visitTypeApply(JCTypeApply tree) {
        point++;
        scan(tree.clazz);
        scan(tree.arguments);
    }

    public void visitTypeUnion(JCTypeUnion tree) {
        point++;
        scan(tree.alternatives);
    }

    public void visitTypeParameter(JCTypeParameter tree) {
        point++;
        scan(tree.bounds);
    }

    @Override
    public void visitWildcard(JCWildcard tree) {
        point++;
        scan(tree.kind);
        if (tree.inner != null)
            scan(tree.inner);
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind that) {
        point++;
    }

    public void visitModifiers(JCModifiers tree) {
        point++;
        scan(tree.annotations);
    }

    public void visitAnnotation(JCAnnotation tree) {
        point++;
        scan(tree.annotationType);
        scan(tree.args);
    }

    public void visitErroneous(JCErroneous tree) {
        point++;
    }

    public void visitLetExpr(LetExpr tree) {
        point++;
        scan(tree.defs);
        scan(tree.expr);
    }

    public void visitTree(JCTree tree) {
        point++;
        Assert.error();
    }
}        
