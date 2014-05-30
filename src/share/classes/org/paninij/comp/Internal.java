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
 * http://www.paninij.org/
 *
 * Contributor(s): Rex Fernando
 */

package org.paninij.comp;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.util.List;
import java.util.HashMap;
import com.sun.tools.javac.tree.JCTree.*;

public class Internal {
    protected TreeMaker make;
    protected Names names;
    protected int closureCounter;
    protected HashMap<Name, Type> oldFieldTypes;
    protected HashMap<Name, JCExpression> oldFieldExpressions;
    public static HashMap<Name, Integer> numEventHandlers;
    public static HashMap<String, Integer> numRefining;
    protected int specCounter;

    public JCIdent leftmostIdent(JCExpression tree) {
        if (tree.getTag() == JCTree.Tag.SELECT)
            return leftmostIdent(((JCFieldAccess)tree).selected);
        if (tree.getTag() == JCTree.Tag.APPLY)
            return leftmostIdent(((JCMethodInvocation)tree).meth);
        if (tree.getTag() == JCTree.Tag.INDEXED)
            return leftmostIdent(((JCArrayAccess)tree).indexed);
        if (tree.getTag() == JCTree.Tag.IDENT)
            return (JCIdent)tree;

        System.out.println("This shouldn't happen.");
        System.exit(5555); //TODOREX actual error message here
        return null;
    }

    public void replaceLeftmostIdent(JCExpression tree, final JCExpression replacement) {
        class IdentTranslator extends TreeTranslator {
            public void visitApply(JCMethodInvocation tree) {
                tree.meth = translate(tree.meth);
                result = tree;
            }

            public void visitSelect(JCFieldAccess tree) {
                tree.selected = translate(tree.selected);
                result = tree;
            }

            public void visitIndexed(JCArrayAccess tree) {
                tree.indexed = translate(tree.indexed);
                result = tree;
            }

            public void visitIdent(JCIdent tree) {
                result = replacement;
            }
        }

        IdentTranslator i = new IdentTranslator();
        i.translate(tree);
    }

    public Internal(TreeMaker make, Names names) {
        this.make = make;
        this.names = names;
        oldFieldTypes = new HashMap<Name, Type>();
        oldFieldExpressions = new HashMap<Name, JCExpression>();
    }
    
    protected JCExpression thist() {
        return id(names._this);
    }

    protected TreeMaker make0() {
        return make.at(Position.NOPOS);
    }

    protected JCModifiers mods(long flags) {
        return make0().Modifiers(flags, List.<JCAnnotation>nil());
    }

    protected JCModifiers mods(JCModifiers m, long flags) {
        return make0().Modifiers(m.flags | flags, List.<JCAnnotation>nil());
    }

    protected JCModifiers mods(long flags, ListBuffer<JCAnnotation> annotations) {
        return make0().Modifiers(flags, annotations.toList());
    }

    protected JCAnnotation ann(String name, ListBuffer<JCExpression> args) {
        String[] objects = name.split("\\.");
        
        JCExpression s;
        if (objects.length == 0) 
            s = make0().Ident(names.fromString(name));
        else
            s = make0().Ident(names.fromString(objects[0]));
        
        for (int i = 1; i < objects.length; i++)
            s = make0().Select(s, names.fromString(objects[i]));

        return make0().Annotation(s, args.toList());
    }
    
    protected JCAnnotation ann(JCExpression s, List<JCExpression> args) {
        return make0().Annotation(s, args);
    }

    protected JCExpression select(String name) {
        String[] objects = name.split("\\.");
        
        JCExpression s;
        if (objects.length == 0) 
            s = make0().Ident(names.fromString(name));
        else
            s = make0().Ident(names.fromString(objects[0]));
        
        for (int i = 1; i < objects.length; i++)
            s = make0().Select(s, names.fromString(objects[i]));

        return s;
    }

    protected JCAnnotation ann(String name) {
        return ann(name, new ListBuffer<JCExpression>());
    }

    protected JCMethodDecl constructor(JCModifiers mods, ListBuffer<JCVariableDecl> params, JCBlock body) {
        return make0().MethodDef(mods, 
                                 names.init, 
                                 null, 
                                 List.<JCTypeParameter>nil(), 
                                 params.toList(), 
                                 List.<JCExpression>nil(),
                                 body,
                                 null);
    }

    protected JCMethodDecl method(JCModifiers mods, Name name, JCExpression type, List<JCVariableDecl> params) {
        return make0().MethodDef(mods, 
                                 name, 
                                 type, 
                                 List.<JCTypeParameter>nil(), 
                                 params, 
                                 List.<JCExpression>nil(),
                                 body(),
                                 null);
    }
    
