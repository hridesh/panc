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
 * @summary Compile the Pi example.
 * @compile Pi.java

 */

capsule Worker (double batchSize) {
	Random prng = new Random ();
	int computeNumberOfPointsWithinCircle() {
		int _circleCount = 0;
		for (int j = 0; j < batchSize; j++) {
			double x = prng.nextDouble();
			double y = prng.nextDouble();
			if ((x * x + y * y) < 1) _circleCount++;
		}
		return _circleCount;
	}
}

capsule Pi (String[] args) {
	double totalSamples = 0;

	design {
		totalSamples = Math.pow(10,Integer.parseInt(args[0]));
		Worker workers[10];	
		wireall(workers, totalSamples/workers.length);
    }

	void run(){
	 	double startTime = System.currentTimeMillis();
		int[] results = foreach(Worker w: workers) w.computeNumberOfPointsWithinCircle();

		int total = 0;
		for (int i=0; i < workers.length; i++)
		 	total += results[i];

		double pi = 4.0 * ((double)total) / ((double)totalSamples);
		System.out.println("Pi : " + pi);
		double endTime = System.currentTimeMillis();
		System.out.println("Time to compute Pi using " + totalSamples + " samples was:" + (endTime - startTime) + "ms.");
	}
}
