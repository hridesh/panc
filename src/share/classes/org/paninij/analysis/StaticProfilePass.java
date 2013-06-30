package org.paninij.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.util.List;

public final class StaticProfilePass extends TreeScanner {
	
	private int methodCost = 0;
	private static final int threshold = 1000;
	//local list of invoked capsule procedures for a given method
	private java.util.List<String> invokedCapsuleProcs;
	// local list of blocking calls for a given method
	private java.util.List<String> blockingCalls; // TODO: deallocate memory
	// caching global information about cost, blocking capsules,
	// so that, finalize stage can retrieve this information and,
	// compute more accurate method costs
	public static HashMap<String, Integer> costs = new HashMap<String, Integer>();
	// global list of invoked capsule procedures for a given method,
	// cannot get the method information
	public static HashMap<JCMethodDecl, java.util.List<String>> invokedProcs = new HashMap<JCMethodDecl, java.util.List<String>>();
	public static java.util.List<String> blockingCapsules = new ArrayList<String>();
	// add attribute in Capsule class and update it as highCostCapsule,
	// if it has any method whose cost is more than threshold
	public static java.util.List<String> highCostCapsules = new ArrayList<String>();

	public void visitMethodDef(JCMethodDecl tree) {
		invokedCapsuleProcs = new ArrayList<String>();
		blockingCalls = new ArrayList<String>();
		this.methodCost = tree.cost; // reset methodCost

		// visit method body
		JCBlock body = tree.body;
		if (body != null) {
			body.accept(this);
		}
		// check for any capsule procedure invocations
		if (invokedCapsuleProcs.size() > 0) {
			invokedProcs.put(tree, invokedCapsuleProcs);
		}
		// check for any blocking method calls
		if (blockingCalls.size() > 0) {
			tree.hasBlocking = true;
		} else
			tree.hasBlocking = false;

		// get method signature to update the cost
		Symbol sym = tree.sym;
		if (sym != null) {
			ClassSymbol cls = (ClassSymbol) sym.owner;
			String type = tree.type.getReturnType().toString();
			String clsw = cls.fullname.toString();
			if (clsw.contains("$")) {
				int d = clsw.indexOf("$");
				clsw = clsw.substring(0, d);
			}
			String method = type + " " + clsw + "." + sym.toString();
			costs.put(method, this.methodCost);
			if (tree.hasBlocking)
				blockingCapsules.add(clsw);
		}
		this.methodCost = 0;
	}

	private static String capsuleInstanceOwnerName(JCMethodDecl tree) {
		Symbol sym = tree.sym;
		if (sym != null) {
			ClassSymbol cls = (ClassSymbol) sym.owner;
			String clsw = cls.fullname.toString();
			if (clsw.contains("$")) {
				int d = clsw.indexOf("$");
				clsw = clsw.substring(0, d);
				return clsw;
			}
		}
		return null;
	}

	public void visitApply(JCMethodInvocation tree) {
		JCExpression meth = tree.meth;
		List<JCExpression> args = tree.args;
		Symbol sym = null;
		if (meth instanceof JCFieldAccess) {
			JCFieldAccess m = (JCFieldAccess) meth;
			sym = m.sym;
		} else if (meth instanceof JCIdent) {
			JCIdent m = (JCIdent) meth;
			sym = m.sym;
		}

		if (sym != null) {
			ClassSymbol cls = (ClassSymbol) sym.owner;
			String type = meth.type.getReturnType().toString();
			String method = type + " " + cls.fullname + "." + sym.toString();
			if ( sym.owner.isCapsule() ) {
				//this.methodCost += sym.tree.cost;
				invokedCapsuleProcs.add(method); // TODO: library methods cost
			} else {
				for (String blkCall : Blocking.thread_methods) {
					if (method.equals(blkCall))
						blockingCalls.add(method); // TODO: currently not
													// checking for blocking
													// calls
				}

			}
		}
		
		// fill the start/end/exit nodes
		meth.accept(this);
	}

	public static void finalizeCost() {
		for (Entry<JCMethodDecl, java.util.List<String>> entry : invokedProcs
				.entrySet()) {
			JCMethodDecl key = entry.getKey();
			java.util.List<String> values = entry.getValue();
			for (String value : values) {
				if (costs.containsKey(value))
					key.cost += costs.get(value);
			}
			if (key.cost > threshold) { // TODO: change 1000 to some valid
										// number later
				highCostCapsules.add(capsuleInstanceOwnerName(key));
			}
			// System.out.println("Final cost of "+ key.name +" : "+ key.cost);
		}
	}

}
