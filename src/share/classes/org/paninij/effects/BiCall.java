package org.paninij.effects;

public class BiCall {
	public final CallEffect ce1;
	public final CallEffect ce2;

	// Indicate whether the call can be of the same index.
	public boolean notsameindex = false;

	public BiCall(CallEffect ce1, CallEffect ce2) {
		this.ce1 = ce1;
		this.ce2 = ce2;
	}

	public int hashCode() { return ce1.hashCode() + ce2.hashCode(); }

	public boolean equals(Object o) {
		if (o instanceof BiCall) { 
			BiCall g = (BiCall)o;

			return ce1.equals(g.ce1) && ce2.equals(g.ce2) &&
			notsameindex == g.notsameindex;
		}
		return false;
	}

	public void printCalls(String deliminator) {
		ce1.printEffect();
		System.out.print(deliminator);
		ce2.printEffect();
		System.out.println(deliminator + "\tnotsameindex = " + notsameindex);
	}
}
