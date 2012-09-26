package AILib;

import java.util.Vector;

abstract public class Individual {

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