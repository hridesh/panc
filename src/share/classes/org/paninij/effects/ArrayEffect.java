package org.paninij.effects;

import org.paninij.path.*;
import com.sun.tools.javac.code.Type;

public class ArrayEffect implements EffectEntry {
	public final Path path;
	public final Type type;

	public ArrayEffect(Path path, Type type) {
		this.path = path;
		this.type = type;
	}

	public ArrayEffect(Type type) {
		this.path = Path_Unknown.unknow;
		this.type = type;
	}

	public int hashCode() {
		return path.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof ArrayEffect) {
			ArrayEffect rwe = (ArrayEffect) obj;
			return path.equals(rwe.path);
		}
		return false;
	}

	public void printEffect() {
		System.out.println("ArrayEffect base = " + path.printPath() +
				"\ttype = " + type);
	}

	public String effectToString() {
		return "A"+this.type.toString();
	}
}