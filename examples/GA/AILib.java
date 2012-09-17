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

library AILib {

	include java.util.Random;
	include java.util.Vector;
	include java.util.ArrayList;

	public abstract class Individual {

		private Parents _parents;

		private Vector<Individual> offspring;

		public Individual() {
			offspring = new Vector<Individual>(10, 10);
			_parents = new Parents();
		}

		public Individual(Individual i) {
			this();
		}

		public final void addOffspring(Individual i) {
			if (offspring.lastIndexOf(i) == -1)
				offspring.addElement(i);
		}

		public final void setParents(Parents _parents) {
			this._parents = _parents;
			_parents.fst.addOffspring(this);
			_parents.snd.addOffspring(this);
		}

		public final Parents getParents() {
			return _parents;
		}

		public final boolean isOffspringOf(Individual i) {
			return (i.offspring.lastIndexOf(this) != -1)
					&& (i.equals(_parents.fst) || i.equals(_parents.snd));
		}

		public final boolean isParentOf(Individual i) {
			return (offspring.lastIndexOf(i) != -1)
					&& (i._parents.fst.equals(this) || i._parents.snd.equals(this));
		}

		public abstract Individual getRandomIndividual();

		public abstract int getFitness();

		public abstract int getMaxFitness();

		public abstract Individual[] crossWith(Individual other);

		public abstract Individual getMutation();
	}

	public class BooleanIndividual extends Individual {
		protected boolean chromosome[];
		protected static int LENGTH = 20;

		public BooleanIndividual(boolean[] chromosome) {
			super();
			this.chromosome = chromosome;
		}

		public BooleanIndividual() {
			this(getRandomChromosome());
		}

		public BooleanIndividual(BooleanIndividual i) {
			this(i.getChromosome());
		}

		public boolean[] getChromosome() {
			return chromosome;
		}

		// function for character representation
		public String toString() {
			String ret = "";
			for (int i = 0; i < chromosome.length; i++)
				ret += chromosome[i] ? "T" : "F";
			return ret;
		}
		
		public int getFitness() {
			int health = 0;
			for (int i = 0; i < chromosome.length; i++)
				if (chromosome[i])
					health++;
			return health * health;
		}

		public int getMaxFitness() {
			return LENGTH * LENGTH;
		}

		public Individual getRandomIndividual() {
			return new BooleanIndividual(getRandomChromosome());
		}

		private static boolean[] getRandomChromosome() {
			boolean chromo[] = new boolean[LENGTH];
			for (int i = 0; i < chromo.length; i++) {
				chromo[i] = (Math.random() >= 0.5);
			}
			return chromo;
		}

		public Individual[] crossWith(Individual mom) {
			boolean dadsDNA[] = this.getChromosome();
			boolean momsDNA[] = ((BooleanIndividual) mom).getChromosome();
			int len = dadsDNA.length;
			if (len > momsDNA.length)
				len = momsDNA.length;
			boolean temp;
			for (int i = Util.getRandomInt(len); i < len; i++) {
				temp = dadsDNA[i];
				dadsDNA[i] = momsDNA[i];
				momsDNA[i] = temp;
			}
			Individual ret[] = new Individual[2];
			ret[0] = new BooleanIndividual(dadsDNA);
			ret[1] = new BooleanIndividual(momsDNA);
			return ret;
		}

		public Individual getMutation() {
			boolean DNA[] = this.getChromosome();
			for (int i = 0; i < DNA.length; i++)
				DNA[i] = (Math.random() < .10) ? !DNA[i] : DNA[i];
				return new BooleanIndividual(DNA);
		}
	}
	
	/***
	 * Generation: a collection of individuals
	 */
	public class Generation extends ArrayList<Individual> {

		/***
		 * Creates a generation of size <code>num</code> with individuals of type baseIndividual.
		 * @param num
		 * @param baseIndividual
		 */
		public Generation(int num, Individual baseIndividual) { 
			super(num);
			for (int i = 0; i < num; i++) {
				Individual newIndividual = baseIndividual.getRandomIndividual();
				this.add(newIndividual);
			}
			this.depth = 0;
		}

		/***
		 * Creates a generation of size equal to the argument generation <code>g</code>'s size.
		 * The type of the elements in the new generation are the same as in the original generation.
		 * @param g
		 */
		public Generation(Generation g) {
			super(g.size());
			this.depth = g.depth + 1;
		}

		public int[] getFitnessArray() {
			int len = size();
			int values[] = new int[len];
			for (int i = 0; i < len; i++) {
				try {
					values[i] = get(i).getFitness();
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Incorrect Indexing in Generation.getFitnessArray()");
				}
			}
			return values;
		}

		private Fitness cachedFitness = null;
		public Fitness getFitness() {
			if(cachedFitness == null)
				cachedFitness = new Fitness(this.getFitnessArray()); 
			return 	cachedFitness;
		}

		public Parents pickParents(){
			Fitness f = this.getFitness();
			int fstIndex = Util.chooseItem(f.values);
			int sndIndex = Util.chooseItem(f.values);
			if (fstIndex == sndIndex) // try once to make sure the parents are different
				sndIndex = Util.chooseItem(f.values);
			Individual fst = this.get(fstIndex); 
			Individual snd = this.get(sndIndex);
			return new Parents(fst,snd);
		}
		
		public boolean add(Individual[] individuals){
			int len = individuals.length;
			for (int j = 0; j < len; j++)
				this.add(individuals[j]);
			return true;
		}
		
		public int getDepth(){ return depth; }
		private int depth = 0;
		
		private static final long serialVersionUID = 200123359115758587L;
	}


	/***
	* Fitness of a generation.
 	*/

	class Fitness {
		int [] values;
    
		public Fitness(int[] values){
			this.values = values;
		}
    
		public float average(){
			return Util.average(values);
		}
    
		public float maximum(){
			return Util.maximum(values);
		}
	}


public class Parents{
	Individual fst; Individual snd; 

	public Parents(){
	  this.fst = this.snd = null; 
	}

	public Parents(Individual fst, Individual snd){
		  this.fst = fst; this.snd = snd; 
	}
	
	public Individual[] tryCrossOver(float crossOverProbability){
		if (Util.getRandomFloat() <= crossOverProbability) 
			return crossOver();
		return asOffsprings();		
	}

	public Individual[] crossOver(){
		Individual offspring[] = fst.crossWith(snd);
		int len = offspring.length;
		for (int j = 0; j < len; j++)
			offspring[j].setParents(this);
		return offspring;
	}
	
	public Individual[] asOffsprings(){
		Individual offspring[] = new Individual[]{ this.fst, this.snd };
		int len = offspring.length;
		for (int j = 0; j < len; j++)
			offspring[j].setParents(this);
		return offspring;
	}	
	
	public Individual[] tryMutation(float mutationProbability){
		Individual offsprings[] = new Individual[]{ this.fst, this.snd };
		if (Util.getRandomFloat() <= mutationProbability)
  		 offsprings[0] = offsprings[0].getMutation();
		if (Util.getRandomFloat() <= mutationProbability)
	  		 offsprings[0] = offsprings[0].getMutation();
		return offsprings;
	}
}	

	/***
	 * Utility methods
	 */
	public final class Util {
		public static Random rand = new Random(System.currentTimeMillis());

		public static int getRandomInt(int max) {
			return (int) (getRandomFloat() * (max + 1));
		}

		public static final int chooseItem(float[] list) {
			float probs[] = normalize(list);
			float p = getRandomFloat();
			int i;
			for (i = 0; (i < probs.length) && (p > probs[i]); i++)
				p -= probs[i];
			if (i >= probs.length)
				i = probs.length - 1;
			return i;
		}

		public static final int chooseItem(int[] list) {
			float probs[] = normalize(list);
			float p = getRandomFloat();
			int i;
			for (i = 0; (i < probs.length) && (p > probs[i]); i++)
				p -= probs[i];
			if (i >= probs.length)
				i = probs.length - 1;
			return i;
		}

		public static final float[] normalize(float[] vec) {
			return scale(vec, 1 / summation(vec));
		}

		public static final float[] normalize(int[] vec) {
			return scale(vec, 1 / (float) summation(vec));
		}

		public static final float summation(float[] vec) {
			float sum = 0;
			for (int i = 0; i < vec.length; i++)
				sum += vec[i];
			return sum;
		}

		public static final int summation(int[] vec) {
			int sum = 0;
			for (int i = 0; i < vec.length; i++)
				sum += vec[i];
			return sum;
		}

		public static float average(float[] vec) {
			return summation(vec) / (vec.length);
		}

		public static int average(int[] vec) {
			return (int) Math.rint(summation(vec) / (double) vec.length);
		}

		public static float[] scale(float[] vec, float scalar) {
			float ret[] = new float[vec.length];
			for (int i = 0; i < vec.length; i++)
				ret[i] = vec[i] * scalar;
			return ret;
		}

		public static float[] scale(int[] vec, float scalar) {
			float ret[] = new float[vec.length];
			for (int i = 0; i < vec.length; i++)
				ret[i] = vec[i] * scalar;
			return ret;
		}

		public static int[] scale(int[] vec, int scalar) {
			int ret[] = new int[vec.length];
			for (int i = 0; i < vec.length; i++)
				ret[i] = vec[i] * scalar;
			return ret;
		}

		public static float getRandomFloat() {
			return (float) rand.nextDouble();
		}

		public static float minimum(float[] vec) {
			if (vec.length != 0) {
				float min = vec[0];
				for (int i = 0; i < vec.length; i++)
					if (vec[i] < min)
						min = vec[i];
				return min;
			}
			return 0;
		}

		public static int minimum(int[] vec) {
			if (vec.length != 0) {
				int min = vec[0];
				for (int i = 0; i < vec.length; i++)
					if (vec[i] < min)
						min = vec[i];
				return min;
			}
			return 0;
		}

		public static float maximum(float[] vec) {
			if (vec.length != 0) {
				float max = vec[0];
				for (int i = 0; i < vec.length; i++)
					if (vec[i] > max)
						max = vec[i];
				return max;
			}
			return 0;
		}

		public static int maximum(int[] vec) {
			if (vec.length != 0) {
				int max = vec[0];
				for (int i = 0; i < vec.length; i++)
					if (vec[i] > max)
						max = vec[i];
				return max;
			}
			return 0;
		}

		public static int indexOfMinimum(float[] vec) {
			float prev = vec[0];
			int index = 0;
			for (int i = 1; i < vec.length; i++) {
				if (prev < vec[i]) {
					prev = vec[i];
					index = i;
				}
			}
			return index;
		}

		public static int indexOfMinimum(int[] vec) {
			int prev = vec[0];
			int index = 0;
			for (int i = 1; i < vec.length; i++) {
				if (prev < vec[i]) {
					prev = vec[i];
					index = i;
				}
			}
			return index;
		}

		public static int indexOfMaximum(float[] vec) {
			float prev = vec[0];
			int index = 0;
			for (int i = 1; i < vec.length; i++) {
				if (prev > vec[i]) {
					prev = vec[i];
					index = i;
				}
			}
			return index;
		}

		public static int indexOfMaximum(int[] vec) {
			int prev = vec[0];
			int index = 0;
			for (int i = 1; i < vec.length; i++) {
				if (prev > vec[i]) {
					prev = vec[i];
					index = i;
				}
			}
			return index;
		}
	}
}
