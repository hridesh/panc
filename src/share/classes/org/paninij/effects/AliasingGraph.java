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
 * Contributor(s): Yuheng Long
 */

package org.paninij.effects;

import java.util.*;

import javax.lang.model.element.ElementKind;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;

import org.paninij.analysis.*;
import org.paninij.path.*;

public class AliasingGraph {
	public HashSet<ForallAliasing> forall_alias;

	// path that points to a new node.
	public HashMap<Path, Type> pathsToNewNode;

	// path that points to a return value from a capsule.
	public HashMap<Path, CallEffect> pathsToCap;

	// local variables that has been written on
	public HashSet<Symbol> writtenLocals;
	// fields that has been written on
	public HashSet<Symbol> writtenFields;

	public boolean unanalyzable;
	public HashSet<HashSet<Path>> aliasingPaths;

	public boolean processedReturnType = false;
	public Type returnType;

	public boolean isInit = false;

	public static final Type unknownType = new Type(0, null);

	public String printGraph() {
		String result = "isInit = " + isInit + "\t";

		result += "\treturnType = " + returnType + "\tprocessedReturnType = " +
		processedReturnType;

		result += "\tpointToNewNodePaths = : " + pathsToNewNode.size() + "\t";
		for (Path p : pathsToNewNode.keySet()) {
			result += p.printPath() + "\ttype = " + pathsToNewNode.get(p)
			+ "\t";
		}

		result += "\tpathsToCap = : " + pathsToCap.size() + "\t";
		for (Path p : pathsToCap.keySet()) {
			result += p.printPath() + "\ttype = " + pathsToCap.get(p) + "\t";
		}

		result += "\twrittenFields:";
		for (Symbol p : writtenFields) {
			result += p + "\t";
		}

		result += "\twrittenLocals:";
		for (Symbol p : writtenLocals) {
			result += p + "\t";
		}
		result += "\tunanalyzable = " + unanalyzable;

		result += "\tsets {";
		for (HashSet<Path> setPFC : aliasingPaths) {
			result += "{";
			for (Path p : setPFC) {
				result += p.printPath() + "\t";
			}
			result += "}";
		}

		if (!forall_alias.isEmpty()) {
			for (ForallAliasing fa : forall_alias) {
				result += fa.printString() + "\t";
			}
		}

		result += "}\n";
		return result;
	}

	public AliasingGraph() {
		pathsToNewNode = new HashMap<Path, Type>();
		pathsToCap = new HashMap<Path, CallEffect>();
		writtenFields = new HashSet<Symbol>();
		writtenLocals = new HashSet<Symbol>();
		unanalyzable = false;
		aliasingPaths = new HashSet<HashSet<Path>>();
		forall_alias = new HashSet<ForallAliasing>();
	}

	public AliasingGraph(boolean b) {
		this();
		isInit = b;
	}

	public AliasingGraph(AliasingGraph x) {
		pathsToNewNode = new HashMap<Path, Type>(x.pathsToNewNode);
		pathsToCap = new HashMap<Path, CallEffect>(x.pathsToCap);
		writtenFields = new HashSet<Symbol>(x.writtenFields);
		writtenLocals = new HashSet<Symbol>(x.writtenLocals);
		unanalyzable = x.unanalyzable;
		aliasingPaths = new HashSet<HashSet<Path>>(x.aliasingPaths);
		forall_alias = new HashSet<ForallAliasing>(x.forall_alias);
		if (x.isInit) { isInit = true; }
	}

	public void init(AliasingGraph x) {
		pathsToNewNode = new HashMap<Path, Type>(x.pathsToNewNode);
		pathsToCap = new HashMap<Path, CallEffect>(x.pathsToCap);
		writtenFields = new HashSet<Symbol>(x.writtenFields);
		writtenLocals = new HashSet<Symbol>(x.writtenLocals);
		unanalyzable = x.unanalyzable;
		aliasingPaths = new HashSet<HashSet<Path>>(x.aliasingPaths);
		forall_alias = new HashSet<ForallAliasing>(x.forall_alias);
		if (x.isInit) { isInit = true; }
	}

