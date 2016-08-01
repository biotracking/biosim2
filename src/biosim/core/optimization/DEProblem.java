package biosim.core.optimization;

import ec.util.MersenneTwisterFast;

public interface DEProblem{
	public void evaluate(DEIndividual individual);
	public double getMutationRate();
	public int getPopSize();
	public DEIndividual generateRandomIndividual();
	public boolean stopIterations(int iteration, DEIndividual best);
	public MersenneTwisterFast getRandom();
	public DEIndividual oneChildCrossover(DEIndividual p1, DEIndividual p2);
}