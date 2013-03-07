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

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.paninij.analysis.CFG;
import org.paninij.analysis.CFGBuilder;
import org.paninij.analysis.CFGPrinter;
import org.paninij.analysis.ASTCFGPrinter;

import static com.sun.tools.javac.code.Flags.*;

public class CapsuleEffectsComp {
    protected static final Context.Key<CapsuleEffectsComp> secKey =
        new Context.Key<CapsuleEffectsComp>();

    public HashSet<JCMethodDecl> processedMethods = new HashSet<JCMethodDecl>();
    MethodEffectsComp methodEffectsComp = new MethodEffectsComp();
    EffectsSub effectsSub;
    ReachedProcsComp reachedProcsComp;
    private LinkedList<JCMethodDecl> methodsToProcess;

    public static CapsuleEffectsComp instance(Context context) {
        CapsuleEffectsComp instance = context.get(secKey);
        if (instance == null)
            instance = new CapsuleEffectsComp(context);
        return instance;
    }

    protected CapsuleEffectsComp(Context context) {
        context.put(secKey, this);
        effectsSub = EffectsSub.instance(context);
        reachedProcsComp = ReachedProcsComp.instance(context);
        CFGBuilder.setNames(Names.instance(context));
    }

    public void computeEffects(JCCapsuleDecl capsule) {
        methodsToProcess = new LinkedList<JCMethodDecl>();
        HashSet<JCMethodDecl> visitedMethods = new HashSet<JCMethodDecl>();
        for(JCTree def : capsule.defs) {
        	if(def.getTag() == Tag.METHODDEF) {
                JCMethodDecl method = (JCMethodDecl)def;
                if ((method.sym.flags() & PRIVATE) != 0 ||
                    (method.sym.name.toString().equals("run") &&
                     capsule.sym.hasRun)) {
                    methodsToProcess.offer(method);
                }
            }
        }
        
        while (!methodsToProcess.isEmpty()) {
            JCMethodDecl method = methodsToProcess.poll();
            visitedMethods.add(method);

            CFGBuilder.buildCFG(capsule, method);
            System.out.println(method.nodesInOrder);
            new AliasingComp().fillInAliasingInfo(method);
            reachedProcsComp.todoReachedProcs(capsule);
//            new CFGPrinter().printCFG(cfg);
            if (method.body != null) {
                System.out.println("digraph G {");
                method.body.accept(new ASTCFGPrinter());
                System.out.println("}");
            }

            
            for (MethodSymbol.MethodInfo calledMethodInfo : method.sym.calledMethods) {
                MethodSymbol calledMethod = calledMethodInfo.method;
                if (calledMethod.tree == null) continue;
                //if (calledMethod.ownerCapsule() != capsule.sym) continue; // null means library symbol
                if (!visitedMethods.contains(calledMethod.tree))
                    methodsToProcess.offer(calledMethod.tree);
            }
            EffectSet effects = methodEffectsComp.computeEffectsForMethod(method, capsule.sym);
            effects.method = method;
            method.effects = effects;
            processedMethods.add(method);
        }
        
        effectsSub.capsule = capsule;
        effectsSub.substituteMethodEffects(processedMethods);
    }
}