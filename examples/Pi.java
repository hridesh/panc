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
// import java.lang.Math;
import java.util.Random;

class Number {
	long value;
	Number (long value){ this.value = value; }
	void incr() { value ++; }
	long value() { return value; }
}

module Worker (int num) {
	Number _circleCount;
	Random prng = new Random ();
	void compute() {
		long result = 0;
		for (int j = 0; j < num; j++) {
			double x = prng.nextDouble();
			double y = prng.nextDouble();
			if ((x * x + y * y) < 1)
				result++;
		}
		_circleCount = new Number(result);
	}
	Number getCircleCount() { return _circleCount; }
}

module Master (int totalCount, Worker[] workers) {
	void run(){
		long totalCircleCount = 0;
		for (Worker w : workers) w.compute();
		for (Worker w : workers) totalCircleCount += w.getCircleCount().value();
		double pi = 4.0 * totalCircleCount / totalCount;
		System.out.println("Pi : " + pi);
	}
}

system Pi {
	Master master; Worker workers[10];
	master(50000000, workers);
	for (Worker w : workers)
		w(5000000);
}