	public int hashCode() {
		return pathsToNewNode.hashCode() + writtenFields.hashCode() +
		writtenLocals.hashCode() + aliasingPaths.hashCode() +
		pathsToCap.hashCode() + forall_alias.hashCode();
	}

	public boolean equals(Object o) {
		assert isInit;
		if (o instanceof AliasingGraph) {
			AliasingGraph g = (AliasingGraph)o;
			if (!isInit && !g.isInit) { return true; }
			if (!isInit) { return false; }
			if (g.isInit) {
				return pathsToNewNode.equals(g.pathsToNewNode) &&
				writtenFields.equals(g.writtenFields) &&
				unanalyzable == g.unanalyzable &&
				writtenLocals.equals(g.writtenLocals) &&
				aliasingPaths.equals(g.aliasingPaths) &&
				pathsToCap.equals(g.pathsToCap) &&
				forall_alias.equals(g.forall_alias);
			}
			return false;
		}
		throw new Error("should not compare other object = " + o.getClass());
	}

	// confinement indicates whether this algorithm has confinement assumption.
	public final boolean isPathNew (Path path, boolean confinement) {
		Path temp = path;

		if (confinement) {
			if (path instanceof Path_Parameter) {
				Path_Parameter pp = (Path_Parameter)path;
				if (pp.id != 0) { return true; }
			}

			while (temp instanceof Path_Compound) {
				Path_Compound pc = (Path_Compound)temp;
				temp = pc.base;
				if (temp instanceof Path_Parameter) {
					Path_Parameter pp = (Path_Parameter)temp;
					if (pp.id != 0) {
						return true;
					}
				}
			}	
		}

		if (pathsToNewNode.containsKey(path)) { return true; }

		return false;
	}

	public final CallEffect capEffect (JCTree tree) {
		if (tree instanceof JCExpression) {
			JCExpression jce = (JCExpression)tree;
			tree = AnalysisUtil.getEssentialExpr(jce);
		}

		if (tree instanceof JCIdent) {
			JCIdent jr = (JCIdent)tree;
			Symbol sr = jr.sym;
			ElementKind kind = sr.getKind();
			if (kind == ElementKind.LOCAL_VARIABLE ||
					kind == ElementKind.PARAMETER) {
				return pathsToCap.get(new Path_Var(sr, false));
			}
		}

		if (tree instanceof JCArrayAccess) {
			JCArrayAccess jcaa = (JCArrayAccess)tree;
			JCExpression indexed = AnalysisUtil.getEssentialExpr(jcaa.indexed);
			if (indexed instanceof JCIdent) {
				JCIdent jr = (JCIdent)indexed;
				Symbol sr = jr.sym;
				ElementKind kind = sr.getKind();
				if (kind == ElementKind.LOCAL_VARIABLE ||
						kind == ElementKind.PARAMETER) {
					return pathsToCap.get(new Path_Var(sr, false));
				}
			}
		}

		return null;
	}

	// confinement indicates whether this algorithm has confinement assumption.
	public final boolean isReceiverNew (JCTree tree, boolean confinement) {
		if (tree instanceof JCExpression) {
			JCExpression jce = (JCExpression)tree;
			tree = AnalysisUtil.getEssentialExpr(jce);
		}

		Path path = createPathForExp((JCExpression)tree);

		return isPathNew(path, confinement);
	}

	public final Symbol aliasingState (JCTree tree) {
		if (tree instanceof JCExpression) {
			JCExpression jce = (JCExpression)tree;
			tree = AnalysisUtil.getEssentialExpr(jce);
		}

		Path path = createPathForExp((JCExpression)tree);
		if (path instanceof Path_Compound) {
			Path_Compound pc = (Path_Compound)path;
			Path path_base = pc.base;
			if (path_base instanceof Path_Parameter) {
				Path_Parameter pp = (Path_Parameter)path_base;
				if (pp.id == 0) {
					return pc.field;
				}
			}
		}

		return null;
	}

