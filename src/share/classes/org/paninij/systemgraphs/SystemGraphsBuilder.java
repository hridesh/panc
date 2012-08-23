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
import java.util.HashSet;

import org.paninij.effects.ASTChainNodeBuilder;
import org.paninij.systemgraphs.SystemGraphs.*;

public class SystemGraphsBuilder extends TreeScanner {
    protected static final Context.Key<SystemGraphsBuilder> sgbKey =
        new Context.Key<SystemGraphsBuilder>();

    private final Symtab syms;
    private final Names names;
    private SystemGraphs graphs;
    private Node currentModule;

    private HashMap<String, Node> moduleNames;

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
        moduleNames = new HashMap<String, Node>();
        scan(system.body);

        for (Node module : moduleNames.values()) {
            System.out.println("in " + module.sym);
            for (Symbol s : module.sym.members_field.getElements()) {
                if (s instanceof MethodSymbol) {
                    MethodSymbol method = (MethodSymbol)s;
                    System.out.println("traversing " + method);
                    if (method.name.toString().contains("$Original") ||
                        (method.name.toString().equals("run") &&
                         module.sym.hasRun)
                        ) {
                        System.out.println("asdf");
                        currentModule = module;
                        traverseCallGraph(method);
                        System.out.println("/asdf");
                    }
                }
            }
        }

//        System.out.println(graphs);
        return graphs;
    }

    public void finishCallGraph() {
        for (ASTChainNodeBuilder.TodoItem t : ASTChainNodeBuilder.callGraphTodos) {
            VarSymbol moduleField = ASTChainNodeBuilder.moduleField(t.tree);
            MethodSymbol methSym = (MethodSymbol)TreeInfo.symbol(t.tree.meth);
            String methodName = TreeInfo.symbol(t.tree.meth).toString();
            methodName = methodName.substring(0, methodName.indexOf("("))+"$Original";
            MethodSymbol origMethSym = (MethodSymbol)((ClassSymbol)methSym.owner).members_field.lookup(names.fromString(methodName)).sym;
            t.method.sym.calledMethods.add(new MethodSymbol.MethodInfo(origMethSym,
                                                                       moduleField));
            origMethSym.callerMethods.add(t.method.sym);
        }
    }
    
    public void traverseCallGraph(MethodSymbol method) {
        System.out.println("called methods: " + method.calledMethods);
        for (MethodSymbol.MethodInfo calledMethodInfo : method.calledMethods) {
            if (calledMethodInfo.module != null) {
                for (ConnectionEdge edge : graphs.forwardConnectionEdges.get(currentModule)) {
                    if (edge.varName.equals(calledMethodInfo.module.name.toString())) {
                        Node calledModule = edge.to;
                        System.out.println("called method " + calledMethodInfo.method);
                        System.out.println("called module " + calledModule);
                    }
                }
            }
        }
    }

    

    public void visitVarDef(JCVariableDecl tree) {
        String moduleName = tree.vartype.toString();
        String varName = tree.name.toString();
        if(syms.modules.containsKey(names.fromString(moduleName))) {
            ClassSymbol c = syms.modules.get(names.fromString(moduleName));
            moduleNames.put(varName, graphs.addModule(c, varName));
        }
    }

    public void visitApply(JCMethodInvocation tree) {
        Node module = moduleNames.get(tree.meth.toString());
        for (int i = 0; i < tree.args.size(); i++) {
            JCExpression arg = tree.args.get(i);
            String name = ((JCModuleDecl)module.sym.tree).params.get(i).name.toString();
            if (arg.getTag()==Tag.IDENT) {
                Node argModule = moduleNames.get(arg.toString());
                if (module != null)
                    graphs.addConnectionEdge(module, argModule, name);
            }
        }
    }

/*    public void visitForeachLoop(JCEnhancedForLoop tree) {

      }*/
/*    public void visitModuleArrayCall(JCModuleArrayCall tree) { 
        
      }*/
}