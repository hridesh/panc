/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javac.comp;

import java.util.*;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileManager;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Scope.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.main.Option.PkgInfo;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.tree.JCTree.Tag.*;
// Panini code
import com.sun.tools.javac.parser.ParserFactory;
import org.paninij.comp.AnnotationProcessor;
import org.paninij.util.PaniniConstants;
import org.paninij.util.ListUtils;
import org.paninij.util.Predicate;
// end Panini code

/** This class enters symbols for all encountered definitions into
 *  the symbol table. The pass consists of two phases, organized as
 *  follows:
 *
 *  <p>In the first phase, all class symbols are intered into their
 *  enclosing scope, descending recursively down the tree for classes
 *  which are members of other classes. The class symbols are given a
 *  MemberEnter object as completer.
 *
 *  <p>In the second phase classes are completed using
 *  MemberEnter.complete().  Completion might occur on demand, but
 *  any classes that are not completed that way will be eventually
 *  completed by processing the `uncompleted' queue.  Completion
 *  entails (1) determination of a class's parameters, supertype and
 *  interfaces, as well as (2) entering all symbols defined in the
 *  class into its scope, with the exception of class symbols which
 *  have been entered in phase 1.  (2) depends on (1) having been
 *  completed for a class and all its superclasses and enclosing
 *  classes. That's why, after doing (1), we put classes in a
 *  `halfcompleted' queue. Only when we have performed (1) for a class
 *  and all it's superclasses and enclosing classes, we proceed to
 *  (2).
 *
 *  <p>Whereas the first phase is organized as a sweep through all
 *  compiled syntax trees, the second phase is demand. Members of a
 *  class are entered when the contents of a class are first
 *  accessed. This is accomplished by installing completer objects in
 *  class symbols for compiled classes which invoke the member-enter
 *  phase for the corresponding class tree.
 *
 *  <p>Classes migrate from one phase to the next via queues:
 *
 *  <pre>
 *  class enter -> (Enter.uncompleted)         --> member enter (1)
 *              -> (MemberEnter.halfcompleted) --> member enter (2)
 *              -> (Todo)                      --> attribute
 *                                              (only for toplevel classes)
 *  </pre>
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Enter extends JCTree.Visitor {
    protected static final Context.Key<Enter> enterKey =
        new Context.Key<Enter>();

    Log log;
    Symtab syms;
    Check chk;
    TreeMaker make;
    ClassReader reader;
    Annotate annotate;
    MemberEnter memberEnter;
    Types types;
    Lint lint;
    Names names;
    JavaFileManager fileManager;
    PkgInfo pkginfoOpt;
    // Panini code
    AnnotationProcessor annotationProcessor;
    // end Panini code

    private final Todo todo;

    public static Enter instance(Context context) {
        Enter instance = context.get(enterKey);
        if (instance == null)
            instance = new Enter(context);
        return instance;
    }

    protected Enter(Context context) {
        context.put(enterKey, this);

        log = Log.instance(context);
        reader = ClassReader.instance(context);
        make = TreeMaker.instance(context);
        syms = Symtab.instance(context);
        chk = Check.instance(context);
        memberEnter = MemberEnter.instance(context);
        types = Types.instance(context);
        annotate = Annotate.instance(context);
        lint = Lint.instance(context);
        names = Names.instance(context);

        predefClassDef = make.ClassDef(
            make.Modifiers(PUBLIC),
            syms.predefClass.name, null, null, null, null);
        predefClassDef.sym = syms.predefClass;
        todo = Todo.instance(context);
        fileManager = context.get(JavaFileManager.class);

        Options options = Options.instance(context);
        pkginfoOpt = PkgInfo.get(options);

        // Panini code
        annotationProcessor = new AnnotationProcessor(names, make, ParserFactory.instance(context), log);
        // end Panini code
    }

    /** A hashtable mapping classes and packages to the environments current
     *  at the points of their definitions.
     */
    Map<TypeSymbol,Env<AttrContext>> typeEnvs =
            new HashMap<TypeSymbol,Env<AttrContext>>();

    /** Accessor for typeEnvs
     */
    public Env<AttrContext> getEnv(TypeSymbol sym) {
        return typeEnvs.get(sym);
    }

    public Env<AttrContext> getClassEnv(TypeSymbol sym) {
        Env<AttrContext> localEnv = getEnv(sym);
        Env<AttrContext> lintEnv = localEnv;
        while (lintEnv.info.lint == null)
            lintEnv = lintEnv.next;
        localEnv.info.lint = lintEnv.info.lint.augment(sym.attributes_field, sym.flags());
        return localEnv;
    }

    /** The queue of all classes that might still need to be completed;
     *  saved and initialized by main().
     */
    ListBuffer<ClassSymbol> uncompleted;

    /** A dummy class to serve as enclClass for toplevel environments.
     */
    private JCClassDecl predefClassDef;

/* ************************************************************************
 * environment construction
 *************************************************************************/


    /** Create a fresh environment for class bodies.
     *  This will create a fresh scope for local symbols of a class, referred
     *  to by the environments info.scope field.
     *  This scope will contain
     *    - symbols for this and super
     *    - symbols for any type parameters
     *  In addition, it serves as an anchor for scopes of methods and initializers
     *  which are nested in this scope via Scope.dup().
     *  This scope should not be confused with the members scope of a class.
     *
     *  @param tree     The class definition.
     *  @param env      The environment current outside of the class definition.
     */
    public Env<AttrContext> classEnv(JCClassDecl tree, Env<AttrContext> env) {
        Env<AttrContext> localEnv =
            env.dup(tree, env.info.dup(new Scope(tree.sym)));
        localEnv.enclClass = tree;
        localEnv.outer = env;
        localEnv.info.isSelfCall = false;
        localEnv.info.lint = null; // leave this to be filled in by Attr,
                                   // when annotations have been processed
        return localEnv;
    }

    /** Create a fresh environment for toplevels.
     *  @param tree     The toplevel tree.
     */
    Env<AttrContext> topLevelEnv(JCCompilationUnit tree) {
        Env<AttrContext> localEnv = new Env<AttrContext>(tree, new AttrContext());
        localEnv.toplevel = tree;
        localEnv.enclClass = predefClassDef;
        tree.namedImportScope = new ImportScope(tree.packge);
        tree.starImportScope = new StarImportScope(tree.packge);
        localEnv.info.scope = tree.namedImportScope;
        localEnv.info.lint = lint;
        return localEnv;
    }

    public Env<AttrContext> getTopLevelEnv(JCCompilationUnit tree) {
        Env<AttrContext> localEnv = new Env<AttrContext>(tree, new AttrContext());
        localEnv.toplevel = tree;
        localEnv.enclClass = predefClassDef;
        localEnv.info.scope = tree.namedImportScope;
        localEnv.info.lint = lint;
        return localEnv;
    }

    /** The scope in which a member definition in environment env is to be entered
     *  This is usually the environment's scope, except for class environments,
     *  where the local scope is for type variables, and the this and super symbol
     *  only, and members go into the class member scope.
     */
    Scope enterScope(Env<AttrContext> env) {
        return (env.tree.hasTag(JCTree.Tag.CLASSDEF))
            ? ((JCClassDecl) env.tree).sym.members_field
            : env.info.scope;
    }