	// The intersection of the two HashMap
	public static final HashMap<Path, CallEffect> capRetainAll(
			HashMap<Path, CallEffect> hm1,
			HashMap<Path, CallEffect> hm2) {
		HashMap<Path, CallEffect> result = new HashMap<Path, CallEffect>();
		Set<Path> key2 = hm2.keySet();
		for (Path p1 : hm1.keySet()) {
			if (key2.contains(p1)) {
				CallEffect ce = hm1.get(p1);
				if (ce.equals(hm2.get(p1))) { result.put(p1, ce); }
			}
		}
		return result;
	}

	// The intersection of the two HashMap
	public static final HashMap<Path, Type> newNodePathsRetainAll(
			HashMap<Path, Type> hm1, HashMap<Path, Type> hm2) {
		HashMap<Path, Type> result = new HashMap<Path, Type>();
		Set<Path> key2 = hm2.keySet();
		for (Path p1 : hm1.keySet()) {
			if (key2.contains(p1)) {
				Type t = hm1.get(p1);
				p1 = p1.clonePath();
				if (!t.equals(hm2.get(p1))) { t = unknownType; }

				result.put(p1, t);
			}
		}
		return result;
	}

	public static final HashSet<Path> getAffectedPahts (
			HashMap<Path, Type> newNodePaths, HashSet<Symbol> writtenFields) {
		HashSet<Path> toberemoved = new HashSet<Path>();
		for (Symbol field : writtenFields) {
			for (Path p : newNodePaths.keySet()) {
				if (p.isAffected_Path(field)) {
					toberemoved.add(p);
				}
			}
		}
		return toberemoved;
	}

	public static final HashSet<Path> getAffectedLocalPahts (
			HashMap<Path, Type> newNodePaths, HashSet<Symbol> writtenLocals) {
		HashSet<Path> toberemoved = new HashSet<Path>();
		for (Symbol field : writtenLocals) {
			for (Path p : newNodePaths.keySet()) {
				if (p.isAffected_Local(field)) {
					toberemoved.add(p);
				}
			}
		}
		return toberemoved;
	}

	public static final void removeAffectedPaths (
			HashMap<Path, Type> newNodePaths, HashSet<Path> paths) {
		for (Path p : paths) {
			newNodePaths.remove(p);
		}
	}

	public static final HashSet<HashSet<Path>> aliasRetainAll(
			HashSet<HashSet<Path>> h1, HashSet<HashSet<Path>> h2) {
		HashSet<HashSet<Path>> result = new HashSet<HashSet<Path>>();
		for (HashSet<Path> hp1 : h1) {
			if (h2.contains(hp1)) {
				HashSet<Path> temp = new HashSet<Path>(hp1.size());
				for (Path p : hp1) {
					temp.add(p.clonePath());
				}
				result.add(temp);
			}
		}
		return result;
	}

	public static final void addAlias(HashSet<HashSet<Path>> aliasingPaths,
			Path p1, Path p2) {
		if (p1 != null && p2 != null) {
			boolean exist = false;
			for (HashSet<Path> aliasSet : aliasingPaths) {
				if (aliasSet.contains(p1)) {
					aliasSet.add(p2);
					exist = true;
				}
				if (aliasSet.contains(p2)) {
					aliasSet.add(p1);
					exist = true;
				}
			}
			if (!exist) {			
				HashSet<Path> newSet = new HashSet<Path>();
				newSet.add(p2);
				newSet.add(p1);
				aliasingPaths.add(newSet);
			}
		}
	}

