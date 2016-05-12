//LinregModel.java

package biosim.core.learning;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;


public class LinregModel implements LearnerAgent{
	protected double[][] parameters;

	protected boolean useBias=true;
	public boolean getBias(){return useBias;}
	public void setBias(boolean p){useBias=p;}

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
		double[] realFeatures = features;
		if(outputs == null){
			outputs = new double[parameters[0].length];
		}
		if(useBias){
			if(features.length != parameters.length-1){
				throw new RuntimeException("Bias enabled, but features.length != parameters.length-1! Aborting");
			}
			realFeatures = new double[features.length+1];
			System.arraycopy(features,0,realFeatures,0,features.length);
			realFeatures[features.length] = 1.0;
		}
		for(int out=0; out<outputs.length;out++){
			outputs[out] = 0.0;
			for(int feat=0; feat<parameters.length; feat++){
				outputs[out] += realFeatures[feat]*parameters[feat][out];
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