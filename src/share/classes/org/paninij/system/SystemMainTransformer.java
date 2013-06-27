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
package org.paninij.system;

import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.TypeTags.INT;
import static com.sun.tools.javac.tree.JCTree.Tag.APPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.CAPSULEARRAY;
import static com.sun.tools.javac.tree.JCTree.Tag.EXEC;
import static com.sun.tools.javac.tree.JCTree.Tag.FOREACHLOOP;
import static com.sun.tools.javac.tree.JCTree.Tag.LT;
import static com.sun.tools.javac.tree.JCTree.Tag.MAAPPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.PREINC;
import static com.sun.tools.javac.tree.JCTree.Tag.TYPEIDENT;
import static com.sun.tools.javac.tree.JCTree.Tag.VARDEF;

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
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.CapsuleExtras;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
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
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSystemDecl;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.PaniniConstants;

/**
 * @author Sean L. Mooney
 * @since panini-0.9.2
 *
 * TODO: This should merge with the system decl intrepretor eventually.
 */
public class SystemMainTransformer extends TreeTranslator {


    public final ListBuffer<JCStatement> decls = new ListBuffer<JCStatement>();
    public final ListBuffer<JCStatement> inits = new ListBuffer<JCStatement>();
    public final ListBuffer<JCStatement> assigns = new ListBuffer<JCStatement>();
    public final ListBuffer<JCStatement> submits = new ListBuffer<JCStatement>();
    public final ListBuffer<JCStatement> starts = new ListBuffer<JCStatement>();
    public final ListBuffer<JCStatement> joins = new ListBuffer<JCStatement>();
    public final Map<Name, Name> variables = new HashMap<Name, Name>();
    public final Map<Name, Integer> modArrays = new HashMap<Name, Integer>();

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
    JCSystemDecl systemDecl;

    public SystemMainTransformer(Symtab syms, Names names, Types types, Log log,
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
    }

