package org.paninij.systemgraphs;

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

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Collection;
import java.util.HashSet;

// encapsulates two graphs, one with system-specified module connections as
// edges, and one with inter-procedure calls as edges. Both graphs use
// system-specified module instances as nodes.
public class SystemGraphs {
    static class Node { // a module instance
        ClassSymbol sym; String name;
        public Node(ClassSymbol sym, String name) { this.sym = sym; this.name = name; }
        public String toString() { return sym + " " + name; }
    }
    static class ProcEdge {
        MethodSymbol caller, called;
        Node from, to;
    }
    static class ConnectionEdge {
        String varName;
        Node from, to;
        public ConnectionEdge(Node from, Node to, String varName) { this.from = from; this.to = to; this.varName = varName; }
        public String toString() { return from + " -> " + to; }
    }

    HashMap<Node, HashSet<ProcEdge>> forwardProcEdges = new HashMap<Node, HashSet<ProcEdge>>();
    HashMap<Node, HashSet<ConnectionEdge>> forwardConnectionEdges = new HashMap<Node, HashSet<ConnectionEdge>>();

    public Node addModule(ClassSymbol sym, String name) {
        Node n = new Node(sym, name);
        forwardProcEdges.put(n, new HashSet<ProcEdge>());
        forwardConnectionEdges.put(n, new HashSet<ConnectionEdge>());
        return n;
    }

    public void addConnectionEdge(Node from, Node to, String name) {
        forwardConnectionEdges.get(from).add(new ConnectionEdge(from, to, name));
    }

    public String toString() {
        String returnValue = "digraph G {\n";
        for (Collection<ConnectionEdge> edges : forwardConnectionEdges.values()) {
            for (ConnectionEdge edge : edges) {
                returnValue += edge + "\n";
            }
        }
        returnValue += "}";
        return returnValue;
    }
}