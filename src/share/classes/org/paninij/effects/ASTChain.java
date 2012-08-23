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
import java.util.HashSet;
import java.util.LinkedList;

public class ASTChain {
    private HashMap<JCTree, ASTChainNode> nodes = new HashMap<JCTree, ASTChainNode>();
    public ASTChainNode startNode;
    public HeapRepresentation endHeapRepresentation;
    public LinkedList<ASTChainNode> nodesInOrder = new LinkedList<ASTChainNode>();

    public ASTChainNode nodeForTree(JCTree tree) { return nodes.get(tree); }
    
    public void add(ASTChainNode n) { nodes.put(n.tree, n); nodesInOrder.add(n); };
}