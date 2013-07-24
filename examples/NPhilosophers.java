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

interface BooleanI {
	public boolean value();
}

class BooleanC implements BooleanI {
	private boolean v;
	public BooleanC(boolean v) { this.v = v; }
	public boolean value() { return v; }
}

class Index {
	private int i;
	public Index(){		i = 0; 	}
	public Index(int num)	{ 	i = num; }
	public void increment()	{		i++; 	}
	public int get()	{		return i; }
}

capsule Indexer(Index i) {
	Index get() {
		return i;
	}
	Index getAndInc()	 {
		Index ret = new Index(i.get());
		i.increment();
		return ret;
	}
	void run() {	}
}

class PhilUtil {
	public static int getLeftForkIndex(int p, int count) {
		return p;
	}
	public static int getRightForkIndex(int p, int count) {
		if(p == count - 1) {
			return 0;
		} 	else 	{
			return p+1;
		}
	}
	public static String getPhillyName(int p) {
		return "Philly"+p;
	}
}

capsule Fork () {
	boolean isTaken = false;
	BooleanC take() {
		if (isTaken) return new BooleanC(false);
		else {
			isTaken = true; return new BooleanC(true);
		}
	}

	void giveBack() { 
		isTaken = false;
	}
}

capsule Philosopher (Fork left, Fork right, String name) {
	void run() {
		for (int count=3; count>0; count--) {
			think();
			tryEat();
		}
	}

	void think() {
		System.out.println(name + " is thinking");
		yield(1000);
	}

	void tryEat() {
		System.out.println(name + " is hungry so they are trying to take the left fork.");
		boolean ate = false;
		while (!ate) {
			if (left.take().value()) {
				System.out.println(name + " acquired fork 1 so now they are trying to take the right fork.");
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

capsule NPhilosophers {
    design {
        Fork forks[100];
        Philosopher phils[100];
        Indexer i;
        i(new Index());

        for(Philosopher phil : phils) {
            phil(forks[PhilUtil.getLeftForkIndex(i.get().get(), phils.length)],
                    forks[PhilUtil.getRightForkIndex(i.get().get(), phils.length)],
                    PhilUtil.getPhillyName(i.getAndInc().get()));
        }
    }
}