	public void union(AliasingGraph arg) {
		if (arg.isInit) {
			if (isInit) {
				pathsToNewNode =
					newNodePathsRetainAll(pathsToNewNode, arg.pathsToNewNode);
				pathsToCap = capRetainAll(pathsToCap, arg.pathsToCap);

				HashSet<Path> toRemove =
					getAffectedPahts(pathsToNewNode, arg.writtenFields);
				toRemove.addAll(getAffectedPahts(arg.pathsToNewNode,
						writtenFields));
				toRemove.addAll(getAffectedLocalPahts(pathsToNewNode,
						arg.writtenLocals));
				toRemove.addAll(getAffectedLocalPahts(arg.pathsToNewNode,
						writtenLocals));
				removeAffectedPaths(pathsToNewNode, toRemove);

				// for aliasing
				aliasingPaths =
					aliasRetainAll(aliasingPaths, arg.aliasingPaths);

				writtenFields.addAll(arg.writtenFields);
				writtenLocals.addAll(arg.writtenLocals);

				if (unanalyzable || arg.unanalyzable) {
					processUnalyzableAffectedPahts();
				}

				forall_alias.retainAll(arg.forall_alias);
			} else {
				isInit = true;

				unanalyzable = arg.unanalyzable;
				pathsToNewNode =
					new HashMap<Path, Type>(arg.pathsToNewNode);
				pathsToCap = new HashMap<Path, CallEffect>(arg.pathsToCap);
				writtenFields = new HashSet<Symbol>(arg.writtenFields);
				writtenLocals = new HashSet<Symbol>(arg.writtenLocals);

				// for alias
				aliasingPaths = new HashSet<HashSet<Path>>(arg.aliasingPaths);

				forall_alias = new HashSet<ForallAliasing>(arg.forall_alias);
			}
		}
	}

	public final void removeLocal(Symbol l) {
		writtenLocals.add(l);

		for (Object o : pathsToNewNode.keySet().toArray()) {
			Path p = (Path)o;
			if (p.isAffected_Local(l)) {
				pathsToNewNode.remove(p);
			}
		}

		for (Object o : pathsToCap.keySet().toArray()) {
			Path p = (Path)o;
			if (p.isAffected_Local(l)) {
				pathsToCap.remove(p);
			}
		}

		HashSet<HashSet<Path>> toberemoved = new HashSet<HashSet<Path>>();
		for (HashSet<Path> hsp : aliasingPaths) {
			for (Path p : hsp) {
				if (p.isAffected_Local(l)) {
					toberemoved.add(hsp);
					break;
				}
			}
		}
		aliasingPaths.removeAll(toberemoved);
	}

	// left = right
	public void localAssignment(Symbol left, Symbol right) {
		removeLocal(left);

		Path path_right = new Path_Var(right, false);
		addAlias(aliasingPaths, new Path_Var(left,
				pathsToNewNode.containsKey(path_right)), path_right);
	}

	// left = parameter
	public void parameterAssignment(Symbol left, Symbol right) {
		removeLocal(left);

		Path path_right = new Path_Var(right, false);
		addAlias(aliasingPaths, path_right, new Path_Var(left,
				pathsToNewNode.containsKey(path_right)));
	}

	// left = this
	public void localThisAssignment(Symbol left) {
		removeLocal(left);

		addAlias(aliasingPaths,
				new Path_Var(left, false), new Path_Parameter(null, 0));
	}

	// left = right, where right == v.f;
	public void assignFieldToLocal(Symbol left, JCFieldAccess right) {
		assignPathToLocalCommon(left, createPathForField(right));
	}

	private final void assignPathToLocalCommon(Symbol left, Path p) {
		removeLocal(left);

		boolean pointToNewObject = false;

		if (p != null && pathsToNewNode.keySet().contains(p)) {
			pointToNewObject = true;
			pathsToNewNode.put(new Path_Var(left, true), pathsToNewNode.get(p));
		}

		// for aliasing
		addAlias(aliasingPaths, new Path_Var(left, pointToNewObject), p);
	}

	// left = right, where right == this.f; while this is omitted
	public void assignFieldToLocal(Symbol left, Symbol right) {
		assignPathToLocalCommon(left,
				new Path_Compound(new Path_Parameter(null, 0), right));
	}

	// left = right, where right == v[];
	public void assignArrayToLocal(Symbol left, JCArrayAccess right) {
		Path pathIndexed =
			createPathForExp(AnalysisUtil.getEssentialExpr(right.indexed));

		Path resultPath = null;
		if (pathIndexed != null) {
			resultPath = new Path_Array(pathIndexed);
		}
		assignPathToLocalCommon(left, resultPath);
	}

