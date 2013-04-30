package org.paninij.effects;

public class BiCall {
	public final CallEffect ce1;
	public final CallEffect ce2;

	public BiCall(CallEffect ce1, CallEffect ce2) {
		this.ce1 = ce1;
		this.ce2 = ce2;
	}

	public int hashCode() { return ce1.hashCode() + ce2.hashCode(); }

	public boolean equals(Object o) {
		if (o instanceof BiCall) { 
			BiCall g = (BiCall)o;

			return ce1.equals(g.ce1) && ce2.equals(g.ce2);
		}
		return false;
	}
}