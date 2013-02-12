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
 * Calculation of Pi using the Panini language 
 * 
 * This computation uses the Monte Carlo Method.
 */
import java.util.Random;

class Number {
	long value;
	Number (){ this.value = 0; }
	Number (long value){ this.value = value; }
	void incr() { value ++; }
	long value() { return value; }
	static long total(Number[] numbers) {
		long total = 0; 
		for(Number n: numbers) total += n.value();
		return total;
	}
}

capsule Worker (int num) {
	Random prng = new Random ();
	Number compute() {
		Number _circleCount = new Number(0);
		for (int j = 0; j < num; j++) {
			double x = prng.nextDouble();
			double y = prng.nextDouble();
			if ((x * x + y * y) < 1)	_circleCount.incr();
		}
		return _circleCount;
	}
}

capsule Master (int totalCount, Worker[] workers) {
	void run(){
		Number[] results = new Number[workers.length];
		for (int i=0; i< workers.length; i++)
			results[i] = workers[i].compute();

		double pi = 4.0 * Number.total(results) / totalCount; 
		System.out.println("Pi : " + pi);
	}
}

system Pi {
	Master master; Worker workers[10];
	master(50000000, workers);
	for (Worker w : workers)
		w(5000000);
}