	// left = right, where right == (v=...);
	public void assignJCAssignToLocal(Symbol left, JCAssign right) {
		removeLocal(left);
	}

	// left = new Arr[];
	public void assignNewArrayToLocal(Symbol left) {
		removeLocal(left);

		pathsToNewNode.put(new Path_Var(left, true), unknownType);
	}

	// left = capsule.call(...);
	public void assignCapsuleCallToLocal(Symbol left, CallEffect ce) {
		removeLocal(left);

		pathsToNewNode.put(new Path_Var(left, true), unknownType);

		if (ce != null) {
			pathsToCap.put(new Path_Var(left, true), ce);
		}
	}

	// left = new C();
	public void assignNewObjectToLocal(Symbol left, JCNewClass right) {
		removeLocal(left);

		pathsToNewNode.put(new Path_Var(left, true), right.type);
	}

	public void assignNewObjectToLocal(Symbol left, Type right) {
		removeLocal(left);

		pathsToNewNode.put(new Path_Var(left, true), right);
	}

	public void localIsUnknown(Symbol left) {
		removeLocal(left);

		pathsToNewNode.remove(new Path_Var(left, false));
	}

	public void assignParamToLocal(VarSymbol s, int right) {
		addAlias(aliasingPaths, new Path_Var(s, false),
				new Path_Parameter(null, right));
	}

	public void initParam(VarSymbol s, int right) {
		addAlias(aliasingPaths, new Path_Var(s, false),
				new Path_Parameter(s, right));
	}

	public void processReturn(JCExpression expr) {
		Type currentType = pathsToNewNode.get(createPathForExp(expr));
		if (currentType != null) {
			if (!processedReturnType) {
				returnType = currentType;
				processedReturnType = true;
			} else if (!currentType.equals(returnType)) {
				returnBottom();
			}
		} else { returnBottom(); }
	}

	public void returnBottom() {
		processedReturnType = true;
		returnType = null;
	}

	public boolean isReturnBottom () {
		return processedReturnType && (returnType == null);
	}

	/* the followings are for path analysis. */
	public void processUnalyzableAffectedPahts () {
		unanalyzable = true;
		for (Object o : pathsToNewNode.keySet().toArray()) {
			Path p = (Path)o;
			if (p.isAffected_byUnanalyzablePath()) {
				pathsToNewNode.remove(p);
			}
		}
	}

	public boolean isLocalNew (Symbol var) {
		return pathsToNewNode.containsKey(new Path_Var(var, false));
	}

	public void assignLocalToThisField(Symbol field, Symbol var) {
		writeField(field);
		Path_Var pv = new Path_Var(var, true);
		Path p = new Path_Compound(new Path_Parameter(null, 0), field);
		if (isLocalNew(var)) {
			pathsToNewNode.put(p, pathsToNewNode.get(pv));
		}

		addAlias(aliasingPaths, pv, p);
	}

	public void writeField(Symbol field) {
		for (Object o : pathsToNewNode.keySet().toArray()) {
			Path p = (Path)o;
			if (p.isAffected_PathANDField(field)) {
				pathsToNewNode.remove(p);
			}
		}

		HashSet<HashSet<Path>> toberemoved = new HashSet<HashSet<Path>>();
		for (HashSet<Path> hs : aliasingPaths) {
			for (Path p : hs) {
				if (p.isAffected_PathANDField(field)) {
					toberemoved.add(hs);
				}
			}
		}
		aliasingPaths.removeAll(toberemoved);

		writtenFields.add(field);	
	}

	// this.f = right, where this is omitted.
	public void assignPathToThisField(Symbol left, JCFieldAccess right) {
		writeField(left);
		Path p = createPathForField(right);
		if (p != null) {
			Type t = pathsToNewNode.get(p);
			if (t != null) {
				pathsToNewNode.put(new Path_Compound(
						new Path_Parameter(null, 0), left), t);
			}
		}
	}

