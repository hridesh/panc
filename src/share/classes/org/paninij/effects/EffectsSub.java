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
import com.sun.tools.javac.code.Type.*;
import static com.sun.tools.javac.code.Flags.*;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;
import javax.lang.model.element.ElementKind;

import org.paninij.analysis.CFG;
import org.paninij.analysis.CFGNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;


public class EffectsSub extends JCTree.Visitor {
    protected static final Context.Key<EffectsSub> mesKey =
        new Context.Key<EffectsSub>();

    private LinkedList<CFGNode> nodesToProcess;
    private EffectSet visitResult;
    private CFGNode currentNode;
    private CFG cfg;
    HashMap<JCMethodDecl, EffectSet> methodEffects;
    private Names names;
    public JCCapsuleDecl capsule; 

    public static EffectsSub instance(Context context) {
        EffectsSub instance = context.get(mesKey);
        if (instance == null)
            instance = new EffectsSub(context);
        return instance;
    }

    protected EffectsSub(Context context) {
        context.put(mesKey, this);
        names = Names.instance(context);
    }

    public void substituteMethodEffects(HashMap<JCMethodDecl, EffectSet> methodEffects) {
        this.methodEffects = methodEffects;

        LinkedList<JCMethodDecl> methodsToProcess = new LinkedList<JCMethodDecl>(methodEffects.keySet());

        while (!methodsToProcess.isEmpty()) {
            JCMethodDecl method = methodsToProcess.poll();

            EffectSet effects = methodEffects.get(method);
            EffectSet oldEffects = new EffectSet(effects);
            EffectSet toAdd = new EffectSet();

            Iterator<Effect> it = effects.iterator();
            while (it.hasNext()) {
                Effect e = it.next();
                if (e instanceof MethodEffect) {
                    MethodEffect me = (MethodEffect)e;
                    MethodSymbol methSym = me.method;

                    it.remove();
                    if (methSym != method.sym) {
                        if ((methSym.flags() & PRIVATE) == 0 && methSym.owner.isCapsule) {
                            String methodName = methSym.toString();
                            methodName = methodName.substring(0, methodName.indexOf("("))+"$Original";
                            MethodSymbol methSymOrig = (MethodSymbol)((ClassSymbol)methSym.owner).members_field.lookup(names.fromString(methodName)).sym;
                            toAdd.addAll(methodEffects.get(methSymOrig.tree));
                        } else {
                            if (methSym.tree == null) continue;
                            toAdd.addAll(methodEffects.get(methSym.tree));
                        }
                        
                        if (method.sym.toString().contains("$Original")) {
                            String s = method.sym.toString();
                            s = s.substring(0, s.indexOf("$"));
                            MethodSymbol ms = (MethodSymbol)((ClassSymbol)method.sym.owner).members_field.lookup(names.fromString(s)).sym;
                            toAdd.remove(new MethodEffect(ms));
                        } else {
                            toAdd.remove(new MethodEffect(method.sym));
                        }
                    }
                }
            }
            effects.addAll(toAdd);

            if (!oldEffects.equals(effects)) {
                for (MethodSymbol callerMethod : method.sym.callerMethods) {
                    if (callerMethod.tree == null) continue;
//                    if (capsule != null) // otherwise a library
//                        if (callerMethod.ownerCapsule() != capsule.sym) continue;
                    if (callerMethod != method.sym)
                        methodsToProcess.offer(callerMethod.tree);
                }
            }
        }
    }

    public void substituteProcEffects(HashMap<JCMethodDecl, EffectSet> methodEffects) {

        this.methodEffects = methodEffects;

        LinkedList<JCMethodDecl> methodsToProcess = new LinkedList<JCMethodDecl>(methodEffects.keySet());

        while (!methodsToProcess.isEmpty()) {
            JCMethodDecl method = methodsToProcess.poll();

            EffectSet effects = methodEffects.get(method);
            EffectSet oldEffects = new EffectSet(effects);
            EffectSet toAdd = new EffectSet();

            Iterator<Effect> it = effects.iterator();
            while (it.hasNext()) {
                Effect e = it.next();
                if (e instanceof OpenEffect) {
                    OpenEffect me = (OpenEffect)e;
                    MethodSymbol methSym = me.method;

                    it.remove();
/*                    if (methSym != method.sym) {
                        String methodName = methSym.toString();
                        methodName = methodName.substring(0, methodName.indexOf("("))+"$Original";
                        MethodSymbol methSymOrig = (MethodSymbol)((ClassSymbol)methSym.owner).members_field.lookup(names.fromString(methodName)).sym;
                        toAdd.addAll(methodEffects.get(methSymOrig.tree));
                        toAdd.remove(new OpenEffect(method.sym));
                        }*/
                }
            }
            effects.addAll(toAdd);

            if (!oldEffects.equals(effects)) {
                for (MethodSymbol callerMethod : method.sym.callerMethods) {
                    if (callerMethod.tree == null) continue;
                    if (callerMethod != method.sym)
                        methodsToProcess.offer(callerMethod.tree);
                }
            }
        }
    }
}