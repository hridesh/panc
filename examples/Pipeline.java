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
 * A simple example of a pipeline architecture in the Panini language 
 * 
 * This pipeline is organized as:
 * 		Source -> Average -> Sum -> Min -> Max -> Sink
 * 
 * Here, capsule Source generates a set of random numbers, 
 * capsule Average maintains a running average, capsule Sum maintains
 * a running total, and the Sink reports that a number has been 
 * processed by the pipeline.
 * 
 */
import java.util.Random;

signature Stage {
	void consume(long n);
}

capsule Average (Stage next) implements Stage {
	long average = 0; 
	long count = 0; 
	void consume(long n) {
		next.consume(n);
		if (n != -1) {
			average = ((count * average) + n) / (count + 1);
			count++;
		} else 
			System.out.println("Average of " + count + " numbers was " + average + ".");
	}
}

capsule Sum (Stage next) implements Stage {
	long sum = 0; 
	void consume(long n) {
		next.consume(n);
		if (n != -1) {
			sum += n;
		} else 
			System.out.println("Sum of numbers was " + sum + ".");
	}
}

capsule Min (Stage next) implements Stage {
	long min = Long.MAX_VALUE; 
	void consume(long n) {
		next.consume(n);
		if (n != -1) {
			if(n < min) min = n;
		} else 
			System.out.println("Min of numbers was " + min + ".");
	}
}

capsule Max (Stage next) implements Stage {
	long max = 0; 
	void consume(long n) {
		next.consume(n);
		if (n != -1) {
			if ( n > max) max = n;
		} else 
			System.out.println("Max of numbers was " + max + ".");
	}
}

capsule Sink(long num) implements Stage {
	long count = 0;
	void consume(long n) {
		if (n != -1) {
			count++;
		} else 
			System.out.println("Successful " + count + " runs!!");
	}
}

capsule Pipeline {
	design {
		Average avg; Sum sum; Min min; Max max; Sink snk;
		avg(sum); sum(min); min(max); max(snk); snk(500);
	}

	Random prng = new Random ();
	void run() {
		for (int j = 0; j < 500; j++) {
			long n = prng.nextInt(1024);
			avg.consume(n);
		}
		avg.consume(-1);
		yield(2);
	}
}