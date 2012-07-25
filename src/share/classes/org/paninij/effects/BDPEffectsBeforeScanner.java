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


class BDPEffectsBeforeScanner extends TreeScanner {
    int point;
    JCTree loopBody;
    BlockDivisionPoint bdp;
    EffectSet effects;
    boolean lhs, ignore;

    public EffectSet effectsBefore(BlockDivisionPoint bdp) {
        this.bdp = bdp;
        effects = new EffectSet();
        loopBody = null;
        point = 0;
        lhs = true; ignore = false;
        
        scan(bdp.block);

        return effects;
    }
      
    public void visitProcApply(JCProcInvocation tree) {
        if (checkPoint()) return;

        Symbol sym = TreeInfo.symbol(tree.meth);
        effects.add(new OpenEffect((MethodSymbol)sym));

        boolean oi = ignore;
        ignore = true;
        scan(tree.typeargs);
        scan(tree.meth);
        ignore = oi;
        scan(tree.args);
        
    }
    
    public void visitFree(JCFree tree){
        if (checkPoint()) return;
    	scan(tree.exp);
    }
    
    public void visitClassDef(JCClassDecl tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.mods);
        scan(tree.typarams);
        scan(tree.extending);
        scan(tree.implementing);
        scan(tree.defs);
        ignore = oi;
    }

    public void visitMethodDef(JCMethodDecl tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.mods);
        scan(tree.restype);
        scan(tree.typarams);
        scan(tree.params);
        scan(tree.thrown);
        scan(tree.defaultValue);
        scan(tree.body);
    }

    public void visitVarDef(JCVariableDecl tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.mods);
        scan(tree.vartype);
        ignore = oi;
        scan(tree.init);
    }

    public void visitSkip(JCSkip tree) {
        if (checkPoint()) return;
    }

    public void visitBlock(JCBlock tree) {
        if (checkPoint()) return;
        scan(tree.stats);
    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        JCTree b = loopBody;
        loopBody = tree.body;
        if (checkPoint()) return;
        scan(tree.body);
        scan(tree.cond);
        loopBody = b;
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        JCTree b = loopBody;
        b = tree.body;
        if (checkPoint()) return;
        scan(tree.cond);
        scan(tree.body);
        loopBody = b;
    }

    public void visitForLoop(JCForLoop tree) {
        JCTree b = loopBody;
        b = tree.body;
        if (checkPoint()) return;
        scan(tree.init);
        scan(tree.cond);
        scan(tree.step);
        scan(tree.body);
        loopBody = b;
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        JCTree b = loopBody;
        b = tree.body;
        if (checkPoint()) return;
        scan(tree.var);
        scan(tree.expr);
        scan(tree.body);
        loopBody = b;
    }

    public void visitLabelled(JCLabeledStatement tree) {
        if (checkPoint()) return;
        scan(tree.body);
    }

    public void visitSwitch(JCSwitch tree) {
        if (checkPoint()) return;
        scan(tree.selector);
        scan(tree.cases);
    }

    public void visitCase(JCCase tree) {
        if (checkPoint()) return;
        scan(tree.pat);
        scan(tree.stats);
    }

    public void visitSynchronized(JCSynchronized tree) {
        if (checkPoint()) return;
        scan(tree.lock);
        scan(tree.body);
    }

    public void visitTry(JCTry tree) {
        if (checkPoint()) return;
        scan(tree.resources);
        scan(tree.body);
        scan(tree.catchers);
        scan(tree.finalizer);
    }

    public void visitCatch(JCCatch tree) {
        if (checkPoint()) return;
        scan(tree.param);
        scan(tree.body);
    }

    public void visitConditional(JCConditional tree) {
        if (checkPoint()) return;
        scan(tree.cond);
        scan(tree.truepart);
        scan(tree.falsepart);
    }

    public void visitIf(JCIf tree) {
        if (checkPoint()) return;
        scan(tree.cond);
        scan(tree.thenpart);
        scan(tree.elsepart);
    }

    public void visitExec(JCExpressionStatement tree) {
        if (checkPoint()) return;
        scan(tree.expr);
    }

    public void visitBreak(JCBreak tree) {
        if (checkPoint()) return;
    }

    public void visitContinue(JCContinue tree) {
        if (checkPoint()) return;
    }

    public void visitReturn(JCReturn tree) {
        if (checkPoint()) return;
        scan(tree.expr);
    }

    public void visitThrow(JCThrow tree) {
        if (checkPoint()) return;
        scan(tree.expr);
    }

    public void visitAssert(JCAssert tree) {
        if (checkPoint()) return;
        scan(tree.cond);
        scan(tree.detail);
    }

    public void visitApply(JCMethodInvocation tree) {
        if (checkPoint()) return;

        Symbol sym = TreeInfo.symbol(tree.meth);
        effects.add(new MethodEffect((MethodSymbol)sym));

        boolean oi = ignore;
        ignore = true;
        scan(tree.typeargs);
        scan(tree.meth);
        ignore = oi;
        scan(tree.args);
    }

    public void visitNewClass(JCNewClass tree) {
        if (checkPoint()) return;

        effects.add(new MethodEffect((MethodSymbol)tree.constructor));
        scan(tree.encl);
        scan(tree.clazz);
        scan(tree.typeargs);
        scan(tree.args);
        scan(tree.def);
    }

    public void visitNewArray(JCNewArray tree) {
        if (checkPoint()) return;
        scan(tree.elemtype);
        scan(tree.dims);
        scan(tree.elems);
    }

    public void visitLambda(JCLambda tree) {
        if (checkPoint()) return;
        scan(tree.body);
        scan(tree.params);
    }

    public void visitParens(JCParens tree) {
        if (checkPoint()) return;
        scan(tree.expr);
    }

    public void visitAssign(JCAssign tree) {
        if (checkPoint()) return;
        boolean ol = lhs;
        lhs = true;
        scan(tree.lhs);
        lhs = ol;
        scan(tree.rhs);
    }

    public void visitAssignop(JCAssignOp tree) {
        if (checkPoint()) return;
        boolean ol = lhs;
        lhs = true;
        scan(tree.lhs);
        lhs = ol;
        scan(tree.rhs);
    }

    public void visitUnary(JCUnary tree) {
        if (checkPoint()) return;
        scan(tree.arg);
    }

    public void visitBinary(JCBinary tree) {
        if (checkPoint()) return;
        scan(tree.lhs);
        scan(tree.rhs);
    }

    public void visitTypeCast(JCTypeCast tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.clazz);
        ignore = oi;
        scan(tree.expr);
    }

    public void visitTypeTest(JCInstanceOf tree) {
        if (checkPoint()) return;
        scan(tree.expr);
        boolean oi = ignore;
        ignore = true;
        scan(tree.clazz);
        ignore = oi;
    }

    public void visitIndexed(JCArrayAccess tree) {
        if (checkPoint()) return;
        scan(tree.indexed);
        scan(tree.index);
    }

    public void visitSelect(JCFieldAccess tree) {
        if (checkPoint()) return;
        if (!ignore) {
            if (lhs)
                effects.add(new FieldWriteEffect((VarSymbol)tree.sym));
            else
                effects.add(new FieldReadEffect((VarSymbol)tree.sym));
        }
        scan(tree.selected);
    }

    public void visitReference(JCMemberReference tree) {
        if (checkPoint()) return;
        scan(tree.expr);
        scan(tree.typeargs);
    }

    public void visitIdent(JCIdent tree) {
        if (checkPoint()) return;
        if (!ignore && tree.sym.owner instanceof ClassSymbol) {
            if (tree.sym instanceof VarSymbol) {
                if (lhs) 
                    effects.add(new FieldWriteEffect((VarSymbol)tree.sym));
                else 
                    effects.add(new FieldReadEffect((VarSymbol)tree.sym));
            }
        }
    }

    public void visitLiteral(JCLiteral tree) {
        if (checkPoint()) return;
    }

    public void visitTypeIdent(JCPrimitiveTypeTree tree) {
        if (checkPoint()) return;
    }

    public void visitTypeArray(JCArrayTypeTree tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.elemtype);
        ignore = oi;
    }

    public void visitTypeApply(JCTypeApply tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.clazz);
        ignore = oi;
        scan(tree.arguments);
    }

    public void visitTypeUnion(JCTypeUnion tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.alternatives);
        ignore = oi;
    }

    public void visitTypeParameter(JCTypeParameter tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.bounds);
        ignore = oi;
    }

    @Override
    public void visitWildcard(JCWildcard tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.kind);
        if (tree.inner != null)
            scan(tree.inner);
        ignore = oi;
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind that) {
        if (checkPoint()) return;
    }

    public void visitModifiers(JCModifiers tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.annotations);
        ignore = oi;
    }

    public void visitAnnotation(JCAnnotation tree) {
        if (checkPoint()) return;
        boolean oi = ignore;
        ignore = true;
        scan(tree.annotationType);
        scan(tree.args);
        ignore = oi;
    }

    public void visitErroneous(JCErroneous tree) {
        if (checkPoint()) return;
    }

    public void visitLetExpr(LetExpr tree) {
        if (checkPoint()) return;
        scan(tree.defs);
        scan(tree.expr);
    }

    public void visitTree(JCTree tree) {
        Assert.error();
    }

    public boolean checkPoint() {
        if (bdp.point == point) 
            return true;
        else {
            point++; return false;
        }
    }
}        
