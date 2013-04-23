package org.paninij.effects;

public class IOEffect implements CallEffect {
	public IOEffect() {}

	public void printEffect() {
		System.out.println("IO effect");
	}

	public int hashCode() {
		return 1;
	}

	public boolean equals(Object obj) {
		return obj instanceof IOEffect;
	}
}