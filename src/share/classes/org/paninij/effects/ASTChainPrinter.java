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
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.code.Type.*;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.HashMap;
import java.util.LinkedList;

public class ASTChainPrinter {

    public void printChain(CFG chain) {
        LinkedList<CFGNode> nodesToProcess = new LinkedList<CFGNode>(chain.nodesInOrder);
        
        System.out.println("digraph G {");

        while (!nodesToProcess.isEmpty()) {
            CFGNode node = nodesToProcess.poll();
            
            for (CFGNode next : node.next) {
                System.out.println(nodeText(node) + " -> " + nodeText(next));
            }
        }

        System.out.println("}");
    }

    public String nodeText(CFGNode node) {
        return "\"" + node.id + " " + node.tree.toString().replace("\"", "\\\"") + (node.lhs ? ", lhs" : "") + "\"";
    }
}