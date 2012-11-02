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
    public static class Node { // a module instance
        public ClassSymbol sym; public String name; public int index;
        public Node(ClassSymbol sym, String name) { this.sym = sym; this.name = name; this.index = -1; }
        public Node(ClassSymbol sym, String name, int index) { this.sym = sym; this.name = name; this.index = index; }
        public String toString() { 
            if (index==-1) return sym + " " + name;
            else return sym + " " + name + "[" + index + "]";
        }
    }
    public static class ProcEdge {
        public String varName;
        public MethodSymbol caller, called;
        public Node from, to;
        public int index;
        public ProcEdge(Node from, Node to, 
                        MethodSymbol caller,
                        MethodSymbol called,
                        String varName, int index) {
            this.from = from; this.to = to; 
            this.caller = caller; this.called = called;
            this.varName = varName;
            this.index = index;
        }

        public ProcEdge(Node from, Node to, 
                        MethodSymbol caller,
                        MethodSymbol called,
                        String varName) {
            this.from = from; this.to = to; 
            this.caller = caller; this.called = called;
            this.varName = varName;
            this.index = -1;
        }
        public String toString() {
            return "\"" + from + "\" -> \"" + to + "\" [label=" + called + "]";
        }
    }
    public static class ConnectionEdge {
        public String varName;
        public int index;
        public Node from, to;
        public ConnectionEdge(Node from, Node to, String varName) { this.from = from; this.to = to; this.varName = varName; index = -1;}
        public ConnectionEdge(Node from, Node to, String varName, int index) { this.from = from; this.to = to; this.varName = varName; this.index = index;}
        public String toString() { return "\"" + from + "\" -> \"" + to + "\""; }
    }

    public HashMap<Node, HashSet<ProcEdge>> forwardProcEdges = new HashMap<Node, HashSet<ProcEdge>>();
    public HashMap<Node, HashSet<ConnectionEdge>> forwardConnectionEdges = new HashMap<Node, HashSet<ConnectionEdge>>();

    public Node addModule(ClassSymbol sym, String name) {
        Node n = new Node(sym, name);
        forwardProcEdges.put(n, new HashSet<ProcEdge>());
        forwardConnectionEdges.put(n, new HashSet<ConnectionEdge>());
        return n;
    }

    public Node addModule(ClassSymbol sym, String name, int i) {
        Node n = new Node(sym, name, i);
        forwardProcEdges.put(n, new HashSet<ProcEdge>());
        forwardConnectionEdges.put(n, new HashSet<ConnectionEdge>());
        return n;
    }

    public void addConnectionEdge(Node from, Node to, String name) {
        forwardConnectionEdges.get(from).add(new ConnectionEdge(from, to, name));
    }

    public void addConnectionEdge(Node from, Node to, String name, int index) {
        forwardConnectionEdges.get(from).add(new ConnectionEdge(from, to, name, index));
    }

    public void addProcEdge(Node from, Node to, MethodSymbol caller, 
                            MethodSymbol called, String varName) {
        forwardProcEdges.get(from).add(new ProcEdge(from, to, caller, called, 
                                                    varName));
    }

        public void addProcEdge(Node from, Node to, MethodSymbol caller, 
                                MethodSymbol called, String varName, int index) {
        forwardProcEdges.get(from).add(new ProcEdge(from, to, caller, called, 
                                                    varName, index));
    }

    public String toString() {
        String returnValue = "digraph C {\n";
        for (Collection<ConnectionEdge> edges : forwardConnectionEdges.values()) {
            for (ConnectionEdge edge : edges) {
                returnValue += edge + "\n";
            }
        }
        returnValue += "}\n";

        returnValue += "digraph P {\n";
        for (Collection<ProcEdge> edges : forwardProcEdges.values()) {
            for (ProcEdge edge : edges) {
                returnValue += edge + "\n";
            }
        }
        returnValue += "}";
        return returnValue;
    }
}