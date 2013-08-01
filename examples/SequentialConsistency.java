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
 * Contributor(s): Rex Fernando
 */

class IntegerC {
	int value; public IntegerC(int value) { this.value = value; }
	public int value() { return value; }
}

capsule Main (Indirection i1, Indirection i2) {
	void doSomething(int i) {
		if(i<12) i1.set(new IntegerC(1));
		else i2.set(new IntegerC(2));
		i1.get().value();
	}
}

capsule Store () {
	int state = 0;
	void set(IntegerC newState) {
		state = newState.value();
	}
	IntegerC get() {
		return new IntegerC(state);
	}
}

capsule Indirection (Store s) {
	void set(IntegerC newStore) {
		yield((long)(Math.random()*1000));
		s.set(newStore);
	}
	IntegerC get() {
		yield((long)(Math.random()*1000));
		return s.get();
	}
}

capsule SequentialConsistency {
	design {
		Main m; Store s; Indirection i1, i2;
		i1(s); i2(s); m(i1, i2);
	}
	void run() {
		m.doSomething(13);
	}
}
