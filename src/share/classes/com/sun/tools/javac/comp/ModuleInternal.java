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
 * http://www.paninij.org
 *
 * Contributor(s): Rex Fernando
 */

package com.sun.tools.javac.comp;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModuleDecl;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.PaniniConstants;

import javax.lang.model.element.ElementKind;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.tree.JCTree.Tag.GE;
import static com.sun.tools.javac.tree.JCTree.Tag.POSTINC;

public class ModuleInternal extends Internal {
	ListBuffer<JCTree> contractDefs;
	Symtab syms;
	Enter enter;
	MemberEnter memberEnter;

	public ModuleInternal(TreeMaker make, Names names, Enter enter,
			MemberEnter memberEnter, Symtab syms) {
		super(make, names);
		this.enter = enter;
		this.syms = syms;
		this.memberEnter = memberEnter;
		specCounter = 0;
		contractDefs = new ListBuffer<JCTree>();
	}

	public JCBlock generateComputeMethodBody(JCModuleDecl tree) {
		JCModifiers noMods = mods(0);
		ListBuffer<JCStatement> messageLoopBody = new ListBuffer<JCStatement>();
		messageLoopBody.append(var(noMods, "d", PaniniConstants.DUCK_INTERFACE_NAME,
				apply(PaniniConstants.PANINI_GET_NEXT_DUCK)));

		ListBuffer<JCCase> cases = new ListBuffer<JCCase>();
		int varIndex = 0;

		for (JCMethodDecl method : tree.publicMethods) {
			ListBuffer<JCStatement> caseStatements = new ListBuffer<JCStatement>();
			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
			for (int i = 0; i < method.params.size(); i++) {
				JCExpression varType = method.params.get(i).vartype;
				caseStatements.append(var(
						noMods,
						"var" + varIndex,
						varType,
						cast(varType,
								make.Select(
										cast(
												PaniniConstants.DUCK_INTERFACE_NAME + "$"
														+ method.restype.toString() + "$" + tree.name.toString(),
												id("d")), method.params.get(i).name.append(names.fromString("$"))
												.append(method.name)))));
				args.append(id("var" + varIndex++));
			}

			Type returnType = ((MethodType) method.sym.type).restype;
			if (returnType.tag == TypeTags.VOID) {
				caseStatements.append(es(createOriginalCall(method, args)));
				caseStatements.append(es(apply("d", "panini$finish", args(nullv()))));
			} else if (returnType.tag == TypeTags.CLASS) {
				caseStatements.append(es(apply("d", "panini$finish",
						args(createOriginalCall(method, args)))));
			} else {
				System.out.println("Unsupported return type in a public module method. Can only be void or non-primitive.");
				System.exit(5555);
			}
			caseStatements.append(break_());
			cases.append(case_(
					id(PaniniConstants.PANINI_METHOD_CONST + method.name.toString()),
					caseStatements));
		}

		ListBuffer<JCStatement> shutDownBody = createShutdownLogic();
		cases.append(case_(intlit(-1), shutDownBody));

		ListBuffer<JCStatement> exitBody = createTerminationLogic();
		cases.append(case_(intlit(-2), exitBody));

		messageLoopBody.append(swtch(apply("d", PaniniConstants.PANINI_MESSAGE_ID), cases));
		
		JCBlock b = body(
				var(mods(0), PaniniConstants.PANINI_TERMINATE,
						make.TypeIdent(TypeTags.BOOLEAN), falsev()),
				whilel(nott(id(PaniniConstants.PANINI_TERMINATE)), body(messageLoopBody)));
		return b;
	}

	private JCMethodInvocation createOriginalCall (final JCMethodDecl method, final ListBuffer<JCExpression> args) {
		return apply(thist(), method.name.toString()	+ "$Original", args); 
	}
	
	private ListBuffer<JCStatement> createShutdownLogic() {
		ListBuffer<JCStatement> shutDownBody = new ListBuffer<JCStatement>();
		shutDownBody.append(ifs(
				gt(select(thist(), "size"), intlit(0)),
				body(
						es(make.Apply(List.<JCExpression> nil(), id("push"),
								List.<JCExpression> of(id("d")))), break_())));
		return shutDownBody;
	}

