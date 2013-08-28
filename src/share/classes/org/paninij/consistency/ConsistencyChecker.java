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
 * Contributor(s): Eric Lin, Yuheng Long
 */

package org.paninij.consistency;

import org.paninij.systemgraph.*;
import org.paninij.systemgraph.SystemGraph.Edge;
import org.paninij.systemgraph.SystemGraph.Node;
import org.paninij.systemgraph.SystemGraph.Path;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

import org.paninij.effects.*;

public class ConsistencyChecker {
	private SystemGraph graph;
	private Log log;


	public ConsistencyChecker(SystemGraph graph, Log log) {
		this.graph = graph;
		this.log = log;
	}
	
	HashSet<Node> visitedNode;
	Node headNode;
	HashMap<Node, HashSet<Path>> paths;
	List<HashSet<Path>> pathCandidates = List.<HashSet<Path>>nil();

	/** Do the first part of the analysis.
	 * This checks if there are more than one simple paths between any two
	 * vertices of the graph.
	 */
	public void potentialPathCheck() {
		for (Node node : graph.nodes.values()) {
			visitedNode = new HashSet<Node>();
			headNode = node;
			paths = new HashMap<Node, HashSet<Path>>();
			Path currentPath = new Path();
			if (traverse(node, currentPath)) {
				Iterator<Entry<Node, HashSet<Path>>> iter =
					paths.entrySet().iterator();
				while (iter.hasNext()) {
					Entry<Node, HashSet<Path>> entry = iter.next();
					if (entry.getValue().size() > 1) {
						HashSet<Path> set = new HashSet<Path>();
						for (Path path : entry.getValue()) {
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
	 * Find the actual paths for set of potential paths, and see if potential
	 * trouble exists. 
	 */
	private void pathCheck() {
		// for each sets of path
		for (HashSet<Path> paths : pathCandidates) {
			currentReport = new Report();
			endingProcedure = List.<MethodSymbol>nil();
			firstCall = new HashMap<MethodSymbol, HashSet<Edge>>();
			write = new HashMap<String, HashSet<Edge>>();
			read = new HashMap<String, HashSet<Edge>>();
			// Will have a list of EffectSets in endEffects after this is
			// called.
			pathCheck(paths);
			checkEffects(paths);
		}
	}
	
	private HashMap<String, HashSet<Edge>> write;
	private HashMap<String, HashSet<Edge>> read;
	
	private void checkEffects(HashSet<Path> paths) {
		if (endingProcedure.size() > 1) {
			for (MethodSymbol es : endingProcedure) {
				if(es.effect.isBottom){
					HashSet<Edge> edges = new HashSet<Edge>();
					edges.addAll(firstCall.get(es));
					printWarning(edges, currentReport.startingNode);
				}
				for (EffectEntry entry :es.effect.read) {
					if (entry instanceof FieldEffect) {
						String field = ((FieldEffect) entry).f.name.toString();
						if (write.containsKey(field)) {
							printWarning(write.get(field),
									currentReport.startingNode);
							continue;
						}
						if (read.containsKey(field)) {
							read.get(field).addAll(firstCall.get(es)); 
						} else {
							HashSet<Edge> edges = new HashSet<Edge>();
							edges.addAll(firstCall.get(es));
							read.put(field, edges);
						}
					}
				}
				for (EffectEntry entry : es.effect.write) {
					if (entry instanceof FieldEffect) {
						String field = ((FieldEffect) entry).f.name.toString();
						if (write.containsKey(field)) {
							write.get(field).addAll(firstCall.get(es)); 
							printWarning(write.get(field),
									currentReport.startingNode);
							continue;
						} else {
							HashSet<Edge> edges = new HashSet<Edge>();
							edges.addAll(firstCall.get(es));
							write.put(field, edges);
						}
						if (read.containsKey(field)) {
							read.get(field).addAll(firstCall.get(es)); 
							printWarning(read.get(field),
									currentReport.startingNode);
							continue;
						} else {
							HashSet<Edge> edges = new HashSet<Edge>();
							edges.addAll(firstCall.get(es));
							read.put(field, edges);
						}
					}
				}
				for (EffectEntry entry :es.effect.read) {
					if (entry instanceof FieldEffect) {
						String field = ((FieldEffect) entry).f.name.toString();
						if (read.containsKey(field)) {
							read.get(field).addAll(firstCall.get(es)); 
						} else {
							HashSet<Edge> edges = new HashSet<Edge>();
							edges.addAll(firstCall.get(es));
							read.put(field, edges);
						}
					}
				}
			}
		}
	}

	private void printWarning(HashSet<Edge> hashSet, Node startingNode) {
		List<String> sets = List.<String>nil();
		for (Edge edge : hashSet) {
			String set = "";
			for(Entry<Name, Node> e : edge.fromNode.connections.entrySet()){
				if(e.getValue()==edge.toNode)
					set += e.getKey();
			}
			set += "." + edge.toProcedure;
			boolean duplicate = false;
			for(String s : sets){
				if(s.equals(set))
					duplicate = true;
			}
			if(!duplicate)
				sets = sets.append(set);
		}
		log.warning("sequential.inconsistency.warning", sets,
				startingNode.capsule.capsule_info.parentCapsule.name);
	}

	private void pathCheck(HashSet<Path> paths) {
		for (Path path : paths) { // for each path
			currentReport.startingNode = path.nodes.head;
			getActualPaths(path.nodes);
		}
	}

	private Edge currentStartingCall;
	private void getActualPaths(List<Node> path) {
		for (MethodSymbol sym : path.head.procedures) {
			// can add restrictions to filter out unnecessary methods.?
			List<Edge> edges = graph.getEdges(path.head, sym, path.tail);
			for (Edge e : edges) {
				currentStartingCall = e;
				getActualPaths(e, path.tail);
			}
		}
	}
	
	private void addSymbolToEndingProcedure(MethodSymbol m){
		boolean duplicate = false;
		for(MethodSymbol ms : endingProcedure){
			if(ms.toString().equals(m.toString())){
				duplicate = true;
			}
		}
		if(!duplicate){
			endingProcedure = endingProcedure.append(m);
		}
	}
	
	private void getActualPaths(Edge edge, List<Node> path) {
		// end of path
		if (path.tail.isEmpty()) {
 			for (MethodSymbol m : path.head.procedures) {
				if (m.toString().equals(edge.toProcedure.toString())){
					if (m.effect != null) {
						if (firstCall.containsKey(m))
							firstCall.get(m).add(currentStartingCall);
						else {
							HashSet<Edge> e = new HashSet<Edge>();
							e.add(currentStartingCall);
							firstCall.put(m, e);
						}
						addSymbolToEndingProcedure(m);
					}
				}
			}
		} else {
			List<Edge> edges = graph.getEdges(edge.toNode, edge.toProcedure,
					path.tail);
			for (Edge e : edges) {
				getActualPaths(e, path.tail);
			}
		}
	}

	private boolean traverse(Node node, final Path currentPath) {
		Path newPath = new Path(currentPath);
		newPath.nodes = newPath.nodes.append(node);
		if (paths.containsKey(node))
			paths.get(node).add(newPath);
		else {
			HashSet<Path> hs = new HashSet<Path>();
			hs.add(newPath);
			paths.put(node, hs);
		}
		if (!visitedNode.add(node)) {
			return true;
		}
		if (node.connections.isEmpty())
			return false;
		boolean found = false;
		for (Entry<Name, Node> co : node.connections.entrySet()) {
			if(co.getValue()!=null)
				found |= traverse(co.getValue(), newPath);
		}
		return found;
	}
}