    @Override
    public void visitSystemDef(JCSystemDecl tree) {
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
        /* Create a new VariableDef. Needed to properly attribute
         * the main method that will get written. If not copied, the
         * symbol gets aliased, which causes the scope resolution logic
         * to think the name is defined in an inner class.
         */
        JCVariableDecl vdecl = make.VarDef(
                tree.getModifiers(),
                tree.name,
                tree.vartype,
                tree.init
                );

        env.info.getScope().enter(tree.sym);

        //Use the type from the previously attributed tree. It has a type
        //associated with it already, vdecl does not.
        if (syms.isCapsuleSym(tree.type.tsym.name)) {
            processCapsuleDef(systemDecl, vdecl);
        } else {
            if (vdecl.vartype.getTag() == CAPSULEARRAY)
                processCapsuleArray(systemDecl, vdecl, env);
            else {
                if (vdecl.vartype.getTag() == TYPEIDENT
                        || vdecl.vartype.toString().equals("String")) {
                    //vdecl.mods.flags |= FINAL;
                    decls.add(vdecl);
                } else
                    log.error(vdecl.pos(),
                            "only.primitive.types.or.strings.allowed");
            }
        }

        result = vdecl;
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
            }
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
        try{
            assigns.appendList(transWiring(wiring, env, false));
        }catch (NullPointerException e){
            //FIXME: Should be caught by typechecking.
            log.error(wiring.pos(), "only.capsule.types.allowed");
        }
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
        List<JCExpression> args = mi.args;
        for(; cparams.nonEmpty();
                cparams = cparams.tail,
                args = args.tail) {

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
                if (syms.capsules.containsKey(names.fromString(cparams.head.vartype.toString()))) {
                    systemGraphBuilder.addConnection(sysGraph,
                            names.fromString(mi.capsule.toString()),
                            cparams.head.getName(),
                            names.fromString(args.head.toString()));
                }
            }
        }

        return assigns.toList();
    }

    private void processCapsuleArray(JCSystemDecl tree, JCVariableDecl vdecl, Env<AttrContext> env) {
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
        JCNewArray s= make.NewArray(make.Ident(c.type.tsym),
                List.<JCExpression>of(make.Literal(mat.size)), null);
        JCVariableDecl newArray =
                make.VarDef(make.Modifiers(0),
                        vdecl.name, make.TypeArray(make.Ident(c.type.tsym)), s);
        decls.add(newArray);
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
        if(c.capsule_info.definedRun){
            for(int j = mat.size-1; j>=0;j--){
                if(tree.activeCapsuleCount==0)
                    starts.append(make.Exec(make.Apply(List.<JCExpression>nil(),
                            make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)), names.fromString("run")),
                            List.<JCExpression>nil())));
                else{
                    starts.prepend(make.Exec(make.Apply(List.<JCExpression>nil(),
                            make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)), names.fromString(PaniniConstants.PANINI_START)),
                            List.<JCExpression>nil())));
                    joins.prepend(make.Try(make.Block(0,List.<JCStatement>of(make.Exec(make.Apply(List.<JCExpression>nil(),
                            make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)),
                                    names.fromString(PaniniConstants.PANINI_JOIN)), List.<JCExpression>nil())))),
                                    List.<JCCatch>of(make.Catch(make.VarDef(make.Modifiers(0),
                                            names.fromString("e"), make.Ident(names.fromString("InterruptedException")),
                                            null), make.Block(0, List.<JCStatement>nil()))), null));
                }
                tree.activeCapsuleCount += mat.size;
            }
        }
        else{
            for(int j = mat.size-1; j>=0;j--){
                starts.prepend(make.Exec(make.Apply(List.<JCExpression>nil(),
                        make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)), names.fromString(PaniniConstants.PANINI_START)),
                        List.<JCExpression>nil())));
            }
            for(int j=0; j<mat.size;j++){
                submits.append(make.Exec(make.Apply(List.<JCExpression>nil(),
                        make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)),
                                names.fromString(PaniniConstants.PANINI_SHUTDOWN)), List.<JCExpression>nil())));
            }
        }
        //                  for(int j = 0; j<mat.size; j++)
        //                      tree.defs = tree.defs.append(createOwnerInterface(mat.elemtype.toString()+"_"+vdecl.name.toString()+"_"+j));

        variables.put(vdecl.name, c.name);
        if(c.capsule_info.capsuleParameters.nonEmpty())
            capsulesToWire.add(vdecl.name);
        modArrays.put(vdecl.name, mat.size);
    }

    private void processCapsuleDef(JCSystemDecl tree, JCVariableDecl vdecl) {
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
        decls.add(vdecl);
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
        JCExpressionStatement startAssign = make.Exec(make.Apply(List.<JCExpression>nil(),
                make.Select(make.Ident(vdecl.name), names.fromString(PaniniConstants.PANINI_START)),
                List.<JCExpression>nil()));
        if(c.capsule_info.definedRun){
            if(tree.activeCapsuleCount==0)
                starts.append(make.Exec(make.Apply(List.<JCExpression>nil(),
                        make.Select(make.Ident(vdecl.name), names.fromString("run")),
                        List.<JCExpression>nil())));
            else{
                starts.prepend(startAssign);
                joins.append(make.Try(make.Block(0,List.<JCStatement>of(make.Exec(make.Apply(List.<JCExpression>nil(),
                        make.Select(make.Ident(vdecl.name),
                                names.fromString(PaniniConstants.PANINI_JOIN)), List.<JCExpression>nil())))),
                                List.<JCCatch>of(make.Catch(make.VarDef(make.Modifiers(0),
                                        names.fromString("e"), make.Ident(names.fromString("InterruptedException")),
                                        null), make.Block(0, List.<JCStatement>nil()))), null));
            }
            tree.activeCapsuleCount++;
        }
        else{
            starts.prepend(startAssign);
            submits.append(make.Exec(make.Apply(List.<JCExpression>nil(),
                    make.Select(make.Ident(vdecl.name),
                            names.fromString(PaniniConstants.PANINI_SHUTDOWN)), List.<JCExpression>nil())));
        }
        //      JCClassDecl ownerIface = createOwnerInterface(
        //              vdecl.vartype.toString()+"_"+vdecl.name.toString(), make, names);
        //      enter.classEnter(ownerIface, env);
        //      tree.defs = tree.defs.append(ownerIface);
        variables.put(vdecl.name, c.name);
        if(c.capsule_info.capsuleParameters.nonEmpty())
            capsulesToWire.add(vdecl.name);
    }
}
