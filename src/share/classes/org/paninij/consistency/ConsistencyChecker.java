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
import org.paninij.systemgraph.SystemGraph.Edge;
import org.paninij.systemgraph.SystemGraph.Node;
import org.paninij.systemgraph.SystemGraph.Path;
import org.paninij.systemgraph.SystemGraph.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symbol;
import org.paninij.effects.analysis.*;

public class ConsistencyChecker {

	SystemGraph graph;
	Log log;
	public ConsistencyChecker(SystemGraph graph, Log log){
		this.graph = graph;
		this.log = log;
	}
	
	HashSet<Node> visitedNode;
	Node headNode;
	HashMap<Node, HashSet<Path>> paths;
	List<HashSet<Path>> pathCandidates = List.<HashSet<Path>>nil();
	/**Do the first part of the analysis.
	 * - this checks if there are more than one simple paths between any two vertices of the graph.
	 */
	public void potentialPathCheck(){
		for(Node node : graph.nodes.values()){
			visitedNode = new HashSet<Node>();
			headNode = node;
			paths = new HashMap<Node, HashSet<Path>>();
			Path currentPath;
			currentPath = new Path();
			if(traverse(node, currentPath)){
				Iterator<Entry<Node, HashSet<Path>>> iter = paths.entrySet().iterator();
				while(iter.hasNext()){
					Entry<Node, HashSet<Path>> entry = iter.next();
					if(entry.getValue().size()>1){
//						System.out.println("Multiple paths found from node "+headNode.name+ " to "+entry.getKey().name);
						HashSet<Path> set = new HashSet<Path>();
						for(Path path : entry.getValue()){
//							System.out.println(path);
							set.add(path);
						}
						pathCandidates = pathCandidates.append(set);
					}
				}
			}
		}
		pathCheck();
	}
	
	class Report{
		Node startingNode;
		List<Edge> startingCalls;
	}
	
	
	Report currentReport;
	List<MethodSymbol> endingProcedure;
	HashMap<MethodSymbol, HashSet<Edge>> firstCall;
	/**
	 * Find the actual paths for set of potential paths, and see if potential trouble exists. 
	 */
	private void pathCheck(){
		for(HashSet<Path> paths : pathCandidates){//for each sets of path
			currentReport = new Report();
			endingProcedure = List.<MethodSymbol>nil();
			firstCall = new HashMap<MethodSymbol, HashSet<Edge>>();
			write = new HashMap<String, HashSet<Edge>>();
			pathCheck(paths);//will have a list of EffectSets in endEffects after this is called.
//			for(EffectSet es : endEffects){
//				es.printEffect();
//			}
			checkEffects();
		}
	}
	
//	HashMap<String, List<MethodSymbol>> write;//fields being written from
	HashMap<String, HashSet<Edge>> write;
	
	private void checkEffects(){
		if(endingProcedure.size()>1){
			for(MethodSymbol es : endingProcedure){
				for(EffectEntry entry :es.ars.write){
					if(entry instanceof FieldEffect){
						String field = ((FieldEffect) entry).f.name.toString();
						if(write.containsKey(field)){
							write.get(field).addAll(firstCall.get(es)); 
//							printWarning(field, write.get(((FieldEffect) entry).f.name.toString()), (CapsuleSymbol)es.owner);
							printWarning(write.get(field), currentReport.startingNode);
						}else{
							HashSet<Edge> edges = new HashSet<Edge>();
							edges.addAll(firstCall.get(es));
							write.put(field, edges);
						}
					}
				}
				for(EffectEntry entry :es.ars.read){
					if(entry instanceof FieldEffect){
						String field = ((FieldEffect) entry).f.name.toString();
						if(write.containsKey(field)){
							//add method to the list with out creating redundant entries.
//							printWarning(field, write.get(((FieldEffect) entry).f.name.toString()), (CapsuleSymbol)es.owner);
							printWarning(write.get(field), currentReport.startingNode);
						}
					}
				}
			}
		}
	}
	
	private void printWarning(HashSet<Edge> hashSet, Node startingNode) {
		String set = "";
		for(Edge edge : hashSet){
			set += edge.toNode.name;
			set += "." + edge.toProcedure +", ";
		}
		if(set.length()>0)
			set = set.substring(0, set.length()-2);
		log.warning("sequential.inconsistency.warning", set, startingNode.capsule.parentCapsule.name);
	}
	
	private void pathCheck(HashSet<Path> paths){
		for(Path path : paths){//for each path
			currentReport.startingNode = path.nodes.head;
			getActualPaths(path.nodes);
		}
	}
	
	private Edge currentStartingCall;
	private void getActualPaths(List<Node> path){
//		if(path.tail.isEmpty())
//			//path with only one node.
//			;
		for(MethodSymbol sym : path.head.procedures){//can add restrictions to filter out unnecessary methods.?
			List<Edge> edges = graph.getEdges(path.head, sym, path.tail);
			for(Edge e : edges){
				currentStartingCall = e;
				getActualPaths(e, path.tail);
			}
		}
	}
	
	private void getActualPaths(Edge edge, List<Node> path) {
		if(path.tail.isEmpty()){//end of path
 			for(MethodSymbol m : path.head.procedures){
				if(m.toString().equals(edge.toProcedure.toString()))
					if(m.ars!=null){
						if(firstCall.containsKey(m))
							firstCall.get(m).add(currentStartingCall);
						else{
							HashSet<Edge> e = new HashSet<Edge>();
							e.add(currentStartingCall);
							firstCall.put(m, e);
						}
						endingProcedure = endingProcedure.append(m);
					}
			}
		}else{
			List<Edge> edges = graph.getEdges(edge.toNode, edge.toProcedure, path.tail);
			for(Edge e : edges){
				getActualPaths(e, path.tail);
			}
		}
	}

	private boolean traverse(Node node, final Path currentPath){
		Path newPath = new Path(currentPath);
		newPath.nodes = newPath.nodes.append(node);
		if(paths.containsKey(node))
			paths.get(node).add(newPath);
		else{
			HashSet<Path> hs = new HashSet<Path>();
			hs.add(newPath);
			paths.put(node, hs);
		}
		if(!visitedNode.add(node)){
			return true;
		}
		if(node.connections.isEmpty())
			return false;
		boolean found = false;
		for(Entry<Name, Node> co : node.connections.entrySet()){
			found |= traverse(co.getValue(), newPath);
		}
		return found;
	}
}
