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
import java.util.LinkedList;
import static com.sun.tools.javac.code.Flags.*;


public class SideEffectsComp {
    HashMap<JCMethodDecl, EffectSet> methodEffects = new HashMap<JCMethodDecl, EffectSet>();
    MethodEffectsComp methodEffectsComp = new MethodEffectsComp();
    MethodEffectsSub methodEffectsSub = new MethodEffectsSub();
    private LinkedList<JCMethodDecl> methodsToProcess;

    public void computeEffects(JCModuleDecl module) {
        methodsToProcess = new LinkedList<JCMethodDecl>();
        for(JCTree def : module.defs) {
        	if(def.getTag() == Tag.METHODDEF) {
                JCMethodDecl method = (JCMethodDecl)def;
                if ((method.sym.flags() & PRIVATE) != 0) {
                    methodsToProcess.offer(method);
                }
            }
        }
        
        while (!methodsToProcess.isEmpty()) {
            JCMethodDecl method = methodsToProcess.poll();

            ASTChain chain = ASTChainBuilder.buildChain(method);
            new AliasingComp().fillInAliasingInfo(chain);
            new ASTChainPrinter().printChain(chain);
            
            for (MethodSymbol callerMethod : chain.callerMethods) {
                methodsToProcess.offer(callerMethod.tree);
            }
            
            methodEffects.put(method, methodEffectsComp.computeEffectsForMethod(chain, module.sym));
        }

        methodEffectsSub.substituteMethodEffects(methodEffects);
    }
}