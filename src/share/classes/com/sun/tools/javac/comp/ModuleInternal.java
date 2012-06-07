/*
 * This file is part of the Ptolemy project at Iowa State University.
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
 * http://www.cs.iastate.edu/~ptolemy/
 *
 * Contributor(s): Rex Fernando
 */

package com.sun.tools.javac.comp;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;
import java.util.Iterator;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.PaniniConstants;
import javax.lang.model.type.TypeKind;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;

public class ModuleInternal extends Internal
{
    ListBuffer<JCTree> contractDefs;
    Symtab syms;
    Enter enter;
    MemberEnter memberEnter;
    
    public ModuleInternal(TreeMaker make, Names names, Enter enter, MemberEnter memberEnter, Symtab syms) {
        super(make, names);
        this.enter = enter;
        this.syms = syms;
        this.memberEnter = memberEnter;
        specCounter = 0;
        contractDefs = new ListBuffer<JCTree>();
    }

    public JCBlock generateComputeMethodBody(JCModuleDecl tree) {
        JCModifiers mods = mods(Flags.PROTECTED);
        JCModifiers noMods = mods(0);

        ListBuffer<JCStatement> ifBody = new ListBuffer<JCStatement>();

        ifBody.append(es(apply("queueLock", "lock")));
        ifBody.append(es(mm(id("size"))));
        ifBody.append(var(noMods, "name", id("Object"), aindex(id("objects"), pp(id("head")))));
        ifBody.append(ifs(geq(id("head"), select("objects", "length")), es(assign("head", intlit(0)))));

        
        for (JCMethodDecl method : tree.publicMethods) {
            ListBuffer<JCStatement> innerIfStatements = new ListBuffer<JCStatement>();
            Type restype = ((MethodType)method.sym.type).restype;

            if (restype.tag==TypeTags.VOID) {
                innerIfStatements.append(es(apply("queueLock", "unlock")));
                innerIfStatements.append(es(apply(thist(), method.name.toString() + "$Original")));
            } else if (restype.tag==TypeTags.CLASS) {
                innerIfStatements.append(es(mm(id("size"))));
                innerIfStatements.append(var(noMods, "d", ta(id("PaniniDuck"), typeargs(id("BooleanC"))),
                                             cast(ta(id("PaniniDuck"), typeargs(id("BooleanC"))), 
                                                  aindex(id("objects"), pp(id("head"))))));
                innerIfStatements.append(ifs(geq(id("head"), select("objects", "length")), es(assign("head", intlit(0)))));
                innerIfStatements.append(es(apply("queueLock", "unlock")));
                innerIfStatements.append(es(apply("d", "panini$finish",
                                                  args(apply(thist(), method.name.toString() + "$Original")))));
                innerIfStatements.append(sync(id("d"), body(es(apply("d", "notifyAll")))));

            } else {
                System.out.println("Unsupported return type in a public module method. Can only be void or non-primitive.");
                System.exit(5555);
            }

            ifBody.append(ifs(apply("name", "equals", lb(id(PaniniConstants.PANINI_METHOD_CONST + method.name.toString()))), body(innerIfStatements)));
        }
        return body(
            whilel(truev(),
                   body(
                       ifs(eqNum(select(thist(), "size"), 0),
                           body(ifBody
/*
                               

                               var(noMods, "d", ta(id("PaniniDuck"), typeargs(id("BooleanC"))),
                                   cast(ta(id("PaniniDuck"), typeargs(id("BooleanC"))), 
                                        aindex(id("objects"), pp(id("head")))))
                                   
*/                                 
                                                                                                
                               )
                           )
                       )
                )
            );
    }
/*
    public void generateInternalInterfaceDef(JCEventDecl tree, int pos) {
        ListBuffer<JCTree> defs = new ListBuffer<JCTree>();

        JCModifiers publicMods = mods(Flags.PUBLIC);
        JCModifiers publicSyntheticMods = mods(Flags.PUBLIC);
        JCModifiers privateMods = mods(Flags.PRIVATE);
        JCModifiers noMods = mods(0);
        JCModifiers publicInterfaceMods = mods(Flags.PUBLIC | Flags.INTERFACE);
        JCModifiers publicFinalMods = mods(Flags.PUBLIC | Flags.FINAL);
        JCModifiers publicStaticMods = mods(Flags.PUBLIC | Flags.STATIC);
        JCModifiers privateStaticMods = mods(Flags.PRIVATE | Flags.STATIC);
        JCModifiers privateStaticFinalMods = mods(Flags.PRIVATE | Flags.STATIC | Flags.FINAL);

        int i = 1;
        for (JCTree c : tree.getContextVariables()) {
            JCVariableDecl contextVariable = (JCVariableDecl) c;
            JCModifiers contextVariableMods = mods(Flags.PUBLIC, lb(ann(PtolemyConstants.CONTEXT_VARIABLE_ANN_TYPE_NAME,
                                                                        args(assign("index", intc(i))))));

            defs.append(method(contextVariableMods, contextVariable.getName().toString(), (JCExpression) contextVariable.getType()));
            
            i++;
        }

        defs.append(method(publicSyntheticMods, PtolemyConstants.INVOKE_METHOD_NAME, tree.getReturnType(), params(), throwing(id("Throwable"))));
        

        JCModifiers registerMods = mods(Flags.PUBLIC | Flags.STATIC | Flags.SYNCHRONIZED,
                                        lb(ann(PtolemyConstants.IGNORE_REFINEMENT_CHECKING_ANN_TYPE_NAME)));
        JCModifiers handlersChangedMods = mods(Flags.PRIVATE | Flags.STATIC | Flags.SYNCHRONIZED,
                                        lb(ann(PtolemyConstants.IGNORE_REFINEMENT_CHECKING_ANN_TYPE_NAME)));
        JCModifiers announceMods = mods(Flags.PUBLIC | Flags.STATIC,
                                        lb(ann(PtolemyConstants.IGNORE_REFINEMENT_CHECKING_ANN_TYPE_NAME)));


        //Handler interface    
        defs.append(clazz(publicInterfaceMods, PtolemyConstants.EVENT_HANDLER_IFACE_NAME, defs(
                              method(publicSyntheticMods, PtolemyConstants.EVENT_HANDLER_METHOD_NAME, tree.getReturnType(), params(
                                         var(noMods, "next", id(tree.getSimpleName()))), throwing(
                                             id("Throwable"))))));
        
        //EventFrame class
        ListBuffer<JCTree> eventFrameDefs = defs(
            var(publicStaticMods, PtolemyConstants.HANDLERS_LIST_NAME, ta(select("java.util.List"), typeargs(id(PtolemyConstants.EVENT_HANDLER_IFACE_NAME)))),
            var(publicStaticMods, "cachedHandlerRecords", PtolemyConstants.EVENT_FRAME_TYPE_NAME),
            // handlers changed method (helper for (un)register)
            method(handlersChangedMods, PtolemyConstants.HANDLERS_CHANGED_METHOD_NAME, voidt(),
                    params(),
                    body(
 			           var(noMods, "i", select("java.util.Iterator"), apply(PtolemyConstants.HANDLERS_LIST_NAME, "iterator")),
 			           var(noMods, "newRecords", PtolemyConstants.EVENT_FRAME_TYPE_NAME, newt(PtolemyConstants.EVENT_FRAME_TYPE_NAME, args(id("i")))),
 			           es(apply("recordWriteLock", "lock")),
 			           tryt(body(es(assign("cachedHandlerRecords", id("newRecords")))),
 			                body(es(apply("recordWriteLock", "unlock")))))),
            // register method
            method(registerMods, PtolemyConstants.REGISTER_METHOD_NAME, PtolemyConstants.EVENT_HANDLER_IFACE_NAME,
                   params(
                       var(noMods, "h", PtolemyConstants.EVENT_HANDLER_IFACE_NAME)),
                   body(
                       ift(isNull(PtolemyConstants.HANDLERS_LIST_NAME), es(assign(PtolemyConstants.HANDLERS_LIST_NAME, apply("java.util.Collections", "synchronizedList", args(
                                                                                                                                 newt(ta(select("java.util.ArrayList"), typeargs(id(PtolemyConstants.EVENT_HANDLER_IFACE_NAME))), args())))))),
                       es(apply(PtolemyConstants.HANDLERS_LIST_NAME, "add", args(id("h")))),
                       es(apply(PtolemyConstants.HANDLERS_CHANGED_METHOD_NAME)),
                       returnt(id("h")))),
            // unregister method
            method(registerMods, PtolemyConstants.UNREGISTER_METHOD_NAME, PtolemyConstants.EVENT_HANDLER_IFACE_NAME,
                   params(
                       var(noMods, "h", PtolemyConstants.EVENT_HANDLER_IFACE_NAME)),
                   body(
                       ift(isNull(PtolemyConstants.HANDLERS_LIST_NAME), returnt(id("h"))),
                       ift(nott(apply(PtolemyConstants.HANDLERS_LIST_NAME, "contains", args(id("h")))), returnt(id("h"))),
                       es(apply(PtolemyConstants.HANDLERS_LIST_NAME, "remove", args(apply(PtolemyConstants.HANDLERS_LIST_NAME, "lastIndexOf", args(id("h")))))),
                       ift(apply(PtolemyConstants.HANDLERS_LIST_NAME, "isEmpty"), body(es(assign("cachedHandlerRecords", nullt())), es(assign(PtolemyConstants.HANDLERS_LIST_NAME, nullt()))), es(apply(PtolemyConstants.HANDLERS_CHANGED_METHOD_NAME))),
                       returnt(id("h")))),
            var(privateStaticMods, "body", id(tree.getSimpleName())),
            // announce method
            method(announceMods, PtolemyConstants.ANNOUNCE_METHOD_NAME, tree.getReturnType(),
                   params(
                       var(noMods, "ev", id(tree.getSimpleName()))),
                   throwing(id("Throwable")),
                   body(
                       var(noMods, "record", PtolemyConstants.EVENT_FRAME_TYPE_NAME),
                       es(apply("recordReadLock", "lock")),
                       tryt(body(es(assign("record", id("cachedHandlerRecords")))),
                            body(es(apply("recordReadLock", "unlock")))),
                       ift(notNull(select("record.nextRecord")),
                           es(assign(select("EventFrame.body"), id("ev")))),
                       isVoid(tree.getReturnType()) ? 
                       es(apply("record.handler", PtolemyConstants.EVENT_HANDLER_METHOD_NAME, args(
                                    id("record"))))
                       :
                       returnt(apply("record.handler", PtolemyConstants.EVENT_HANDLER_METHOD_NAME, args(
                                         id("record")))))),
            
            // invoke method
            method(publicMods, PtolemyConstants.INVOKE_METHOD_NAME, tree.getReturnType(),
                   params(),
                   throwing(id("Throwable")),
                   body(
                       ift(notNull(select("nextRecord.handler")),
                           isVoid(tree.getReturnType()) ?
                           body(
                               es(apply("nextRecord.handler", PtolemyConstants.EVENT_HANDLER_METHOD_NAME, args(
                                            id("nextRecord")))),
                               returnt())
                           :
                           returnt(apply("nextRecord.handler", PtolemyConstants.EVENT_HANDLER_METHOD_NAME, args(
                                             id("nextRecord"))))),
                       isVoid(tree.getReturnType()) ?
                       es(apply("EventFrame.body", PtolemyConstants.INVOKE_METHOD_NAME))
                       :
                       returnt(apply("EventFrame.body", PtolemyConstants.INVOKE_METHOD_NAME)))),
            var(privateMods, "handler", PtolemyConstants.EVENT_HANDLER_IFACE_NAME),
            var(privateMods, "nextRecord", PtolemyConstants.EVENT_FRAME_TYPE_NAME),
            // constructor
            constructor(privateMods, 
                        params(
                            var(noMods, "chain", select("java.util.Iterator"))),
                        body(
                            es(supert()),
                            
                            ift(apply("chain", "hasNext"), body(
                                    es(assign("handler", cast(PtolemyConstants.EVENT_HANDLER_IFACE_NAME, apply("chain", "next")))),
                                                      es(assign("nextRecord", newt(PtolemyConstants.EVENT_FRAME_TYPE_NAME, args(id("chain"))))),
                                    returnt())),
                            es(assign("nextRecord", nullt())))),
            var(privateStaticFinalMods, "recordLock", select("java.util.concurrent.locks.ReentrantReadWriteLock"), newt(select("java.util.concurrent.locks.ReentrantReadWriteLock"))),
            var(privateStaticFinalMods, "recordReadLock", select("java.util.concurrent.locks.Lock"), apply("recordLock", "readLock")),
            var(privateStaticFinalMods, "recordWriteLock", select("java.util.concurrent.locks.Lock"), apply("recordLock", "writeLock")));
        
        for (JCTree c : tree.getContextVariables()) {
            JCVariableDecl contextVariable = (JCVariableDecl) c;
            JCModifiers contextVariableMods = mods(Flags.PUBLIC/* | Flags.SYNTHETIC//);
            
            eventFrameDefs.append(method(contextVariableMods, 
                                         contextVariable.getName(), 
                                         (JCExpression) contextVariable.getType(),
                                         body(
                                             returnt(apply("body", contextVariable.getName().toString())))));
            
        }

        defs.append(clazz(publicFinalMods, PtolemyConstants.EVENT_FRAME_TYPE_NAME, 
                          implementing(id(tree.getSimpleName())), eventFrameDefs));
                          
        
        ListBuffer<JCTree> eventClosureDefs = new ListBuffer<JCTree>();
        ListBuffer<JCVariableDecl> constructorParams = new ListBuffer<JCVariableDecl>();
        ListBuffer<JCStatement> constructorBodyStatements = new ListBuffer<JCStatement>();

        for (JCTree c : tree.getContextVariables()) {
            JCVariableDecl contextVariable = (JCVariableDecl) c;
            JCModifiers contextVariableMods = mods(Flags.PUBLIC | Flags.FINAL);

            eventClosureDefs.append(var(privateMods, 
                                        contextVariable.getName().toString(), 
                                        (JCExpression) contextVariable.getType()));
            
            eventClosureDefs.append(method(contextVariableMods, 
                                         contextVariable.getName(), 
                                         (JCExpression) contextVariable.getType(),
                                         body(
                                             returnt(id(contextVariable.getName().toString())))));

            constructorParams.append(var(noMods, 
                                         contextVariable.getName().toString(), 
                                         (JCExpression)contextVariable.getType()));
            constructorBodyStatements.append(es(assign(select(thist(), 
                                                              contextVariable.getName().toString()), 
                                                       id(contextVariable.getName()))));
        }

        eventClosureDefs.append(method(publicMods, PtolemyConstants.INVOKE_METHOD_NAME, tree.getReturnType(),
                                       params(),
                                       isVoid(tree.getReturnType()) ?
                                       body()
                                       :
                                       body(returnt(defaultt(tree.getReturnType())))));

        eventClosureDefs.append(constructor(publicMods, constructorParams, body(constructorBodyStatements)));
        

        defs.append(clazz(publicStaticMods, PtolemyConstants.EVENT_CLOSURE_TYPE_NAME,
                          implementing(id(tree.getSimpleName())), 
                          eventClosureDefs));

        JCModifiers internalMods = mods(tree.getModifiers().flags | Flags.INTERFACE, lb(
                                            ann(PtolemyConstants.EVENT_TYPE_DECL_ANN_TYPE_NAME),
                                            ann(PtolemyConstants.EVENT_CONTRACT_DECL_ANN_TYPE_NAME,
                                                args(assign("assumesBlock", 
                                                            tree.contract == null ?
                                                            stringc("null")
                                                            :
                                                            stringc(tree.contract.assumesBlock.toString()))))));
                
        tree.mods = internalMods;
        tree.defs = defs.toList();

        /*JCClassDecl internalTree = make.at(pos).ClassDef(internalMods,
                                                         tree.getSimpleName(), 
                                                         List.<JCTypeParameter>nil(),
                                                         null,
                                                         List.<JCExpression>nil(),
                                                         defs.toList());

                                                         tree.internalInterface = internalTree; //
    }*/
}
