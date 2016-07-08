//LinregModel.java

package biosim.core.learning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

// import org.apache.commons.math.linear.*;
import org.jblas.*;

public class LinregModel implements LearnerAgent{
	protected double[][] parameters;

	protected boolean useBias=true;
	public boolean getBias(){return useBias;}
	public void setBias(boolean p){useBias=p;}

	public LearnerAgent deepCopy(){
		LinregModel rv = new LinregModel();
		rv.useBias = useBias;
		rv.parameters = new double[parameters.length][parameters[0].length];
		for(int i=0;i<parameters.length;i++){
			System.arraycopy(parameters[i],0,rv.parameters[i],0,parameters[0].length);
		}
		return rv;
	}

	public void configure(Properties settings){
		setBias(Boolean.parseBoolean(settings.getProperty("USE_BIAS","true")));
	}

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
				throw new RuntimeException("Bias enabled, but features.length ("+features.length+") != parameters.length-1 ("+(parameters.length-1)+")! Aborting");
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

	public void saveParameters(BufferedWriter outf) throws IOException {
		for(int i=0;i<parameters.length;i++){
			for(int j=0;j<parameters[0].length;j++){
				String spacer=" ";
				if(j==parameters[0].length-1) spacer="";
				outf.write(parameters[i][j]+spacer);
			}
			if(i!=parameters.length-1){
				outf.write("\n");
			}
		}
	}

	public void train(double[][] inputs, double[][] outputs){
		double[][] realInputs = inputs;
		if(useBias){
			realInputs = new double[inputs.length][inputs[0].length+1];
			for(int row=0;row<inputs.length;row++){
				System.arraycopy(inputs[row],0,realInputs[row],0,inputs[row].length);
				realInputs[row][inputs[row].length] = 1.0;
			}
		}
		// RealMatrix ins = MatrixUtils.createRealMatrix(realInputs);
		// RealMatrix outs = MatrixUtils.createRealMatrix(outputs);
		// DecompositionSolver solver = new SingularValueDecompositionImpl(ins).getSolver();
		// RealMatrix coeffs = solver.solve(outs);
		// parameters = coeffs.getData();
		parameters = Solve.solveLeastSquares(new DoubleMatrix(realInputs), new DoubleMatrix(outputs)).toArray2();
	}

	public static void main(String[] args){
		double[][] inputs = {{1.,2.,3.}, {4.,5.,6.}, {7.,8.,9.}};
		double[][] outputs = {{1.,2.,3.}, {4.,5.,6.}, {7.,8.,9.}};
		LinregModel lrm = new LinregModel();
		lrm.setBias(false);
		lrm.train(inputs,outputs);
		double[] test_out = null;
		test_out = lrm.computeOutputs(inputs[0],test_out);
		System.out.println("["+test_out[0]+", "+test_out[1]+", "+test_out[2]+"]");
	}
}