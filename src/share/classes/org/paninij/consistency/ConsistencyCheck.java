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

import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;


public class ConsistencyCheck {
    HashMap<ClassSymbol, HashSet<ClassSymbol>> moduleEdges;
    HashSet<MethodSymbol> visitedMethods;
    ClassSymbol currentModule;

    
    public void checkConsistency(LinkedList<JCMethodDecl> methods) {
        for (JCMethodDecl method : methods) {
            moduleEdges = new HashMap<ClassSymbol, HashSet<ClassSymbol>>();
            visitedMethods = new HashSet<MethodSymbol>();

            currentModule = (ClassSymbol)method.sym.owner;
            visitMethod(method.sym);
        }
    }

    private void visitMethod(MethodSymbol sym) {
        visitedMethods.add(sym);

                
        for (MethodSymbol calledMethod : sym.calledMethods) {
            if ((calledMethod.flags() & PRIVATE) == 0 && calledMethod.owner.isModule && calledMethod.owner != currentModule) {
                
            }

            if (!visitedMethods.contains(calledMethod)) {
                visitMethod(calledMethod);
            }
        }
    }

}