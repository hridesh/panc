package AILib;

import java.util.ArrayList;

public class Generation {
	
	private final ArrayList<Individual> inds;

	/***
	 * Creates a generation of size <code>num</code> with individuals of type baseIndividual.
	 * @param num
	 * @param baseIndividual
	 */
	public Generation(int num, Individual baseIndividual) { 
		inds = new ArrayList<Individual>(num);
		for (int i = 0; i < num; i++) {
			Individual newIndividual = baseIndividual.getRandomIndividual();
			inds.add(newIndividual);
		}
		this.depth = 0;
	}

	/***
	 * Creates a generation of size equal to the argument generation <code>g</code>'s size.
	 * The type of the elements in the new generation are the same as in the original generation.
	 * @param g
	 */
	public Generation(Generation g) {
		inds = new ArrayList<Individual>(g.size());
		this.depth = g.depth + 1;
	}
	
	public Generation() {
		inds = new ArrayList<Individual>();
		this.depth = 0;
	}

	public int[] getFitnessArray() {
		int len = inds.size();
		int values[] = new int[len];
		for (int i = 0; i < len; i++) {
			try {
				values[i] = inds.get(i).getFitness();
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
		Individual fst = inds.get(fstIndex);
		Individual snd = inds.get(sndIndex);
		return new Parents(fst,snd);
	}

	public void add(Individual i) {
		inds.add(i);
	}
	
	public boolean add(Individual[] individuals){
		int len = individuals.length;
		for (int j = 0; j < len; j++)
			this.add(individuals[j]);
		return true;
	}

	public int size() {
		return inds.size();
	}
	
	public int getDepth(){ return depth; }
	private int depth = 0;
	
	private static final long serialVersionUID = 200123359115758587L;
}
