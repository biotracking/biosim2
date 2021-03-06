//LearnerAgent.java
package biosim.core.learning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Properties;

import biosim.core.body.Body;

public interface LearnerAgent{
	public void train(double[][] inputs, double[][] outputs);
	public double[] computeOutputs(double[] features, double[] outputs);
	public void loadParameters(BufferedReader source) throws IOException;
	public void saveParameters(BufferedWriter outf) throws IOException;
	public void configure(Properties settings);
	public Properties getSettings();
	public LearnerAgent deepCopy();
}