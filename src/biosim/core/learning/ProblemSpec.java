//ProblemSpec.java
package biosim.core.learning;

import biosim.core.body.Body;

public interface ProblemSpec{
	public double[] computeFeatures(Body b);
	public int getNumFeatures();
	public int getNumOutputs();
}