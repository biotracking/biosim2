// DifferentialEvolution.java
package biosim.core.optimization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import ec.util.MersenneTwisterFast;

import biosim.core.util.ArgsToProps;

public class DifferentialEvolution{
	public static long timeout=2000;//2 seconds 
	public class DEResult{
		public DEIndividual best;
		// public ArrayList<Double[]> generationBest, bestSoFar;
		public DEResult(){
			best = null;
		}
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
		long curTime, lastTime = System.currentTimeMillis();
		while(!problem.stopIterations(iterationCtr,rv.best)){
			for(int i=0;i<problem.getPopSize();i++){
				problem.evaluate(children.get(i));
				if((parents != null) && (parents.get(i).fitness > children.get(i).fitness)){
					children.set(i,parents.get(i));
				}
				if(rv.best==null || children.get(i).fitness > rv.best.fitness){
					rv.best = children.get(i);
				}
			}
			curTime = System.currentTimeMillis();
			if(curTime - lastTime > timeout){
				System.out.println("Iteration "+iterationCtr+" best "+rv.best);
				lastTime = curTime;
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
				DEIndividual d, tmp = parents.get(b).sub(parents.get(c));
				tmp = tmp.scale(problem.getMutationRate());
				d = parents.get(a).add(tmp);
				tmp = problem.oneChildCrossover(d,parents.get(i));
				children.add(tmp);
				// children.add(problem.oneChildCrossover(parents.get(a).add(parents.get(b).sub(parents.get(c)).scale(problem.getMutationRate())),parents.get(i)));
			}
			iterationCtr++;
		}
		return rv;
	}
	public static void main(String[] args){
		try{
			Properties cmdLineArgs = ArgsToProps.parse(args);
			final int maxIters = Integer.parseInt(cmdLineArgs.getProperty("--maxIters","100"));
			final int popSize = Integer.parseInt(cmdLineArgs.getProperty("--popSize","100"));
			final int vecSize = Integer.parseInt(cmdLineArgs.getProperty("--vecSize","10"));

			DEProblem maxOnes = new DEProblem(){
				MersenneTwisterFast rng = new MersenneTwisterFast();
				public void evaluate(DEIndividual dei){
					dei.fitness = 0.0;
					for(int i=0;i<dei.phenotype.length;i++) dei.fitness += -Math.abs(1.0-dei.phenotype[i]);
				}
				public double getMutationRate(){ return 0.1;}
				public int getPopSize(){ return popSize; }
				public DEIndividual generateRandomIndividual(){
					DEIndividual rv = new DEIndividual();
					rv.phenotype = new double[vecSize];
					for(int i=0;i<rv.phenotype.length;i++) rv.phenotype[i] = getRandom().nextDouble();
					return rv;
				}
				public boolean stopIterations(int it, DEIndividual b){
					double sum = 0.0;
					if(b != null){
						for(int i=0;i<b.phenotype.length;i++){
							sum += b.phenotype[i];
						}
						if (sum == b.phenotype.length){
							System.out.println("Max individual found at iteration "+it);
							return true;
						}
					}
					return it >= maxIters;
				}
				public MersenneTwisterFast getRandom(){ return rng; }
				public DEIndividual oneChildCrossover(DEIndividual p1, DEIndividual p2){
					DEIndividual rv = new DEIndividual();
					rv.phenotype = new double[p1.phenotype.length];
					//uniform random crossover, choose one gene from p2 to keep
					boolean pickedP2 = false;
					for(int i=0;i<rv.phenotype.length;i++){
						if(getRandom().nextDouble() > 0.5){
							rv.phenotype[i] = p1.phenotype[i];
						} else {
							rv.phenotype[i] = p2.phenotype[i];
							pickedP2 = true;
						}
					}
					if(!pickedP2){
						int randFromP2 = getRandom().nextInt(rv.phenotype.length);
						rv.phenotype[randFromP2] = p2.phenotype[randFromP2];
					}
					return rv;
				}
			};
			DifferentialEvolution.DEResult der = (new DifferentialEvolution()).solve(maxOnes);
			System.out.println("Best individual found:");
			System.out.println(der.best);
		} catch(IOException ioe){
			throw new RuntimeException("[DifferentialEvolution] Failed test main max ones: "+ioe);
		}
	}
}