package AILib;

public class Fitness {
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