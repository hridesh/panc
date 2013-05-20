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

package org.paninij.comp;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.PaniniConstants;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;

public class CapsuleInternal extends Internal {
	protected Symtab syms;
	protected Enter enter;
	protected MemberEnter memberEnter;

	public CapsuleInternal(TreeMaker make, Names names, Enter enter,
			MemberEnter memberEnter, Symtab syms) {
		super(make, names);
		this.enter = enter;
		this.syms = syms;
		this.memberEnter = memberEnter;
		specCounter = 0;
	}

	protected JCBlock generateThreadCapsuleComputeMethodBody(JCCapsuleDecl tree) {
		JCModifiers noMods = mods(0);
		ListBuffer<JCStatement> messageLoopBody = new ListBuffer<JCStatement>();
		messageLoopBody.append(var(noMods, PaniniConstants.PANINI_DUCK_TYPE,
				PaniniConstants.DUCK_INTERFACE_NAME,
				apply(PaniniConstants.PANINI_GET_NEXT_DUCK)));

		ListBuffer<JCCase> cases = new ListBuffer<JCCase>();
		int varIndex = 0;

		TreeCopier<Void> tc = new TreeCopier<Void>(make);
		for (JCMethodDecl method : tree.publicMethods) {
			ListBuffer<JCStatement> caseStatements = new ListBuffer<JCStatement>();
			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
			for (int i = 0; i < method.params.size(); i++) {
				JCExpression varType = tc.copy(method.params.get(i).vartype);
				caseStatements.append(var(
						noMods,
						"var" + varIndex,
						varType,
						cast(varType,
								select(cast(PaniniConstants.DUCK_INTERFACE_NAME
										+ "$" + method.restype.toString() + "$"
										+ tree.name.toString(),
										id(PaniniConstants.PANINI_DUCK_TYPE)),
										createFieldString(method.name
												.toString(),
												varType.toString(),
												method.params.get(i).name
														.toString(),
												method.params)))));
				args.append(id("var" + varIndex++));
			}

			Type returnType = ((MethodType) method.sym.type).restype;
			if (returnType.tag == TypeTags.VOID) {
				caseStatements.append(es(createOriginalCall(method, args)));
				caseStatements.append(es(apply(
						PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_FINISH, args(nullv()))));
			} else if (returnType.tag == TypeTags.CLASS) {
				caseStatements.append(es(apply(
						PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_FINISH,
						args(createOriginalCall(method, args)))));
			} else {
				System.out.println("Unsupported return type in a public capsule method. Can only be void or non-primitive.");
				System.exit(5555);
			}
			caseStatements.append(break_());
			String constantName = PaniniConstants.PANINI_METHOD_CONST
					+ method.name.toString();
			if (method.params.nonEmpty())
				for (JCVariableDecl param : method.params) {
					constantName = constantName + "$"
							+ param.vartype.toString();
				}
			cases.append(case_(id(constantName), caseStatements));
		}

		ListBuffer<JCStatement> shutDownBody = createShutdownLogic();
		cases.append(case_(intlit(-1), shutDownBody));

		ListBuffer<JCStatement> exitBody = createTerminationLogic();
		cases.append(case_(intlit(-2), exitBody));

		messageLoopBody.append(swtch(
				apply(PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_MESSAGE_ID), cases));

		JCBlock b = body(
				var(mods(0), PaniniConstants.PANINI_TERMINATE,
						make.TypeIdent(TypeTags.BOOLEAN), falsev()),
				whilel(nott(id(PaniniConstants.PANINI_TERMINATE)),
						body(messageLoopBody)));
		return b;
	}