    protected JCMethodDecl method(JCModifiers mods, Name name, JCExpression type, ListBuffer<JCVariableDecl> params, JCBlock body) {
        return make0().MethodDef(mods, 
                                 name, 
                                 type, 
                                 List.<JCTypeParameter>nil(), 
                                 params.toList(), 
                                 List.<JCExpression>nil(),
                                 body,
                                 null);
    }

    protected JCMethodDecl method(JCModifiers mods, Name name, JCExpression type, ListBuffer<JCVariableDecl> params, ListBuffer<JCExpression> throwing, JCBlock body) {
        return make0().MethodDef(mods, 
                                 name, 
                                 type, 
                                 List.<JCTypeParameter>nil(), 
                                 params.toList(), 
                                 throwing.toList(),
                                 body,
                                 null);
    }

    protected JCMethodDecl method(JCModifiers mods, String name, JCExpression type, ListBuffer<JCVariableDecl> params, JCBlock body) {
        return make0().MethodDef(mods, 
                                 names.fromString(name), 
                                 type, 
                                 List.<JCTypeParameter>nil(), 
                                 params.toList(), 
                                 List.<JCExpression>nil(),
                                 body,
                                 null);
    }
    
    protected JCMethodDecl method(JCModifiers mods, Name name, JCExpression type, JCBlock body) {
        return make0().MethodDef(mods, 
                                 name, 
                                 type, 
                                 List.<JCTypeParameter>nil(), 
                                 List.<JCVariableDecl>nil(), 
                                 List.<JCExpression>nil(),
                                 body,
                                 null);
    }

    protected JCMethodDecl method(JCModifiers mods, String name, JCExpression type, ListBuffer<JCVariableDecl> params, ListBuffer<JCExpression> throwing, JCBlock body) {
        return method(mods, names.fromString(name), type, params, throwing, body);
    }

    protected JCMethodDecl method(JCModifiers mods, String name, JCExpression type, ListBuffer<JCVariableDecl> params) {
        return method(mods, name, type, params,(JCBlock)null);
    }

    protected JCMethodDecl method(JCModifiers mods, Name name, JCExpression type, ListBuffer<JCVariableDecl> params) {
        return method(mods, name, type, params, (JCBlock)null);
    }

    protected JCMethodDecl method(JCModifiers mods, Name name, JCExpression type, ListBuffer<JCVariableDecl> params, ListBuffer<JCExpression> throwing) {
        return method(mods, name, type, params, throwing, null);
    }

    protected JCMethodDecl method(JCModifiers mods, String name, JCExpression type, ListBuffer<JCVariableDecl> params, ListBuffer<JCExpression> throwing) {
        return method(mods, names.fromString(name), type, params, throwing, null);
    }

    protected JCMethodDecl method(JCModifiers mods, String name, JCExpression type) {
        return method(mods, name, type, new ListBuffer<JCVariableDecl>());
    }

    protected JCMethodDecl method(JCModifiers mods, String name, String type, ListBuffer<JCVariableDecl> params, JCBlock body) {
        return method(mods, name, make0().Ident(names.fromString(type)), params, body);
    }

    protected JCMethodDecl method(JCModifiers mods, String name, String type, ListBuffer<JCVariableDecl> params) {
        return method(mods, name, make0().Ident(names.fromString(type)), params);
    }

    protected JCMethodDecl method(JCModifiers mods, String name, String type) {
        return method(mods, name, make0().Ident(names.fromString(type)));
    }

    protected JCClassDecl clazz(JCModifiers mods, String name, ListBuffer<JCTree> defs) {
        return make0().ClassDef(mods,
                                names.fromString(name),
                                List.<JCTypeParameter>nil(),
                                null,
                                List.<JCExpression>nil(),
                                defs.toList());
    }

    protected JCClassDecl clazz(JCModifiers mods, String name, ListBuffer<JCExpression> implementing, ListBuffer<JCTree> defs) {
        return make0().ClassDef(mods,
                                names.fromString(name),
                                List.<JCTypeParameter>nil(),
                                null,
                                implementing.toList(),
                                defs.toList());
    }

    protected JCClassDecl clazz(JCModifiers mods, String name, JCExpression extending, ListBuffer<JCTree> defs) {
        return make0().ClassDef(mods,
                                names.fromString(name),
                                List.<JCTypeParameter>nil(),
                                extending,
                                List.<JCExpression>nil(),
                                defs.toList());
    }

    protected JCVariableDecl var(JCModifiers mods, String name, String type) {
        return make0().VarDef(mods,
                              names.fromString(name),
                              make0().Ident(names.fromString(type)),
                              null);
    }
    
