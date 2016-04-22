//LearnerAgent.java
package biosim.core.learning;

import biosim.core.body.Body;

public interface LearnerAgent{
	public double[] computeFeatures(Body b);
	public double[] computeOutputs(double[] features, double[] outputs);
}