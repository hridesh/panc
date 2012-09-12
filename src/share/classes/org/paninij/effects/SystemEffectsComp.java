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


public class SystemEffectsComp {
    protected static final Context.Key<SystemEffectsComp> secKey =
        new Context.Key<SystemEffectsComp>();

    ReachedProcsComp reachedProcsComp;
    public ModuleEffectsComp moduleEffectsComp;
    public LibraryEffectsComp libraryEffectsComp;
    private EffectsSub effectsSub;

    public static SystemEffectsComp instance(Context context) {
        SystemEffectsComp instance = context.get(secKey);
        if (instance == null)
            instance = new SystemEffectsComp(context);
        return instance;
    }

    protected SystemEffectsComp(Context context) {
        context.put(secKey, this);
        reachedProcsComp = ReachedProcsComp.instance(context);
        moduleEffectsComp = ModuleEffectsComp.instance(context);
        libraryEffectsComp = LibraryEffectsComp.instance(context);
        libraryEffectsComp.methodEffects = moduleEffectsComp.methodEffects;
        effectsSub = EffectsSub.instance(context);
    }

    public void computeEffects(JCModuleDecl module) {
        moduleDecls.add(module);
        moduleEffectsComp.computeEffects(module);

    }

    public void computeEffects(JCLibraryDecl library) {
        libraryEffectsComp.computeEffects(library);
    }

    LinkedList<JCModuleDecl> moduleDecls = new LinkedList<JCModuleDecl>();

    public void substituteProcEffects(JCSystemDecl system) {

        effectsSub.substituteProcEffects(moduleEffectsComp.methodEffects);
           for (JCModuleDecl module : moduleDecls) 
            reachedProcsComp.computeReachedProcs(module);
           /*for (JCMethodDecl m : moduleEffectsComp.methodEffects.keySet()) {
            System.out.println(m);
            System.out.println(moduleEffectsComp.methodEffects.get(m));
            }*/

    }

    public void substituteLibEffects(JCSystemDecl system) {

        effectsSub.substituteLibEffects(moduleEffectsComp.methodEffects);

        for (JCMethodDecl m : moduleEffectsComp.methodEffects.keySet()) {
            System.out.println(m);
            System.out.println(moduleEffectsComp.methodEffects.get(m));
        }

    }
}