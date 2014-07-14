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
 * Contributor(s): Eric Lin
 */

package org.paninij.systemgraph;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashMap;
import java.util.Stack;

import javax.tools.JavaFileObject;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.util.*;

public class SystemGraph {
	public static class Path{
		public List<Node> nodes;
		public Path() {
			nodes = List.<Node>nil();
		}
		public Path(Node node) {
			nodes = List.<Node>nil();
			nodes = nodes.append(node);
		}
		public Path(Path path){
			nodes = List.<Node>nil();
			nodes = nodes.appendList(path.nodes);
		}
		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}

		public void toString(StringBuilder sb) {
		    sb.append(nodes.get(0).name);
		    final int size = nodes.size();
            for(int i=1; i<size; i++){
                sb.append(" --> ");
                sb.append(nodes.get(i).name);
            }
		}
	}
	public static class Node{
		public Set<MethodSymbol> procedures = new HashSet<MethodSymbol>();
//		public Set<Connection> connections = new HashSet<Connection>();
		public HashMap<Name, Node> connections = new HashMap<Name, Node>();
		public ClassSymbol capsule;//symbol of the capsule instance
		public Name name;//name of the capsule instance
		public int indegree;
		
		Node(Name name, ClassSymbol sym){
			capsule = sym;
			this.name = name;
			for(Symbol s : sym.members().getElements()){
				if(s instanceof MethodSymbol)
					addProc((MethodSymbol)s);
			}
		}
		
		private void addProc(MethodSymbol ms){
			procedures.add(ms);
		}
		
		void addConnection(Name name, Node node){
		    if (node != null){
		        node.indegree++;
		    }
			connections.put(name, node);
		}

		public String toString(){
			String string = capsule.name+" "+ name + " {";
			for(MethodSymbol n : procedures){
				string += n.toString()+",";
			}
			string += "}";
			return string;
		}
	}
//	public static class Connection{
//		public Name name; //alias name of the capsule connected
//		public Node node; //destination of the connection 
//		public Connection(Name name, Node node){
//			this.name = name;
//			this.node = node;
//		}
//	}
	// edge from {fromNode, fromProcedure} to {toNode, toProcedure}
	public static class Edge {
		public final Node fromNode, toNode;
		public final MethodSymbol fromProcedure, toProcedure;
		// the source code postion of this call edge
		public final int pos, line;
		// the source code statement of this call edge
		public final JCMethodInvocation tree;
		// the source file that contains this effect
		public final JavaFileObject source_file;
		
		Edge(Node fromNode, MethodSymbol fromProcedure, Node toNode,
				MethodSymbol toProcedure, int pos, int line,
				JCMethodInvocation tree, JavaFileObject source_file) {
			this.fromNode = fromNode;
			this.fromProcedure = fromProcedure;
			this.toNode = toNode;
			this.toProcedure = toProcedure;
			this.pos = pos;
			this.line = line;
			this.tree = tree;
			this.source_file = source_file;
		}

		public String toString(){
			StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}

		public void toString(StringBuilder sb) {
		    sb.append(fromNode.name); sb.append(".");sb.append(fromProcedure);
		    sb.append(" --> ");
		    sb.append(toNode.name); sb.append("."); sb.append(toProcedure);
		    sb.append("\n");
		}

		public final int hashCode() {
			return fromNode.hashCode() + toNode.hashCode() +
			fromProcedure.hashCode() + toProcedure.hashCode() + pos;
		}

		public final boolean equals(Object obj) {
	        if (obj instanceof Edge) {
	        	Edge other = (Edge)obj;
	        	return fromNode.equals(other.fromNode) &&
	        	toNode.equals(other.toNode) &&
	        	fromProcedure.equals(other.fromProcedure) &&
	        	toProcedure.equals(other.toProcedure) && pos == other.pos;
	        }
	        return false;
	    }
	}
	
	public HashMap<Name, Node> nodes = new HashMap<Name, Node>();
	public Set<Edge> edges = new HashSet<Edge>(); 
	// this is to save size of arrays. maybe view arrays as an whole instead.
	public HashMap<Name, Integer> capsuleArrays = new HashMap<Name, Integer>();
	
	void addNode(Name name, ClassSymbol sym){
	    Assert.check(!nodes.containsKey(name),
	    		"Graph already contains node for " + name);
		nodes.put(name, new Node(name, sym));
	}
	
	void setConnection(Name fromNode, Name alias, Name toNode){
		if(!toNode.toString().equals("null")){
			nodes.get(fromNode).addConnection(alias, nodes.get(toNode));
		}else
			nodes.get(fromNode).addConnection(alias, null);
	}

	void setEdge(Node fromNode, MethodSymbol fromProc, Node toNode,
			MethodSymbol toProc, int pos, int line, JCMethodInvocation tree,
			JavaFileObject source_file) {
		edges.add(new Edge(fromNode, fromProc, toNode, toProc, pos, line,
				tree, source_file));
	}

	public String toString(){
		StringBuilder s = new StringBuilder();
		s.append("Nodes: \n");
		for(Node node : nodes.values()){
			s.append("\t");
			s.append(node);
			s.append("\n");
		}
		s.append("Connections: \n");
		for(Node node : nodes.values()){
		    s.append("\tNode ");
		    s.append(node.name);
		    s.append(":\n");
			for(Entry<Name, Node> c : node.connections.entrySet()){
				s.append("\t\t");
				s.append(c.getKey());
				s.append(" --> ");
				if(c.getValue()!=null)
					s.append(c.getValue().name);
				else
					s.append("null");
				s.append("\n");;
			}
		}
		s.append("Edges: \n");
		for(Edge edge : edges){
			edge.toString(s);
		}
		return s.toString();
	}

	public List<Edge> getEdges(Node head, MethodSymbol fromSym, List<Node> tail) {
		List<Edge> edges = List.<Edge>nil();
		for(Edge e : this.edges){
			if(e.fromNode == head && e.fromProcedure.toString().equals(fromSym.toString()) && e.toNode == tail.head){
				edges = edges.append(e);
			}
		}
		return edges;
	}
	
	public List<Pair<Name, Name>> detectCyclicReferences(Name _this) {
		ListBuffer<Pair<Name, Name>> cycles = new ListBuffer<Pair<Name,Name>>();
		HashMap<Name, Node> visited = new HashMap<Name, Node>();
		Node start = nodes.get(_this);
		if (start == null)
		    return cycles.toList();

		Stack<Node> toVisit = new Stack<Node>();
		toVisit.push(start);
		do {
			boolean newNode = false;
			Node fromNode = toVisit.peek();
			for (Node toNode : fromNode.connections.values()) {
				if (toNode == null)	continue;
				if (visited.containsKey(toNode.name))	continue;
				if (!toVisit.contains(toNode)) {
					toVisit.push(toNode);
					newNode = true;
					break;
				} else {
					cycles.add(new Pair<Name, Name>(fromNode.name, toNode.name));
				}
			}
			if (newNode)	continue;
			// process the node
			Node n = toVisit.pop();
			visited.put(n.name, n);
		} while (!toVisit.isEmpty());

		return cycles.toList();
	}
}
