//LearnerAgent.java
package biosim.core.learning;

import java.io.BufferedReader;
import java.io.IOException;

import biosim.core.body.Body;

public interface LearnerAgent{
	public double[] computeOutputs(double[] features, double[] outputs);
	public void loadParameters(BufferedReader source) throws IOException;
}