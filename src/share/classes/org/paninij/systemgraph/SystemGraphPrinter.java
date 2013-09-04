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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Set;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.source.tree.CapsuleArrayCallTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.Name;

import org.paninij.analysis.AnalysisUtil;
import org.paninij.systemgraph.SystemGraph.Edge;
import org.paninij.systemgraph.SystemGraph.Node;

public class SystemGraphPrinter {
	public static void print_console(SystemGraph graph) {
		System.out.print("digraph system_graph {\n");
		
		HashMap<Name, Node> nodes = graph.nodes;
		int i = 0;

		HashMap<Node, Integer> cs_i = new HashMap<Node, Integer>();
		for (Name name : nodes.keySet()) {
			System.out.print("\tsubgraph \"cluster_error" + i + ".h\" {\n");

			System.out.print("\t\tnode [style=filled,color=yellow];\n");
			System.out.print("\t\tstyle=filled;\n");

			Node node = nodes.get(name);

			ClassSymbol cs = node.capsule;
			cs_i.put(node, i);
			for (MethodSymbol ms : node.procedures) {
				if ((cs.capsule_info.definedRun &&
						ms.toString().compareTo("run()") == 0) ||
						ms.toString().indexOf("$Original") != -1) {
					System.out.print("\t\t\"" + i + ":" +
							AnalysisUtil.rmDollarOriginal(
									ms.toString()) + "\"\n");
				}
			}

			System.out.print("\t\tlabel = \"" + name + ":" +
					AnalysisUtil.rmDollar(cs.className()) +"\"\n");
			System.out.print("\t}\n");

			i++;
		}

		for (Edge e : graph.edges) {
			if (e.fromProcedure.toString().indexOf("$Original") == -1) {
				System.out.print("\"" + cs_i.get(e.fromNode) + ":" +
						AnalysisUtil.rmDollarOriginal(
								e.fromProcedure.toString()) +
						"\" -> \"" + cs_i.get(e.toNode) + ":" +
						AnalysisUtil.rmDollarOriginal(
								e.toProcedure.toString()) +
						"\";\n");
			}
		}

		System.out.print("}\n");
	}
}
