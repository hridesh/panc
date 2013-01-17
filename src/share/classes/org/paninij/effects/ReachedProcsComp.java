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

// computes each method's reached (possibly) intercapsule procedure calls 
public class ReachedProcsComp {
     protected static final Context.Key<ReachedProcsComp> rpcKey =
        new Context.Key<ReachedProcsComp>();

    private ClassSymbol currentCapsule;
    private LinkedList<JCMethodDecl> methodsToProcess;

    public static ReachedProcsComp instance(Context context) {
        ReachedProcsComp instance = context.get(rpcKey);
        if (instance == null)
            instance = new ReachedProcsComp(context);
        return instance;
    }

    protected ReachedProcsComp(Context context) {
        context.put(rpcKey, this);
        methodsToProcess = new LinkedList<JCMethodDecl>();
        visitedMethods = new HashSet<MethodSymbol>();
    }

    // For each procedure call/run method, traverse call graph and make todo queue.
    // Called in CapsuleEffectsComp
    public void todoReachedProcs(JCCapsuleDecl capsule) {
        for(JCTree def : capsule.defs) {
        	if(def.getTag() == Tag.METHODDEF) {
                JCMethodDecl method = (JCMethodDecl)def;
                if ((method.sym.flags() & PRIVATE) != 0 ||
                    (method.sym.name.toString().equals("run") &&
                     capsule.sym.hasRun)) { // is a procedure call or the run method
                    methodsToProcess.add(method);
                    traverseCallGraph(method.sym);
                }
            }
        }
    }
    
    // Process the todo queue. 
    // Called in Attr.visitSystemDef -> SystemEffectsComp.substituteProcEffects
    public void computeReachedProcs(JCCapsuleDecl capsule) {
        currentCapsule = capsule.sym;

        while (!methodsToProcess.isEmpty()) {
            JCMethodDecl method = methodsToProcess.poll();
            int oldSize = method.sym.reachedProcs.size();

            for (MethodSymbol.MethodInfo calledMethodInfo : method.sym.calledMethods) {
                MethodSymbol calledMethod = calledMethodInfo.method;

                if (calledMethod.tree == null) continue;
                if (calledMethodInfo.capsule != null)
                    method.sym.reachedProcs.add(calledMethodInfo);
            }

            if (method.sym.reachedProcs.size() != oldSize) {
                for (MethodSymbol callerMethod : method.sym.callerMethods) {
                    methodsToProcess.add(callerMethod.tree);
                }
            }
        }
    }

    // traverses intracapsule call graph, adding methods to a todo queue
    HashSet<MethodSymbol> visitedMethods = new HashSet<MethodSymbol>();
    private void traverseCallGraph(MethodSymbol method) {
        visitedMethods.add(method);

        for (MethodSymbol.MethodInfo calledMethodInfo : method.calledMethods) {
            if (calledMethodInfo.method.tree == null) continue; // why?
            if (calledMethodInfo.capsule == null) { // keep on going if not a capsule call
                if (!visitedMethods.contains(calledMethodInfo.method)) {
                    methodsToProcess.add(calledMethodInfo.method.tree);
                    traverseCallGraph(calledMethodInfo.method);
                }
            }
        }
    }
}