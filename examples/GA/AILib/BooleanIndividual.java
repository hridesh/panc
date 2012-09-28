package AILib;

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