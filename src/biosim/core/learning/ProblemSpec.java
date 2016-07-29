//ProblemSpec.java
package biosim.core.learning;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import biosim.core.body.Body;
import biosim.core.sim.Environment;
import biosim.core.util.BTFData;
import biosim.core.util.BTFDataLogger;

import ec.util.MersenneTwisterFast;


public interface ProblemSpec{
	public class Dataset{
		public double[][] features, outputs;
	}
	public double[] computeFeatures(Body b);
	public int getNumFeatures();
	public int getNumOutputs();
	public Dataset btf2array(BTFData btf);
	public Environment getEnvironment(LearnerAgent la, BTFData btf, Integer ignoredID);
	public BTFDataLogger getLogger();
	public LearnerAgent makeLearner();
	public ArrayList<PerformanceMetric> evaluate(ArrayList<BTFData> testSet, LearnerAgent learner, ExecutorService threadPool);
	public void configure(Properties p);
	public Properties getSettings();
	public long getSeed();
	public void setSeed(long seed);
	public MersenneTwisterFast getRNG();
}