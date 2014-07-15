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
 * Contributor(s): Sean L. Mooney
 */
package org.paninij.comp;

import static com.sun.tools.javac.code.Flags.PUBLIC;
import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.Flags.SYNTHETIC;

import java.io.File;

import javax.tools.JavaFileObject;

import sun.util.logging.resources.logging;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;

/**Desugar elements to final ASTs.
 * Companion/parallel to {@link com.sun.tools.javac.comp.Lower}
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public class Lower {

    protected static final Context.Key<Lower> lowerKey =
            new Context.Key<Lower>();

    final com.sun.tools.javac.comp.Attr jAttr;
    final org.paninij.comp.Attr pAttr;
    final MemberEnter memberEnter;
    final Names names;
    final TreeMaker make;
    final Resolve rs;
    final Symtab syms;
    final Types types;


    public static Lower instance(Context context) {
        Lower instance = context.get(lowerKey);
        if (instance == null)
            instance = new Lower(context);
        return instance;
    }

    protected Lower(Context context) {
        context.put(lowerKey, this);

        jAttr = com.sun.tools.javac.comp.Attr.instance(context);
        pAttr = Attr.instance(context);
        memberEnter = MemberEnter.instance(context);
        names = Names.instance(context);
        make  = TreeMaker.instance(context);
        rs    = Resolve.instance(context);
        syms  = Symtab.instance(context);
        types = Types.instance(context);
    }

    /**
     * Create a main method for a closed capsule.
     *
     * @param tree
     * @return
     */
    protected JCMethodDecl createMainMethod(JCCapsuleDecl tree, Env<AttrContext> env) {
        /* main looks like:
        CapsuleType c = new CapsuleType();
        (c.args = args)? //only if the capsule has a sing paramater of type String[]
        c.run();
        */

        Type stringArrayType = new ArrayType(syms.stringType, syms.arrayClass);
        // /*synthetic*/ public static void main(String[] args)
        MethodSymbol msym = new MethodSymbol(
                PUBLIC | STATIC | SYNTHETIC,
                names.panini.Main,
                new MethodType(List.<Type>of(stringArrayType),
                        syms.voidType,
                        List.<Type>nil(),
                        syms.methodClass),
               tree.sym);

        ListBuffer<JCStatement> mainStmts = new ListBuffer<JCTree.JCStatement>();
        Name sysArgs = names.fromString("args");
        // String[] args parameter.
        JCVariableDecl mainArg = make.VarDef(
                new VarSymbol(0, sysArgs, stringArrayType, null), null);

        createMainMethodBody(tree, sysArgs, mainStmts, env);

        //Add the statements to the method tree.
        JCMethodDecl maindecl = make.MethodDef(msym, make.Block(0, mainStmts.toList()));
        maindecl.params = List.<JCVariableDecl> of(mainArg);

        memberEnter.memberEnter(maindecl, env);
        return maindecl;
    }

    /**
     * Create the method body. <p> Either create a new instance of the capsule
     * kind an run it, or throw an exception for a non executable capsule.
     *
     * <b>pre:</b> tree.sym.flags_field & Flags.ACTIVE) != 0
     *
     * @param tree      [in]
     * @param sysArgs   [in]
     * @param mainStmts [out]
     */
    private void createMainMethodBody(JCCapsuleDecl tree, Name sysArgs,  ListBuffer<JCStatement> mainStmts, Env<AttrContext> env) {
        Assert.check((tree.sym.flags_field & Flags.ACTIVE) != 0 ,
                "Attempting to install a main method in a non-thread capsule kind"
                );
        // Body throws an exception if refCount == 0, and user did not provide a run method.
        if ( tree.sym.capsule_info.refCount == 0 && tree.hasSynthRunMethod ) {
            createMainThrowsUnrunnable(mainStmts);
        } else {
			if (!env.toplevel.sourcefile.isNameCompatible(
					tree.parentCapsule.name.toString(),
					JavaFileObject.Kind.SOURCE))
				pAttr.log.error("active.capsule.filename.mismatch",
						tree.parentCapsule.name,
						new File(env.toplevel.sourcefile.getName()).getName());
			createMainRunCapsule(tree, sysArgs, mainStmts);
		}
    }

    /**
     * @param tree      [in]
     * @param sysArgs   [in]
     * @param mainStmts [out]
     */
    private void createMainRunCapsule(JCCapsuleDecl tree, Name sysArgs, ListBuffer<JCStatement> mainStmts) {

        Name c = names.fromString("c$0");
        //Generated elems. there isn't a source code position.
        make.at(Position.NOPOS);
        // CapsuleType c = new CapsuleType();
        mainStmts.add(
                make.VarDef(make.Modifiers(0),
                        c, make.Ident(tree.name),
                        make.NewClass(null, null,
                                make.Ident(tree.name), List.<JCExpression>nil(), null))
                );


        final int numParams = tree.params.length();
        //wire the command line args, if the capsule needs them.
        if (numParams == 1) {
            JCVariableDecl pDecl = tree.params.head;
            if (types.isArray(pDecl.vartype.type)
                    && types.elemtype(pDecl.vartype.type).equals(
                            syms.stringType)) {

            }
            mainStmts.add( // c$0.args = args;
                    make.Exec(make.Assign(
                            make.Select(make.Ident(c), pDecl.name),
                            make.Ident(sysArgs))));
        }

        // run | start
        // Active capsules use run, serial capsules use start
        Name initMethod = names.panini.Run ;
        mainStmts.add( //c$0.start(); or c$0.run();
                make.Exec(
                        make.Apply(List.<JCExpression>nil(),
                                make.Select(make.Ident(c), initMethod),
                                List.<JCExpression>nil())) );
    }

    /** Body throws a new UnrunnableCapsule exception.
     * @param mainStmts [out]
     */
    private void createMainThrowsUnrunnable(ListBuffer<JCStatement> mainStmts) {
        JCExpression pid;
        pid = make.Ident(names.panini.Org);
        pid = make.Select(pid, names.panini.Paninij);
        pid = make.Select(pid, names.panini.Runtime);
        pid = make.Select(pid, names.panini.UnrunnableCapsuleExceptionClass);
        //throw new UnrunnableCapsule();
        JCStatement throwsStmt = make.Throw(
                make.NewClass(
                        null, List.<JCExpression>nil(),
                        make.Ident(names.panini.UnrunnableCapsuleExceptionClass),
                        List.<JCExpression>nil(), null));
        mainStmts.add(throwsStmt);
    }

    /**
     * Capsule kinds need a main method if they are closed and define their own run method
     * or are serial. Capsules are assumed to define their own run method if the
     * method referenced by {@link #computeMethod} is not marked as synthetic.
     * <p>
     * Closed capsules have either no params, or a single param of type String[].
     * <p>
     * Install a main method into all closed active capsule.
     */
    protected boolean needsMainMethod(JCCapsuleDecl tree) {
        boolean isClosedCapsule = tree.params.isEmpty()
                || (tree.params.size() == 1 //Check for a String[] type parameter.
                        && types.isArray(tree.params.head.vartype.type)
                        && types.isSameType( //Check for String
                                types.elemtype(tree.params.head.vartype.type),
                                syms.stringType));

        if (isClosedCapsule) {
            return (tree.sym.flags_field & Flags.ACTIVE) != 0 ;
        } else {
            return false;
        }
    }

    public final void visitCapsuleDef(final JCCapsuleDecl tree, Env<AttrContext> env){
        if (needsMainMethod(tree)) {
            JCMethodDecl mainMeth = createMainMethod(tree, env);
            final boolean prevCheckCapState = pAttr.checkCapStateAcc;
            try{
                tree.switchToCapsule();
                pAttr.checkCapStateAcc = false;
                Scope sysScope = pAttr.enterSystemScope(env);
                sysScope.enterIfAbsent(mainMeth.sym);
                tree.defs = tree.defs.append(mainMeth);
                jAttr.attribStat(mainMeth, env);
            } finally {
                tree.switchToClass();
                pAttr.checkCapStateAcc = prevCheckCapState;
            }
        }
    }
}
