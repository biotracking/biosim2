// BiasedLinearAgent.java

package biosim.app.fishreynolds;

import biosim.core.learning.LinregModel;
import biosim.core.learning.RNGConsumer;

import ec.util.MersenneTwisterFast;

public class BiasedLinearAgent extends LinregModel implements RNGConsumer{

	protected MersenneTwisterFast random=null;
	public MersenneTwisterFast getRandom(){return random;}
	public void setRandom(MersenneTwisterFast r){random=r;}

	public double[] computeOutputs(double[] features,double[] outputs){
		outputs = super.computeOutputs(features,outputs);
		outputs[0] = outputs[0]+ (0.1+random.nextGaussian()*0.01);
		return outputs;
	}
	/*
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
	*/
}