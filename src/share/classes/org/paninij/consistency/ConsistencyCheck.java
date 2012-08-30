package org.paninij.consistency;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.code.Type.*;
import static com.sun.tools.javac.code.Flags.*;

import org.paninij.effects.*;
import org.paninij.systemgraphs.SystemGraphs.*;
import org.paninij.systemgraphs.SystemGraphs;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;


public class ConsistencyCheck {
    HashMap<ClassSymbol, HashSet<ClassSymbol>> moduleEdges;
    HashSet<MethodSymbol> visitedMethods;
    Node currentModule;
    SystemGraphs graphs;
    HashMap<JCMethodDecl, EffectSet> methodEffects = new HashMap<JCMethodDecl, EffectSet>();    
    MethodSymbol currentMethod;

    public ConsistencyCheck(HashMap<JCMethodDecl, EffectSet> methodEffects) {
        this.methodEffects = methodEffects;
    }
    
    public void checkConsistency(SystemGraphs graphs,
                                 Node module) {
        this.graphs = graphs;
        currentModule = module;

        for (Symbol s : module.sym.members_field.getElements()) {
            if (s instanceof MethodSymbol) {
                MethodSymbol method = (MethodSymbol)s;
                if (method.name.toString().contains("$Original") ||
                    (method.name.toString().equals("run") &&
                     module.sym.hasRun)
                    ) {
                    currentMethod = method;
                    checkMethodConsistency(module, method);
                }
            }
        }
    }

    int currentBranch;

    private static class NodeMethod {
        Node n;
        MethodSymbol m;
        public NodeMethod(Node n, MethodSymbol m) { this.n = n; this.m = m; }
        public boolean equals(Object o) {
            if (o instanceof NodeMethod) {
                NodeMethod nm = (NodeMethod)o;
                return nm.n.equals(this.n) && nm.m.equals(this.m);
            } return false;
        }
        public int hashCode() {
            return n.hashCode() + m.hashCode();
        }
    }

    private HashSet<NodeMethod> nmsIntersect(HashSet<NodeMethod> nms1,
                                             HashSet<NodeMethod> nms2) {
        for (NodeMethod nm1 : nms1) {
            for (NodeMethod nm2 : nms2) {
                if (nm1.n.equals(nm2.n)) {
                    HashSet<NodeMethod> s = new HashSet<NodeMethod>();
                    s.add(nm1); s.add(nm2);
                    return s;
                }
            }
        }
        return null;
    }

    private HashSet<NodeMethod> checkMethodConsistency(Node module,
                                                 MethodSymbol method) {
        HashSet<NodeMethod> visitedModules = new HashSet<NodeMethod>();
        visitedModules.add(new NodeMethod(module, method));
        for (ProcEdge edge : edgesFromProc(module, method)) {
            System.out.println(": " + edge);
            HashSet<NodeMethod> subVisitedModules = checkMethodConsistency(edge.to,
                                                                           edge.called);
            HashSet<NodeMethod> intersect = nmsIntersect(visitedModules, 
                                                         subVisitedModules);
            System.out.println(intersect);
            if (intersect != null) {
                if (intersect.size() == 1) {
                    NodeMethod nm = new NodeMethod(null, null);
                    for (NodeMethod nm1 : intersect) nm = nm1;
                    System.out.println("potential consistency problem starting in " + currentMethod + " ending in " + nm.n + "'s " + nm.m);
                    System.exit(1);
                }

                EffectSet effects = new EffectSet();
                for (NodeMethod nm : intersect) {
                    EffectSet e = methodEffects.get(nm.m.tree);
                    System.out.println(effects);
                    System.out.println(e);
                    if (effects.intersects(e)) {
                        System.out.println("potential consistency problem starting in " + currentMethod + " ending in " + intersect);
                        System.exit(1);
                    }
                }
            }

            visitedModules.addAll(subVisitedModules);
        }

        return visitedModules;
    }


    private LinkedHashSet<ProcEdge> edgesFromProc(Node module, 
                                            MethodSymbol method) {
        LinkedHashSet<ProcEdge> edges = new LinkedHashSet<ProcEdge>();
        for (MethodSymbol.MethodInfo reachedProcInfo : method.reachedProcs) {
            ProcEdge edge = edgeForReachedProc(module, method,
                                               reachedProcInfo);
            if (edge==null) Assert.error();
            edges.add(edge);
        }
        return edges;
    }

    private ProcEdge edgeForReachedProc(Node module, MethodSymbol method,
                                        MethodSymbol.MethodInfo reachedProc) {
        for (ProcEdge edge : graphs.forwardProcEdges.get(module)) {
            if (edge.varName.equals(reachedProc.module.name.toString())
                && edge.caller == method 
                && edge.called == reachedProc.method)
                return edge;
        }
        return null;
    }

    private void visitMethod(MethodSymbol sym) {
        
    }

    

}