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
 * Contributor(s): 
 */
class Bool {
	private boolean v;
	public Bool(boolean v) { this.v = v; }
	public boolean value() { return v; }
}

capsule Fork () {
	boolean isTaken = false;
	Bool take() {
		if (isTaken) return new Bool(false);
		else {
			isTaken = true; return new Bool(true);
		}
	}
	void giveBack() { 
		isTaken = false;
	}
}

capsule Philosopher (Fork left, Fork right, String name) {
	void act() {
		for (int count=3; count>0; count--) {
			think();
			tryEat();
		}
	}
	private void think() {
		System.out.println(name + " is thinking");
		yield(1000);
	}
	private void tryEat() {
		System.out.println(name + " is hungry so they are trying to take fork 1.");
		boolean ate = false;
		while (!ate) {
			if (left.take().value()) {
				System.out.println(name + " acquired fork 1 so now they are trying to take fork 2.");
				if (right.take().value()) {
					System.out.println(name + " acquired both forks so now they are eating.");
					for (int eat = 0, temp=0; eat < 10000; eat++) 
						temp = eat * eat * eat * eat;
					ate = true;
					right.giveBack();
				}
				left.giveBack();
				if(!ate) yield(100);
			} 
		}
	}

}

capsule Philosophers {
    design {
        Fork f1, f2, f3; Philosopher p1, p2, p3;
        p1(f1,f2, "Aristotle");
        p2(f2,f3, "Demosthenes");
        p3(f3,f1, "Socrates");
    }
    void run() {
        p1.act();
        p2.act();
        p3.act();
    }
}

