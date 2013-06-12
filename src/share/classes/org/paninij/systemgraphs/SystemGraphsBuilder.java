package org.paninij.systemgraphs;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.util.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.code.Type.*;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;

import org.paninij.systemgraphs.SystemGraphs.*;

public class SystemGraphsBuilder extends TreeScanner {
    protected static final Context.Key<SystemGraphsBuilder> sgbKey =
        new Context.Key<SystemGraphsBuilder>();

    private final Symtab syms;
    private final Names names;
    private SystemGraphs graphs;
    private Node currentCapsule;

    static class CapsuleNameMap {
        HashMap<String, ArrayList<Node>> nodes 
            = new HashMap<String, ArrayList<Node>>();

        public void put(String name, Node n) {
            ArrayList<Node> nodeArray = new ArrayList<Node>();
            nodeArray.add(n);
            nodes.put(name, nodeArray);
        }

        public Node get(String name) {
            ArrayList<Node> nodeArray = nodes.get(name);
            if (nodeArray.size() == 1) return nodeArray.get(0);
            else return null;
        }

        public void put(String name, ArrayList<Node> nodeArray) {
            nodes.put(name, nodeArray);
        }

        public Node get(String name, int index) {
            ArrayList<Node> nodeArray = nodes.get(name);
            return nodeArray.get(index);
        }

        public int howMany(String name) {
            return nodes.get(name).size();
        }

        public Collection<Node> allNodes() {
            LinkedList<Node> concatenation = new LinkedList<Node>();
            for (ArrayList<Node> nodeArray : nodes.values()) {
                concatenation.addAll(nodeArray);
            }
            return concatenation;
        }
    }

    private CapsuleNameMap capsuleNames;
    private HashSet<NodeMethod> finishedMethods;
    private HashSet<String> systemConstantNames;

    public static SystemGraphsBuilder instance(Context context) {
        SystemGraphsBuilder instance = context.get(sgbKey);
        if (instance == null)
            instance = new SystemGraphsBuilder(context);
        return instance;
    }

    protected SystemGraphsBuilder(Context context) {
        context.put(sgbKey, this);

        syms = Symtab.instance(context);
        names = Names.instance(context);
    }

    public SystemGraphs buildGraphs(JCSystemDecl system) {
        //finishCallGraph();
        graphs = new SystemGraphs();
        capsuleNames = new CapsuleNameMap();
        finishedMethods = new HashSet<NodeMethod>();
        systemConstantNames = new HashSet<String>();

        scan(system.body);

        for (Node capsule : capsuleNames.allNodes()) {
            for (MethodSymbol method : capsule.sym.procedures.keySet()) {
                currentCapsule = capsule;
                traverseCallGraph(method);
            }
        }

		for (Collection<ConnectionEdge> edges : graphs.forwardConnectionEdges.values()) {
			for (ConnectionEdge edge : edges) {
				Node from = edge.from;
				Node to = edge.to;
				from.outdegree++;
				to.indegree++;
			}
		}
		
        //System.out.println(graphs);

        return graphs;
    }

    

    /*public void finishCallGraph() {
        for (CFGNodeBuilder.TodoItem t : CFGNodeBuilder.callGraphTodos) {
            VarSymbol capsuleField = CFGNodeBuilder.capsuleField(t.tree); // doesn't handle capsule array calls
            MethodSymbol methSym = (MethodSymbol)TreeInfo.symbol(t.tree.meth);
            String methodName = TreeInfo.symbol(t.tree.meth).toString();
            methodName = methodName.substring(0, methodName.indexOf("("))+"$Original";
            MethodSymbol origMethSym = (MethodSymbol)((ClassSymbol)methSym.owner).members_field.lookup(names.fromString(methodName)).sym;
            if (origMethSym != null) {
                t.method.sym.calledMethods.add(new MethodSymbol.MethodInfo(origMethSym,
                                                                       capsuleField));
                origMethSym.callerMethods.add(t.method.sym);
            } // otherwise could be an already compiled method like print() or something
        }
        }*/