	// this.f = this.right, where this are both omitted.
	public void assignThisFieldToThisField(Symbol left, JCIdent right) {
		writeField(left);
		Path p = new Path_Compound(new Path_Parameter(null, 0), right.sym);
		Type t = pathsToNewNode.get(p);
		if (t != null) {
			pathsToNewNode.put(
					new Path_Compound(new Path_Parameter(null, 0), left), t);
		}
	}

	private final Path pathForLocal(Symbol sym) {
		Path temp = new Path_Var(sym, false);

		for (HashSet<Path> paths : aliasingPaths) {
			if (paths.contains(temp)) {
				for (Path p : paths) {
					if (/*p instanceof Path_Parameter &&*/ !p.equals(temp)) {
						return p;
					}
				}
				break;
			}
		}
		return new Path_Var(sym, pathsToNewNode.containsKey(temp));
	}

	public Path createPathForExp(JCExpression exp) {
		exp = AnalysisUtil.getEssentialExpr(exp);
		if (exp instanceof JCFieldAccess) {
			return createPathForField((JCFieldAccess)exp);
		} else if (exp instanceof JCIdent) {
			JCIdent jr = (JCIdent)exp;
			Symbol sr = jr.sym;
			ElementKind rightkind = sr.getKind();
			if (rightkind == ElementKind.LOCAL_VARIABLE ||
					rightkind == ElementKind.PARAMETER) {
				return pathForLocal(sr);
			} else if (rightkind == ElementKind.FIELD) { // this.f = ...
				if (sr.name.toString().compareTo("this") == 0 ||
						sr.name.toString().compareTo("super") == 0) {
					return new Path_Parameter(null, 0);
				}
				return new Path_Compound(new Path_Parameter(null, 0), sr);
			} else if (rightkind == ElementKind.EXCEPTION_PARAMETER ||
					rightkind == ElementKind.CLASS ||
					rightkind == ElementKind.INTERFACE ||
					rightkind == ElementKind.PACKAGE ||
					rightkind == ElementKind.METHOD ||
					rightkind == ElementKind.ENUM) {
				return null;
			} else throw new Error("assignment match failure = " + exp +
					"\t" + rightkind);
		} else if (exp instanceof JCLiteral) {
			JCLiteral jcl = (JCLiteral)exp;
			Object o = jcl.value;
			if (o instanceof Integer) {
				return new Path_Literal((Integer)o);				
			}
		}

		return Path_Unknown.unknow;
	}

	public Path createPathForField (JCFieldAccess field) {
		JCExpression selected = field.selected;
		Symbol sym = field.sym;

		selected = AnalysisUtil.getEssentialExpr(selected);
		if (selected instanceof JCFieldAccess) {
			Path previous = createPathForField((JCFieldAccess)selected);
			if (previous == null) { return null; }
			return new Path_Compound(previous, sym);
		} else if (selected instanceof JCIdent) {
			JCIdent jr = (JCIdent)selected;
			Symbol sr = jr.sym;
			ElementKind rightkind = sr.getKind();
			if (rightkind == ElementKind.LOCAL_VARIABLE ||
					rightkind == ElementKind.PARAMETER) {
				return new Path_Compound(pathForLocal(sr), sym);
			} else if (rightkind == ElementKind.FIELD) { // this.f = ...
				if (sr.name.toString().compareTo("this") == 0 ||
						sr.name.toString().compareTo("super") == 0) {
					return new Path_Compound(new Path_Parameter(null, 0), sym);
				}
				return new Path_Compound(new Path_Compound(
						new Path_Parameter(null, 0), sr), sym);
			} else if (rightkind == ElementKind.EXCEPTION_PARAMETER ||
					rightkind == ElementKind.CLASS ||
					rightkind == ElementKind.INTERFACE ||
					rightkind == ElementKind.PACKAGE ||
					rightkind == ElementKind.METHOD ||
					rightkind == ElementKind.ENUM) {
				return null;
			} else throw new Error("assignment match failure = " + field +
					"\t" + rightkind);
		}

		return null;
	}

	public void assignNewArrayToThisField(Symbol field) {
		writeField(field);
		pathsToNewNode.put(new Path_Compound(new Path_Parameter(null, 0),
				field), unknownType);
	}

