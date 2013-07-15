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

/* 
 * @test
 * @summary Complie the Pi example.
 * @compile Pi.java
 */

class Number {
	double value;
	Number (){ this.value = 0; }
	Number (double value){ this.value = value; }
	void incr() { value ++; }
	double value() { return value; }
	static double total(Number[] numbers) {
	       double total = 0;
	       for(Number n: numbers) total += n.value();
		return total;
        }
}

capsule Worker (double num) {
	Random prng = new Random ();
	Number compute() {
		Number _circleCount = new Number(0);
		for (double j = 0; j < num; j++) {
			double x = prng.nextDouble();
			double y = prng.nextDouble();
			if ((x * x + y * y) < 1) _circleCount.incr();
		}
		return _circleCount;
	}
}

capsule Master (double totalCount, Worker[] workers) {
	void run(){
		double startTime = System.currentTimeMillis();
		Number[] results = foreach(Worker w: workers) w.compute();

		double total = 0;
		for (int i=0; i < workers.length; i++)
			total += results[i].value();

		double pi = 4.0 * total / totalCount;
		System.out.println("Pi : " + pi);
		double endTime = System.currentTimeMillis();
		System.out.println("Time to compute Pi using " + totalCount + " samples was:" + (endTime - startTime) + "ms.");
	}
}

system Pi (String[] args) {
    double totalSamples = Math.pow(10,Integer.parseInt(args[0]));
	Master master; Worker workers[10];
	master(totalSamples, workers);
	wireall(workers, totalSamples/workers.length);
}
