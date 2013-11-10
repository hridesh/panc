package org.paninij.effects;

public interface CallEffect extends EffectEntry {
	// The position of the method call in the source code. It serves as a unique
	// identifier for the method call within the capsule.
	public int pos();
}
