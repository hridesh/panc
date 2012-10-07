package AILib;

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