    private static class NodeMethod {
        Node n; MethodSymbol m;
        public NodeMethod(Node n, MethodSymbol m) { this.n = n; this.m = m; }
        public boolean equals(Object o) {
            if (o instanceof NodeMethod) {
                NodeMethod nm = (NodeMethod)o;
                return nm.n.equals(n) && nm.m.equals(m);
            }
            return false;
        }
        public int hashCode() {
            return n.hashCode() + m.hashCode();
        }
    }
    
    public void traverseCallGraph(MethodSymbol method) {
        if (finishedMethods.contains(new NodeMethod(currentCapsule, method))) return;
        finishedMethods.add(new NodeMethod(currentCapsule, method));

//        System.out.println("===========");
//        System.out.println(currentCapsule + "." + method);

        for (MethodSymbol.MethodInfo calledMethodInfo : method.calledMethods) {
//            System.out.println("----------");
//            System.out.println(calledMethodInfo.method + ":");

            if (calledMethodInfo.capsule != null) {
                for (ConnectionEdge edge : graphs.forwardConnectionEdges.get(currentCapsule)) {
//                    System.out.print(edge);
                    if (edge.varName.equals(calledMethodInfo.capsule.name.toString())) {
//                        System.out.println(" x ");
                        Node calledCapsule = edge.to;
                        graphs.addProcEdge(currentCapsule, calledCapsule,
                                           method, calledMethodInfo.method,
                                           edge.varName);
                    } else if (edge.arrayConnection() 
                               && edge.to.sym.type.toString().equals(calledMethodInfo.capsule.type.toString())) {
//                        System.out.println(" a ");
                        // if it's an array call we do an estimate (add an edge to every reachable capsule of the right type)
                        Node calledCapsule = edge.to;
                        graphs.addProcEdge(currentCapsule, calledCapsule,
                                           method, calledMethodInfo.method,
                                           edge.varName, calledCapsule.index);
                    } else if (edge.arrayConnection() 
                               && calledMethodInfo.capsule.type instanceof ArrayType) {
                        if (edge.to.sym.type.toString().equals(((ArrayType)calledMethodInfo.capsule.type).elemtype.toString())) {
                            Node calledCapsule = edge.to;
                            graphs.addProcEdge(currentCapsule, calledCapsule,
                                               method, calledMethodInfo.method,
                                               edge.varName, calledCapsule.index);
                        }
                    }
                }
            } else {
                // don't need to traverse capsule procedures, since
                // they are starting points
                traverseCallGraph(calledMethodInfo.method);
            }
//            System.out.println("----------");
        }
//        System.out.println("===========");
    }

    

    public void visitVarDef(JCVariableDecl tree) {
        if (tree.vartype.getTag()==Tag.CAPSULEARRAY) {
            JCCapsuleArray type = (JCCapsuleArray)tree.vartype;
            String capsuleName = type.elemtype.toString();
            String varName = tree.name.toString();
            if(syms.capsules.containsKey(names.fromString(capsuleName))) {
                CapsuleSymbol c = syms.capsules.get(names.fromString(capsuleName));
                ArrayList<Node> nodes = new ArrayList<Node>(type.amount);
                for (int i = 0; i < type.amount; i++) {
                    nodes.add(graphs.addCapsule(c, varName, i));
                }
                capsuleNames.put(varName, nodes);
            }
        } else {
            String capsuleName = tree.vartype.toString();
            String varName = tree.name.toString();
            if(syms.capsules.containsKey(names.fromString(capsuleName))) {
                CapsuleSymbol c = syms.capsules.get(names.fromString(capsuleName));
                capsuleNames.put(varName, graphs.addCapsule(c, varName));
            } else {
                systemConstantNames.add(varName);
            }
        }
    }

