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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;

import org.paninij.effects.ASTChainNodeBuilder;
import org.paninij.systemgraphs.SystemGraphs.*;

public class SystemGraphsBuilder extends TreeScanner {
    protected static final Context.Key<SystemGraphsBuilder> sgbKey =
        new Context.Key<SystemGraphsBuilder>();

    private final Symtab syms;
    private final Names names;
    private SystemGraphs graphs;
    private Node currentModule;

    static class ModuleNameMap {
        HashMap<String, ArrayList<Node>> nodes = new HashMap<String, ArrayList<Node>>();
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

    private ModuleNameMap moduleNames;
    private HashSet<NodeMethod> finishedMethods;

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
        finishCallGraph();
        graphs = new SystemGraphs();
        moduleNames = new ModuleNameMap();
        scan(system.body);

        finishedMethods = new HashSet<NodeMethod>();

        for (Node module : moduleNames.allNodes()) {
            for (Symbol s : module.sym.members_field.getElements()) {
                if (s instanceof MethodSymbol) {
                    MethodSymbol method = (MethodSymbol)s;
                    if (method.name.toString().contains("$Original") ||
                        (method.name.toString().equals("run") &&
                         module.sym.hasRun)
                        ) {
                        currentModule = module;
                        traverseCallGraph(method);
                    }
                }
            }
        }

        System.out.println(graphs);

        return graphs;
    }

    public void finishCallGraph() {
        for (ASTChainNodeBuilder.TodoItem t : ASTChainNodeBuilder.callGraphTodos) {
            VarSymbol moduleField = ASTChainNodeBuilder.moduleField(t.tree);
            MethodSymbol methSym = (MethodSymbol)TreeInfo.symbol(t.tree.meth);
            String methodName = TreeInfo.symbol(t.tree.meth).toString();
            methodName = methodName.substring(0, methodName.indexOf("("))+"$Original";
            MethodSymbol origMethSym = (MethodSymbol)((ClassSymbol)methSym.owner).members_field.lookup(names.fromString(methodName)).sym;
            System.out.println("todo " + origMethSym + " " + moduleField);
            t.method.sym.calledMethods.add(new MethodSymbol.MethodInfo(origMethSym,
                                                                       moduleField));
            origMethSym.callerMethods.add(t.method.sym);
        }
    }

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
        if (finishedMethods.contains(new NodeMethod(currentModule, method))) return;
        finishedMethods.add(new NodeMethod(currentModule, method));
        for (MethodSymbol.MethodInfo calledMethodInfo : method.calledMethods) {
            if (calledMethodInfo.module != null) {
                for (ConnectionEdge edge : graphs.forwardConnectionEdges.get(currentModule)) {
                    if (edge.varName.equals(calledMethodInfo.module.name.toString())) {
                        Node calledModule = edge.to;
                        graphs.addProcEdge(currentModule, calledModule,
                                           method, calledMethodInfo.method,
                                           edge.varName);
                    } else if (edge.to.sym.type.toString().equals(calledMethodInfo.module.type.toString())) {
                        Node calledModule = edge.to;
                        graphs.addProcEdge(currentModule, calledModule,
                                           method, calledMethodInfo.method,
                                           edge.varName, calledModule.index);
                    }
                }
            } else {
                // don't need to traverse module procedures, since
                // they are starting points
                traverseCallGraph(calledMethodInfo.method);
            }
        }
    }

    

    public void visitVarDef(JCVariableDecl tree) {
        if (tree.vartype.getTag()==Tag.MODULEARRAY) {
            JCModuleArray type = (JCModuleArray)tree.vartype;
            String moduleName = type.elemtype.toString();
            String varName = tree.name.toString();
            if(syms.modules.containsKey(names.fromString(moduleName))) {
                ClassSymbol c = syms.modules.get(names.fromString(moduleName));
                ArrayList<Node> nodes = new ArrayList<Node>(type.amount);
                for (int i = 0; i < type.amount; i++) {
                    nodes.add(graphs.addModule(c, varName, i));
                }
                moduleNames.put(varName, nodes);
            }
        } else {
            String moduleName = tree.vartype.toString();
            String varName = tree.name.toString();
            if(syms.modules.containsKey(names.fromString(moduleName))) {
                ClassSymbol c = syms.modules.get(names.fromString(moduleName));
                moduleNames.put(varName, graphs.addModule(c, varName));
            }
        }
    }

    // processing module connection statements in the system def.
    // Makes connection edges for each module connection described in
    // the statement.
    public void visitApply(JCMethodInvocation tree) {
        Node module = moduleNames.get(tree.meth.toString());

        for (int i = 0; i < tree.args.size(); i++) {
            JCExpression arg = tree.args.get(i);
            String name = ((JCModuleDecl)module.sym.tree).params.get(i).name.toString();

            if (arg.getTag()==Tag.IDENT) { // arg could just be some constant; don't need to look at those

                int arraySize = moduleNames.howMany(arg.toString());
                if (arraySize > 1) { // Connecting a module to an array of modules
                    for (int j = 0; j < arraySize; j++) {
                        Node argArrayModule = moduleNames.get(arg.toString(), j);
                        graphs.addConnectionEdge(module, argArrayModule, name, j);
                    }
                } else {
                    // possibly connecting a module to another single module 
                    Node argModule = moduleNames.get(arg.toString());

                    if (module != null)
                        graphs.addConnectionEdge(module, argModule, name);
                }
            }
        }
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        final String varName = tree.var.name.toString();
        final String moduleArrayName = tree.expr.toString();
        if (moduleNames.howMany(moduleArrayName) == 0) {
            System.out.println("Illegal foreach loop argument in system decl");
            System.exit(1);
        }
        
        for (int i = 0; i < moduleNames.howMany(moduleArrayName); i++) {
            final Node module = moduleNames.get(moduleArrayName, i);
            final int j = i;
            new TreeScanner() {
                public void visitApply(JCMethodInvocation tree) {
                    String recipient = tree.meth.toString();
                    Node module;
                    if (recipient.equals(varName))
                        module = moduleNames.get(moduleArrayName, j);
                    else 
                        module = moduleNames.get(recipient);
                    for (int i = 0; i < tree.args.size(); i++) {
                        JCExpression arg = tree.args.get(i);
                        String name = ((JCModuleDecl)module.sym.tree).params.get(i).name.toString();
                        if (arg.getTag()==Tag.IDENT) {
                            Node argModule;
                            if (arg.toString().equals(varName))
                                argModule = moduleNames.get(moduleArrayName, j);
                            else
                                argModule = moduleNames.get(arg.toString());
                            if (module != null)
                                graphs.addConnectionEdge(module, argModule, name);
                        }
                    }
                }
            }.scan(tree.body);
        }
    }
    public void visitModuleArrayCall(JCModuleArrayCall tree) { 
        if(tree.index.getTag()!=Tag.LITERAL) { 
            System.out.println("Illegal module array call index");
            System.exit(1);
        }
        JCLiteral indexExp = (JCLiteral)tree.index;
        int indexValue = (Integer)indexExp.value;

        Node module = moduleNames.get(tree.name.toString(), indexValue);
    }
}