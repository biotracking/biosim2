// DifferentialEvolution.java
package biosim.core.optimization;

import java.util.ArrayList;

import ec.util.MersenneTwisterFast;

public class DifferentialEvolution{
	public class DEIndividual{
		public double[] phenotype;
		public Double fitness;
		public DEIndividual(){
			phenotype = null;
			fitness = null;
		}
		// public DEIndividual add(DEIndividual other){}
	}
	public class DEResult{
		public DEIndividual best;
		// public ArrayList<Double[]> generationBest, bestSoFar;
		public DEResult(){
			best = null;
		}
	}
	public interface DEProblem{
		void evaluate(DEIndividual individual);
		double getMutationRate();
		int getPopSize();
		DEIndividual generateRandomIndividual();
		boolean stopIterations(int iteration, DEIndividual best);
		MersenneTwisterFast getRandom();
		DEIndividual oneChildCrossover(DEIndividual p1, DEIndividual p2);
	}
	public DEResult solve(DEProblem problem){
		// Taken from "Essentials of Metaheuristics", Second Edition
		// https://cs.gmu.edu/~sean/book/metaheuristics/
		DEResult rv = new DEResult();
		ArrayList<DEIndividual> children, parents;
		parents = null;
		children = new ArrayList<DEIndividual>();
		for(int i=0;i<problem.getPopSize();i++){
			children.add(problem.generateRandomIndividual());
		}
		int iterationCtr = 0;
		while(!problem.stopIterations(iterationCtr,rv.best)){
			for(int i=0;i<problem.getPopSize();i++){
				problem.evaluate(children.get(i));
				if((parents != null) && (parents.get(i).fitness > children.get(i).fitness)){
					children.set(i,parents.get(i));
				}
				if(rv.best==null || children.get(i).fitness > rv.best.fitness){
					rv.best = children.get();
				}
			}
			parents = children;
			children = new ArrayList<DEIndividual>();
			for(int i=0;i<problem.getPopSize();i++){
				int a = i;
				while(a==i) a= problem.getRandom().nextInt(problem.getPopSize());
				int b = i;
				while(b==i || b==a) b = problem.getRandom().nextInt(problem.getPopSize());
				int c = i;
				while(c==i || c==a || c==b) c = problem.getRandom().nextInt(problem.getPopSize());
			}
		}
		return rv;
	}
}