    // processing capsule connection statements in the system def.
    // Makes connection edges for each capsule connection described in
    // the statement.
    public void visitApply(JCMethodInvocation tree) {
        Node capsule = capsuleNames.get(tree.meth.toString());

        for (int i = 0; i < tree.args.size(); i++) {
            JCExpression arg = tree.args.get(i);
            String name = capsule.sym.capsuleParameters.get(i).name.toString();

            if (arg.getTag()==Tag.IDENT) { // arg could just be some literal; don't need to look at those
                if (!arg.toString().equals("args") // ignore commandline params. This shouldn't be hardcoded like this
                    && !systemConstantNames.contains(arg.toString())) { // make sure the variable isn't a constant
                    int arraySize = capsuleNames.howMany(arg.toString());
                    if (arraySize > 1) { // Connecting a capsule to an array of capsules
                        for (int j = 0; j < arraySize; j++) {
                            Node argArrayCapsule = capsuleNames.get(arg.toString(), j);
                            graphs.addConnectionEdge(capsule, argArrayCapsule, name, j);
                        }
                    } else {
                        // possibly connecting a capsule to another single capsule 
                        Node argCapsule = capsuleNames.get(arg.toString());

                        if (capsule != null)
                            graphs.addConnectionEdge(capsule, argCapsule, name);
                    }
                }
            }
        }
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        final String varName = tree.var.name.toString();
        final String capsuleArrayName = tree.expr.toString();
        if (capsuleNames.howMany(capsuleArrayName) == 0) {
            System.out.println("Illegal foreach loop argument in system decl");
            System.exit(1);
        }
        
        for (int i = 0; i < capsuleNames.howMany(capsuleArrayName); i++) {
            final int j = i;
            new TreeScanner() {
                public void visitApply(JCMethodInvocation tree) {
                    String recipient = tree.meth.toString();
                    Node capsule;
                    if (recipient.equals(varName))
                        capsule = capsuleNames.get(capsuleArrayName, j);
                    else 
                        capsule = capsuleNames.get(recipient);
                    for (int i = 0; i < tree.args.size(); i++) {
                        JCExpression arg = tree.args.get(i);
                        String name = capsule.sym.capsuleParameters.get(i).name.toString();
                        if (arg.getTag()==Tag.IDENT) {
                            Node argCapsule;
                            if (arg.toString().equals(varName))
                                argCapsule = capsuleNames.get(capsuleArrayName, j);
                            else
                                argCapsule = capsuleNames.get(arg.toString());
                            if (capsule != null)
                                graphs.addConnectionEdge(capsule, argCapsule, name);
                        }
                    }
                }
            }.scan(tree.body);
        }
    }

    public void visitIndexedCapsuleWiring(JCCapsuleArrayCall tree) {
        if(tree.index.getTag()!=Tag.LITERAL) { 
            System.out.println("Illegal capsule array call index");
            System.exit(1);
        }
        JCLiteral indexExp = (JCLiteral)tree.index;
        int indexValue = (Integer)indexExp.value;

        Node capsule = capsuleNames.get(tree.name.toString(), indexValue);

        for (int i = 0; i < tree.arguments.size(); i++) {
            JCExpression arg = tree.arguments.get(i);
            String name = capsule.sym.capsuleParameters.get(i).name.toString();

            if (arg.getTag()==Tag.IDENT) { // arg could just be some literal; don't need to look at those
                if (!arg.toString().equals("args") // ignore commandline params. This shouldn't be hardcoded like this
                    && !systemConstantNames.contains(arg.toString())) { // make sure the variable isn't a constant
                    int arraySize = capsuleNames.howMany(arg.toString());
                    if (arraySize > 1) { // Connecting a capsule to an array of capsules
                        for (int j = 0; j < arraySize; j++) {
                            Node argArrayCapsule = capsuleNames.get(arg.toString(), j);
                            graphs.addConnectionEdge(capsule, argArrayCapsule, name, j);
                        }
                    } else {
                        // possibly connecting a capsule to another single capsule 
                        Node argCapsule = capsuleNames.get(arg.toString());

                        if (capsule != null)
                            graphs.addConnectionEdge(capsule, argCapsule, name);
                    }
                }
            }
        }
    }
}