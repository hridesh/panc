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
 * Contributor(s): Hridesh Rajan, Eric Lin, Sean L. Mooney
 */
package org.paninij.comp;

import static com.sun.tools.javac.code.TypeTags.INT;
import static com.sun.tools.javac.tree.JCTree.Tag.APPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.CAPSULEARRAY;
import static com.sun.tools.javac.tree.JCTree.Tag.EXEC;
import static com.sun.tools.javac.tree.JCTree.Tag.LT;
import static com.sun.tools.javac.tree.JCTree.Tag.PREINC;
import static com.sun.tools.javac.tree.JCTree.Tag.TYPEIDENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.paninij.systemgraph.SystemGraph;
import org.paninij.systemgraph.SystemGraphBuilder;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArray;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArrayCall;
import com.sun.tools.javac.tree.JCTree.JCCapsuleWiring;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCDesignBlock;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import org.paninij.util.PaniniConstants;

/**
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public class DesignDeclTransformer extends TreeTranslator {

    /**
     * Decls to copy into the rewritten body. This list <b>will not</b> include
     * capsule decls. They are moved out as 'field' scope elements.
     */
    public final ListBuffer<JCStatement> decls = new ListBuffer<JCStatement>();
    public final ListBuffer<JCStatement> inits = new ListBuffer<JCStatement>();
    public final ListBuffer<JCStatement> assigns = new ListBuffer<JCStatement>();
    public final ListBuffer<JCStatement> starts = new ListBuffer<JCStatement>();
    public final Map<Name, Name> variables = new HashMap<Name, Name>();
    public final Map<Name, Integer> modArrays = new HashMap<Name, Integer>();
    public final Map<Name, JCFieldAccess> refCountStats = new HashMap<Name, JCFieldAccess>();
	public Map<Name, java.util.List<Name>> capsuleAliases;
	public Map<Name, java.util.List<Name>> removedAliases = new HashMap<Name, java.util.List<Name>>();
	public final Map<Name, Integer> aliasRefCounts = new HashMap<Name, Integer>();
    
    final Symtab syms;
    final Names names;
    final Types types;
    final Log log;
    public final SystemGraph sysGraph;
    final HashSet<Name> capsulesToWire = new HashSet<Name>();
    final Resolve rs;
    final Env<AttrContext> env;
    final TreeMaker make;
    final SystemGraphBuilder systemGraphBuilder;

    /**
     * The system decl being transformed.
     * Some the helper methods need a reference to it.
     */
    JCDesignBlock systemDecl;

    public DesignDeclTransformer(Symtab syms, Names names, Types types, Log log,
            Resolve rs, Env<AttrContext> env,
            TreeMaker make, SystemGraphBuilder builder) {
        this.syms = syms;
        this.names = names;
        this.types = types;
        this.log = log;
        this.rs = rs;
        this.env = env;
        this.make = make;
        this.systemGraphBuilder = builder;
        //create a system graph for this transformer.
        this.sysGraph = systemGraphBuilder.createSystemGraph();
        //Add a node for 'this'. The 'this' ref is never declared
        //and will not be added 'automatically' by the visitVarDef method.
        systemGraphBuilder.addSingleNode(this.sysGraph,
                names._this, env.enclClass.sym);
    }

    @Override
    public void visitDesignBlock(JCDesignBlock tree) {
        systemDecl = tree;
        //Only visit the body. We can ignore defs (they get handled
        //in the panini Attr, and we can ignore the params, they just
        //get copied straight into the main method def.
        tree.body.accept(this);
        if (!capsulesToWire.isEmpty()) {
            for (Name n : capsulesToWire) {
                log.error("capsule.instance.not.initialized", n);
            }
        }
        result = tree;
    }

    public void visitVarDef(JCVariableDecl tree) {
       env.info.getScope().enter(tree.sym);

        //Use the type from the previously attributed tree. It has a type
        //associated with it already, vdecl does not.
        if (syms.isCapsuleSym(tree.type.tsym.name)) {
            processCapsuleDef(systemDecl, tree);
        } else {
            if (tree.vartype.getTag() == CAPSULEARRAY)
                processCapsuleArray(systemDecl, tree, env);
            else {
                if (tree.vartype.getTag() == TYPEIDENT
                        || tree.vartype.toString().equals("String")) {
                    //vdecl.mods.flags |= FINAL;
                    decls.add(tree);
                } else
                    log.error(tree.pos(),
                            "only.primitive.types.or.strings.allowed");
            }
        }

        result = tree;
    }

    @Override
    public void visitCapsuleWiring(JCCapsuleWiring tree) {
        Env<AttrContext> wiringEnv = env.dup(tree, env.info.dup());
        processCapsuleWiring(tree, wiringEnv);
        result = tree;
    }

    @Override
    public void visitIndexedCapsuleWiring(JCCapsuleArrayCall tree) {
        processCapsuleArrayWiring(tree, env);
        result = tree;
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop tree) {
        processForEachLoop(tree, assigns, variables, capsulesToWire, sysGraph);
        result = tree;
    }
    
    @Override
    public void visitAssign(JCAssign tree) {
        this.assigns.add(make.Exec(tree));
        result = tree;
    }

    @Override
    public void visitAssignop(JCAssignOp tree) {
        this.assigns.add(make.Exec(tree));
        result = tree;
    }

    //FIXME: Case for types that shouldn't make it through the parser?    throw new AssertionError("Invalid statement gone through the parser");

    private void processCapsuleArrayWiring(JCCapsuleArrayCall mi, Env<AttrContext> env) {
        if(!variables.containsKey(names
                .fromString(mi.name.toString()))){
            log.error(mi.pos(), "symbol.not.found");
        }
        ClassSymbol c = (ClassSymbol) rs
                .findType(env, variables.get(names
                        .fromString(mi.name.toString())));
        if(mi.index.getTag()!=Tag.LITERAL)
            log.error(mi.index.pos(), "capsule.array.call.illegal.index");
        JCLiteral ind = (JCLiteral)mi.index;
        if((Integer)ind.value<0||(Integer)ind.value>=modArrays.get(names
                .fromString(mi.name.toString())))
        {
            log.error(mi.index.pos(), "capsule.array.call.index.out.of.bound", ind.value, modArrays.get(names
                    .fromString(mi.name.toString())));
        }
        if (mi.arguments.length() != c.capsule_info.capsuleParameters.length()) {
            log.error(mi.pos(), "arguments.of.wiring.mismatch");
        } else {
            for (int j = 0; j < mi.arguments.length(); j++) {
                JCAssign newAssign = make
                        .at(mi.pos())
                        .Assign(make.Select
                                (make.TypeCast(make.Ident(variables.get(names
                                        .fromString(mi.indexed.toString()))), make.Indexed
                                        (mi.indexed,
                                                mi.index)),
                                                c.capsule_info.capsuleParameters.get(j)
                                                .getName()), mi.arguments.get(j));
                JCExpressionStatement assignAssign = make
                        .Exec(newAssign);
                assigns.append(assignAssign);
                if(c.capsule_info.capsuleParameters.get(j).vartype.getTag()== Tag.TYPEARRAY){
                    if(syms.capsules.containsKey(names
                            .fromString(((JCArrayTypeTree)c.capsule_info.capsuleParameters.get(j).vartype).elemtype
                                    .toString())))
                        systemGraphBuilder.addConnectionsOneToMany(sysGraph,
                                names.fromString(mi.indexed.toString()+"["+mi.index+"]"),
                                c.capsule_info.capsuleParameters.get(j).getName(),
                                names.fromString(mi.arguments.get(j).toString()));
                }
                if (syms.capsules.containsKey(names
                        .fromString(c.capsule_info.capsuleParameters.get(j).vartype
                                .toString()))) {
                    systemGraphBuilder.addConnection(sysGraph,
                            names.fromString(mi.indexed.toString()+"["+mi.index+"]"),
                            c.capsule_info.capsuleParameters.get(j).getName(),
                            names.fromString(mi.arguments.get(j).toString()));
                }
                
				if (syms.capsules
						.containsKey(names
								.fromString(c.capsule_info.capsuleParameters
										.get(j).vartype.toString()))) {
					// let's do some work!
					Name alias, where;
					if (!variables.containsKey(names.fromString(mi.arguments
							.get(j).toString()))) {
						if (mi.arguments.get(j) instanceof JCIdent) {
							where = ((JCIdent) mi.arguments.get(j)).sym.owner.name;
						} else if (mi.arguments.get(j) instanceof JCArrayAccess) {
							JCIdent indexed = (JCIdent)((JCArrayAccess) mi.arguments.get(j)).indexed;
							if (indexed.sym == null)
								indexed.sym = rs.findType(env, variables.get(indexed.name));
							where = indexed.sym.owner.name;
						} else {
							where = null; // do not process any other types.
						}
						alias = null;
						if (mi.arguments.get(j).type != null)
							alias = mi.arguments.get(j).type.tsym.name;
						//alias = mi.arguments.get(j).type.tsym.name;
						if (capsuleAliases.containsKey(alias)) {
							java.util.List<Name> aliases = capsuleAliases
									.get(alias);
							// got through removed aliases
							if ((removedAliases != null)
									&& (removedAliases.get(c.name) != null))
								for (Name name : removedAliases.get(c.name)) {
									if (name.toString().equals(
											c.name.toString())) {
										aliases.add(where);
									}
								}
							for (Name name : aliases) {
								if (name.toString().equals(c.name.toString())) {
									aliases.remove(name);
									if (removedAliases.containsKey(c.name)) {
										if (!removedAliases.get(c.name)
												.contains(name)) {
											removedAliases.get(c.name)
													.add(name);
										}
									} else {
										java.util.List<Name> values = new ArrayList<Name>();
										values.add(name);
										removedAliases.put(c.name, values);
									}
									aliases.add(where);
								}
							}

							capsuleAliases.get(alias).add(where);
						} else {
							ArrayList<Name> values = new ArrayList<Name>();
							values.add(where);
							capsuleAliases.put(alias, values);
						}
					} else {
						alias = null;
						if (mi.arguments.get(j).type != null)
							alias = mi.arguments.get(j).type.tsym.name;
						Name alias_interface = null;//
						if (mi.arguments.get(j).type instanceof ClassType) {
							ClassType alias_type = (ClassType) mi.arguments
									.get(j).type;
							if (alias_type.all_interfaces_field.head != null)
								alias_interface = alias_type.all_interfaces_field.head.tsym.name;
						}

						/*
						 * (mi.arguments.get(j).type instanceof ClassType) ?
						 * ((ClassType) mi.arguments
						 * .get(j).type).all_interfaces_field.head.tsym.name :
						 * null;
						 */
						java.util.List<Name> values = null;
						if (capsuleAliases.containsKey(alias)) {
							values = capsuleAliases.get(alias);
						} else if (capsuleAliases.containsKey(alias_interface)) {
							values = capsuleAliases.get(alias_interface);
						}
						if (values != null) {
							for (Name name : values) {
								if (name.toString().equals(c.toString())) {
									// Got a match, lets add this
									Name candidate = names
											.fromString(mi.arguments.get(j)
													.toString());
									/*System.out.println(c + "(" + alias + ")"
											+ " : " + candidate);*/
									if (aliasRefCounts.containsKey(candidate)) {
										int val = aliasRefCounts.get(candidate) + 1;
										aliasRefCounts.remove(candidate);
										aliasRefCounts.put(candidate, val);
									} else {
										aliasRefCounts.put(candidate, 1);
									}
								}
							}
						}
					}
				}
            }
            // updated refcount stats
            Name variableName = names.fromString(mi.indexed.toString()+"["+mi.index+"]");
            JCFieldAccess refAccess = make.Select
                    (make.TypeCast(make.Ident(variables.get(names
                            .fromString(mi.indexed.toString()))), make.Indexed
                            (mi.indexed,
                                    mi.index)),
                                    names.fromString(PaniniConstants.PANINI_REF_COUNT));
            refCountStats.put(variableName, refAccess);
            capsulesToWire.remove(mi.name);
        }
    }

    private void processForEachLoop(JCEnhancedForLoop loop, ListBuffer<JCStatement> assigns, Map<Name, Name> variables, Set<Name> capsules, SystemGraph sysGraph) {
        ClassSymbol c = syms.capsules.get(names.fromString(loop.var.vartype.toString()));
        if(c==null){
            log.error(loop.pos(), "capsule.array.type.error", loop.var.vartype);
        }
        ClassSymbol d = syms.capsules.get(variables.get(names.fromString(loop.expr.toString())));
        if(d==null)
            log.error(loop.expr.pos(), "symbol.not.found");
        variables.put(loop.var.name, names.fromString(d.name.toString()));
        //                  if(!types.isSameType(c.type, d.type)){
        //                      log.error(loop.var.pos(),"expected", d.type);
        //                  }// this won't work any more
        // Type checking: find a way to check if type matches
        ListBuffer<JCStatement> loopBody = new ListBuffer<JCStatement>();
        JCVariableDecl vdecl = make.at(loop.pos).VarDef(make.Modifiers(0),
                loop.var.name,
                make.Ident(d.name),
                make.Indexed(loop.expr, make.Ident(names.fromString("index$"))));
        loopBody.add(vdecl);

        JCVariableDecl arraycache = make.at(loop.pos).VarDef(make.Modifiers(0),
                names.fromString("index$"),
                make.TypeIdent(INT),
                make.Literal(0));
        JCBinary cond = make.at(loop.pos).Binary(LT, make.Ident(names.fromString("index$")),
                make.Select(loop.expr,
                        names.fromString("length")));
        JCUnary unary = make.at(loop.pos).Unary(PREINC, make.Ident(names.fromString("index$")));
        JCExpressionStatement step =
                make.at(loop.pos).Exec(unary);
        if(loop.body.getTag() == Tag.BLOCK){
            JCBlock jb = (JCBlock)loop.body;
            for(JCStatement s : jb.stats){
                if(s.getTag()!=EXEC){
                    log.error(s.pos(),"foreachloop.statement.error");
                }else if(((JCExpressionStatement)s).expr.getTag()!=APPLY){
                    log.error(s.pos(),"foreachloop.statement.error");
                }
                JCCapsuleWiring mi = (JCCapsuleWiring)((JCExpressionStatement)s).expr;
                loopBody.appendList(transWiring(mi, env, true));
                if(loop.var.name.toString().equals(mi.capsule.toString()))
                    capsules.remove(names.fromString(loop.expr.toString()));
                if (mi.args.length() != c.capsule_info.capsuleParameters.length()) {
                    log.error(mi.pos(), "arguments.of.wiring.mismatch");
                } else {
                    for(int j=0;j<mi.args.length();j++){
                        if(c.capsule_info.capsuleParameters.get(j).vartype.getTag()== Tag.TYPEARRAY){
                            if(syms.capsules.containsKey(names
                                    .fromString(((JCArrayTypeTree)c.capsule_info.capsuleParameters.get(j).vartype).elemtype
                                            .toString())))
                                systemGraphBuilder.addConnectionsManyToMany(sysGraph,
                                        names.fromString(loop.expr.toString()), c.capsule_info.capsuleParameters.get(j).getName(),
                                        names.fromString(mi.args.get(j).toString()));
                        }
                        if (syms.capsules.containsKey(names
                                .fromString(c.capsule_info.capsuleParameters.get(j).vartype
                                        .toString()))) {
                            systemGraphBuilder.addConnectionsManyToOne(sysGraph,
                                    names.fromString(loop.expr.toString()),
                                    c.capsule_info.capsuleParameters.get(j).getName(),
                                    names.fromString(mi.args.get(j).toString()));
                        }
                    }
                }
            }
        }
        else{
            if(loop.body.getTag()!=EXEC){
                log.error(loop.body.pos(),"foreachloop.statement.error");
            }else if(((JCExpressionStatement)loop.body).expr.getTag()!=APPLY){
                log.error(loop.body.pos(),"foreachloop.statement.error");
            }
            JCCapsuleWiring mi = (JCCapsuleWiring)((JCExpressionStatement)loop.body).expr;
            loopBody.appendList(transWiring(mi, env, true));
            if(loop.var.name.toString().equals(mi.capsule.toString()))
                capsules.remove(names.fromString(loop.expr.toString()));
            if (mi.args.length() != c.capsule_info.capsuleParameters.length()) {
                log.error(mi.pos(), "arguments.of.wiring.mismatch");
            } else {
                for(int j=0;j<mi.args.length();j++){
                    if(c.capsule_info.capsuleParameters.get(j).vartype.getTag()== Tag.TYPEARRAY){
                        if(syms.capsules.containsKey(names
                                .fromString(((JCArrayTypeTree)c.capsule_info.capsuleParameters.get(j).vartype).elemtype
                                        .toString())))
                            systemGraphBuilder.addConnectionsManyToMany(sysGraph,
                                    names.fromString(loop.expr.toString()), c.capsule_info.capsuleParameters.get(j).getName(),
                                    names.fromString(mi.args.get(j).toString()));
                    }
                    if (syms.capsules.containsKey(names
                            .fromString(c.capsule_info.capsuleParameters.get(j).vartype
                                    .toString()))) {
                        systemGraphBuilder.addConnectionsManyToOne(sysGraph,
                                names.fromString(loop.expr.toString()),
                                c.capsule_info.capsuleParameters.get(j).getName(),
                                names.fromString(mi.args.get(j).toString()));
                    }
                }
            }
        }

        JCForLoop floop =
                make.at(loop.pos).ForLoop(List.<JCStatement>of(arraycache),
                        cond,
                        List.of(step),
                        make.Block(0, loopBody.toList()));
        assigns.append(floop);
    }

    private void processCapsuleWiring(final JCCapsuleWiring wiring, Env<AttrContext> env) {
        assigns.appendList(transWiring(wiring, env, false));
    }

    private List<JCStatement> transWiring(final JCCapsuleWiring mi, Env<AttrContext> env, boolean forEachLoop){
        ClassSymbol c = null;
        //A 'fresh' identifier for the translated wiring.
        //Creating a fresh one ensures the expression will be
        //typed/attributed correctly.
        JCIdent capId = null;
        if(mi.capsule.hasTag(Tag.IDENT)) {
            JCIdent mId = (JCIdent)mi.capsule;
            capsulesToWire.remove(mId.name);
            Symbol s = rs.findType(env, variables.get(mId.name) );
            
            if (s.kind == Kinds.TYP) {
                c = (ClassSymbol)s;
            } else {
                Assert.error("Unknown type for " + mi.capsule);
            }

            capId = mId;
        } else {
            Assert.error("unknown object to wire " + mi.capsule);
        }

        ListBuffer<JCStatement> assigns = new ListBuffer<JCStatement>();

        List<JCVariableDecl> cparams = c.capsule_info.capsuleParameters;
        List<Type> wiringTypes = c.capsule_info.wiringSym.type.getParameterTypes();
        List<JCExpression> args = mi.args;
        for(; cparams.nonEmpty();
                cparams = cparams.tail,
                args = args.tail,
                wiringTypes = wiringTypes.tail
                ) {
        	
			if (wiringTypes.head.tsym.isCapsule()) {
				Name alias, where;
				Name arg = names.fromString(args.head.toString());
				if (!variables.containsKey(arg)) {
					// cparams.head.sym.type.tsym.name
					if (args.head.type != null)
						alias = args.head.type.tsym.name;
					else
						alias = null;
					if (args.head.getTree() instanceof JCIdent)
						where = ((JCIdent) args.head.getTree()).sym.owner.name;
					else where = null;
					//System.out.println(where + "(" + alias + ")");
					if (capsuleAliases.containsKey(alias)) {
						java.util.List<Name> aliases = capsuleAliases
								.get(alias);
						for (Name name : aliases) {
							if (name.toString().equals(c.name.toString())) {
								aliases.remove(name);
								aliases.add(where);
							}
						}
						capsuleAliases.get(alias).add(where);
					} else {
						ArrayList<Name> values = new ArrayList<Name>();
						values.add(where);
						capsuleAliases.put(alias, values);
					}

				} else {
					//alias = args.head.type.tsym.name;
					if (args.head.type != null)
						alias = args.head.type.tsym.name;
					else
						alias = null;
					Name alias_interface = null;

					if (args.head.type instanceof ClassType) {
						ClassType argType = (ClassType) args.head.type;
						if (argType.all_interfaces_field != null
								&& argType.all_interfaces_field.head != null) {
							alias_interface = argType.all_interfaces_field.head.tsym.name;
						}
					}

					// tsym.attributes_field.head.type.tsym.name;
					java.util.List<Name> values = null;
					if (capsuleAliases.containsKey(alias)) {
						values = capsuleAliases.get(alias);

					} else if (capsuleAliases.containsKey(alias_interface)) {
						values = capsuleAliases.get(alias_interface);
					}
					if (values != null) {
						for (Name name : values) {
							if (name.toString().equals(c.toString())) {
								// Got a match, lets add this
								Name candidate = names.fromString(args.head
										.toString());// cparams.head.name;
								/*System.out.println(c + "(" + alias + ")"
										+ " : " + candidate);*/
								if (aliasRefCounts.containsKey(candidate)) {
									int val = aliasRefCounts.get(candidate) + 1;
									aliasRefCounts.remove(candidate);
									aliasRefCounts.put(candidate, val);
								} else {
									aliasRefCounts.put(candidate, 1);
								}
							}
						}
					}
				}
			}

            JCAssign newAssign = make
                    .at(mi.pos())
                    .Assign(make.Select(make.TypeCast(make.Ident(c), capId),
                                        cparams.head.name),
                            args.head);
            JCExpressionStatement assignAssign = make
                    .Exec(newAssign);
            assigns.append(assignAssign);
            if(!forEachLoop){
                if(cparams.head.vartype.getTag()== Tag.TYPEARRAY){
                    if(args.head.getTag()!=Tag.NEWARRAY)
                        if(syms.capsules.containsKey(names
                                .fromString(((JCArrayTypeTree)cparams.head.vartype).elemtype
                                        .toString())))
                            systemGraphBuilder.addConnectionsOneToMany(sysGraph,
                                    names.fromString(mi.capsule.toString()), cparams.head.getName(),
                                    names.fromString(args.head.toString()));
                }

                if (wiringTypes.head.tsym.isCapsule()) {
                    systemGraphBuilder.addConnection(sysGraph,
                            names.fromString(mi.capsule.toString()),
                            cparams.head.getName(),
                            names.fromString(args.head.toString()));
                }
            }
        }
        // update refaccess
        Name variableName = capId.name;
        JCFieldAccess refCountAccess = make.Select(make.TypeCast(make.Ident(c), capId),
        		names.fromString(PaniniConstants.PANINI_REF_COUNT));
        refCountStats.put(variableName,refCountAccess);
        
        return assigns.toList();
    }

    private void processCapsuleArray(JCDesignBlock tree, JCVariableDecl vdecl, Env<AttrContext> env) {
        JCCapsuleArray mat = (JCCapsuleArray)vdecl.vartype;
        String initName = mat.elemtype.toString()+"$thread";
        if((vdecl.mods.flags & Flags.TASK) !=0){
            initName = mat.elemtype.toString()+"$task";
            tree.hasTaskCapsule = true;
        }
        else if((vdecl.mods.flags & Flags.SERIAL) !=0)
            initName = mat.elemtype.toString()+"$serial";
        else if((vdecl.mods.flags & Flags.MONITOR) !=0)
            initName = mat.elemtype.toString()+"$monitor";
        ClassSymbol c = syms.capsules.get(names.fromString(initName));
        if(c==null){
            log.error(vdecl.pos(), "capsule.array.type.error", mat.elemtype);
        }
        systemGraphBuilder.addMultipleNodes(sysGraph, vdecl.name, mat.size, c);

        // Instantiate the capsule array.
        JCNewArray s= make.NewArray(make.Ident(c.type.tsym),
                List.<JCExpression>of(make.Literal(mat.size)), null);
        JCAssign newArray =
                make.at(vdecl.pos()).Assign(make.Ident(vdecl.name), s);
        inits.add(make.Exec(newArray));

        ListBuffer<JCStatement> loopBody = new ListBuffer<JCStatement>();
        JCVariableDecl arraycache = make.VarDef(make.Modifiers(0),
                names.fromString("index$"),
                make.TypeIdent(INT),
                make.Literal(0));
        JCBinary cond = make.Binary(LT, make.Ident(names.fromString("index$")),
                make.Select(make.Ident(vdecl.name),
                        names.fromString("length")));
        JCUnary unary = make.Unary(PREINC, make.Ident(names.fromString("index$")));
        JCExpressionStatement step =
                make.Exec(unary);
        JCNewClass newClass = make.NewClass(null, null,
                make.QualIdent(c.type.tsym), List.<JCExpression>nil(), null);
        newClass.constructor = rs.resolveConstructor
                (tree.pos(), env, c.type, List.<Type>nil(), null,false,false);
        newClass.type = c.type;
        loopBody.add(make.Exec(make.Assign(
                make.Indexed(make.Ident(vdecl.name), make.Ident(names.fromString("index$"))),
                newClass)));
        JCForLoop floop =
                make.ForLoop(List.<JCStatement>of(arraycache),
                        cond,
                        List.of(step),
                        make.Block(0, loopBody.toList()));
        assigns.append(floop);
        // TODO: assigning name to thread
        if(initName.contains("$thread")) {
        	ListBuffer<JCStatement> lbody = new ListBuffer<JCStatement>();
            JCVariableDecl ac = make.VarDef(make.Modifiers(0),
                    names.fromString("index$"),
                    make.TypeIdent(INT),
                    make.Literal(0));
            JCBinary condition = make.Binary(LT, make.Ident(names.fromString("index$")),
                    make.Select(make.Ident(vdecl.name),
                            names.fromString("length")));
            JCUnary u = make.Unary(PREINC, make.Ident(names.fromString("index$")));
            JCExpressionStatement stmt =
                    make.Exec(u);
            
            String prefix = vdecl.name.toString()+"[";
        	String postfix = "]"+":"+((JCCapsuleDecl)c.tree).parentCapsule.name.toString();
        	JCBinary rhs = make.Binary(Tag.PLUS, 
        			make.Binary(Tag.PLUS, 
        					make.Literal(TypeTags.CLASS, prefix), make.Ident(names.fromString("index$"))),
        			make.Literal(TypeTags.CLASS, postfix));
        	ListBuffer<JCExpression> exprs = new ListBuffer<JCExpression>();
        	exprs.add(make.Ident(names.fromString("thName$")));
        	lbody.add(make.VarDef(make.Modifiers(0), 
        			names.fromString("thName$"), 
        			make.Ident(names.fromString("String")), 
        			rhs));
        	lbody.add(make
					.Exec(make.Apply(
							List.<JCExpression> nil(),
							make.Select(
									make.TypeCast(
											make.Ident(c.type.tsym),
											make.Indexed(make.Ident(vdecl.name), make.Ident(names.fromString("index$")))),
									names.fromString("setName")),
									exprs.toList())));
            JCForLoop thNameLoop =
                    make.ForLoop(List.<JCStatement>of(ac),
                    		condition,
                            List.of(stmt),
                            make.Block(0, lbody.toList()));
            assigns.append(thNameLoop);
        }

        JCVariableDecl arraycache2 = make.VarDef(make.Modifiers(0),
                names.fromString("index$"),
                make.TypeIdent(INT),
                make.Literal(0));
        JCBinary cond2 = make.Binary(LT, make.Ident(names.fromString("index$")),
                make.Select(make.Ident(vdecl.name),
                        names.fromString("length")));
        JCUnary unary2 = make.Unary(PREINC, make.Ident(names.fromString("index$")));
        JCExpressionStatement step2 =
                make.Exec(unary2);
        ListBuffer<JCStatement> loopBody2 = new ListBuffer<JCStatement>();
        loopBody2.add(make.Exec(make.Apply(List.<JCExpression>nil(),
                make.Select(make.Indexed(make.Ident(vdecl.name), make.Ident(names.fromString("index$"))), names.panini.Start),
                List.<JCExpression>nil())));
        JCForLoop floop2 =
                make.ForLoop(List.<JCStatement>of(arraycache2),
                        cond2,
                        List.of(step2),
                        make.Block(0, loopBody2.toList()));
        starts.prepend(floop2);
        
        final boolean capTypeDefinedRun = c.capsule_info.definedRun;
        for(int j = mat.size-1; j>=0;j--){
            final Name connectCapIdx = names.fromString(vdecl.name.toString()+"["+j+"]");
            systemGraphBuilder.addConnection(sysGraph, names._this,
                    connectCapIdx, connectCapIdx);
            
            JCFieldAccess refCountAccess = make.Select(make.TypeCast(
            		make.Ident(c), make.Indexed(make.Ident(vdecl.name), make.Literal(j))),
            		names.fromString(PaniniConstants.PANINI_REF_COUNT));
            refCountStats.put(connectCapIdx,refCountAccess);
        }

        variables.put(vdecl.name, c.name);
        if(c.capsule_info.capsuleParameters.nonEmpty())
            capsulesToWire.add(vdecl.name);
        modArrays.put(vdecl.name, mat.size);
    }

    private void processCapsuleDef(JCDesignBlock tree, JCVariableDecl vdecl) {
        String initName = vdecl.vartype.toString()+"$thread";
        if((vdecl.mods.flags & Flags.TASK) !=0){
            initName = vdecl.vartype.toString()+"$task";
            tree.hasTaskCapsule = true;
        }
        else if((vdecl.mods.flags & Flags.SERIAL) !=0)
            initName = vdecl.vartype.toString()+"$serial";
        else if((vdecl.mods.flags & Flags.MONITOR) !=0)
            initName = vdecl.vartype.toString()+"$monitor";
        ClassSymbol c = syms.capsules.get(names.fromString((initName)));
        if(c==null)
            log.error(vdecl.pos, "invalid.capsule.type");
        systemGraphBuilder.addSingleNode(sysGraph, vdecl.name, c);
        //Implicit link from this to the design capsule decl.
        systemGraphBuilder.addConnection(sysGraph, names._this, vdecl.name, vdecl.name);

        JCNewClass newClass = make.at(vdecl.pos()).NewClass(null, null,
                make.Ident(c.type.tsym), List.<JCExpression>nil(), null);
        newClass.constructor = rs.resolveConstructor
                (tree.pos(), env, c.type, List.<Type>nil(), null,false,false);
        newClass.type = c.type;
        JCAssign newAssign = make.at(vdecl.pos()).Assign(make.Ident(vdecl.name),
                newClass);
        newAssign.type = vdecl.type;
        JCExpressionStatement nameAssign = make.at(vdecl.pos()).Exec(newAssign);
        nameAssign.type = vdecl.type;
        inits.append(nameAssign);
        // TODO: assigning name to thread
        if(initName.contains("$thread")) {
        	String name = vdecl.name.toString()+":"+vdecl.type.tsym.name.toString();
        	JCLiteral sName = make.Literal(TypeTags.CLASS, name);
        	ListBuffer<JCExpression> exprs = new ListBuffer<JCExpression>();
        	exprs.add(sName);
        	JCStatement stmt = make
					.Exec(make.Apply(
							List.<JCExpression> nil(),
							make.Select(
									make.TypeCast(
											make.Ident(c.type.tsym),
											make.Ident(vdecl.name)),
									names.fromString("setName")),
									exprs.toList()));
        	inits.append(stmt);
        }
        JCExpressionStatement startAssign = make.Exec(make.Apply(List.<JCExpression>nil(),
                make.Select(make.Ident(vdecl.name), names.panini.Start),
                List.<JCExpression>nil()));
        starts.prepend(startAssign);

        variables.put(vdecl.name, c.name);
        if(c.capsule_info.capsuleParameters.nonEmpty())
            capsulesToWire.add(vdecl.name);
    }
    
    public void setCapsuleAliases(Map<Name, java.util.List<Name>> aliases) {
		capsuleAliases = aliases;
	}
}