    protected JCVariableDecl var(JCModifiers mods, Name name, JCExpression type) {
        return make0().VarDef(mods,
                              name,
                              type,
                              null);
    }

    protected JCVariableDecl var(JCModifiers mods, String name, JCExpression type) {
        return make0().VarDef(mods,
                              names.fromString(name),
                              type,
                              null);
    }

    protected JCVariableDecl var(JCModifiers mods, String name, String type, JCExpression init) {
        return make0().VarDef(mods,
                              names.fromString(name),
                              make0().Ident(names.fromString(type)),
                              init);
    }

    protected JCVariableDecl var(JCModifiers mods, String name, JCExpression type, JCExpression init) {
        return make0().VarDef(mods,
                              names.fromString(name),
                              type,
                              init);
    }

    protected JCPrimitiveTypeTree voidt() {
        return make0().TypeIdent(TypeTags.VOID);
    }

    protected JCPrimitiveTypeTree booleant() {
        return make0().TypeIdent(TypeTags.BOOLEAN);
    }
    
    protected JCPrimitiveTypeTree intt() {
        return make0().TypeIdent(TypeTags.INT);
    }
    
    protected JCPrimitiveTypeTree bytet() {
        return make0().TypeIdent(TypeTags.BYTE);
    }
    
    protected JCPrimitiveTypeTree chart() {
        return make0().TypeIdent(TypeTags.CHAR);
    }
    
    protected JCPrimitiveTypeTree longt() {
        return make0().TypeIdent(TypeTags.LONG);
    }
    
    protected JCPrimitiveTypeTree doublet() {
        return make0().TypeIdent(TypeTags.DOUBLE);
    }

    protected JCPrimitiveTypeTree floatt() {
        return make0().TypeIdent(TypeTags.FLOAT);
    }
    
    protected JCPrimitiveTypeTree shortt() {
        return make0().TypeIdent(TypeTags.SHORT);
    }
    
    protected JCLiteral truev() {
        return make0().Literal(new Boolean(true));
    }

    protected JCLiteral falsev() {
        return make0().Literal(new Boolean(false));
    }
    
    protected ListBuffer<JCExpression> throwing(JCExpression ... a) {
        return lb(a);
    }
    
    protected boolean isVoid(JCTree t) {
        if (t.getTag() == JCTree.Tag.TYPEIDENT && ((JCPrimitiveTypeTree) t).typetag == TypeTags.VOID)
            return true;
        return false;
    }

    protected JCExpression defaultt(JCExpression returnType) {
        if (returnType.getTag() == JCTree.Tag.TYPEIDENT) {
            JCPrimitiveTypeTree t = (JCPrimitiveTypeTree)returnType;
            if (t.typetag == TypeTags.BOOLEAN)
                return make0().Literal(TypeTags.BOOLEAN, new Integer(0));
            return make0().Literal(TypeTags.INT, new Integer(0));
        }
        return nullv();
    }

    @SuppressWarnings({"unchecked", "varargs"})
    protected <T> ListBuffer<T> lb(T ... a) {
        ListBuffer<T> returnValue = new ListBuffer<T>();
        for (int i = 0; i < a.length; i++)
            returnValue.append(a[i]);
        return returnValue;
    }

    protected ListBuffer<JCVariableDecl> params(JCVariableDecl ... a) {
        return lb(a);
    }

    protected ListBuffer<JCTree> defs(JCTree ... a) {
        return lb(a);
    }

    protected ListBuffer<JCExpression> args(JCExpression ... a) {
        return lb(a);
    }

    protected ListBuffer<JCExpression> args(ListBuffer<JCExpression> a) {
        return a;
    }

    protected ListBuffer<JCExpression> implementing(JCExpression ... a) {
        return lb(a);
    }

    protected ListBuffer<JCExpression> typeargs(JCExpression ... a) {
        return lb(a);
    }

    protected JCTypeApply ta(JCExpression t, ListBuffer<JCExpression> args) {
        return make0().TypeApply(t, args.toList());
    }

    protected JCBlock body(JCStatement ... a) {
        return make0().Block(0, lb(a).toList());
    }


    protected JCBlock body(ListBuffer<JCStatement> a) {
        return make0().Block(0, a.toList());
    }

    protected JCWhileLoop whilel(JCExpression cond, JCStatement body) {
        return make0().WhileLoop(cond, body);
    }

    protected JCNewClass newt(List<JCExpression> typeArgs, JCExpression type, ListBuffer<JCExpression> args) {
        return make0().NewClass(null, typeArgs, type, args.toList(), null);
    }

