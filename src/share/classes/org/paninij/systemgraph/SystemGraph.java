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

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.*;
import java.util.*;
import com.sun.tools.javac.util.*;

public class SystemGraph {
	public static class Node{
		HashMap<Name, MethodSymbol> procedures = new HashMap<Name, MethodSymbol>();
		HashSet<Connection> connections = new HashSet<Connection>();
		CapsuleSymbol capsule;//symbol of the capsule instance
		Name name;//name of the capsule instance

		Node(Name name, CapsuleSymbol sym){
			capsule = sym;
			this.name = name;
			for(Symbol s : sym.members().getElements()){
				if(s instanceof MethodSymbol)
					addProc((MethodSymbol)s);
			}
		}
		
		private void addProc(MethodSymbol ms){
			procedures.put(ms.name, ms);
		}
		
		public void addConnection(Name name, Node node){
			connections.add(new Connection(name, node));
		}
		
		@Override
		public String toString(){
			String string = capsule.name+" "+ name + " {";
			for(Name n : procedures.keySet()){
				string += n.toString()+",";
			}
			string += "}";
			return string;
		}
	}
	static class Connection{
		Name name; //alias name of the capsule connected
		Node node; //destination of the connection
		public Connection(Name name, Node node){
			this.name = name;
			this.node = node;
		}
	}
	static class Edge{//edge from {fromNode, fromProcedure} to {toNode, toProcedure}
		Node fromNode, toNode;
		Name fromProcedure, toProcedure;
		
		Edge(Node fromNode, Name fromProcedure, Node toNode, Name toProcedure){
			this.fromNode = fromNode;
			this.fromProcedure = fromProcedure;
			this.toNode = toNode;
			this.toProcedure = toProcedure;
		}
	}
	
	HashMap<Name, Node> nodes = new HashMap<Name, Node>();
	HashSet<Edge> edges = new HashSet<Edge>(); 
	
	void addNode(Name name, CapsuleSymbol sym){
		nodes.put(name, new Node(name, sym));
	}
	
	void setConnection(Name fromNode, Name alias, Name toNode){
		nodes.get(fromNode).connections.add(new Connection(alias, nodes.get(toNode)));
	}
	
	@Override
	public String toString(){
		String s = "Nodes: \n";
		for(Node node : nodes.values()){
			s += "\t"+node.toString()+"\n";
		}
		s += "Connections: \n";
		for(Node node : nodes.values()){
			s += "\tNode "+node.name+ ":\n";
			for(Connection c : node.connections){
				s += "\t\t"+c.name + " --> " + c.node.name + "\n"; 
			}
		}
		return s;
	}
}
