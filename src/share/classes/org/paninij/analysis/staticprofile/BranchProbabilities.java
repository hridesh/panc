package org.paninij.analysis.staticprofile;

public class BranchProbabilities {

	int heuristic;
	// Both probabilities are represented in this structure for faster access.
	public float probabilityTaken;
	public float probabilityNotTaken; 
	String name;
	
	public BranchProbabilities (int h, float pt, float pnt, String n) {
		heuristic = h;
		probabilityTaken = pt;
		probabilityNotTaken = pnt;
		name = n;
	}
}