    protected JCNewClass newt(JCExpression type, ListBuffer<JCExpression> args) {
        return newt(List.<JCExpression>nil(), type, args);
    }

    protected JCNewClass newt(JCExpression type) {
        return newt(List.<JCExpression>nil(), type, new ListBuffer<JCExpression>());
    }

    protected JCNewClass newt(String type, ListBuffer<JCExpression> args) {
        return newt(List.<JCExpression>nil(), make0().Ident(names.fromString(type)), args);
    }

	protected JCNewClass newt(String type, ListBuffer<JCExpression> args,
			ListBuffer<JCTree> body) {
		return make0().NewClass(null, List.<JCExpression> nil(),
				make0().Ident(names.fromString(type)), args.toList(),
				make0().AnonymousClassDef(mods(0), body.toList()));
    }
    
    protected JCNewClass newt(String type) {
        return newt(List.<JCExpression>nil(), make0().Ident(names.fromString(type)), new ListBuffer<JCExpression>());
    }

    protected JCMethodInvocation apply(String object, String function, ListBuffer<JCExpression> args) {
        String[] objects = object.split("\\.");
        
        JCExpression s;
        if (objects.length == 0) 
            s = make0().Ident(names.fromString(object));
        else
            s = make0().Ident(names.fromString(objects[0]));
        
        for (int i = 1; i < objects.length; i++)
            s = make0().Select(s, names.fromString(objects[i]));

        return make0().Apply(List.<JCExpression>nil(),
                             make0().Select(s,
                                            names.fromString(function)),
                                            args.toList());
    }

    protected JCMethodInvocation apply(String object, String function) {
        return apply(object, function, new ListBuffer<JCExpression>());
    }
   
    protected JCMethodInvocation apply(String meth) {
        return make0().Apply(List.<JCExpression>nil(), id(meth), List.<JCExpression>nil());
    }
 
    protected JCMethodInvocation apply(JCExpression selection, String function) {
        return make0().Apply(List.<JCExpression>nil(),
               make0().Select(selection, names.fromString(function)),
                  List.<JCExpression>nil());
    }
    
    protected JCMethodInvocation apply(JCExpression selection, String function, ListBuffer<JCExpression> args ) {
        return make0().Apply(List.<JCExpression>nil(),
               make0().Select(selection, names.fromString(function)),
                  args.toList());
    }
    
    protected JCMethodInvocation apply(JCExpression selection, String function, List<JCExpression> args ) {
        return make0().Apply(List.<JCExpression>nil(),
               make0().Select(selection, names.fromString(function)),
                  args);
    }
    
    protected JCMethodInvocation apply(JCExpression selection, Name n) {
        return make0().Apply(List.<JCExpression>nil(),
                             make0().Select(selection, n),
                             List.<JCExpression>nil());
    }

    protected JCMethodInvocation apply(String s, Name n) {
        return make0().Apply(List.<JCExpression>nil(),
                             make0().Select(id(s), n),
                             List.<JCExpression>nil());
    }
    
    protected JCMethodInvocation apply(String function, ListBuffer<JCExpression> args) {
        return make0().Apply(List.<JCExpression>nil(),
                             id(function),
                             args.toList());
    }

    protected JCMethodInvocation supert() {
        return make0().Apply(List.<JCExpression>nil(),
                             make0().Ident(names._super),
                             List.<JCExpression>nil());
    }

    protected JCFieldAccess select(String object, String subobject) {
        return make0().Select(make0().Ident(names.fromString(object)), names.fromString(subobject));
    }
    
    protected JCFieldAccess select(String object, Name n) {
        return make0().Select(make0().Ident(names.fromString(object)), n);
    }
    
    protected JCFieldAccess select(Name object, Name n) {
        return make0().Select(make0().Ident(object), n);
    }

    protected JCFieldAccess select(JCExpression object, String subobject) {
        return make0().Select(object, names.fromString(subobject));
    }
    
    protected JCIf ifs(JCExpression cond, JCStatement then) {
        return make0().If(cond, then, null);
    }
  
    protected JCIf ifs(JCExpression cond, JCStatement then, JCStatement els) {
        return make0().If(cond, then, els);
    }

    protected JCAssign assign(String lhs, JCExpression rhs) {
        return make0().Assign(make0().Ident(names.fromString(lhs)), rhs);
    }

    protected JCAssign assign(JCExpression lhs, JCExpression rhs) {
        return make0().Assign(lhs, rhs);
    }

    protected JCUnary nott(JCExpression cond) {
        return make0().Unary(JCTree.Tag.NOT, cond);
    }

