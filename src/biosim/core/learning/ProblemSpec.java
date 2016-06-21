//ProblemSpec.java
package biosim.core.learning;

import java.util.ArrayList;

import biosim.core.body.Body;
import biosim.core.sim.Environment;
import biosim.core.util.BTFData;
import biosim.core.util.BTFDataLogger;

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
	public ArrayList<PerformanceMetric> evaluate(ArrayList<Dataset> testSet, LearnerAgent learner);
}