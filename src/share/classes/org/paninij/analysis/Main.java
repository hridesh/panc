package org.paninij.analysis;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;

public final class Main {
	/**
	 * Additional Panini-related semantics checks 
	 * during attribution of a parse tree.
	 * 
	 * @returns the attributed parse tree
	 */
	public static Env<AttrContext> attribute(boolean doGraphs, Env<AttrContext> env, Log log) {
		JCClassDecl root = env.enclClass;

		// eliminate processing of duck classes
		if (!root.sym.name.toString().contains("Panini$Duck")) {
			// System.out.println("Processing class: " + root.sym);
			List<JCTree> defs = root.defs;

			for (JCTree tree : defs) {
				if (tree instanceof JCMethodDecl) {
					JCMethodDecl m = (JCMethodDecl) tree;
					if (m.body != null) {
						/*
						 * System.out.println("m = " + m.name + "\tc = " +
						 * root.name); System.out.println(m);
						 */
					    String graphName = m.sym.owner.name + "." + m.sym.name;
						if (doGraphs) {
							System.out.println("digraph \""+ graphName + "\" {");
							m.body.accept(new
									org.paninij.analysis.ASTCFGPrinter());
							System.out.println("}"); System.out.println();
						}
					}
				}
			}

			if (root instanceof JCCapsuleDecl) {
				if (root.sym.toString().indexOf("$") == -1 ||
							root.sym.toString().substring(
									root.sym.toString().indexOf("$")
									+ 1).compareTo("thread") == 0) {
					LeakDetection ld = new LeakDetection();
					ld.inter((JCCapsuleDecl)root, log);
				}
			}
		}

		// Compilation strategy analysis, 
		// make sure this pass is called after CFG and SytemGraph construction
		// phases
		if (doGraphs) {
			analyzeCapsule(root);
		}

		return env;
	}

	private static void analyzeCapsule(JCClassDecl root) {
		// eliminate processing of duck classes
		if (!root.sym.name.toString().contains("Panini$Duck")) {
			// eliminate processing of task, thread versions except for
			// capsule with run method
			if (!root.sym.name.toString().contains("$serial")
					&& (root.sym.isCapsule() && !root.sym.capsule_info.definedRun))
				return;

			// System.out.println("Processing class: " + root.sym);
			List<JCTree> defs = root.defs;
			for (JCTree tree : defs) {
				if (tree instanceof JCMethodDecl) {
					JCMethodDecl m = (JCMethodDecl) tree;
					if (m.body != null) {
						tree.accept(new org.paninij.analysis.StaticProfilePass());
					}
				}
			}
		}
		// TODO: assumes that all capsules and system are in the same file
		// All capsules are processed before the system
		System.out.println("Ignoring Finalize Cost for " + root.sym.name);
		/*if (root.sym instanceof Symbol.SystemSymbol) {// inter-capsule cost update
			org.paninij.analysis.StaticProfilePass.finalizeCost();

			// Rules to decide execution model for capsules in the system
			// 1. capsule instance with run() method: thread
			// 2. capsule instance with no run() method, and one indegree
			// and low cost: serial, high cost: task
			// 3. capsule instance with no run() method, and more than one
			// indegree and low cost: monitor
			// 4. capsule instance with no run() method, and more than one
			// indegree and high cost and low PIC: task
			// 5. capsule instance with no run() method, and more than one
			// indegree and high cost and high PIC: thread

//			Stack<org.paninij.systemgraphs.SystemGraphs.Node> visited = new Stack<org.paninij.systemgraphs.SystemGraphs.Node>();
//			org.paninij.systemgraphs.SystemGraphs graphs = ((Symbol.SystemSymbol)root.sym).graphs;
//			if (graphs == null)	return;
//			for (Collection<org.paninij.systemgraphs.SystemGraphs.ConnectionEdge> edges : graphs.forwardConnectionEdges
//					.values()) {
//				for (org.paninij.systemgraphs.SystemGraphs.ConnectionEdge edge : edges) {
//					org.paninij.systemgraphs.SystemGraphs.Node from = edge.from;
//					org.paninij.systemgraphs.SystemGraphs.Node to = edge.to;
//					if (!visited.contains(from)) {
//						decide(from);
//						visited.add(from);
//					}
//					if (!visited.contains(to)) {
//						decide(to);
//						visited.add(to);
//					}
//				}
//			}
		}
		*/
	}

//	private static void decide(org.paninij.systemgraphs.SystemGraphs.Node node) {
//		if ((node.sym.isCapsule() && (node.sym.capsule_info.definedRun) && (node.indegree == 0))
//				|| org.paninij.analysis.StaticProfilePass.blockingCapsules
//						.contains(node.sym.name)) {
//			// thread
//			System.out.println(node.toString() + " := THREAD");
//		} else if (node.indegree == 1) {
//			if (org.paninij.analysis.StaticProfilePass.highCostCapsules
//					.contains(node.sym.name.toString()))
//				System.out.println(node.toString() + " := TASK");
//			else {
//				// serial
//				System.out.println(node.toString() + " := SERIAL");
//			}
//		} else {
//			// monitor
//			System.out.println(node.toString() + " := MONITOR");
//		}
//	}
}
