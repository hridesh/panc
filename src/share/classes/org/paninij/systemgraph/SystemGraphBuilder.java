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

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Symbol.CapsuleSymbol;
import com.sun.tools.javac.util.*;

public class SystemGraphBuilder {
	Symtab syms;
	Names names;
	Log log;
	
	public SystemGraphBuilder(Symtab syms, Names names, Log log){
		this.syms = syms;
		this.names = names;
		this.log = log;
	}
	
	/**returns an empty systemGraph
	 */
	public SystemGraph createSystemGraph(){
		return new SystemGraph();
	}
	
	/** adds a single Node with the name and capsuleSymbol to graph
	 */
	public void addSingleNode(SystemGraph graph, Name name, final CapsuleSymbol c){
		graph.addNode(name, c);
	}

	/** Adds an array of capsule as multiple nodes.
	 * Name of these nodes are represented as "capsuleName[index]" in the graph
	 */
	public void addMultipleNodes(SystemGraph graph, Name name, int amount, final CapsuleSymbol c) {
		for(int i=0;i<amount;i++){
			graph.addNode(names.fromString(name+"["+i+"]"), c);
		}
	}
	
	public void addConnection(SystemGraph graph, Name fromNode, Name arg, Name toNode){
		graph.setConnection(fromNode, arg, toNode);
	}
}
