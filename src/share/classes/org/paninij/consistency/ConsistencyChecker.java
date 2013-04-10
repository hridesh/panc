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
package org.paninij.consistency;

import org.paninij.systemgraph.*;
import org.paninij.systemgraph.SystemGraph.*;
import java.util.*;

public class ConsistencyChecker {

	SystemGraph graph;
	public ConsistencyChecker(SystemGraph graph){
		this.graph = graph;
	}
	
	HashSet<Node> visitedNode;
	Node headNode;
	/**Do the first part of the analysis.
	 * - this checks if there are more than one simple paths between any two vertices of the graph.
	 */
	public void potentialPathCheck(){
		for(Node node : graph.nodes.values()){
			visitedNode = new HashSet<Node>();
			headNode = node;
			traverse(node);
		}
	}
	
	private boolean traverse(Node node){
		if(!visitedNode.add(node)){
			System.out.println("Multiple paths found from node "+headNode.name+ " to "+node.name);
			return true;
		}
		
		if(node.connections.isEmpty())
			return false;
		boolean found = false;
		for(Connection co : node.connections){
			found |= traverse(co.node);
		}
		return found;
	}
}
