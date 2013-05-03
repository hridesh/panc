package org.paninij.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.tools.JavaFileObject;

import org.paninij.analysis.CommonMethod;
import org.paninij.path.*;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.List;

public class EffectInter {
	public CapsuleSymbol curr_cap;
	public JCMethodDecl curr_meth;

	// effects for all the method in each capsule.
	private static final Path switchPath(Path path, JCExpression meth,
			List<JCExpression> args, AliasingGraph ag) {
		Path pathBase = path.getBasePath();
		if (pathBase instanceof Path_Class) {
			return path.clonePath();
		} else if (pathBase instanceof Path_Parameter) {
			int baseIndex = path.getBase();
			if (baseIndex == 0) {
				meth = CommonMethod.essentialExpr(meth);

				if (meth instanceof JCIdent) { // this.m
					return path.switchBase(0);
				} else if (meth instanceof JCFieldAccess) {
					JCFieldAccess jcf = (JCFieldAccess)meth;
					return path.switchBaseWithPath(
							ag.createPathForExp(jcf.selected));
				} else throw new Error("method call match failure = " +
						meth + "\t" + meth.getClass());
			} else {
				if (args != null) {
					JCExpression tree = args.get(baseIndex - 1);
					return path.switchBaseWithPath(
							ag.createPathForExp(tree));
				}
				return Path_Unknown.unknow;
			}
		} else if (pathBase instanceof Path_Unknown) {
			return Path_Unknown.unknow;
		} else if (pathBase instanceof Path_Var) {
			return Path_Unknown.unknow;
		} else if (pathBase instanceof Path_Literal) {
			return pathBase;
		} else throw new Error("field effect match failure = " +
				path.printPath() + "\t" + path.getClass());
	}

	private static final void addFieldEffect(AliasingGraph ag, Path p,
			FieldEffect eff, HashSet<EffectEntry> result) {
		if (!ag.isPathNew(p)) {
			result.add(eff);
		}
	}

	private static void addArrayEffect(AliasingGraph ag, Path p,
			ArrayEffect eff, HashSet<EffectEntry> result) {
		if (!ag.isPathNew(p)) {
			result.add(eff);
		}
	}

	private final static void mergeReadWriteEffect(AliasingGraph ag, 
			HashSet<EffectEntry> source, JCMethodInvocation stmt,
			HashSet<EffectEntry> result) {
		for (EffectEntry rwe : source) {
			if (rwe instanceof FieldEffect) {
				FieldEffect rwep = (FieldEffect)rwe;
				Path path = rwep.path;
				Symbol f = rwep.f;

				Path resultPath = switchPath(path, stmt.meth, stmt.args, ag);

				addFieldEffect(ag, resultPath, new FieldEffect(resultPath, f),
						result);
			} else if (rwe instanceof ArrayEffect) {
				ArrayEffect rwep = (ArrayEffect)rwe;

				Path path = rwep.path;
				JCExpression meth = stmt.meth;
				List<JCExpression> args = stmt.args;

				// indexed[index]
				Path indexededPath = switchPath(path, meth, args, ag);

				addArrayEffect(ag, indexededPath,
						new ArrayEffect(indexededPath, rwep.type), result);
			} else throw new Error("method call match failure = " +
					rwe.getClass());
		}
	}

