//LinregModel.java

package biosim.core.learning;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;


public class LinregModel implements LearnerAgent{
	protected double[][] parameters;

	public LinregModel(){
		parameters = null;
	}
	public LinregModel(int numFeatures, int numOutputs){
		parameters = new double[numFeatures][numOutputs];
	}
	public LinregModel(double[][] params){
		parameters = new double[params.length][params[0].length];
		setParameters(params);
	}
	public double[][] getParameters(double[][] rv){
		for(int i=0;i<parameters.length;i++){
			System.arraycopy(parameters[i],0,rv[i],0,parameters[i].length);
		}
		return rv;
	}
	public void setParameters(double[][] src){
		for(int i=0;i<parameters.length;i++){
			System.arraycopy(src[i],0,parameters[i],0,parameters[i].length);
		}
	}
	public void setParameters(double val, int i, int j){
		parameters[i][j] = val;
	}

	public double[] computeOutputs(double[] features,double[] outputs){
		if(outputs == null){
			outputs = new double[parameters[0].length];
		}
		for(int out=0; out<outputs.length;out++){
			outputs[out] = 0.0;
			for(int feat=0; feat<parameters.length; feat++){
				outputs[out] += features[feat]*parameters[feat][out];
			}
		}
		return outputs;
	}

	public void loadParameters(BufferedReader source) throws IOException {
		ArrayList<String> coeffLines = new ArrayList<String>();
		for(String line=null; source.ready(); ){
			line = source.readLine();
			coeffLines.add(line);
		}
		int readNumCoeffs = coeffLines.size();
		int readNumOutputs = coeffLines.get(0).split(" ").length;
		double[][] newParameters = new double[readNumCoeffs][readNumOutputs];
		for(int i=0;i<readNumCoeffs;i++){
			String[] tmp = coeffLines.get(i).split(" ");
			for(int j=0;j<readNumOutputs;j++){
				newParameters[i][j] = Double.parseDouble(tmp[j]);
			}
		}
		parameters = newParameters;
	}
}