    protected JCUnary pp(JCExpression cond) {
        return make0().Unary(JCTree.Tag.POSTINC, cond);
    }

    protected JCUnary mm(JCExpression cond) {
        return make0().Unary(JCTree.Tag.POSTDEC, cond);
    }

    protected JCBinary isNull(String s) {
        return make0().Binary(JCTree.Tag.EQ, make0().Ident(names.fromString(s)), make0().Literal(TypeTags.BOT, null));
    }

    protected JCBinary isNull(JCExpression s) {
        return make0().Binary(JCTree.Tag.EQ, s, make0().Literal(TypeTags.BOT, null));
    }

    protected JCBinary notNull(JCExpression e) {
        return make0().Binary(JCTree.Tag.NE, e, make0().Literal(TypeTags.BOT, null));
    }

    protected JCBinary eqNum(JCExpression e, int n) {
        return make0().Binary(JCTree.Tag.EQ, e, make0().Literal(new Integer(n)));
    }

    protected JCBinary eq(JCExpression e, JCExpression e1) {
        return make0().Binary(JCTree.Tag.EQ, e, e1);
    }

    protected JCBinary notNum(JCExpression e, int n) {
        return make0().Binary(JCTree.Tag.NE, e, make0().Literal(new Integer(n)));
    }

    protected JCBinary geq(JCExpression e, JCExpression e1) {
        return make0().Binary(JCTree.Tag.GE, e, e1);
    }

    protected JCBinary gt(JCExpression e, JCExpression e1) {
        return make0().Binary(JCTree.Tag.GT, e, e1);
    }

    protected JCBinary isFalse(String s) {
        return make0().Binary(JCTree.Tag.EQ,
        					  make0().Ident(names.fromString(s)),
        					  make0().Literal(TypeTags.BOOLEAN, 0));
    }

    protected JCBinary isTrue(String s) {
        return make0().Binary(JCTree.Tag.EQ,
        					  make0().Ident(names.fromString(s)),
        					  make0().Literal(TypeTags.BOOLEAN, true));
    }

    protected JCLiteral intlit(int i) {
        return make0().Literal(i);
    }
    
    protected JCLiteral nullv() {
        return make0().Literal(TypeTags.BOT, null);
    }

    protected JCStatement es(JCExpression e) {
        return make0().Exec(e);
    }

    protected JCExpression id(String s) {
        return make0().Ident(names.fromString(s));
    }

    protected JCExpression id(Name n) {
        return make0().Ident(n);
    }

    protected JCLiteral intc(int i) {
        return make0().Literal(TypeTags.INT, new Integer(i));
    }

    protected JCLiteral stringc(String s) {
        return make0().Literal(TypeTags.CLASS, s);
    }

    protected JCTry tryt(JCBlock body, JCBlock finalizer) {
        return make0().Try(body, List.<JCCatch>nil(), finalizer);
    }

    protected JCTypeCast cast(String clazz, JCExpression expr) {
        return make0().TypeCast(make0().Ident(names.fromString(clazz)), expr);
    }

    protected JCTypeCast cast(JCExpression clazz, JCExpression expr) {
        return make0().TypeCast(clazz, expr);
    }


    protected JCReturn returnt(JCExpression e) {
        return make0().Return(e);
    }
    
    protected JCReturn returnt(String s) {
        return returnt(make0().Ident(names.fromString(s)));
    }

    protected JCMethodInvocation supert(ListBuffer<JCExpression> args) {
        return make0().Apply(List.<JCExpression>nil(),
                             make0().Ident(names._super),
                             args.toList());
    }
    
    protected JCReturn returnt() {
        return make0().Return(null);
    }

    protected JCSynchronized sync(JCExpression lock, JCBlock body) {
        return make0().Synchronized(lock, body);
    }

    protected JCAssert assrt(JCExpression cond) {
        return make0().Assert(cond, null);
    }

    protected JCSwitch swtch(JCExpression selector, ListBuffer<JCCase> cases) {
        return make0().Switch(selector, cases.toList());
    }


    protected JCSwitch swtch(JCExpression selector, JCCase ... cases) {
        return swtch(selector, lb(cases));
    }

    protected JCCase case_(JCExpression pattern, ListBuffer<JCStatement> s) {
        return make0().Case(pattern, s.toList());
    }

    protected JCCase case_(JCExpression pattern, JCStatement ... s) {
        return case_(pattern, lb(s));
    }

    protected JCBreak break_() {
        return make0().Break(null);
    }

    protected JCArrayAccess aindex(JCExpression array, JCExpression index) {
        return make0().Indexed(array, index);
    }
}
