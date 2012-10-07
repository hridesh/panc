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

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import static com.sun.tools.javac.code.Flags.*;


public class ModuleEffectsComp {
    protected static final Context.Key<ModuleEffectsComp> secKey =
        new Context.Key<ModuleEffectsComp>();

    public HashMap<JCMethodDecl, EffectSet> methodEffects = new HashMap<JCMethodDecl, EffectSet>();
    MethodEffectsComp methodEffectsComp = new MethodEffectsComp();
    EffectsSub effectsSub;
    ReachedProcsComp reachedProcsComp;
    private LinkedList<JCMethodDecl> methodsToProcess;

    public static ModuleEffectsComp instance(Context context) {
        ModuleEffectsComp instance = context.get(secKey);
        if (instance == null)
            instance = new ModuleEffectsComp(context);
        return instance;
    }

    protected ModuleEffectsComp(Context context) {
        context.put(secKey, this);
        effectsSub = EffectsSub.instance(context);
        reachedProcsComp = ReachedProcsComp.instance(context);
        ASTChainBuilder.setNames(Names.instance(context));
    }

    public void computeEffects(JCModuleDecl module) {
        methodsToProcess = new LinkedList<JCMethodDecl>();
        HashSet<JCMethodDecl> visitedMethods = new HashSet<JCMethodDecl>();
        for(JCTree def : module.defs) {
        	if(def.getTag() == Tag.METHODDEF) {
                JCMethodDecl method = (JCMethodDecl)def;
                if ((method.sym.flags() & PRIVATE) != 0 ||
                    (method.sym.name.toString().equals("run") &&
                     module.sym.hasRun)) {
                    methodsToProcess.offer(method);
                }
            }
        }
        
        while (!methodsToProcess.isEmpty()) {
            JCMethodDecl method = methodsToProcess.poll();
            visitedMethods.add(method);

            ASTChain chain = ASTChainBuilder.buildChain(module, method);
            new AliasingComp().fillInAliasingInfo(chain);
            reachedProcsComp.todoReachedProcs(module);
            new ASTChainPrinter().printChain(chain);
            
            for (MethodSymbol.MethodInfo calledMethodInfo : method.sym.calledMethods) {
                MethodSymbol calledMethod = calledMethodInfo.method;
                if (calledMethod.tree == null) continue;
                //if (calledMethod.ownerModule() != module.sym) continue; // null means library symbol
                if (!visitedMethods.contains(calledMethod.tree))
                    methodsToProcess.offer(calledMethod.tree);
            }
            EffectSet effects = methodEffectsComp.computeEffectsForMethod(chain, module.sym);
            effects.chain = chain;
            methodEffects.put(method, effects);
        }
        
        effectsSub.module = module;
        effectsSub.substituteMethodEffects(methodEffects);
    }
}