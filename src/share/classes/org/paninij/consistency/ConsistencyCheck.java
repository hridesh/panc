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
    HashMap<ClassSymbol, HashSet<ClassSymbol>> capsuleEdges;
    HashSet<MethodSymbol> visitedMethods;
    Node currentCapsule;
    SystemGraphs graphs;
    HashMap<JCMethodDecl, EffectSet> methodEffects = new HashMap<JCMethodDecl, EffectSet>();    
    MethodSymbol currentMethod;

    public ConsistencyCheck(HashMap<JCMethodDecl, EffectSet> methodEffects) {
        this.methodEffects = methodEffects;
    }
    
    public void checkConsistency(SystemGraphs graphs,
                                 Node capsule) {
        this.graphs = graphs;
        currentCapsule = capsule;

        for (Symbol s : capsule.sym.members_field.getElements()) {
            if (s instanceof MethodSymbol) {
                MethodSymbol method = (MethodSymbol)s;
                if (method.name.toString().contains("$Original") ||
                    (method.name.toString().equals("run") &&
                     capsule.sym.hasRun)
                    ) {
                    currentMethod = method;
                    checkMethodConsistency(capsule, method);
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

    private HashSet<NodeMethod> checkMethodConsistency(Node capsule,
                                                 MethodSymbol method) {
        HashSet<NodeMethod> visitedCapsules = new HashSet<NodeMethod>();
        visitedCapsules.add(new NodeMethod(capsule, method));
        for (ProcEdge edge : edgesFromProc(capsule, method)) {
            HashSet<NodeMethod> subVisitedCapsules = checkMethodConsistency(edge.to,
                                                                           edge.called);
            HashSet<NodeMethod> intersect = nmsIntersect(visitedCapsules, 
                                                         subVisitedCapsules);
//            System.out.println(intersect);
            if (intersect != null) {
                if (intersect.size() == 1) {
                    NodeMethod nm = new NodeMethod(null, null);
                    for (NodeMethod nm1 : intersect) nm = nm1;
                    System.out.println("potential consistency problem starting in " + currentMethod + " ending in " + nm.n + "'s " + nm.m);
//                    System.exit(1);
                }

                EffectSet effects = new EffectSet();
                for (NodeMethod nm : intersect) {
                    EffectSet e = methodEffects.get(nm.m.tree);
//                    System.out.println(effects);
//                    System.out.println(e);
                    if (effects.intersects(e)) {
                        System.out.println("potential consistency problem starting in " + currentMethod + " ending in " + intersect);
//                        System.exit(1);
                    }
                }
            }

            visitedCapsules.addAll(subVisitedCapsules);
        }

        return visitedCapsules;
    }


    private LinkedHashSet<ProcEdge> edgesFromProc(Node capsule, 
                                            MethodSymbol method) {
        LinkedHashSet<ProcEdge> edges = new LinkedHashSet<ProcEdge>();
        for (MethodSymbol.MethodInfo reachedProcInfo : method.reachedProcs) {
            LinkedHashSet<ProcEdge> rpEdges = edgesForReachedProc(capsule, method,
                                                                reachedProcInfo);
            if (rpEdges==null) Assert.error(); // Because every reached proc has to have an edge reaching it, I think
            edges.addAll(rpEdges);
        }
        return edges;
    }

    // could be multiple edges because we have to estimate array call edges
    private LinkedHashSet<ProcEdge> edgesForReachedProc(Node capsule, MethodSymbol method,
                                                       MethodSymbol.MethodInfo reachedProc) {
        
        LinkedHashSet<ProcEdge> edges = new LinkedHashSet<ProcEdge>();
                
        for (ProcEdge edge : graphs.forwardProcEdges.get(capsule)) {
            if (edge.varName.equals(reachedProc.capsule.name.toString())
                && edge.caller == method 
                && edge.called == reachedProc.method) 
                edges.add(edge);
            else if (edge.arrayConnection()
                     && edge.to.sym.type.toString().equals(reachedProc.capsule.type.toString())
                     && edge.caller == method
                     && edge.called == reachedProc.method)
                edges.add(edge);
            else if (edge.arrayConnection()
                     && reachedProc.capsule.type instanceof ArrayType) 
                if (edge.to.sym.type.toString().equals(((ArrayType)reachedProc.capsule.type).elemtype.toString()))
                    edges.add(edge);
        }
        if (edges.size()==0) return null;
        return edges;
    }

    private void visitMethod(MethodSymbol sym) {
        
    }
}