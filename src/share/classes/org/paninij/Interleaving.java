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

package org.paninij;

import java.util.*;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.tools.JavaFileObject;

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

import org.paninij.effects.*;


public class Interleaving {
    protected static final Context.Key<Interleaving> mmiKey =
        new Context.Key<Interleaving>();


    SideEffectsComp sideEffectsComp;

    public static Interleaving instance(Context context) {
        Interleaving instance = context.get(mmiKey);
        if (instance == null)
            instance = new Interleaving(context);
        return instance;
    }
    
    public Interleaving(Context context) {
        context.put(mmiKey, this);
    }

    public void insertInterleaving(JCModuleDecl module, JCMethodDecl method) {
        for (BlockDivisionPoint p : computeInterleavingPoints(method)) {
            insertInterleavingAtPoint(module, method, p);
        }
    }

    public void insertInterleavingAtPoint(JCModuleDecl module, JCMethodDecl method, BlockDivisionPoint p) {
        EffectSet before = sideEffectsComp.methodEffectsBeforePoint(method, p); 
        EffectSet after = sideEffectsComp.methodEffectsAfterPoint(method, p); 
        
        ListBuffer<Integer> safeMessages = new ListBuffer<Integer>();
        EffectSet[] messageEffects = new EffectSet[module.publicMethods.size()];
        for (int i = 0; i < module.publicMethods.size(); i++) {
            messageEffects[i] = sideEffectsComp.moduleMessageEffects(module, i);
            if (!messageEffects[i].doesInterfere(before, after)) {
                safeMessages.append(i);
            }
        } 

        List<JCStatement> interleavingStatements = generateInterleavingStatements(method, safeMessages.toList());

        method.body = p.insertStatementsAtPoint(method.body, interleavingStatements);
    }
    
    private final List<BlockDivisionPoint> computeInterleavingPoints(JCMethodDecl m) {
     return List.<BlockDivisionPoint>nil();
    }

    private final List<JCStatement> generateInterleavingStatements(JCMethodDecl method, List<Integer> messages) {
        return List.<JCStatement>nil();
    }
}