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

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.CapsuleSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

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
			String s = nodes.get(0).name.toString();
			for(int i=1;i<nodes.size();i++){
				s += " --> "+ nodes.get(i).name; 
			}
			return s;
		}
	}
	public static class Node{
		public Set<MethodSymbol> procedures = new HashSet<MethodSymbol>();
//		public Set<Connection> connections = new HashSet<Connection>();
		public HashMap<Name, Node> connections = new HashMap<Name, Node>();
		public CapsuleSymbol capsule;//symbol of the capsule instance
		public Name name;//name of the capsule instance

		Node(Name name, CapsuleSymbol sym){
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
		
		public void addConnection(Name name, Node node){
			connections.put(name, node);
		}
		
		@Override
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
	public static class Edge{//edge from {fromNode, fromProcedure} to {toNode, toProcedure}
		public Node fromNode, toNode;
		public MethodSymbol fromProcedure, toProcedure;
		
		Edge(Node fromNode, MethodSymbol fromProcedure, Node toNode, MethodSymbol toProcedure){
			this.fromNode = fromNode;
			this.fromProcedure = fromProcedure;
			this.toNode = toNode;
			this.toProcedure = toProcedure;
		}
		
		@Override
		public String toString(){
			String s = fromNode.name + "." + fromProcedure + " --> " + toNode.name+"."+ toProcedure+"\n";
			return s;
		}
		
	}
	
	public HashMap<Name, Node> nodes = new HashMap<Name, Node>();
	public Set<Edge> edges = new HashSet<Edge>(); 
	public HashMap<Name, Integer> capsuleArrays = new HashMap<Name, Integer>();//this is to save size of arrays. maybe view arrays as an whole instead.
	
	void addNode(Name name, CapsuleSymbol sym){
		nodes.put(name, new Node(name, sym));
	}
	
	void setConnection(Name fromNode, Name alias, Name toNode){
		nodes.get(fromNode).connections.put(alias, nodes.get(toNode));
	}
	
	void setEdge(Node fromNode, MethodSymbol fromProc, Node toNode, MethodSymbol toProc){
		edges.add(new Edge(fromNode, fromProc, toNode, toProc));
	}
	
	@Override
	public String toString(){
		String s = "Nodes: \n";
		for(Node node : nodes.values()){
			s += "\t"+node+"\n";
		}
		s += "Connections: \n";
		for(Node node : nodes.values()){
			s += "\tNode "+node.name+ ":\n";
			for(Entry<Name, Node> c : node.connections.entrySet()){
				s += "\t\t"+c.getKey() + " --> " + c.getValue().name + "\n"; 
			}
		}
		s += "Edges: \n";
		for(Edge edge : edges){
			s += edge.toString(); 
		}
		return s;
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
}
