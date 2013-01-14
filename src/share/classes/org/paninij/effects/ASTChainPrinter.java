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
        LinkedList<ASTChainNode> nodesToProcess = new LinkedList<ASTChainNode>(chain.nodesInOrder);
        
        System.out.println("digraph G {");

        while (!nodesToProcess.isEmpty()) {
            ASTChainNode node = nodesToProcess.poll();
            
            for (ASTChainNode next : node.next) {
                System.out.println(nodeText(node) + " -> " + nodeText(next));
            }
        }

        System.out.println("}");
    }

    public String nodeText(ASTChainNode node) {
        return "\"" + node.id + " " + node.tree.toString().replace("\"", "\\\"") + (node.lhs ? ", lhs" : "") + "\"";
    }
}