	private final void knowCallee (Symbol meth, JCMethodInvocation stmt,
			AliasingGraph aliasing, JCMethodDecl curr_meth, EffectSet ars,
			EffectIntra intra, EffectSet result) {
		MethodSymbol ms = (MethodSymbol)meth;
		HashSet<MethodSymbol> callers = ms.callers;
		if (callers == null) {
			ms.callers = new HashSet<MethodSymbol>();
			callers = ms.callers;
		}
		callers.add(curr_meth.sym);
		if (ars != null) {
			if (ars.isBottom) {
				result.makeButtom();
			} // else {
				HashSet<EffectEntry> read_result = result.read;
				HashSet<EffectEntry> write_result = result.write;
				HashSet<CallEffect> calls_result = result.calls;
				// for call
				HashSet<CallEffect> alive_result = result.alive;
				HashSet<CallEffect> collected_result = result.collected;

				// for call
				HashSet<CallEffect> alive = ars.alive;
				HashSet<CallEffect> collected = ars.collected;
				HashSet<EffectEntry> read = ars.read;
				HashSet<EffectEntry> write = ars.write;
				HashSet<CallEffect> calls = ars.calls;

				// pair of calls that need to be tested
				for (CallEffect ce1 : alive_result) {
					for (CallEffect ce2 : alive) {
						intra.direct.add(new BiCall(ce1, ce2));
					}
					for (CallEffect ce2 : collected) {
						intra.direct.add(new BiCall(ce1, ce2));
					}
				}
				for (CallEffect ce1 : collected_result) {
					for (CallEffect ce2 : alive) {
						intra.indirect.add(new BiCall(ce1, ce2));
					}
					for (CallEffect ce2 : collected) {
						intra.indirect.add(new BiCall(ce1, ce2));
					}
				}

				for (CallEffect oe : calls) {
					if (oe instanceof CapsuleEffect) {
						calls_result.add(oe);
					} else if (oe instanceof ForeachEffect) {
						calls_result.add(oe);
					} else if (oe instanceof IOEffect) {
						calls_result.add(oe);
					}
				}
				mergeReadWriteEffect(aliasing, read, stmt, read_result);
				mergeReadWriteEffect(aliasing, write, stmt, write_result);

				if (ars.isWriteBottom && calls.size() != 0) { 
					result.isWriteBottom = true;
				}

				result.writtenFields.addAll(ars.writtenFields);
				if (result.isWriteBottom) {
					result.removedAffectedByUnanalyzableBottom();
				} else {
					result.removedAffectedFields(result.writtenFields);
				}

				alive_result.addAll(alive);
				collected_result.addAll(collected);
			// }
		}
	}