	protected JCBlock generateTaskCapsuleComputeMethodBody(JCCapsuleDecl tree) {
		JCModifiers noMods = mods(0);
		ListBuffer<JCStatement> messageLoopBody = new ListBuffer<JCStatement>();

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
								select(cast(PaniniConstants.DUCK_INTERFACE_NAME
										+ "$" + method.restype.toString() + "$"
										+ tree.name.toString(),
										id(PaniniConstants.PANINI_DUCK_TYPE)),
										createFieldString(method.name
												.toString(),
												varType.toString(),
												method.params.get(i).name
														.toString(),
												method.params)))));
				args.append(id("var" + varIndex++));
			}

			Type returnType = ((MethodType) method.sym.type).restype;
			if (returnType.tag == TypeTags.VOID) {
				caseStatements.append(es(createOriginalCall(method, args)));
				caseStatements.append(es(apply(
						PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_FINISH, args(nullv()))));
			} else if (returnType.tag == TypeTags.CLASS) {
				caseStatements.append(es(apply(
						PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_FINISH,
						args(createOriginalCall(method, args)))));
			} else {
				System.out.println("Unsupported return type in a public capsule method. Can only be void or non-primitive.");
				System.exit(5555);
			}
			caseStatements.append(returnt(falsev()));
			String constantName = PaniniConstants.PANINI_METHOD_CONST
					+ method.name.toString();
			if (method.params.nonEmpty())
				for (JCVariableDecl param : method.params) {
					constantName = constantName + "$"
							+ param.vartype.toString();
				}
			cases.append(case_(id(constantName), caseStatements));
		}

		cases.append(case_(
				intlit(-1),
				ifs(gt(select(thist(), PaniniConstants.PANINI_CAPSULE_SIZE),
						intlit(0)),
						body(es(make
								.Apply(List.<JCExpression> nil(),
										id(PaniniConstants.PANINI_PUSH),
										List.<JCExpression> of(id(PaniniConstants.PANINI_DUCK_TYPE)))),
								returnt(falsev())))));

		cases.append(case_(intlit(-2),
				es(assign(PaniniConstants.PANINI_TERMINATE, truev())),
				returnt(truev())));

		messageLoopBody.append(swtch(
				apply(PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_MESSAGE_ID), cases));

		JCBlock b = body(
				var(mods(0), PaniniConstants.PANINI_TERMINATE,
						make.TypeIdent(TypeTags.BOOLEAN), falsev()),
				var(noMods, PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.DUCK_INTERFACE_NAME,
						apply(PaniniConstants.PANINI_GET_NEXT_DUCK)));
		b.stats = b.stats.appendList(messageLoopBody);
		b.stats = b.stats.append(returnt(falsev()));
		return b;
	}

	private JCMethodInvocation createOriginalCall(final JCMethodDecl method,
			final ListBuffer<JCExpression> args) {
		TreeCopier<Void> tc = new TreeCopier<Void>(make);
		return apply(thist(), method.name.toString() + "$Original", tc.copy(args.toList()));
	}

	private ListBuffer<JCStatement> createShutdownLogic() {
		ListBuffer<JCStatement> shutDownBody = new ListBuffer<JCStatement>();
		shutDownBody
				.append(ifs(
						gt(select(thist(), PaniniConstants.PANINI_CAPSULE_SIZE),
								intlit(0)),
						body(es(make
								.Apply(List.<JCExpression> nil(),
										id(PaniniConstants.PANINI_PUSH),
										List.<JCExpression> of(id(PaniniConstants.PANINI_DUCK_TYPE)))),
								break_())));
		return shutDownBody;
	}

	private ListBuffer<JCStatement> createTerminationLogic() {
		ListBuffer<JCStatement> exitBody = new ListBuffer<JCStatement>();
		exitBody.append(es(assign(PaniniConstants.PANINI_TERMINATE, truev())));
		exitBody.append(break_());
		return exitBody;
	}

	public List<JCClassDecl> generateClassWrappers(JCCapsuleDecl tree,
			Env<AttrContext> env, Resolve rs) {
		ListBuffer<JCClassDecl> classes = new ListBuffer<JCClassDecl>();
		Map<String, JCClassDecl> alreadedAddedDuckClasses = new HashMap<String, JCClassDecl>();

		for (JCMethodDecl method : tree.publicMethods) {
			ListBuffer<JCTree> constructors = new ListBuffer<JCTree>();

			Type restype = ((MethodType) method.sym.type).restype;
			ClassSymbol c = checkAndResolveReturnType(env, rs, restype);

			Iterator<Symbol> iter = c.members().getElements().iterator();
			if (restype.tag == TypeTags.CLASS) {
				if (!alreadedAddedDuckClasses.containsKey(restype.toString())) {
					JCClassDecl duckClass = generateNewDuckClass(
							tree.name.toString(), method, constructors,
							restype, iter, c, env);
					classes.add(duckClass);
					alreadedAddedDuckClasses.put(restype.toString(), duckClass);
				} else {
					if (!method.params.isEmpty()) {
						JCClassDecl duckClass = alreadedAddedDuckClasses
								.get(restype.toString());
						adaptDuckClass(method, iter, duckClass);
					}
				}
			} else if (restype.toString().equals("void")) {
				if (!alreadedAddedDuckClasses.containsKey(restype.toString())) {
					List<JCExpression> implement;
					ListBuffer<JCTree> wrappedMethods = new ListBuffer<JCTree>();

					JCVariableDecl fieldMessageId = var(mods(PRIVATE | FINAL),
							PaniniConstants.PANINI_MESSAGE_ID,
							make.TypeIdent(TypeTags.INT));
					JCVariableDecl fieldRedeemed = var(mods(PRIVATE),
							PaniniConstants.REDEEMED,
							make.TypeIdent(TypeTags.BOOLEAN),
							make.Literal(TypeTags.BOOLEAN, 0));

					wrappedMethods
							.add(constructor(
									mods(PUBLIC),
									params(var(mods(0),
											PaniniConstants.PANINI_MESSAGE_ID,
											make.TypeIdent(TypeTags.INT))),
									body(es(supert()),
											es(assign(
													select(thist(),
															PaniniConstants.PANINI_MESSAGE_ID),
													id(PaniniConstants.PANINI_MESSAGE_ID))))));

					wrappedMethods.add(createValueMethod());

					// Code to be generated for the flag logic.
					// public final void panini$finish(Void t) {
					// synchronized (this) {
					// redeemed = true;
					// notifyAll();
					// }
					// }
					JCMethodDecl paniniFinish;
					paniniFinish = method(
							mods(PUBLIC | FINAL),
							PaniniConstants.PANINI_FINISH,
							voidt(),
							params(var(mods(0), "t", id("Void"))),
							body(sync(
									thist(),
									// The duck is ready.
									body(es(assign(PaniniConstants.REDEEMED,
											truev())), es(apply("notifyAll"))))));

					wrappedMethods.add(this.createPaniniMessageID());
					wrappedMethods.add(createVoidFutureGetMethod());
					implement = implementing(
							ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
									args(id("Void")))).toList();

					ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();

					if (!method.params.isEmpty()) {
						ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
						ListBuffer<JCVariableDecl> consParams = new ListBuffer<JCVariableDecl>();
						consParams.add(var(mods(0),
								PaniniConstants.PANINI_MESSAGE_ID,
								make.TypeIdent(TypeTags.INT)));
						consBody.add(es(assign(
								select(thist(),
										PaniniConstants.PANINI_MESSAGE_ID),
								id(PaniniConstants.PANINI_MESSAGE_ID))));

						for (JCVariableDecl par : method.params) {
							consParams.add(var(mods(0), par.name, par.vartype));
							consBody.add(es(assign(
									select(thist(),
											createFieldString(method.name, par,
													method.params)),
									id(par.name))));
							variableFields.add(var(mods(PUBLIC), names
									.fromString(createFieldString(method.name,
											par, method.params)), par.vartype));
							if (!par.vartype.type.isPrimitive())
								paniniFinish.body.stats = paniniFinish.body.stats
										.append(es(assign(
												select(thist(),
														createFieldString(
																method.name,
																par,
																method.params)),
												nullv())));
						}
						constructors.add(constructor(mods(PUBLIC), consParams,
								body(consBody)));
					}
					wrappedMethods.add(paniniFinish);

					JCClassDecl wrappedClass = make
							.ClassDef(
									mods(0),
									names.fromString(PaniniConstants.DUCK_INTERFACE_NAME
											+ "$"
											+ restype.toString()
											+ "$"
											+ tree.name.toString()), List
											.<JCTypeParameter> nil(), null,
									implement,
									defs(fieldMessageId, fieldRedeemed)
											.appendList(variableFields)
											.appendList(wrappedMethods)
											.appendList(constructors).toList());

					classes.add(wrappedClass);
					alreadedAddedDuckClasses.put(restype.toString(),
							wrappedClass);
				} else {
					if (!method.params.isEmpty()) {
						JCClassDecl duckClass = alreadedAddedDuckClasses
								.get(restype.toString());
						adaptDuckClass(method, iter, duckClass);
					}
				}
			}
		}
		return classes.toList();
	}
	
	private JCMethodDecl createDuckConstructor(Iterator<Symbol> iter, boolean isVoid){
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
		MethodSymbol constructor = null;
		List<Type> thrownTypes = List.<Type>nil();
		if(!isVoid){
			while (iter.hasNext()) {
				Symbol s = iter.next();
				if (s.getKind() == ElementKind.CONSTRUCTOR) {
					MethodSymbol m = (MethodSymbol) s;
					if (m.isStatic() || ((m.flags() & PRIVATE) != 0))
						continue; // Is this correct?
					if (constructor == null){
						constructor = m;
						thrownTypes = m.getThrownTypes();
					}
					else if (m.params().length() < constructor.params().length()){
						constructor = m;
						thrownTypes = m.getThrownTypes();
					}
				}
			}
			if (constructor != null)
				for (VarSymbol v : constructor.params()) {
					if (v.type.toString().equals("boolean"))
						args.add(falsev());
					else if (v.type.isPrimitive())
						args.add(intlit(0));
					else{
						args.add(make.TypeCast(make.Type(v.type), nullv()));
					}
				}
		}

		List<JCExpression> thrown = make.Types(thrownTypes);
		ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
		consBody.add(es(make.Apply(List.<JCExpression> nil(),
				id(names._super), args.toList())));
		consBody.add(es(assign(
				select(thist(), PaniniConstants.PANINI_MESSAGE_ID),
				id(PaniniConstants.PANINI_MESSAGE_ID))));
		return make.MethodDef(mods(PUBLIC), 
				names.init,
				null,
				List.<JCTypeParameter>nil(),
				params(var(mods(0), PaniniConstants.PANINI_MESSAGE_ID,
				make.TypeIdent(TypeTags.INT))).toList(),
				thrown,
				body(consBody),
				null
				);
	}
	
	private JCMethodDecl createDuckConstructor(Iterator<Symbol> iter, boolean isVoid, JCMethodDecl method, JCMethodDecl paniniFinish){
		JCMethodDecl constructor = createDuckConstructor(iter, isVoid);
		for (JCVariableDecl par : method.params) {
			constructor.params = constructor.params.append(var(mods(0), par.name, par.vartype));
			constructor.body.stats = constructor.body.stats.append(es(assign(
					select(thist(),
							createFieldString(method.name, par,
									method.params)), id(par.name))));
			if (!par.vartype.type.isPrimitive())
				paniniFinish.body.stats = paniniFinish.body.stats
						.append(es(assign(
								select(thist(),
										createFieldString(method.name, par,
												method.params)), nullv())));
		}
		return constructor;
	}

	private ListBuffer<JCExpression> superArgs(Iterator<Symbol> iter) {
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
		MethodSymbol constructor = null;
		while (iter.hasNext()) {
			Symbol s = iter.next();
			if (s.getKind() == ElementKind.CONSTRUCTOR) {
				MethodSymbol m = (MethodSymbol) s;
				if (m.isStatic() || ((m.flags() & PRIVATE) != 0))
					continue; // Is this correct?
				if (constructor == null)
					constructor = m;
				else if (m.params().length() < constructor.params().length())
					constructor = m;
			}
		}
		if (constructor != null)
			for (VarSymbol v : constructor.params()) {
				if (v.type.toString().equals("boolean"))
					args.add(falsev());
				else if (v.type.isPrimitive())
					args.add(intlit(0));
				else
					args.add(make.TypeCast(make.Ident(v.type.tsym), nullv()));
			}
		return args;
	}

	private void adaptDuckClass(JCMethodDecl method, Iterator<Symbol> iter,
			JCClassDecl duckClass) {
		ListBuffer<JCTree> newFields = new ListBuffer<JCTree>();
		JCMethodDecl paniniFinish = null;
		for (JCTree def : duckClass.defs) {
			if (def.hasTag(Tag.METHODDEF))
				if (((JCMethodDecl) def).name.toString().equals(
						PaniniConstants.PANINI_FINISH))
					paniniFinish = (JCMethodDecl) def;
		}
		if (!hasDuplicate(duckClass, method.params, method.name)) {
			if (paniniFinish == null)
				Assert.error();// ///shouldn't happen
			duckClass.defs = duckClass.defs.append(createDuckConstructor(iter, method.restype.toString().equals("void"), method, paniniFinish));
		}
		for (JCVariableDecl par : method.params) {
			newFields.add(var(mods(PUBLIC), names.fromString(createFieldString(
					method.name, par, method.params)), par.vartype));
		}
		duckClass.defs = duckClass.defs.appendList(newFields);
	}

	private String trim(String fullName) {
		int index = -1;
		int openParamIndex = fullName.indexOf("<");
		String types = "";
		String rawClassName;
		while (fullName.indexOf(".", index + 1) != -1) {
			if (openParamIndex != -1
					&& fullName.indexOf(".", index + 1) > openParamIndex)
				break;
			index = fullName.indexOf(".", index + 1);
		}
		if (openParamIndex != -1) {
			types = trim(fullName.substring(openParamIndex + 1,
					fullName.indexOf(">")));
			rawClassName = fullName.toString().substring(index + 1,
					openParamIndex)
					+ "<" + types + ">";
		} else
			rawClassName = fullName.toString().substring(index + 1);
		return rawClassName;
	}

	private JCExpression signatureType(ListBuffer<JCExpression> typaram,
			ClassSymbol c) {
		if (typaram.isEmpty())
			return make.Ident(c);
		else
			return make.TypeApply(make.Ident(c), typaram.toList());
	}

	private JCClassDecl generateNewDuckClass(String classNameSuffix,
			JCMethodDecl method, ListBuffer<JCTree> constructors, Type restype,
			Iterator<Symbol> iter, ClassSymbol c, Env<AttrContext> env) {
		String rawClassName = trim(restype.toString());
		ListBuffer<JCTypeParameter> typeParams = new ListBuffer<JCTypeParameter>();
		for (TypeSymbol ts : c.getTypeParameters()) {
			typeParams.append(make.TypeParam(ts.name, (TypeVar) ts.type));
		}
		ListBuffer<JCExpression> typeExpressions = new ListBuffer<JCExpression>();
		for (JCTypeParameter tp : typeParams) {
			typeExpressions.add(make.Ident(tp.name));
		}
		JCVariableDecl fieldWrapped = var(mods(PRIVATE),
				PaniniConstants.PANINI_WRAPPED,
				signatureType(typeExpressions, c), nullv());
		JCVariableDecl fieldMessageId = var(mods(PRIVATE | FINAL),
				PaniniConstants.PANINI_MESSAGE_ID,
				make.TypeIdent(TypeTags.INT), null);
		JCVariableDecl fieldRedeemed = var(mods(PRIVATE),
				PaniniConstants.REDEEMED, make.TypeIdent(TypeTags.BOOLEAN),
				make.Literal(TypeTags.BOOLEAN, 0));

		ListBuffer<JCTree> wrappedMethods = new ListBuffer<JCTree>();
		ListBuffer<JCExpression> inits = new ListBuffer<JCExpression>();
		boolean providesHashCode = false;
		boolean providesEquals = false;
		while (iter.hasNext()) {
			Symbol s = iter.next();
			if (s.getKind() == ElementKind.METHOD) {
				MethodSymbol m = (MethodSymbol) s;
				JCMethodDecl value;
				if(c.packge()!=env.enclClass.sym.packge())
					if (m.isStatic() || ((m.flags() & PUBLIC) == 0)
							|| ((m.flags() & SYNTHETIC) != 0))
						continue; // Do not wrap static methods.
				if (m.isStatic() || ((m.flags() & PROTECTED) != 0) ||((m.flags() & PRIVATE) != 0)
						|| ((m.flags() & SYNTHETIC) != 0))
					continue; // Do not wrap static methods.
				if (!m.type.getReturnType().toString().equals("void")) {
					value = createFutureValueMethod(m, m.name);
				} else {
					value = createVoidFutureValueMethod(m, m.name);
				}
				wrappedMethods.add(value);
				if(!providesHashCode && m.name.contentEquals("hashCode") && m.getParameters().length() == 0)
					providesHashCode = true;
				if(!providesEquals && m.name.contentEquals("equals") && m.getParameters().length() == 1) {
					providesEquals = true;
				}
			}
		}
		iter = c.members().getElements().iterator();
		constructors.add(createDuckConstructor(iter, false));

		JCMethodDecl messageIdMethod = createPaniniMessageID();
		JCMethodDecl finishMethod = createPaniniFinishMethod(c);

		JCExpression extending;
		List<JCExpression> implement;
		if (restype.isInterface()) {
			extending = null;
			implement = implementing(
					ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
							args(id(restype.toString()))),
					id(restype.toString())).toList();
		} else {
			JCMethodDecl get;
			if (restype.toString().equals("void")) {
				extending = id(PaniniConstants.DUCK_INTERFACE_NAME + "$Void");
				implement = implementing(
						ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
								args(id("Void")))).toList();
				get = createVoidFutureGetMethod();
				wrappedMethods.add(get);
			} else {
				extending = signatureType(typeExpressions, c);
				implement = implementing(
						ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
								args(signatureType(typeExpressions, c))))
						.toList();
				get = createFutureGetMethod(c);
				wrappedMethods.add(get);
			}
		}

		ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();

		if (!method.params.isEmpty()) {
			for (JCVariableDecl par : method.params) {
				variableFields.add(var(mods(PUBLIC), names
						.fromString(createFieldString(method.name, par,
								method.params)), par.vartype));
			}
			iter = c.members().getElements().iterator();
			constructors.add(createDuckConstructor(iter, false, method, finishMethod));
		}
		
		ListBuffer<JCTree> wrapperMembers = defs(fieldWrapped, fieldMessageId, fieldRedeemed, finishMethod, messageIdMethod); 
		if(!providesHashCode) wrapperMembers.append(createHashCode());
		if(!providesEquals) wrapperMembers.append(createEquals());

		JCClassDecl wrappedClass = make.ClassDef(
				mods(FINAL),
				names.fromString(PaniniConstants.DUCK_INTERFACE_NAME + "$"
						+ rawClassName + "$" + classNameSuffix),
				typeParams.toList(),
				extending,
				implement,
				wrapperMembers.appendList(variableFields).appendList(constructors)
				.appendList(wrappedMethods).toList());

		return wrappedClass;
	}

	private JCMethodDecl createPaniniFinishMethod(ClassSymbol restype) {
		ListBuffer<JCVariableDecl> finishParams = new ListBuffer<JCVariableDecl>();
		finishParams.add(var(mods(0), "t", make.Ident(restype)));
		JCMethodDecl finishMethod = method(
				mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_FINISH),
				make.Type(syms.voidType),
				finishParams,
				body(sync(
						thist(),
						body(es(assign(PaniniConstants.PANINI_WRAPPED, id("t"))),
								es(assign(PaniniConstants.REDEEMED, truev())),
								es(apply("notifyAll"))))));
		return finishMethod;
	}

	private JCMethodDecl createPaniniMessageID() {
		return method(
				mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_MESSAGE_ID),
				make.TypeIdent(TypeTags.INT),
				body(returnt(select(thist(), PaniniConstants.PANINI_MESSAGE_ID))));
	}

	private JCMethodDecl createHashCode() {
		return method(
				mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_DUCK_HASHCODE),
				make.TypeIdent(TypeTags.INT),
				body(returnt(apply(
						apply(thist(), PaniniConstants.PANINI_DUCK_GET,
								new ListBuffer<JCExpression>()),
						PaniniConstants.PANINI_DUCK_HASHCODE,
						new ListBuffer<JCExpression>()))));
	}

	private JCMethodDecl createEquals() {
		ListBuffer<JCVariableDecl> equalsParams = new ListBuffer<JCVariableDecl>();
		equalsParams.add(var(mods(0), "o", "Object"));
		return method(
				mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_DUCK_EQUALS),
				make.TypeIdent(TypeTags.BOOLEAN),
				equalsParams,
				body(ifs(
						isNull(apply(thist(), PaniniConstants.PANINI_DUCK_GET,
								new ListBuffer<JCExpression>())),
						returnt(isNull("o")),
						returnt(apply(
								select(thist(), PaniniConstants.PANINI_WRAPPED),
								PaniniConstants.PANINI_DUCK_EQUALS,
								args(id("o")))))));
	}

	private JCMethodDecl createDuckConstructor(ListBuffer<JCExpression> inits) {
		return constructor(
				mods(PUBLIC),
				params(var(mods(0), PaniniConstants.PANINI_MESSAGE_ID,
						make.TypeIdent(TypeTags.INT))),
				body(es(make.Apply(List.<JCExpression> nil(),
						make.Ident(names._super), inits.toList())),
						es(assign(
								select(thist(),
										PaniniConstants.PANINI_MESSAGE_ID),
								id(PaniniConstants.PANINI_MESSAGE_ID)))));
	}

	private JCMethodDecl createValueMethod() {
		return method(
				mods(PUBLIC | FINAL),
				"value",
				id("Void"),
				params(),
				body(ifs(isFalse(PaniniConstants.REDEEMED),
						es(apply(thist(), PaniniConstants.PANINI_DUCK_GET))),
						returnt(nullv())));
	}

	private JCMethodDecl createFutureValueMethod(MethodSymbol m,
			Name method_name) {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
		JCExpression restype = make.Type(m.type.getReturnType());

		ListBuffer<JCTypeParameter> tp = new ListBuffer<JCTypeParameter>();
		for (TypeSymbol ts : m.getTypeParameters()) {
			tp.appendList(make.TypeParams(List.<Type> of(ts.type)));
		}
		if (m.getParameters() != null) {
			for (VarSymbol v : m.getParameters()) {
				params.add(make.VarDef(v, null));
				args.add(id(v.name));
			}
		}
		JCMethodDecl value = make.MethodDef(
				mods(PUBLIC | FINAL),
				method_name,
				restype,
				tp.toList(),
				params.toList(),
				make.Types(m.getThrownTypes()),
				body(ifs(isFalse(PaniniConstants.REDEEMED),
						es(apply(thist(), PaniniConstants.PANINI_DUCK_GET))),
						returnt(apply(PaniniConstants.PANINI_WRAPPED,
								method_name.toString(), args))), null);
		return value;
	}

	private JCMethodDecl createVoidFutureValueMethod(MethodSymbol m,
			Name method_name) {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
		ListBuffer<JCTypeParameter> tp = new ListBuffer<JCTypeParameter>();
		for (TypeSymbol ts : m.getTypeParameters()) {
			tp.appendList(make.TypeParams(List.<Type> of(ts.type)));
		}

		if (m.getParameters() != null) {
			for (VarSymbol v : m.getParameters()) {
				params.add(make.VarDef(v, null));
				args.add(id(v.name));
			}
		}
		JCMethodDecl delegate = make.MethodDef(
				mods(PUBLIC | FINAL),
				method_name,
				make.Type(syms.voidType),
				tp.toList(),
				params.toList(),
				make.Types(m.getThrownTypes()),
				body(ifs(isFalse(PaniniConstants.REDEEMED),
						es(apply(thist(), PaniniConstants.PANINI_DUCK_GET))),
						es(apply(PaniniConstants.PANINI_WRAPPED,
								method_name.toString(), args))), null);
		return delegate;
	}

	private JCMethodDecl createFutureGetMethod(ClassSymbol restype) {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();

		List<JCCatch> catchers = List.<JCCatch> of(make.Catch(make.VarDef(
				make.Modifiers(0), names.fromString("e"),
				make.Ident(names.fromString("InterruptedException")), null),
				make.Block(0, List.<JCStatement> nil())));
		JCMethodDecl value = method(
				mods(PUBLIC | FINAL),
				PaniniConstants.PANINI_DUCK_GET,
				make.Ident(restype),
				params,
				body(whilel(isFalse(PaniniConstants.REDEEMED), make.Try(
						body(sync(
								make.This(Type.noType),
								body(whilel(isFalse(PaniniConstants.REDEEMED),
										es(apply("wait")))))), catchers, null)),
						returnt(PaniniConstants.PANINI_WRAPPED)));
		return value;
	}

	private JCMethodDecl createVoidFutureGetMethod() {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();

		List<JCCatch> catchers = List.<JCCatch> of(make.Catch(make.VarDef(
				make.Modifiers(0), names.fromString("e"),
				make.Ident(names.fromString("InterruptedException")), null),
				make.Block(0, List.<JCStatement> nil())));
		JCMethodDecl value = method(
				mods(PUBLIC | FINAL),
				PaniniConstants.PANINI_DUCK_GET,
				id("Void"),
				params,
				body(whilel(isFalse(PaniniConstants.REDEEMED), make.Try(
						body(sync(
								make.This(Type.noType),
								body(whilel(isFalse(PaniniConstants.REDEEMED),
										es(apply("wait")))))), catchers, null)),
						returnt(nullv())));
		return value;
	}

	private ClassSymbol checkAndResolveReturnType(Env<AttrContext> env,
			Resolve rs, Type restype) {
		ClassSymbol c;
		if (restype.toString().equals("void"))
			c = (ClassSymbol) rs.findIdent(
					env,
					names.fromString(PaniniConstants.DUCK_INTERFACE_NAME
							+ "$Void"), TYP);
		else if (restype.isPrimitive()) {
			System.out.println("\n\nNon-void primitive return types for capsule procedure calls are not yet supported.\n\n");
			System.exit(10);
			c = null;
		} else
			c = (ClassSymbol) restype.tsym;

		if (c.type.isFinal()) {
			System.out.println("\n\nFinal classes as return types for capsule procedure calls are not supported.\n\n");
			System.exit(10);
		}
		return c;
	}

	private boolean hasDuplicate(JCClassDecl c, List<JCVariableDecl> v,
			Name name) {
		boolean result = false;
		for (JCTree def : c.defs) {
			if (def.getTag() == Tag.METHODDEF
					&& ((JCMethodDecl) def).name.equals(names.init)) {
				if (((JCMethodDecl) def).params.length() == v.length() + 1) {
					result = true;
					for (int i = 1; i < ((JCMethodDecl) def).params.length(); i++) {
						if (!((JCMethodDecl) def).params.get(i).vartype
								.toString().equals(
										v.get(i - 1).vartype.toString())) {
							result = false;
							i = ((JCMethodDecl) def).params.length();
						}
					}
					if (result) {
						for (int i=0; i<v.length(); i++){
							((JCMethodDecl) def).body.stats = ((JCMethodDecl) def).body.stats
									.append(es(assign(
											select(thist(),
													createFieldString(name,
															v.get(i), v)),
											id(((JCMethodDecl) def).params.get(i+1).name.toString()))));
						}
						break;
					}
				}
			}
		}
		return result;
	}

	private String createFieldString(Name name, JCVariableDecl param,
			List<JCVariableDecl> params) {
		return createFieldString(name.toString(), param.vartype.toString(),
				param.name.toString(), params);
	}

	private String createFieldString(String name, String vartype,
			String paramName, List<JCVariableDecl> params) {
		String fieldName;
		fieldName = vartype + "$" + paramName + "$" + name;
		if (params.nonEmpty())
			for (JCVariableDecl v : params) {
				fieldName = fieldName + "$" + v.vartype.toString();
			}
		return fieldName;
	}
}