	// left = new ...;
	public void assignNewToThisField(Symbol field, JCNewClass right) {
		writeField(field);
		pathsToNewNode.put(new Path_Compound(new Path_Parameter(null, 0),
				field), right.type);
	}

	// left = new ...;
	public void assignNewToThisField(Symbol field, Type right) {
		writeField(field);
		pathsToNewNode.put(new Path_Compound(new Path_Parameter(null, 0),
				field), right);
	}

	// X.f = var;
	public void assignLocalToField(JCFieldAccess left, Symbol var) {
		writePath(left);

		if (isLocalNew(var)) {
			Path p = createPathForField(left);
			if (p != null) {
				pathsToNewNode.put(p,
						pathsToNewNode.get(new Path_Var(var, true)));
			}
		}
	}

	public void assignLocalToArray(JCArrayAccess left, Symbol var) {
		if (isLocalNew(var)) {
			Path p = createPathForExp(left);
			if (p != null) {
				pathsToNewNode.put(p,
						pathsToNewNode.get(new Path_Var(var, true)));
			}
		}
	}

	public void assignThisFieldToField(JCFieldAccess left, Symbol right) {
		writePath(left);
		Path p = new Path_Compound(new Path_Parameter(null, 0), right);
		Type t = pathsToNewNode.get(p);
		if (t != null) {
			Path leftPath = createPathForField(left);
			if (leftPath != null) {
				pathsToNewNode.put(leftPath, t);
			}
		}
	}

	public void assignThisFieldToArray(JCArrayAccess left, Symbol right) {
		Path p = new Path_Compound(new Path_Parameter(null, 0), right);
		Type t = pathsToNewNode.get(p);
		if (t != null) {
			Path leftPath = createPathForExp(left);
			if (leftPath != null) {
				pathsToNewNode.put(leftPath, t);
			}
		}
	}

	public void assignFieldToField(JCFieldAccess left, JCFieldAccess right) {
		writePath(left);

		Path p = createPathForField(right);
		if (p != null) {
			Type t = pathsToNewNode.get(p);
			if (t != null) {
				Path leftPath = createPathForField(left);
				if (leftPath != null) {
					pathsToNewNode.put(leftPath, t);
				}
			}
		}
	}

	public void assignFieldToArray(JCArrayAccess left, JCFieldAccess right) {
		Path p = createPathForField(right);
		if (p != null) {
			Type t = pathsToNewNode.get(p);
			if (t != null) {
				Path leftPath = createPathForExp(left);
				if (leftPath != null) {
					pathsToNewNode.put(leftPath, t);
				}
			}
		}
	}

	public void writePath(JCFieldAccess left) {
		Symbol field = left.sym;
		writeField(field);
	}

	public void assignCapsuleCallToField(JCFieldAccess left) {
		writePath(left);
		Path p = createPathForField(left);
		if (p != null) {
			pathsToNewNode.put(p, unknownType);
		}
	}

	// X.f = new C[];
	public void assignNewArrayToField(JCFieldAccess left) {
		writePath(left);
		Path p = createPathForField(left);
		if (p != null) {
			pathsToNewNode.put(p, unknownType);
		}
	}

	// X.f = new C();
	public void assignNewToField(JCFieldAccess left, JCNewClass jcn) {
		writePath(left);
		Path p = createPathForField(left);
		if (p != null) {
			pathsToNewNode.put(p, jcn.type);
		}
	}

	// X.f = new C();
	public void assignNewToField(JCFieldAccess left, Type type) {
		writePath(left);
		Path p = createPathForField(left);
		if (p != null) {
			pathsToNewNode.put(p, type);
		}
	}

	// arr[i] = new C[];
	public void assignNewArrayToArray(JCArrayAccess left) {
		Path p = createPathForExp(left);
		if (p != null) {
			pathsToNewNode.put(p, unknownType);
		}
	}

	// arr[i] = new C();
	public void assignNewToArray(JCArrayAccess left, JCNewClass jcn) {
		Path p = createPathForExp(left);
		if (p != null) {
			pathsToNewNode.put(p, jcn.type);
		}
	}
}
