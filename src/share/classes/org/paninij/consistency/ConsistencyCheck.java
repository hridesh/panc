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

package org.paninij.consistency;

import com.sun.tools.javac.util.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;

import org.paninij.effects.*;
import org.paninij.systemgraphs.SystemGraphs.*;
import org.paninij.systemgraphs.SystemGraphs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;


public class ConsistencyCheck {
    HashMap<ClassSymbol, HashSet<ClassSymbol>> capsuleEdges;
    HashSet<MethodSymbol> visitedMethods;
    Node currentCapsule;
    SystemGraphs graphs;
//    HashMap<JCMethodDecl, EffectSet> methodEffects = new HashMap<JCMethodDecl, EffectSet>();    
    MethodSymbol currentMethod;
    
    public void checkConsistency(SystemGraphs graphs,
                                 Node capsule) {
        this.graphs = graphs;
        currentCapsule = capsule;

        for (MethodSymbol method : capsule.sym.procedures.keySet()) {
            currentMethod = method;
            checkMethodConsistency(capsule, method);
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

                // this may be a bug
                EffectSet effects = new EffectSet();
//                EffectSet effects = currentMethod.effects;
                for (NodeMethod nm : intersect) {
                    EffectSet e = nm.m.effects;

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