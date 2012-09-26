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
 * http://paninij.org/
 *
 * Contributor(s): Hridesh Rajan
 */
import AILib.*;

module CrossOver (float probability, int max) {
	Generation compute(Generation g) {
		int genSize = g.size();		
		Generation g1 = new Generation(g);
		for (int i = 0; i < genSize; i += 2) {						
			Parents p = g.pickParents();				
			g1.add(	p.tryCrossOver(probability) );
		}
		return g1;
	}
}

module Mutation (float probability, int max) {

	Generation mutate(Generation g) {
		int genSize = g.size();
		Generation g1 = new Generation(g);
		for (int i = 0; i < genSize; i += 2) {						
			Parents p = g.pickParents();										
			g1.add(	p.tryMutation(probability));
		}
		return g1;
	}
}

module Fittest {
	Generation last;
	
	void check(Generation g) {
		if(last ==null) last = g;
		else {
			Fitness f1 = g.getFitness();
			Fitness f2 = last.getFitness();
			if( f1.average() > f2.average() ) last = g;
		}
	}
}

module Logger {
	include AILib;
	void logit(Generation g) {
		logGeneration(g);
	}
	long generationNumber = 0; 
	void logGeneration(Generation g){
		System.out.println("********************************************");
		System.out.println("Generation #"+(generationNumber++));
		Fitness f = g.getFitness();
		System.out.println("Average fitness=" + f.average());
		System.out.println("Maximum fitness="+f.maximum());		
	}	
}

system GA {
	CrossOver c; Mutation m; Fittest f; Logger l ; 
        c(0.9f, 13);
	m(0.0001f, 13);

/*
	public void runGeneticAlgorithm(){
		Individual individual = new BooleanIndividual();
		Generation init = new Generation(100, individual);
		int maxDepth = 13;
		System.out.println("********************************************");
		System.out.println("********FINAL***********RESULTS*************");
		System.out.println("********************************************");
	}
*/
}