	private ListBuffer<JCStatement> createTerminationLogic() {
		ListBuffer<JCStatement> exitBody = new ListBuffer<JCStatement>();
		exitBody.append(es(assign(PaniniConstants.PANINI_TERMINATE, truev())));
		exitBody.append(break_());
		return exitBody;
	}

	public List<JCClassDecl> generateClassWrappers(JCModuleDecl tree,
			Env<AttrContext> env, Resolve rs) {
		ListBuffer<JCClassDecl> classes = new ListBuffer<JCClassDecl>();
		Map<String, JCClassDecl> addedHere = new HashMap<String, JCClassDecl>();

		for (JCMethodDecl method : tree.publicMethods) {
			Type restype = ((MethodType) method.sym.type).restype;
			ListBuffer<JCTree> constructors = new ListBuffer<JCTree>();

			ClassSymbol c;
			if (restype.toString().equals("void"))
				c = (ClassSymbol) rs.findIdent(env,
						names.fromString(PaniniConstants.DUCK_INTERFACE_NAME + "$Void"), TYP);
			else if (restype.isPrimitive()) {
				System.out
						.println("\n\nNon-void primitive return types for module procedure calls are not yet supported.\n\n");
				System.exit(10);
				c = null;
			} else
				c = (ClassSymbol) restype.tsym;

			if (c.type.isFinal()) {
				System.out
						.println("\n\nFinal classes as return types for module procedure calls are not supported.\n\n");
				System.exit(10);

			}

			Iterator<Symbol> iter = c.members().getElements().iterator();
			if (restype.tag == TypeTags.CLASS) {
				if (!addedHere.containsKey(restype.toString())) {
					JCVariableDecl var = var(mods(PRIVATE), 
    						"wrapped", make.TypeApply(id(restype.tsym.toString()), List.<JCExpression>of(id("OWNER"))), nullv());
					JCVariableDecl var2 = var(mods(PRIVATE | FINAL), "messageId",
							make.TypeIdent(TypeTags.INT), null);

					// to implement the flag logic.
					// private boolean redeemed = false;
					JCVariableDecl varRedeemed = var(mods(PRIVATE), PaniniConstants.REDEEMED,
							make.TypeIdent(TypeTags.BOOLEAN), make.Literal(TypeTags.BOOLEAN, 0));

					ListBuffer<JCTree> wrappedMethods = new ListBuffer<JCTree>();
					boolean addedConstructors = false;
					ListBuffer<JCExpression> inits = new ListBuffer<JCExpression>();
					while (iter.hasNext()) {
						Symbol s = iter.next();
						if (s.getKind() == ElementKind.METHOD) {
							MethodSymbol m = (MethodSymbol) s;
							if (!m.type.getReturnType().toString().equals("void")) {
								List<JCCatch> catchers = List.<JCCatch> of(make.Catch(
										make.VarDef(make.Modifiers(0), names.fromString("e"),
												make.Ident(names.fromString("InterruptedException")), null),
										make.Block(
												0,
												List.<JCStatement> of(make.Return(make.Apply(null,
														make.Ident(m.name), List.<JCExpression> nil()))))));
								JCMethodDecl value = method(
										mods(PUBLIC),
										m.name,
										make.Type(m.type.getReturnType()),
										body(make.Try(body(sync(make.This(Type.noType), body(whilel(
										// Test whether the duck is ready.
										// while (redeemed == false) wait();
												isFalse(PaniniConstants.REDEEMED), es(apply("wait")))))), catchers,
												null), returnt(apply("wrapped", m.name))));
								wrappedMethods.add(value);
							} else {
								List<JCCatch> catchers = List.<JCCatch> of(make.Catch(
										make.VarDef(make.Modifiers(0), names.fromString("e"),
												make.Ident(names.fromString("InterruptedException")), null),
										make.Block(0, List.<JCStatement> of(make.Exec(make.Apply(null,
												make.Ident(m.name), List.<JCExpression> nil()))))));
								JCMethodDecl value = method(
										mods(PUBLIC | FINAL),
										m.name,
										make.Type(syms.voidType),
										body(make.Try(body(sync(make.This(Type.noType), body(whilel(
										// Test whether the duck is ready.
										// while (redeemed == false) wait();
												isFalse(PaniniConstants.REDEEMED), es(apply("wait")))))), catchers,
												null), es(apply("wrapped", m.name))));
								wrappedMethods.add(value);
							}
						} else if (s.getKind() == ElementKind.CONSTRUCTOR) {
							MethodSymbol m = (MethodSymbol) s;

							ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
							params.add(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT)));

							for (VarSymbol v : m.params()) {
								if (v.type.toString().equals("boolean"))
									inits.add(falsev());
								else if (v.type.isPrimitive()) {
									inits.add(intlit(0));
								} else
									inits.add(nullv());
							}
							if (!addedConstructors) {
								constructors.add(constructor(
										mods(PUBLIC),
										params(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT))),
										body(es(make.Apply(List.<JCExpression> nil(),
												make.Ident(names._super), inits.toList())),
												es(assign(select(thist(), "messageId"), id("messageId"))))));
								addedConstructors = true;
							}
						}
					}
					ListBuffer<JCVariableDecl> finishParams = new ListBuffer<JCVariableDecl>();
					finishParams.add(var(mods(0),"t",make.TypeApply(id(restype.tsym.toString()), List.<JCExpression>of(id("OWNER")))));