/* ************************************************************************
 * Visitor methods for phase 1: class enter
 *************************************************************************/

    /** Visitor argument: the current environment.
     */
    protected Env<AttrContext> env;

    /** Visitor result: the computed type.
     */
    Type result;

    /** Visitor method: enter all classes in given tree, catching any
     *  completion failure exceptions. Return the tree's type.
     *
     *  @param tree    The tree to be visited.
     *  @param env     The environment visitor argument.
     */
    Type classEnter(JCTree tree, Env<AttrContext> env) {
        Env<AttrContext> prevEnv = this.env;
        try {
            this.env = env;
            tree.accept(this);
            return result;
        }  catch (CompletionFailure ex) {
            return chk.completionError(tree.pos(), ex);
        } finally {
            this.env = prevEnv;
        }
    }

    /** Visitor method: enter classes of a list of trees, returning a list of types.
     */
    public <T extends JCTree> List<Type> classEnter(List<T> trees, Env<AttrContext> env) {
        ListBuffer<Type> ts = new ListBuffer<Type>();
        for (List<T> l = trees; l.nonEmpty(); l = l.tail) {
            Type t = classEnter(l.head, env);
            if (t != null)
                ts.append(t);
        }
        return ts.toList();
    }

    @Override
    public void visitTopLevel(JCCompilationUnit tree) {
        JavaFileObject prev = log.useSource(tree.sourcefile);
        boolean addEnv = false;
        boolean isPkgInfo = tree.sourcefile.isNameCompatible("package-info",
                                                             JavaFileObject.Kind.SOURCE);
        if (tree.pid != null) {
            tree.packge = reader.enterPackage(TreeInfo.fullName(tree.pid));
            if (tree.packageAnnotations.nonEmpty() || pkginfoOpt == PkgInfo.ALWAYS) {
                if (isPkgInfo) {
                    addEnv = true;
                } else {
                    log.error(tree.packageAnnotations.head.pos(),
                              "pkg.annotations.sb.in.package-info.java");
                }
            }
        } else {
            tree.packge = syms.unnamedPackage;
        }
        tree.packge.complete(); // Find all classes in package.
        Env<AttrContext> topEnv = topLevelEnv(tree);
        
        setRuntimeImports(topEnv);

        // Save environment of package-info.java file.
        if (isPkgInfo) {
            Env<AttrContext> env0 = typeEnvs.get(tree.packge);
            if (env0 == null) {
                typeEnvs.put(tree.packge, topEnv);
            } else {
                JCCompilationUnit tree0 = env0.toplevel;
                if (!fileManager.isSameFile(tree.sourcefile, tree0.sourcefile)) {
                    log.warning(tree.pid != null ? tree.pid.pos()
                                                 : null,
                                "pkg-info.already.seen",
                                tree.packge);
                    if (addEnv || (tree0.packageAnnotations.isEmpty() &&
                                   tree.docComments != null &&
                                   tree.docComments.get(tree) != null)) {
                        typeEnvs.put(tree.packge, topEnv);
                    }
                }
            }

            for (Symbol q = tree.packge; q != null && q.kind == PCK; q = q.owner)
                q.flags_field |= EXISTS;

            Name name = names.package_info;
            ClassSymbol c = reader.enterClass(name, tree.packge);
            c.flatname = names.fromString(tree.packge + "." + name);
            c.sourcefile = tree.sourcefile;
            c.completer = null;
            c.members_field = new Scope(c);
            tree.packge.package_info = c;
        }
    // Panini code
        tree.defs = capsuleSplitter(tree.defs);
    // end Panini code
        classEnter(tree.defs, topEnv);
        if (addEnv) {
            todo.append(topEnv);
        }
        log.useSource(prev);
        result = null;
    }
    
    // Panini Code
    /**
     * returns 4 copies of class definitions for each capsule declaration
     */
	private List<JCTree> capsuleSplitter(List<JCTree> defs) {
		ListBuffer<JCTree> copiedDefs = new ListBuffer<JCTree>();
		TreeCopier<Void> tc = new TreeCopier<Void>(make);
		for (List<JCTree> l = defs; l.nonEmpty(); l = l.tail) {
			JCTree def = l.head;
			if (def.getTag() == CAPSULEDEF
					&& (((JCCapsuleDecl) def).mods.flags & INTERFACE) == 0) {
				ListBuffer<JCVariableDecl> stateToInit = new ListBuffer<JCVariableDecl>();
				ListBuffer<JCMethodDecl> initMethods = new ListBuffer<JCMethodDecl>();
				ListBuffer<JCWhen> whenMethods = new ListBuffer<JCWhen>();
				JCCapsuleDecl capsule = (JCCapsuleDecl) def;
				ListBuffer<JCTree> interfaceBody = new ListBuffer<JCTree>();
				reorderDefs(capsule);
				boolean hasRun = false;
				int whenCounter = 0;
				for (List<JCTree> c = capsule.defs; c.nonEmpty(); c = c.tail) {
					JCTree capsuleDefs = c.head;
					if (capsuleDefs.getTag() == METHODDEF) {
						JCMethodDecl mdecl = (JCMethodDecl) capsuleDefs;
						if ((mdecl.name.toString().equals("run") && mdecl.params
								.isEmpty())
								|| mdecl.name
										.equals(names.panini.InternalCapsuleWiring))
							hasRun = true;
						if ((mdecl.mods.flags & PRIVATE) == 0
								&& !mdecl.name
										.equals(names.panini.PaniniCapsuleInit)
								&& !mdecl.name
										.equals(names.panini.InternalCapsuleWiring)) {
							interfaceBody.add(make.MethodDef(
									tc.copy(mdecl.mods), mdecl.name,
									tc.copy(mdecl.restype),
									tc.copy(mdecl.typarams),
									tc.copy(mdecl.params),
									tc.copy(mdecl.thrown), null,
									tc.copy(mdecl.defaultValue)));
							if (!hasRun)
								interfaceBody
										.add(make.MethodDef(
												tc.copy(mdecl.mods),
												mdecl.name.append(names
														.fromString(PaniniConstants.PANINI_ORIGINAL_METHOD_SUFFIX)),
												tc.copy(mdecl.restype), tc
														.copy(mdecl.typarams),
												tc.copy(mdecl.params), tc
														.copy(mdecl.thrown),
												null,
												tc.copy(mdecl.defaultValue)));
						}
						if (mdecl.name.equals(names.panini.PaniniCapsuleInit)) {
							initMethods.add(mdecl);
						}
					} else if (capsuleDefs.getTag() == STATE) {
						JCVariableDecl vdecl = (JCVariableDecl) capsuleDefs;
						stateToInit.add(vdecl);
						interfaceBody.add(make.VarDef(tc.copy(vdecl.mods),
								vdecl.name, tc.copy(vdecl.vartype), null));
					} else if (capsuleDefs.getTag() == CLASSDEF) {
						// skip
					} else if (capsuleDefs.getTag() == WHEN) {
						((JCWhen) capsuleDefs).name = ((JCWhen) capsuleDefs).name
								.append(names.fromString("$" + whenCounter));
						((JCWhen) capsuleDefs).changeTag();
						whenMethods.add((JCWhen) capsuleDefs);
						whenCounter++;
					} else
						interfaceBody.add(tc.copy(capsuleDefs));
				}

				JCExpression excp = make.Ident(names.fromString("java"));
				excp = make.Select(excp, names.fromString("lang"));
				excp = make.Select(excp,
						names.fromString("InterruptedException"));
				JCCapsuleDecl copyActive = make.CapsuleDef(make.Modifiers(
						FINAL, annotationProcessor.createCapsuleAnnotation(
								Flags.ACTIVE, capsule)), names
						.fromString(capsule.name + "$thread"), tc
						.copy(capsule.params), tc.copy(capsule.extending), List
						.<JCExpression> of(make.Ident(capsule.name)), tc
						.copy(capsule.defs));
				copyActive.accessMods = capsule.mods.flags;
				JCCapsuleDecl copyCapsule = make
						.CapsuleDef(
								make.Modifiers(INTERFACE, annotationProcessor
										.createCapsuleAnnotation(
												Flags.INTERFACE, capsule)),
								capsule.name,
								tc.copy(capsule.params),
								tc.copy(capsule.extending),
								capsule.implementing
										.append(make.Ident(names
												.fromString(PaniniConstants.PANINI_QUEUE))),
								interfaceBody.toList());
				if(capsule.needsDelegation)
					copyCapsule.needsDelegation = true;
				// Record the init methods and state decls that still need
				// initialized.
				copyCapsule.initMethods = initMethods.toList();
				copyCapsule.stateToInit = stateToInit.toList();
				//
				for (JCWhen w : whenMethods) {
					copyActive.whenConditions = copyActive.whenConditions
							.append(w.getCondition());
				}
				copyCapsule.accessMods = capsule.mods.flags;
				copiedDefs.add(copyCapsule);
				copiedDefs.add(copyActive);
				copyActive.parentCapsule = copyCapsule;
				if (!hasRun) {
					JCCapsuleDecl copyTask = make.CapsuleDef(make.Modifiers(
							FINAL, annotationProcessor.createCapsuleAnnotation(
									Flags.TASK, capsule)), names
							.fromString(capsule.name + "$task"), tc
							.copy(capsule.params), tc.copy(capsule.extending),
							List.<JCExpression> of(make.Ident(capsule.name)),
							tc.copy(capsule.defs));
					copyTask.accessMods = capsule.mods.flags;
					JCCapsuleDecl copySerial = make.CapsuleDef(make.Modifiers(
							FINAL, annotationProcessor.createCapsuleAnnotation(
									Flags.SERIAL, capsule)), names
							.fromString(capsule.name + "$serial"), tc
							.copy(capsule.params), tc.copy(capsule.extending),
							List.<JCExpression> of(make.Ident(capsule.name)),
							tc.copy(capsule.defs));
					copySerial.accessMods = capsule.mods.flags;
					JCCapsuleDecl copyMonitor = make.CapsuleDef(make.Modifiers(
							FINAL, annotationProcessor.createCapsuleAnnotation(
									Flags.MONITOR, capsule)), names
							.fromString(capsule.name + "$monitor"), tc
							.copy(capsule.params), tc.copy(capsule.extending),
							List.<JCExpression> of(make.Ident(capsule.name)),
							tc.copy(capsule.defs));
					copyMonitor.accessMods = capsule.mods.flags;
					copiedDefs.add(copyTask);
					copyTask.parentCapsule = copyCapsule;
					copiedDefs.add(copySerial);
					copySerial.parentCapsule = copyCapsule;
					copiedDefs.add(copyMonitor);
					copyMonitor.parentCapsule = copyCapsule;
				}
			} else
				copiedDefs.add(tc.copy(def));
		}
		return copiedDefs.toList();
	}

    /**
     * @param capsule
     */
    private void reorderDefs(JCCapsuleDecl capsule) {
        Predicate<JCTree> wiringIsFirst = new Predicate<JCTree>() {
            @Override
            public final boolean apply(JCTree t) {
                return t.getTag() == METHODDEF && t instanceof JCDesignBlock;
            }
        };
        capsule.defs = ListUtils.moveToFirst(capsule.defs, wiringIsFirst);
    }
    // end Panini code
    @Override
    public void visitClassDef(JCClassDecl tree) {
        Symbol owner = env.info.scope.owner;
        Scope enclScope = enterScope(env);
        ClassSymbol c;
        if (owner.kind == PCK) {
            // We are seeing a toplevel class.
            PackageSymbol packge = (PackageSymbol)owner;
            for (Symbol q = packge; q != null && q.kind == PCK; q = q.owner)
                q.flags_field |= EXISTS;
            c = reader.enterClass(tree.name, packge);
            packge.members().enterIfAbsent(c);
            if ((tree.mods.flags & PUBLIC) != 0 && !classNameMatchesFileName(c, env)) {
                log.error(tree.pos(),
                          "class.public.should.be.in.file", tree.name);
            }
        } else {
            if (!tree.name.isEmpty() &&
                !chk.checkUniqueClassName(tree.pos(), tree.name, enclScope)) {
                result = null;
                return;
            }
            if (owner.kind == TYP) {
                // We are seeing a member class.
                c = reader.enterClass(tree.name, (TypeSymbol)owner);
                if ((owner.flags_field & INTERFACE) != 0) {
                    tree.mods.flags |= PUBLIC | STATIC;
                }
            } else {
                // We are seeing a local class.
                c = reader.defineClass(tree.name, owner);
                c.flatname = chk.localClassName(c);
                if (!c.name.isEmpty())
                    chk.checkTransparentClass(tree.pos(), c, env.info.scope);
            }
        }
        tree.sym = c;

        // Enter class into `compiled' table and enclosing scope.
        if (chk.compiled.get(c.flatname) != null) {
            duplicateClass(tree.pos(), c);
            result = types.createErrorType(tree.name, (TypeSymbol)owner, Type.noType);
            tree.sym = (ClassSymbol)result.tsym;
            return;
        }
        chk.compiled.put(c.flatname, c);
        enclScope.enter(c);

        // Set up an environment for class block and store in `typeEnvs'
        // table, to be retrieved later in memberEnter and attribution.
        Env<AttrContext> localEnv = classEnv(tree, env);
        typeEnvs.put(c, localEnv);

        // Fill out class fields.
        c.completer = memberEnter;
        c.flags_field = chk.checkFlags(tree.pos(), tree.mods.flags, c, tree);
        c.sourcefile = env.toplevel.sourcefile;
        c.members_field = new Scope(c);

        ClassType ct = (ClassType)c.type;
        if (owner.kind != PCK && (c.flags_field & STATIC) == 0) {
            // We are seeing a local or inner class.
            // Set outer_field of this class to closest enclosing class
            // which contains this class in a non-static context
            // (its "enclosing instance class"), provided such a class exists.
            Symbol owner1 = owner;
            while ((owner1.kind & (VAR | MTH)) != 0 &&
                   (owner1.flags_field & STATIC) == 0) {
                owner1 = owner1.owner;
            }
            if (owner1.kind == TYP) {
                ct.setEnclosingType(owner1.type);
            }
        }

        // Enter type parameters.
        ct.typarams_field = classEnter(tree.typarams, localEnv);

        // Add non-local class to uncompleted, to make sure it will be
        // completed later.
        if (!c.isLocal() && uncompleted != null) uncompleted.append(c);
//      System.err.println("entering " + c.fullname + " in " + c.owner);//DEBUG

        // Recursively enter all member classes.
        classEnter(tree.defs, localEnv);

        result = c.type;
    }
    //where
        /** Does class have the same name as the file it appears in?
         */
        private static boolean classNameMatchesFileName(ClassSymbol c,
                                                        Env<AttrContext> env) {
            return env.toplevel.sourcefile.isNameCompatible(c.name.toString(),
                                                            JavaFileObject.Kind.SOURCE);
        }

    /** Complain about a duplicate class. */
    protected void duplicateClass(DiagnosticPosition pos, ClassSymbol c) {
        log.error(pos, "duplicate.class", c.fullname);
    }

    /** Class enter visitor method for type parameters.
     *  Enter a symbol for type parameter in local scope, after checking that it
     *  is unique.
     */
    @Override
    public void visitTypeParameter(JCTypeParameter tree) {
        TypeVar a = (tree.type != null)
            ? (TypeVar)tree.type
            : new TypeVar(tree.name, env.info.scope.owner, syms.botType);
        tree.type = a;
        if (chk.checkUnique(tree.pos(), a.tsym, env.info.scope)) {
            env.info.scope.enter(a.tsym);
        }
        result = a;
    }

    /** Default class enter visitor method: do nothing.
     */
    @Override
    public void visitTree(JCTree tree) {
        result = null;
    }
    // Panini code
    private void setRuntimeImports(Env<AttrContext> env){
    	JCExpression pid = make.Ident(names.fromString("java"));
    	pid = make.Select(pid, names.fromString("util"));
    	pid = make.Select(pid, names.fromString("concurrent"));
    	pid = make.Select(pid, names.fromString("locks"));
    	pid = make.Select(pid, names.fromString("ReentrantLock"));
    	env.toplevel.defs = env.toplevel.defs.prepend(make.Import(pid, false));
    	pid = make.Ident(names.fromString("org"));
    	pid = make.Select(pid, names.fromString("paninij"));
    	pid = make.Select(pid, names.fromString("runtime"));
    	pid = make.Select(pid, names.asterisk);
    	env.toplevel.defs = env.toplevel.defs.prepend(make.Import(pid, false));
    	pid = make.Ident(names.fromString("org"));
    	pid = make.Select(pid, names.fromString("paninij"));
    	pid = make.Select(pid, names.fromString("runtime"));
    	pid = make.Select(pid, names.fromString("types"));
    	pid = make.Select(pid, names.fromString("Panini$Duck"));
    	env.toplevel.defs = env.toplevel.defs.prepend(make.Import(pid, false));
    	pid = make.Ident(names.fromString("org"));
    	pid = make.Select(pid, names.fromString("paninij"));
    	pid = make.Select(pid, names.fromString("runtime"));
    	pid = make.Select(pid, names.fromString("types"));
    	pid = make.Select(pid, names.fromString("Panini$Duck$Void"));
    	env.toplevel.defs = env.toplevel.defs.prepend(make.Import(pid, false));
    	pid = make.Ident(names.fromString("org"));
    	pid = make.Select(pid, names.fromString("paninij"));
    	pid = make.Select(pid, names.fromString("runtime"));
    	pid = make.Select(pid, names.fromString("types"));
    	pid = make.Select(pid, names.fromString("Panini$Duck$Array$Types"));
    	env.toplevel.defs = env.toplevel.defs.prepend(make.Import(pid, false));
    	pid = make.Ident(names.fromString("org"));
    	pid = make.Select(pid, names.fromString("paninij"));
    	pid = make.Select(pid, names.fromString("runtime"));
    	pid = make.Select(pid, names.fromString("types"));
    	pid = make.Select(pid, names.fromString("Panini$Duck$Final"));
    	env.toplevel.defs = env.toplevel.defs.prepend(make.Import(pid, false));
    }
    
    public void visitCapsuleDef(JCCapsuleDecl tree){
    	if((tree.mods.flags & Flags.INTERFACE) !=0){
    		tree.needsDefaultRun= false;
    	}
    	Symbol owner = env.info.scope.owner;
        Scope enclScope = enterScope(env);
        ClassSymbol c;
        if (owner.kind == PCK) {
            // We are seeing a toplevel class.
            PackageSymbol packge = (PackageSymbol)owner;
            for (Symbol q = packge; q != null && q.kind == PCK; q = q.owner)
                q.flags_field |= EXISTS;
            c = reader.enterCapsule(tree.name, packge);
            packge.members().enterIfAbsent(c);
            if ((tree.mods.flags & PUBLIC) != 0 && !classNameMatchesFileName(c, env)) {
                log.error(tree.pos(),
                          "class.public.should.be.in.file", tree.name);
            }
        } else {
            if (!tree.name.isEmpty() &&
                !chk.checkUniqueClassName(tree.pos(), tree.name, enclScope)) {
                result = null;
                return;
            }
            if (owner.kind == TYP) {
                // We are seeing a member class.
                c = reader.enterCapsule(tree.name, (TypeSymbol)owner);
                if ((owner.flags_field & INTERFACE) != 0) {
                    tree.mods.flags |= PUBLIC | STATIC;
                }
            } else {
                // We are seeing a local class.
                c = reader.defineCapsule(tree.name, owner);
                c.flatname = chk.localClassName(c);
                if (!c.name.isEmpty())
                    chk.checkTransparentClass(tree.pos(), c, env.info.scope);
            }
        }
        tree.sym = c;
        c.capsule_info.initMethods = tree.initMethods;
        c.capsule_info.stateToInit = tree.stateToInit;
        tree.sym.tree = tree;

        // Enter class into `compiled' table and enclosing scope.
        if (chk.compiled.get(c.flatname) != null) {
            duplicateClass(tree.pos(), c);
            result = types.createErrorType(tree.name, (TypeSymbol)owner, Type.noType);
            tree.sym = (ClassSymbol)result.tsym;
            return;
        }
        chk.compiled.put(c.flatname, c);
        enclScope.enter(c);

        // Set up an environment for class block and store in `typeEnvs'
        // table, to be retrieved later in memberEnter and attribution.
        Env<AttrContext> localEnv = classEnv(tree, env);
        typeEnvs.put(c, localEnv);

        // Fill out class fields.
        c.completer = memberEnter;
        c.flags_field = chk.checkFlags(tree.pos(), tree.mods.flags, c, tree);
        c.sourcefile = env.toplevel.sourcefile;
        c.members_field = new Scope(c);
        ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
        params.appendList(tree.params);
        c.capsule_info.capsuleParameters = params.toList();
        tree.sym = c;
        syms.capsules.put(c.name, c);

        ClassType ct = (ClassType)c.type;
        if (owner.kind != PCK && (c.flags_field & STATIC) == 0) {
            // We are seeing a local or inner class.
            // Set outer_field of this class to closest enclosing class
            // which contains this class in a non-static context
            // (its "enclosing instance class"), provided such a class exists.
            Symbol owner1 = owner;
            while ((owner1.kind & (VAR | MTH)) != 0 &&
                   (owner1.flags_field & STATIC) == 0) {
                owner1 = owner1.owner;
            }
            if (owner1.kind == TYP) {
                ct.setEnclosingType(owner1.type);
            }
        }

        // Enter type parameters.
        ct.typarams_field = classEnter(tree.typarams, localEnv);

        // Add non-local class to uncompleted, to make sure it will be
        // completed later.
        if (!c.isLocal() && uncompleted != null) uncompleted.append(c);
//      System.err.println("entering " + c.fullname + " in " + c.owner);//DEBUG
        
        if((tree.mods.flags & Flags.INTERFACE) ==0){
            //FIXME: OR IN THE CAPSULE FLAG
	        c.flags_field = processCapsuleAnnotations(tree, c);
	        ListBuffer<JCTree> definitions = new ListBuffer<JCTree>();
			if ((c.flags_field & SERIAL) != 0) {
				definitions = translateSerialCapsule(tree, c, localEnv, false, false);
				tree.extending = make.Ident(names
						.fromString(PaniniConstants.PANINI_CAPSULE_SEQUENTIAL));
				tree.parentCapsule.sym.capsule_info.translated_serial = c;
			} else if ((c.flags_field & TASK) != 0) {
				definitions = translateConcurrentCapsule(tree, c, localEnv,
						true);
				tree.extending = make.Ident(names
						.fromString(PaniniConstants.PANINI_CAPSULE_TASK));
				tree.parentCapsule.sym.capsule_info.translated_task = c;
			} else if ((c.flags_field & MONITOR) != 0) {
				definitions = translateSerialCapsule(tree, c, localEnv, true, false);
				tree.extending = make.Ident(names
						.fromString(PaniniConstants.PANINI_CAPSULE_SEQUENTIAL));
				tree.parentCapsule.sym.capsule_info.translated_monitor = c;
			} else { // default action
				definitions = translateConcurrentCapsule(tree, c, localEnv,
						false);
				tree.extending = make.Ident(names
						.fromString(PaniniConstants.PANINI_CAPSULE_THREAD));
				tree.parentCapsule.sym.capsule_info.translated_thread = c;
			}

	        c.capsule_info.parentCapsule = tree.parentCapsule.sym;
	    	List<JCVariableDecl> fields = tree.getParameters();
	    	while(fields.nonEmpty()){
	    		definitions.prepend(make.VarDef(make.Modifiers(PUBLIC),
	    				fields.head.name,
	    				fields.head.vartype,
	    				null));
	    		fields = fields.tail;
	    	}
	    	definitions.add(make.MethodDef(make.Modifiers(PUBLIC), names.init, null, 
	        		List.<JCTypeParameter>nil(), 
	        		List.<JCVariableDecl>nil(), 
	                List.<JCExpression>nil(),
	                make.Block(0, List.<JCStatement>nil()),
	                null));
	    	tree.defs = definitions.toList();
        }else {
        	if(tree.implementing.isEmpty()){//signatures
        		tree.defs = translateSerialCapsule(tree, c, localEnv, false, true).toList();
        	}
        	List<JCVariableDecl> fields = tree.getParameters();
	    	while(fields.nonEmpty()){
	    		tree.defs = tree.defs.prepend(make.VarDef(make.Modifiers(PUBLIC),
	    				fields.head.name,
	    				fields.head.vartype,
	    				null));
	    		fields = fields.tail;
	    	}
        	c.capsule_info.definedRun = true;
        }

        classEnter(tree.defs, localEnv);
        result = c.type;
        annotationProcessor.setDefinedRun(tree, c.capsule_info.definedRun);
        tree.switchToClass();
    }
    
    public List<JCStatement> push(Name n){
    	ListBuffer<JCStatement> stats = new ListBuffer<JCStatement>();
    	stats.add(make.Exec(make.Assign(make.Indexed(make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_OBJECTS)), 
    			make.Unary(POSTINC, 
    					make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_TAIL)))), 
    					make.Ident(n))));
    	stats.add(make.If(make.Binary(GE, make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_TAIL)), 
    			make.Select(make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_OBJECTS)), 
    					names.fromString("length"))), 
    			make.Exec(make.Assign(
    					make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_TAIL)), 
    					make.Literal(0))), 
    			null));
    	return stats.toList();
    }
    
	private ListBuffer<JCTree> translateSerialCapsule(JCCapsuleDecl tree,
			ClassSymbol c, Env<AttrContext> localEnv, boolean isMonitor, boolean isSignature) {
		ListBuffer<JCTree> definitions = new ListBuffer<JCTree>();
		if(isSignature){
			JCMethodDecl push = make.MethodDef(make.Modifiers(PUBLIC), 
					names.fromString(PaniniConstants.PANINI_PUSH), 
					make.TypeIdent(TypeTags.VOID), List.<JCTypeParameter>nil(), 
					List.<JCVariableDecl>of(make.VarDef(make.Modifiers(0), names.fromString("o"),
							make.Ident(names.fromString("Object")), null))
							, List.<JCExpression>nil(), null, null);
			definitions.add(push);
		}
		for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
			JCTree def = l.head;
			if (def.getTag() == Tag.METHODDEF) {
				JCMethodDecl mdecl = (JCMethodDecl) def;
				if (mdecl.name.toString().equals("run")
						&& mdecl.params.isEmpty()) {
					log.error(tree.pos(), "serialize.active.capsules");
				} else if(mdecl.name.equals(names.panini.InternalCapsuleWiring)) {
					//Don't change capsule wiring. It needs to be handled
					//as a special case
					definitions.add(mdecl);
				} else if ((mdecl.mods.flags & PRIVATE) == 0
						&& (mdecl.mods.flags & PROTECTED) == 0) {
					long methodFlags = (PUBLIC | FINAL);
					if(isSignature){
						methodFlags = (PUBLIC);
					}
					JCProcDecl p = make.ProcDef(make.Modifiers(methodFlags),
							mdecl.name, mdecl.restype, mdecl.typarams,
							mdecl.params, mdecl.thrown, mdecl.body, null);
					if (isMonitor)
						p.mods.flags |= Flags.SYNCHRONIZED;
					p.switchToMethod();
					tree.publicMethods = tree.publicMethods.append(p);
					TreeCopier<Void> tc = new TreeCopier<Void>(make);
					JCMethodDecl methodCopy = make.MethodDef(
							make.Modifiers(methodFlags),
							mdecl.name.append(names.fromString("$Original")),
							tc.copy(mdecl.restype), tc.copy(mdecl.typarams),
							tc.copy(mdecl.params), tc.copy(mdecl.thrown),
							tc.copy(mdecl.body), null);
					methodCopy.sym = new MethodSymbol(PUBLIC, methodCopy.name,
							mdecl.restype.type, tree.sym);
					definitions.add(methodCopy);
					definitions.add(p);
				} else
					definitions.add(mdecl);
			} else if (def.getTag() == VARDEF) {
				JCVariableDecl vdecl = (JCVariableDecl) def;
				if (vdecl.mods.flags != 0)
					log.error(vdecl.pos(), "illegal.state.modifiers");
				vdecl.mods.flags |= PRIVATE;
				JCStateDecl state = make.at(vdecl.pos).StateDef(
						make.Modifiers(PRIVATE), vdecl.name, vdecl.vartype,
						vdecl.init);

				definitions.add(state);
			} else
				definitions.add(def);
		}
		c.capsule_info.definedRun = false;
		tree.needsDefaultRun = false;
		return definitions;
	}
    
	private ListBuffer<JCTree> translateConcurrentCapsule(JCCapsuleDecl tree,
			ClassSymbol c, Env<AttrContext> localEnv, boolean isTask) {
		int indexer = 0;
		boolean hasRun = false;
		ListBuffer<JCTree> definitions = new ListBuffer<JCTree>();
		TreeCopier<Void> tc = new TreeCopier<Void>(make);
		for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
			JCTree def = l.head;
			if (def.getTag() == Tag.METHODDEF) {
				JCMethodDecl mdecl = (JCMethodDecl) def;
				if (mdecl.name.equals(names.panini.Run)
						&& mdecl.params.isEmpty()) {
					if (isTask)
						log.error(tree.pos(), "serialize.active.capsules");
					else {
						MethodSymbol msym = new MethodSymbol(PUBLIC | FINAL,
								names.panini.Run, new MethodType(
										List.<Type> nil(), syms.voidType,
										List.<Type> nil(), syms.methodClass),
								tree.sym);
						JCMethodDecl computeDecl = make.MethodDef(msym,
								mdecl.body);
						computeDecl.params = List.<JCVariableDecl> nil();
						memberEnter.memberEnter(computeDecl, localEnv);
						definitions.add(computeDecl);
						tree.computeMethod = computeDecl;
						hasRun = true;
					}
				} else if(mdecl.name.equals(names.panini.InternalCapsuleWiring)) {
					//Don't change capsule wiring. It needs to be handled
					//as a special case
					definitions.add(mdecl);
				} else if ((mdecl.mods.flags & PRIVATE) == 0
						&& (mdecl.mods.flags & PROTECTED) == 0) {
					String constantName = PaniniConstants.PANINI_METHOD_CONST
							+ mdecl.name;
					if (mdecl.params.nonEmpty())
						for (List<JCVariableDecl> p = mdecl.params; p.nonEmpty(); p = p.tail){
							JCVariableDecl param = p.head;
							constantName = constantName + "$"
									+ param.vartype.toString();
						}
					mdecl.mods.flags |= PUBLIC;
					JCVariableDecl v = make.VarDef(
							make.Modifiers(PUBLIC | STATIC | FINAL),
							names.fromString(constantName),
							make.TypeIdent(TypeTags.INT),
							make.Literal(indexer++));
					JCProcDecl p = make.ProcDef(make.Modifiers(PUBLIC),
							mdecl.name, tc.copy(mdecl.restype),
							tc.copy(mdecl.typarams), tc.copy(mdecl.params),
							tc.copy(mdecl.thrown), tc.copy(mdecl.body), null);
					p.mods.annotations = tc.copy(mdecl.mods.annotations);
					p.switchToMethod();
					definitions.prepend(v);
					tree.publicMethods = tree.publicMethods.append(p);
				} else
					definitions.add(tc.copy(mdecl));
			} else if (def.getTag() == VARDEF) {
				JCVariableDecl vdecl = (JCVariableDecl) def;
				if (vdecl.mods.flags != 0)
					log.error(vdecl.pos(), "illegal.state.modifiers");
				vdecl.mods.flags |= PRIVATE;
				JCStateDecl state = make.at(vdecl.pos).StateDef(
						make.Modifiers(PRIVATE), vdecl.name,
						tc.copy(vdecl.vartype), tc.copy(vdecl.init));

				definitions.add(state);
			} else
				definitions.add(tc.copy(def));
		}
		if (!hasRun) {
			definitions.appendList(copyAndAlternatePublicMethods(tree, c));
			JCMethodDecl m = runMethodPlaceholder(tree, isTask);
			memberEnter.memberEnter(m, localEnv);
			definitions.add(m);
			hasRun = true;
			tree.needsDefaultRun = true;
			tree.computeMethod = m;
			tree.hasSynthRunMethod = true;
		} else {
			definitions.appendList(copyPublicMethods(tree, c));
			c.capsule_info.definedRun = true;
		}
		return definitions;
	}
    
	private ListBuffer<JCTree> copyPublicMethods(final JCCapsuleDecl tree,
			final ClassSymbol c) {
		ListBuffer<JCTree> definitions = new ListBuffer<JCTree>();
		TreeCopier<Void> tc = new TreeCopier<Void>(make);
		for (List<JCMethodDecl> l = tree.publicMethods; l.nonEmpty(); l = l.tail) {
			JCMethodDecl mdecl = l.head;
			ListBuffer<JCVariableDecl> vars = new ListBuffer<JCVariableDecl>();
			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
			args.add(make.Ident(names
					.fromString(PaniniConstants.PANINI_METHOD_CONST
							+ mdecl.name)));
			for (List<JCVariableDecl> vl = mdecl.params; vl.nonEmpty(); vl = vl.tail) {
				JCVariableDecl v = vl.head;
				vars.add(make.VarDef(v.mods, v.name, v.vartype, null));
				args.append(make.Ident(v.name));
			}

			JCMethodDecl methodCopy = make.MethodDef(
					make.Modifiers(PUBLIC | FINAL),
					mdecl.name.append(names.fromString("$Original")),
					tc.copy(mdecl.restype), tc.copy(mdecl.typarams),
					tc.copy(vars.toList()), tc.copy(mdecl.thrown),
					tc.copy(mdecl.body), null);
			methodCopy.sym = new MethodSymbol(PUBLIC, methodCopy.name,
					mdecl.restype.type, tree.sym);
			definitions.add(methodCopy);
			definitions.add(mdecl);
		}
		return definitions;
	}

	private ListBuffer<JCTree> copyAndAlternatePublicMethods(
			final JCCapsuleDecl tree, final ClassSymbol c) {
		ListBuffer<JCTree> definitions = new ListBuffer<JCTree>();
		TreeCopier<Void> tc = new TreeCopier<Void>(make);
		for (List<JCMethodDecl> l = tree.publicMethods; l.nonEmpty(); l = l.tail) {
			JCMethodDecl mdecl = l.head;
			String constantName = PaniniConstants.PANINI_METHOD_CONST
					+ mdecl.name;
			if (mdecl.params.nonEmpty())
				for (JCVariableDecl param : mdecl.params) {
					constantName = constantName + "$" + param.vartype;
				}
			c.capsule_info.definedRun = false;
			ListBuffer<JCStatement> copyBody = new ListBuffer<JCStatement>();
			copyBody.append(make.Exec(make.Apply(List.<JCExpression> nil(),
					make.Ident(names.fromString(PaniniConstants.PANINI_PUSH)),
					List.<JCExpression> of(make
							.Ident(names.panini.PaniniDuckFuture)))));
			ListBuffer<JCVariableDecl> vars = new ListBuffer<JCVariableDecl>();
			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
			args.add(make.Ident(names.fromString(constantName)));
			for (List<JCVariableDecl> vl = mdecl.params; vl.nonEmpty(); vl = vl.tail) {
				JCVariableDecl v = vl.head;
				vars.add(make.VarDef(tc.copy(v.mods), v.name,
						tc.copy(v.vartype), null));
				args.append(make.Ident(v.name));
			}
			JCExpression duckType = getDuckType(tree, mdecl);
			copyBody.prepend(make.Try(
					make.Block(0, List.<JCStatement> of(make.Exec(make.Assign(
							make.Ident(names.panini.PaniniDuckFuture), make
									.NewClass(null, List.<JCExpression> nil(),
											duckType, args.toList(), null))))),
					List.<JCCatch> of(make.Catch(
							make.VarDef(make.Modifiers(0),
									names.fromString("e"),
									make.Ident(names.fromString("Exception")),
									null),
							make.Block(
									0,
									List.<JCStatement> of(make.Throw(make.NewClass(
											null,
											List.<JCExpression> nil(),
											make.Ident(names
													.fromString("DuckException")),
											List.<JCExpression> of(make
													.Ident(names
															.fromString("e"))),
											null)))))), null));
			copyBody.prepend(make.VarDef(make.Modifiers(0),
					names.panini.PaniniDuckFuture, duckType,
					make.Literal(TypeTags.BOT, null)));
			if (!mdecl.restype.toString().equals("void"))
				copyBody.append(procedureReturnStatement(mdecl));

			JCMethodDecl methodCopy = make.MethodDef(
					make.Modifiers(PUBLIC | FINAL),
					mdecl.name.append(names.fromString("$Original")),
					tc.copy(mdecl.restype), tc.copy(mdecl.typarams),
					vars.toList(), tc.copy(mdecl.thrown), tc.copy(mdecl.body),
					null);
			methodCopy.sym = new MethodSymbol(PUBLIC, methodCopy.name,
					mdecl.restype.type, tree.sym);
			mdecl.mods.flags |= FINAL;
			mdecl.body = make.Block(0, copyBody.toList());
			definitions.add(methodCopy);
			definitions.add(mdecl);
		}
		return definitions;
	}
	
	private JCMethodDecl runMethodPlaceholder(final JCCapsuleDecl tree,
			final boolean isTask) {
		MethodSymbol msym;
		if (isTask)
			msym = new MethodSymbol(PUBLIC | FINAL, names.fromString("run"),
					new MethodType(List.<Type> nil(), syms.booleanType,
							List.<Type> nil(), syms.methodClass), tree.sym);
		else
			msym = new MethodSymbol(PUBLIC | FINAL, names.fromString("run"),
					new MethodType(List.<Type> nil(), syms.voidType,
							List.<Type> nil(), syms.methodClass), tree.sym);
		JCMethodDecl m = make.MethodDef(msym,
				make.Block(0, List.<JCStatement> nil()));
		m.mods = make.Modifiers(
				PUBLIC | FINAL,
				List.<JCAnnotation> of(make.Annotation(
						make.Ident(names.fromString("SuppressWarnings")),
						List.<JCExpression> of(make.Literal("unchecked")))));
		m.params = List.<JCVariableDecl> nil();
		m.sym = msym;
		return m;
	}
    
    public long processCapsuleAnnotations(JCCapsuleDecl tree, ClassSymbol c){
    	for (List<JCAnnotation> l = tree.mods.annotations; l.nonEmpty(); l = l.tail){
    		JCAnnotation annotation = l.head;
    		if(annotation.annotationType.toString().equals("CapsuleKind")){
    			Object arg = "";
    			if(annotation.args.isEmpty())
    				log.error(tree.pos(), "annotation.missing.default.value", annotation, "value");
    			else if (annotation.args.size()!=1||annotation.args.head.getTag()!=ASSIGN){
    				if(annotation.args.head.getTag()==LITERAL)
    					arg = ((JCLiteral)annotation.args.head).value;
    				else
    					log.error(tree.pos(), "annotation.value.must.be.name.value");
    			}
    			return getCapsuleKind(arg, c, annotation);
    		}
    	}
    	return c.flags_field;
    }
    
    public long getCapsuleKind(Object kind, ClassSymbol c, JCAnnotation annotation){
		if (kind.equals("SERIAL"))
			return c.flags_field |= SERIAL;
		else if (kind.equals("ACTIVE"))
			return c.flags_field |= ACTIVE;
		else if (kind.equals("TASK"))
			return c.flags_field |= TASK;
		else if (kind.equals("MONITOR"))
			return c.flags_field |= MONITOR;
		else
			log.error(annotation.pos(), "annotation.value.not.allowable.type");
		return c.flags_field;
    }
    
    public JCStatement procedureReturnStatement(final JCMethodDecl mdecl){
		String returnType = mdecl.restype.toString();
		JCStatement returnStat;
		Name duckFuture = names.panini.PaniniDuckFuture;
		if (returnType.equals("long")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(duckFuture),
							names.fromString("longValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("boolean")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(duckFuture),
							names.fromString("booleanValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("byte")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(duckFuture),
							names.fromString("byteValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("char")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(duckFuture),
							names.fromString("charValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("double")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(duckFuture),
							names.fromString("doubleValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("float")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(duckFuture),
							names.fromString("floatValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("int")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(duckFuture),
							names.fromString("intValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("short")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(duckFuture),
							names.fromString("shortValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("String")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(duckFuture),
							names.fromString("toString")), List
					.<JCExpression> nil()));
		} else if (returnType.contains("[") && returnType.contains("]")) {
			returnStat = make.Return(make.TypeCast(mdecl.restype, make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(duckFuture),
							names.fromString("arrayValue")), List
					.<JCExpression> nil())));
		} else {
			returnStat = make.Return(make.Ident(duckFuture));
		}
		return returnStat;
    }
    
    public JCExpression getDuckType(final JCCapsuleDecl tree, final JCMethodDecl mdecl){
    	JCExpression duck;
    	String restype = mdecl.restype.toString();
    	if(restype.contains("[")&&restype.contains("]"))
    		duck = make.Ident(names
					.fromString(PaniniConstants.DUCK_INTERFACE_NAME + "$"
							+ PaniniConstants.ARRAY_DUCKS + "$"
							+ tree.name));
    	else duck = make.Ident(names
					.fromString(PaniniConstants.DUCK_INTERFACE_NAME + "$"
							+ restype + "$"
							+ tree.name));
		return duck;
    }
    
    private void translateCapsuleAnnotations(){
    	Set<Map.Entry<Name, ClassSymbol>> classSymbols = new HashSet<Map.Entry<Name, ClassSymbol>>(syms.classes.entrySet());
    	
    	for(Map.Entry<Name, ClassSymbol> entry : classSymbols){
    		ClassSymbol classSymbol = entry.getValue();
    		if(classSymbol.classfile!=null)
    			if(classSymbol.classfile.getKind()== JavaFileObject.Kind.CLASS){
    				classSymbol.complete();
    			}
    		if(classSymbol.attributes_field.size()!=0){
    			for(Attribute.Compound compound : classSymbol.attributes_field){
    				if(compound.type.tsym.getQualifiedName().toString().contains("PaniniCapsuleDecl")){
	 					//ClassSymbol capsuleSymbol;
	 					if((classSymbol.flags_field & Flags.CAPSULE) == 0){
	 					    CapsuleExtras.asCapsuleSymbol(classSymbol);
	 					}
	 					annotationProcessor.translateCapsuleAnnotations(classSymbol, compound);
	 					syms.capsules.put(classSymbol.name, classSymbol);
    				}
    			}
    		}
    	}
    }
    
    private void fillInCapsuleSymbolRest(){
    	for(ClassSymbol capsule : syms.capsules.values()){
    		ClassSymbol c = syms.capsules.get(names.fromString(capsule+"$thread"));
    		if(c!=null){
    			capsule.capsule_info.translated_thread = c;
    			c.capsule_info.parentCapsule = capsule;
    		}
    		c = syms.capsules.get(names.fromString(capsule+"$task"));
    		if(c!=null){
    			capsule.capsule_info.translated_task = c;
    			c.capsule_info.parentCapsule = capsule;
    		}
    		c = syms.capsules.get(names.fromString(capsule+"$monitor"));
    		if(c!=null){
    			capsule.capsule_info.translated_monitor = c;
    			c.capsule_info.parentCapsule = capsule;
    		}
    		c = syms.capsules.get(names.fromString(capsule+"$serial"));
    		if(c!=null){
    			capsule.capsule_info.translated_serial = c;
    			c.capsule_info.parentCapsule = capsule;
    		}
    	}
    }
    
    // end Panini code
    /** Main method: enter all classes in a list of toplevel trees.
     *  @param trees      The list of trees to be processed.
     */
    public void main(List<JCCompilationUnit> trees) {
        complete(trees, null);
        
        // Panini code
        translateCapsuleAnnotations();
        fillInCapsuleSymbolRest();
        // end Panini code
    }

    /** Main method: enter one class from a list of toplevel trees and
     *  place the rest on uncompleted for later processing.
     *  @param trees      The list of trees to be processed.
     *  @param c          The class symbol to be processed.
     */
    public void complete(List<JCCompilationUnit> trees, ClassSymbol c) {
        annotate.enterStart();
        ListBuffer<ClassSymbol> prevUncompleted = uncompleted;
        if (memberEnter.completionEnabled) uncompleted = new ListBuffer<ClassSymbol>();

        try {
            // enter all classes, and construct uncompleted list
            classEnter(trees, null);

            // complete all uncompleted classes in memberEnter
            if  (memberEnter.completionEnabled) {
                while (uncompleted.nonEmpty()) {
                    ClassSymbol clazz = uncompleted.next();
                    if (c == null || c == clazz || prevUncompleted == null)
                        clazz.complete();
                    else
                        // defer
                        prevUncompleted.append(clazz);
                }

                // if there remain any unimported toplevels (these must have
                // no classes at all), process their import statements as well.
                for (JCCompilationUnit tree : trees) {
                    if (tree.starImportScope.elems == null) {
                        JavaFileObject prev = log.useSource(tree.sourcefile);
                        Env<AttrContext> topEnv = topLevelEnv(tree);
                        memberEnter.memberEnter(tree, topEnv);
                        log.useSource(prev);
                    }
                }
            }
        } finally {
            uncompleted = prevUncompleted;
            annotate.enterDone();
        }
    }
}
