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

/* 
 * @test
 * @summary Compile the Pipeline example.
 * @compile Pipeline.java

 */

signature Stage {
    void consume(long n);
    void report();
}

capsule Pipeline () {
    int num = 500;

    design {
        Average avg; Sum sum; Min min; Max max; Sink snk;
        avg(sum); sum(min); min(max); max(snk); snk(num);
    }

    Random prng = new Random ();
    void run() {
        for (int j = 0; j < num; j++) {
            long n = prng.nextInt(1024);
            avg.consume(n);
        }
        avg.report();
        }
}

capsule Average (Stage next) implements Stage {
    long average = 0; 
    long count = 0; 

    void consume(long n) {
        next.consume(n);
        average = ((count * average) + n) / (count + 1);
        count++;
    }

    void report(){
        next.report();
        System.out.println("Average of " + count + " numbers was " + average + ".");
    }
}

capsule Sum (Stage next) implements Stage {
    long sum = 0; 
    void consume(long n) {
        next.consume(n);
        sum += n;
    }

    void report(){
        next.report();
        System.out.println("Sum of numbers was " + sum + ".");
    }
}

capsule Min (Stage next) implements Stage {
    long min = Long.MAX_VALUE; 
    void consume(long n) {
        next.consume(n);
        if(n < min) min = n;
    }

    void report(){
        next.report();
        System.out.println("Min of numbers was " + min + ".");
    }
}

capsule Max (Stage next) implements Stage {
    long max = 0; 
    void consume(long n) {
        next.consume(n);
        if ( n > max) max = n;
    }

    void report(){
        next.report();
        System.out.println("Max of numbers was " + max + ".");
    }
}

capsule Sink(long num) implements Stage {
    long count = 0;
    void consume(long n) {
        count++;
    }

    void report(){
        if (count != num)
            throw new RuntimeException("count should be: " + num + "; but was: " + count);
        System.out.println("Successful " + count + " runs!!");
    }
}

