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
 * Contributor(s): Hridesh Rajan
 */

/*** 
 * This Panini program illustrates the use of a distributor pattern
 * in capsule-oriented programming to implement facilities similar
 * to a task pool. The program itself implements a simple recursive
 * computation of a fibonacci number.
 */

signature Worker {
	Number execute(int num);
}

capsule FibWorker (Worker w) implements Worker {
	Number execute(int n) {
		if (n < 2) return new Number(n);
		if (n < 13) return new Number(helper(n));
		return new Sum (w.execute(n-1), w.execute(n-2));
	}
	private int helper(int n) {
        int prev1=0, prev2=1;
        for(int i=0; i<n; i++) {
            int savePrev1 = prev1;
            prev1 = prev2;
            prev2 = savePrev1 + prev2;
        }
        return prev1;
	}
}

capsule Distributor (Worker[] workers) implements Worker {
	int current = 0;
	Number execute(int num) {
		Number result = workers[current].execute(num);
		current++;
		if(current == workers.length) current = 0;
		return result;
	}
}

capsule Fibonacci (String[] args) {
	design {
		FibWorker workers[10];
		Distributor d;
		d(workers);
		wireall(workers, d);
	}
	void run(){ 
	 	try {
			System.out.println(d.execute(Integer.parseInt(args[0])).v()); 
		} catch (java.lang.ArrayIndexOutOfBoundsException e) {
		 	System.out.println("Usage: panini Fibonacci <Number>");
		}
	}
}

class Number {
	int number;
	Number(int number){ this.number = number; }
	int v(){ return number;}
	public String toString() { return "" + number; }
}

class Sum extends Number {
	Number left; Number right;
	Sum(Number left, Number right){ super(0); this.left = left; this.right = right; }
	@Override
	int v() { return left.v() + right.v(); }
}