	// match to check whether the foreach is called on a capsule.
	public final void intraForeach(JCForeach jcf, AliasingGraph ag,
			EffectIntra intra, EffectSet rs) {
		JCExpression carr = jcf.carr;
		Symbol s = ag.aliasingState(carr);
		if (s != null) {
			if ((s.flags_field & Flags.PRIVATE) == 0) {
				Type fortype = carr.type;
				if (fortype instanceof ArrayType) {
					ArrayType at = (ArrayType)fortype;
					Type tempT = at.elemtype;
					if (tempT instanceof ClassType) {
						ClassType elemtype = (ClassType)tempT;
						ClassSymbol tsym = (ClassSymbol)elemtype.tsym;
	
						if (tsym instanceof CapsuleSymbol) {
							JCMethodInvocation body = jcf.body;
							JCExpression meth = body.meth;
							if (meth instanceof JCFieldAccess) {
								JCFieldAccess jcfa = (JCFieldAccess)meth;
								JCExpression selected = jcfa.selected;
								if (selected instanceof JCIdent) {
									JCIdent jci = (JCIdent)selected;
									if (jci.sym == tsym) {
										JavaFileObject sf = curr_cap.sourcefile;
										DiagnosticSource ds =
											new DiagnosticSource(sf, null);
										int pos = jcf.getPreferredPosition();

										ForeachEffect fe = new ForeachEffect(
												curr_cap, tsym,
												(MethodSymbol)(jcfa.sym), pos,
												ds.getLineNumber(pos),
												ds.getColumnNumber(pos, false),
												sf.toString());

										// pair of calls that need to be tested
										for (CallEffect ce : rs.alive) {
											intra.direct.add(new BiCall(ce, fe));
										}
										for (CallEffect ce : rs.collected) {
											intra.indirect.add(
													new BiCall(ce, fe));
										}

										rs.calls.add(fe);
										rs.alive.add(fe);
										rs.collected.remove(fe);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public static final boolean isCapsuleCall(JCMethodInvocation tree,
			AliasingGraph ag) {
		JCExpression meth = tree.meth;
		meth = CommonMethod.essentialExpr(meth);

		if (meth instanceof JCFieldAccess) { // selected.m(...)
			JCFieldAccess jcf = (JCFieldAccess)meth;
			JCExpression selected = CommonMethod.essentialExpr(jcf.selected);

			Symbol caps = ag.aliasingState(selected);
			if (caps != null) {
				Symbol typeSym = caps.type.tsym;
				// single capsule call.
				if (typeSym instanceof CapsuleSymbol) {
					return true;
				}
			}

			return foreallCall(selected, ag);
		}
		return false;
	}

	public static final CallEffect capsuleCall(JCMethodInvocation tree,
			AliasingGraph ag, CapsuleSymbol cap) {
		JCExpression meth = tree.meth;
		meth = CommonMethod.essentialExpr(meth);

		if (meth instanceof JCFieldAccess) { // selected.m(...)
			JCFieldAccess jcf = (JCFieldAccess)meth;
			JCExpression selected = CommonMethod.essentialExpr(jcf.selected);

			Symbol caps = ag.aliasingState(selected);
			if (caps != null) {
				Symbol typeSym = caps.type.tsym;
				// single capsule call.
				if (typeSym instanceof CapsuleSymbol) {
					DiagnosticSource ds =
						new DiagnosticSource(cap.sourcefile, null);
					int pos = tree.getPreferredPosition();

					return new CapsuleEffect(cap, caps, (MethodSymbol)jcf.sym,
							pos, ds.getLineNumber(pos), // do not expend tab
							ds.getColumnNumber(pos, false),
							cap.sourcefile.toString());
				}
			}

			if (selected instanceof JCArrayAccess) {
				JCArrayAccess jcaa = (JCArrayAccess)selected;
				JCExpression indexed = CommonMethod.essentialExpr(jcaa.indexed);

				Symbol cs = ag.aliasingState(indexed);
				if (cs != null) {
					ArrayType at = (ArrayType)cs.type;
					Symbol typeSym = at.elemtype.tsym;
					// many capsule call.
					if (typeSym instanceof CapsuleSymbol) {
						DiagnosticSource ds =
							new DiagnosticSource(cap.sourcefile, null);
						int pos = tree.getPreferredPosition();

						return new ForeachEffect(cap, cs, (MethodSymbol)jcf.sym,
								pos, ds.getLineNumber(pos), // do not expend tab
								ds.getColumnNumber(pos, false),
								cap.sourcefile.toString());
					}
				}
			}
		}
		return null;
	}

	public static final boolean isCallReturnNew(JCMethodInvocation tree,
			AliasingGraph ag) {
		JCExpression meth = tree.meth;
		meth = CommonMethod.essentialExpr(meth);

		if (meth instanceof JCIdent) { // selected.m(...)
			JCIdent jci = (JCIdent)meth;
			MethodSymbol ms = (MethodSymbol)jci.sym;
			EffectSet es = ms.effect;
			if (es == null) {
				return true;
			}
			return es.returnNewObject;
		}
		return false;
	}

	public final boolean intraCommuteCall(JCMethodInvocation tree,
			AliasingGraph ag) {
		JCExpression meth = tree.meth;
		meth = CommonMethod.essentialExpr(meth);
		if (meth instanceof JCIdent) {
			JCIdent jci = (JCIdent)meth;
			Symbol s = jci.sym;
			if (jci.name.toString().compareTo("this") == 0 ||
					jci.name.toString().compareTo("super") == 0) {
				// this(...) and super(...)
				throw new Error("should not call this or superin a capsule");
			} else { // m(...)
				MethodSymbol ms = (MethodSymbol)s;
				JCMethodDecl jcmd = ms.tree;
				EffectSet es = jcmd.sym.effect;
				if (es != null) {
					if (es.commute) {
						return true;
					}
				}
			}
		} else if (meth instanceof JCFieldAccess) { // selected.m(...)
			JCFieldAccess jcf = (JCFieldAccess)meth;
			JCExpression selected = CommonMethod.essentialExpr(jcf.selected);

			Symbol caps = ag.aliasingState(selected);
			if (caps != null) {
				Symbol typeSym = caps.type.tsym;
				// single capsule call.
				if (typeSym instanceof CapsuleSymbol) {
					return true;
				}
			}

			return foreallCall(selected, ag);
		} else throw new Error("method call match failure = " + meth + "\t" +
				meth.getClass());
		return false;
	} // end of intraCommuteCall

	private static final boolean IOeffect(JCFieldAccess meth) {
		Symbol field = meth.sym;
		if (field.toString().indexOf("print(") != -1 ||
				field.toString().compareTo("println(") != -1) {
			JCExpression selected = meth.selected;
			if (selected instanceof JCFieldAccess) {
				JCFieldAccess jcfa= (JCFieldAccess)selected;
				Symbol fld = jcfa.sym;
				if (fld.toString().compareTo("out") == 0) {
					JCExpression receiver = jcfa.selected;
					if (receiver instanceof JCIdent) {
						JCIdent jci = (JCIdent)receiver;
						return jci.toString().compareTo("System") == 0;
					}
				}
			}
		}
		return false;
	}

	static public final String[][] pureMethods =
	{ 	{"java.lang.","currentTimeMillis"},
		{"java.lang.Math","sqrt"}};

	private static final boolean pure(Symbol meth) {
		Symbol c = meth.enclClass();
		if (c.toString().startsWith("java.lang.String")) {
			return true;
		}
		for (String[] element : pureMethods) {
			if (meth.toString().startsWith(element[1]) &&
					c.toString().startsWith(element[0])) {
				return true;
			}
		}
		return false;
	}

	private static final boolean foreallCall(JCExpression tree,
			AliasingGraph ag) {
		if (tree instanceof JCArrayAccess) {
			JCArrayAccess jcaa = (JCArrayAccess)tree;
			JCExpression indexed = CommonMethod.essentialExpr(jcaa.indexed);

			Symbol caps = ag.aliasingState(indexed);
			if (caps != null) {
				ArrayType at = (ArrayType)caps.type;
				Symbol typeSym = at.elemtype.tsym;
				// many capsule call.
				if (typeSym instanceof CapsuleSymbol) {
					return true;
				}
			}
		}
		return false;
	}

	// return a capsule effect instead of boolean
	private static final ForeachEffect foreachCall(JCExpression tree,
			AliasingGraph ag, CapsuleSymbol curr_cap, MethodSymbol ms,
			JCMethodInvocation jcmd) {
		if (tree instanceof JCArrayAccess) {
			JCArrayAccess jcaa = (JCArrayAccess)tree;
			JCExpression indexed = CommonMethod.essentialExpr(jcaa.indexed);

			Symbol caps = ag.aliasingState(indexed);
			if (caps != null) {
				ArrayType at = (ArrayType)caps.type;
				Symbol typeSym = at.elemtype.tsym;
				// many capsule call.
				if (typeSym instanceof CapsuleSymbol) {
					DiagnosticSource ds =
						new DiagnosticSource(curr_cap.sourcefile, null);
					int pos = jcmd.getPreferredPosition();

					return new ForeachEffect(curr_cap, caps,
							ms, pos, ds.getLineNumber(pos), // do not expend tab
							ds.getColumnNumber(pos, false),
							curr_cap.sourcefile.toString());
				}
			}
		}
		return null;
	}

	public final void intraProcessMethodCall(JCMethodInvocation tree,
			AliasingGraph ag, EffectSet rs, EffectIntra intra) {
		JCExpression meth = tree.meth;
		meth = CommonMethod.essentialExpr(meth);
		if (meth instanceof JCIdent) {
			JCIdent jci = (JCIdent)meth;
			Symbol s = jci.sym;
			if (jci.name.toString().compareTo("this") == 0 ||
					jci.name.toString().compareTo("super") == 0) {
				// this(...) and super(...)
				return;
			}
			if (s.toString().compareTo("yield(long)") == 0 ||
					s.toString().compareTo("yield()") == 0) { // pure
				return;
			}

			if (pure(s)) { // pure methods
				return;
			}
			{ // m(...)
				MethodSymbol ms = (MethodSymbol)s;
				JCMethodDecl jcmd = ms.tree;
				knowCallee(s, tree, ag, curr_meth, jcmd.sym.effect, intra, rs);
			}
		} else if (meth instanceof JCFieldAccess) { // selected.m(...)
			JCFieldAccess jcf = (JCFieldAccess)meth;
			if (IOeffect(jcf)) {
				rs.calls.add(new IOEffect());
				return;
			}

			if (pure(jcf.sym)) { // pure methods
				return;
			}

			JCExpression selected = CommonMethod.essentialExpr(jcf.selected);
			CallEffect calleffect = ag.capEffect(selected);
			if (calleffect != null) {
				rs.alive.remove(calleffect);
				rs.collected.add(calleffect);
				return;
			}

			if (ag.isReceiverNew(selected)) { return; }

			Symbol fld = ag.aliasingState(selected);
			if (fld != null) {
				Symbol typeSym = fld.type.tsym;
				// single capsule call.
				if (typeSym instanceof CapsuleSymbol) {
					DiagnosticSource ds =
						new DiagnosticSource(curr_cap.sourcefile, null);
					int pos = tree.getPreferredPosition();

					CapsuleEffect ce = new CapsuleEffect(curr_cap, fld,
							(MethodSymbol)jcf.sym, pos,
							ds.getLineNumber(pos), // do not expend tab
							ds.getColumnNumber(pos, false),
							curr_cap.sourcefile.toString());

					// pair of calls that need to be tested
					for (CallEffect ce1 : rs.alive) {
						intra.direct.add(new BiCall(ce1, ce));
					}
					for (CallEffect ce1 : rs.collected) {
						intra.indirect.add(new BiCall(ce1, ce));
					}

					rs.calls.add(ce);
					rs.alive.add(ce);
					rs.collected.remove(ce);
				} else {
					rs.write.add(new FieldEffect(
							new Path_Parameter(null, 0), fld));
				}
				return;
			}

			// multiple capsule call
			if (selected instanceof JCArrayAccess) {
				JCArrayAccess jcaa = (JCArrayAccess)selected;
				JCExpression indexed = CommonMethod.essentialExpr(jcaa.indexed);

				fld = ag.aliasingState(indexed);
				if (fld != null) {
					ArrayType at = (ArrayType)fld.type;
					Symbol typeSym = at.elemtype.tsym;
					// many capsule call.
					if (typeSym instanceof CapsuleSymbol) {
						DiagnosticSource ds =
							new DiagnosticSource(curr_cap.sourcefile, null);
						int pos = tree.getPreferredPosition();

						ForeachEffect fe = new ForeachEffect(curr_cap, fld,
								(MethodSymbol)jcf.sym, pos,
								ds.getLineNumber(pos),
								ds.getColumnNumber(pos, false), // no expend tab
								curr_cap.sourcefile.toString());

						// pair of calls that need to be tested
						for (CallEffect ce : rs.alive) {
							intra.direct.add(new BiCall(ce, fe));
						}
						for (CallEffect ce : rs.collected) {
							intra.indirect.add(new BiCall(ce, fe));
						}

						rs.calls.add(fe);
						rs.alive.add(fe);
						rs.collected.remove(fe);
					} else {
						rs.write.add(new FieldEffect(
								new Path_Parameter(null, 0), fld));
					}
					return;
				}
			}

			// synchronization point, e.g., b.isSleeping().value()
			if (selected instanceof JCMethodInvocation) {
				JCMethodInvocation jcmi = (JCMethodInvocation)selected;
				JCExpression inner = jcmi.meth;
				inner = CommonMethod.essentialExpr(inner);

				if (inner instanceof JCFieldAccess) {
					JCFieldAccess jcfa = (JCFieldAccess)inner;
					JCExpression exp =
						CommonMethod.essentialExpr(jcfa.selected);
					Symbol receiver = ag.aliasingState(exp);
					MethodSymbol ms = (MethodSymbol)jcfa.sym;

					// capsule call
					if (receiver != null) {
						Symbol typeSym = receiver.type.tsym;
						// single capsule call.
						if (typeSym instanceof CapsuleSymbol) {
							DiagnosticSource ds =
								new DiagnosticSource(curr_cap.sourcefile, null);
							int pos = selected.getPreferredPosition();

							CapsuleEffect ce = new CapsuleEffect(curr_cap,
									receiver, ms, pos,
									ds.getLineNumber(pos), // do not expend tab
									ds.getColumnNumber(pos, false),
									curr_cap.sourcefile.toString());

							rs.alive.remove(ce);
							rs.collected.add(ce);
							return;
						}
					}

					ForeachEffect fe = foreachCall(exp, ag, curr_cap, ms, jcmi);
					if (fe != null) {
						rs.alive.remove(fe);
						rs.collected.add(fe);
						return;
					}
				}
			}

			// results[i] = workers[i].compute();
			// results[i].value();
			if (selected instanceof JCArrayAccess) {
				JCArrayAccess jcaa = (JCArrayAccess)selected;
				JCExpression inner = jcaa.indexed;
				inner = CommonMethod.essentialExpr(inner);

				if (inner instanceof JCIdent) {
					JCIdent jci = (JCIdent)inner;
					if (ag.isLocalNew(jci.sym)) {
						return;
					}
				}
			}
			rs.makeButtom();
		} else throw new Error("method call match failure = " + meth + "\t" +
				meth.getClass());
	} // end of intraProcessMethodCall

	// This method should be called only when jcmd is non-null
	public void analysis(JCMethodDecl jcmd, CapsuleSymbol cap) {
		curr_meth = jcmd;
		JCBlock body = jcmd.body;
		curr_cap = cap;
		if (body != null) {
			EffectSet oldResult = jcmd.sym.effect;

			HashSet<JCTree> exists = new HashSet<JCTree>();
			for (JCTree tree : body.endNodes) {
				exists.add(tree);
			}
			for (JCTree tree : body.exitNodes) {
				exists.add(tree);
			}
			// Aliasing analysis
			AliasingIntra dai = new AliasingIntra(curr_cap, jcmd);
			dai.analyze(jcmd.order, exists);
			HashMap<JCTree, AliasingGraph> beforeFlow = dai.graphBeforeFlow;

			// Doing the actual intra effect analsyis.
			EffectIntra fcIntra = new EffectIntra(this, curr_meth,
					jcmd.order, beforeFlow);
			java.util.List<JCTree> ends = new ArrayList<JCTree>(body.endNodes);
			ends.addAll(body.exitNodes);
			EffectSet newResult = fcIntra.doAnalysis(ends);			
			newResult.compress();

			// If the effect does not change, no need to put the methods
			// that depend on the current method back to the queue for
			// further analysis. Reaching a fix point.
			if ((oldResult == null) || (!newResult.equals(oldResult))) {
				MethodSymbol meth = jcmd.sym;
				meth.effect = newResult;
				HashSet<MethodSymbol> callers = meth.callers;
				String n1 = meth.toString();

				// copy the effect from method XYZ$Original to XYZ
				if (n1.contains("$Original")) {
					for (MethodSymbol ms : cap.procedures.keySet()) {
						String n2 = ms.name.toString();
						if (n2.equals(n1.substring(0,
								n1.indexOf("$Original")))) {
							ms.effect = meth.effect;
						}
					}
				}
				if (callers != null) {
					for (MethodSymbol s : callers) {
						// if (!s.effect.isBottom) {
							EffectInter ei = new EffectInter();
							ei.analysis(s.tree, cap);
						// }
					}
				}
			}
		}
	}
}