					JCMethodDecl id = method(mods(PUBLIC | FINAL),
							names.fromString(PaniniConstants.PANINI_MESSAGE_ID),
							make.TypeIdent(TypeTags.INT),
							body(returnt(select(thist(), "messageId"))));

					JCMethodDecl finish = method(
							mods(PUBLIC | FINAL),
							names.fromString(PaniniConstants.PANINI_FINISH),
							make.Type(syms.voidType),
							finishParams,
							body(sync(
									thist(),
									// The duck is ready.
									body(es(assign("wrapped", id("t"))), es(assign("redeemed", truev())),
											es(apply("notifyAll"))))));
					JCExpression extending;
					List<JCExpression> implement;
					if (restype.isInterface()) {
						extending = null;
						implement = implementing(
								ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
										args(id(restype.toString()))), id(restype.toString())).toList();
					} else {
						if (restype.toString().equals("void")) {
							extending = id(PaniniConstants.DUCK_INTERFACE_NAME + "$Void");
							implement = implementing(
									ta(id(PaniniConstants.DUCK_INTERFACE_NAME), args(id("Void"))))
									.toList();
						} else {
							extending = make.TypeApply(id(restype.tsym.toString()), List.<JCExpression>of(id("OWNER")));
    						implement = implementing(ta(id(PaniniConstants.DUCK_INTERFACE_NAME), 
    								args(make.TypeApply(id(restype.tsym.toString()), 
    										List.<JCExpression>of(id("OWNER")))))).toList();
						}
					}

					ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();

					if (!method.params.isEmpty()) {
						ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
						ListBuffer<JCVariableDecl> consParams = new ListBuffer<JCVariableDecl>();
						if (addedConstructors) {
							consBody.add(es(make.Apply(List.<JCExpression> nil(), id(names._super),
									inits.toList())));
						}
						consParams.add(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT)));
						consBody.add(es(assign(select(thist(), "messageId"), id("messageId"))));

						for (JCVariableDecl par : method.params) {
							consParams.add(var(mods(0), par.name, par.vartype));
							consBody.add(es(assign(
									select(thist(),
											par.name.append(names.fromString("$")).append(method.name)
													.toString()), id(par.name))));
							variableFields.add(var(mods(PUBLIC),
									par.name.append(names.fromString("$")).append(method.name),
									par.vartype));
						}
						constructors.add(constructor(mods(PUBLIC), consParams, body(consBody)));
					}

					JCClassDecl wrappedClass = make.ClassDef(mods(0), 
    						names.fromString(PaniniConstants.DUCK_INTERFACE_NAME  + "$" + restype.tsym.toString() + "$" + tree.name.toString()), 
    						List.<JCTypeParameter>of(make.TypeParameter(names.fromString("OWNER"), List.<JCExpression>nil())), 
    						extending, implement, 
    						defs(var, var2, finish, id).appendList(variableFields).appendList(constructors).appendList(wrappedMethods).toList());

					classes.add(wrappedClass);
					addedHere.put(restype.toString(), wrappedClass);
				} else {
					if (!method.params.isEmpty()) {
						ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();
						JCClassDecl wrappedClass = addedHere.get(restype.toString());
						if (!hasDuplicate(wrappedClass, method.params, method.name)) {
							ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
							ListBuffer<JCVariableDecl> consParams = new ListBuffer<JCVariableDecl>();

							boolean hasConstructor = false;
							ListBuffer<JCExpression> inits = new ListBuffer<JCExpression>();
							while (iter.hasNext()) {
								Symbol s = iter.next();
								if (s.getKind() == ElementKind.CONSTRUCTOR) {
									MethodSymbol m = (MethodSymbol) s;
									for (VarSymbol v : m.params()) {
										if (v.type.toString().equals("boolean"))
											inits.add(falsev());
										else if (v.type.isPrimitive()) {
											inits.add(intlit(0));
										}
									}
									hasConstructor = true;
									break;
								}
							}
							if (hasConstructor)
								consBody.add(es(make.Apply(List.<JCExpression> nil(), id(names._super),
										inits.toList())));

							consParams.add(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT)));
							consBody.add(es(assign(select(thist(), "messageId"), id("messageId"))));

							for (JCVariableDecl par : method.params) {
								consParams.add(var(mods(0), par.name, par.vartype));
								consBody.add(es(assign(
										select(thist(),
												par.name.append(names.fromString("$")).append(method.name)
														.toString()), id(par.name))));
							}
							// for(JCTree def : wrappedClass.defs){
							// if(def.getTag()==Tag.METHODDEF&&((JCMethodDecl)def).name.equals(names.init)){
							// for(JCTree var : variableFields){
							// ((JCMethodDecl)def).body.stats=((JCMethodDecl)def).body.stats.append(es(assign(((JCVariableDecl)var).name.toString(),
							// nullv())));
							// }
							// }
							// }
							wrappedClass.defs = wrappedClass.defs.append(constructor(mods(PUBLIC),
									consParams, body(consBody)));
						}
						for (JCVariableDecl par : method.params) {
							variableFields.add(var(mods(PUBLIC),
									par.name.append(names.fromString("$")).append(method.name),
									par.vartype));
						}
						wrappedClass.defs = wrappedClass.defs.appendList(variableFields);
					}
				}
			} else if (restype.toString().equals("void")) {
				if (!addedHere.containsKey(restype.toString())) {
					List<JCExpression> implement;
					ListBuffer<JCTree> wrappedMethods = new ListBuffer<JCTree>();

					JCVariableDecl messageId = var(mods(PRIVATE | FINAL), "messageId",
							make.TypeIdent(TypeTags.INT));

					// a flag which indicates whether the future is ready.
					// private boolean redeemed = false;
					JCVariableDecl varRedeemed = var(mods(PRIVATE), PaniniConstants.REDEEMED,
							make.TypeIdent(TypeTags.BOOLEAN), make.Literal(TypeTags.BOOLEAN, 0));

					wrappedMethods.add(constructor(
							mods(PUBLIC),
							params(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT))),
							body(es(supert()),
									es(assign(select(thist(), "messageId"), id("messageId"))))));

					// Code to be generated for the flag logic.
					// public final Void value() {
					// try {
					// synchronized (this) {
					// while (redeemed == false) wait(); // --- New
					// }
					// } catch (InterruptedException e) {
					// return value();
					// }
					// return null;
					// }
					List<JCCatch> catchers = List.<JCCatch> of(make.Catch(
							make.VarDef(make.Modifiers(0), names.fromString("e"),
									make.Ident(names.fromString("InterruptedException")), null),
							make.Block(
									0,
									List.<JCStatement> of(make.Return(make.Apply(null,
											make.Ident(names.fromString(PaniniConstants.VALUE)),
											List.<JCExpression> nil()))))));
					wrappedMethods.add(method(
							mods(PUBLIC | FINAL),
							"value",
							id("Void"),
							params(),
							body(make.Try(body(sync(make.This(Type.noType), body(whilel(
							// Test whether the duck is ready.
									isFalse(PaniniConstants.REDEEMED), es(apply("wait")))))), catchers,
									null), returnt(nullv()))));

					// Code to be generated for the flag logic.
					// public final void panini$finish(Void t) {
					// synchronized (this) {
					// redeemed = true;
					// notifyAll();
					// }
					// }
					wrappedMethods.add(method(mods(PUBLIC | FINAL),
							PaniniConstants.PANINI_FINISH, voidt(),
							params(var(mods(0), "t", id("Void"))), body(sync(thist(),
							// The duck is ready.
									body(es(assign("redeemed", truev())), es(apply("notifyAll")))))));

					wrappedMethods.add(method(mods(PUBLIC | FINAL),
							PaniniConstants.PANINI_MESSAGE_ID, make.TypeIdent(TypeTags.INT),
							params(), body(returnt(select(thist(), "messageId")))));

					implement = implementing(
							ta(id(PaniniConstants.DUCK_INTERFACE_NAME), args(id("Void")))).toList();

					ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();

					if (!method.params.isEmpty()) {
						ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
						ListBuffer<JCVariableDecl> consParams = new ListBuffer<JCVariableDecl>();
						consParams.add(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT)));
						consBody.add(es(assign(select(thist(), "messageId"), id("messageId"))));

						for (JCVariableDecl par : method.params) {
							consParams.add(var(mods(0), par.name, par.vartype));
							consBody.add(es(assign(
									select(thist(),
											par.name.append(names.fromString("$")).append(method.name)
													.toString()), id(par.name))));
							variableFields.add(var(mods(PUBLIC),
									par.name.append(names.fromString("$")).append(method.name),
									par.vartype));
						}
						constructors.add(constructor(mods(PUBLIC), consParams, body(consBody)));
					}

					JCClassDecl wrappedClass = make.ClassDef(mods(0), 
    						names.fromString(PaniniConstants.DUCK_INTERFACE_NAME  + "$" + restype.toString() + "$" + tree.name.toString()), 
    						List.<JCTypeParameter>of(make.TypeParameter(names.fromString("OWNER"), List.<JCExpression>nil())), 
    						null, 
    						implement, 
    						defs(messageId, duckBarrier).appendList(variableFields).appendList(wrappedMethods).appendList(constructors).toList());

					classes.add(wrappedClass);
					addedHere.put(restype.toString(), wrappedClass);
				} else {
					if (!method.params.isEmpty()) {
						ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();
						JCClassDecl wrappedClass = addedHere.get(restype.toString());
						if (!hasDuplicate(wrappedClass, method.params, method.name)) {
							ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
							ListBuffer<JCVariableDecl> consParams = new ListBuffer<JCVariableDecl>();
							consParams.add(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT)));
							consBody.add(es(assign(select(thist(), "messageId"), id("messageId"))));

							for (JCVariableDecl par : method.params) {
								consParams.add(var(mods(0), par.name, par.vartype));
								consBody.add(es(assign(
										select(thist(),
												par.name.append(names.fromString("$")).append(method.name)
														.toString()), id(par.name))));
							}
							wrappedClass.defs = wrappedClass.defs.append(constructor(mods(PUBLIC),
									consParams, body(consBody)));
						}
						for (JCVariableDecl par : method.params) {
							variableFields.add(var(mods(PUBLIC),
									par.name.append(names.fromString("$")).append(method.name),
									par.vartype));
						}
						wrappedClass.defs = wrappedClass.defs.appendList(variableFields);
					}
				}
			}
		}
		return classes.toList();
	}

	boolean hasDuplicate(JCClassDecl c, List<JCVariableDecl> v, Name name) {
		boolean result = false;
		for (JCTree def : c.defs) {
			if (def.getTag() == Tag.METHODDEF
					&& ((JCMethodDecl) def).name.equals(names.init)) {
				if (((JCMethodDecl) def).params.length() == v.length() + 1) {
					result = true;
					for (int i = 1; i < ((JCMethodDecl) def).params.length(); i++) {

						if (!((JCMethodDecl) def).params.get(i).vartype.toString().equals(
								v.get(i - 1).vartype.toString())) {
							result = false;
							i = ((JCMethodDecl) def).params.length();
						}
					}
					if (result) {
						for (JCVariableDecl var : v)
							((JCMethodDecl) def).body.stats = ((JCMethodDecl) def).body.stats
									.append(es(assign(
											select(thist(), var.name.toString() + "$" + name.toString()),
											id(var.name.toString()))));
					}
				}
			}
		}
		return result;
	}
}