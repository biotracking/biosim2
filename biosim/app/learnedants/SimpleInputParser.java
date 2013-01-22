package biosim.app.learnedants;

import biosim.core.util.BTFData;
import biosim.core.util.KernelDensityEstimator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SimpleInputParser extends BIOHMMInputParser {

	protected String[] output, switching,state;
	public static final int SEQMAX=1000;
	public SimpleInputParser(BTFData data){
		super(data);
		this.data = data;
		try{
			output = data.loadColumn("output");
			switching = data.loadColumn("switching");
			state = data.loadColumn("state");
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
	
	public double[] getDataAtIDX(int idx){
		double[] rv = new double[1];
		rv[0] = Double.parseDouble(output[idx].trim());
		return rv;
	}
	public double[] getSensorsAtIDX(int idx){
		double[] rv = {0.0};
		return rv;
	}
	public int getSwitchAtIDX(int idx){
		int k;
		k = Integer.parseInt(switching[idx].trim());
		return k;
	}
	public int partSize(){ return output.length; }
	public int numSwitches(){ return 1; }
	public int outputDim(){ return 1; }
	public int sensorDim(){ return 1; }
	
	public ArrayList<ArrayList<Integer>> getSequences(){
		if(foundSequences != null) return foundSequences;
		ArrayList<ArrayList<Integer>> sequences = new ArrayList<ArrayList<Integer>>();
		for(int j=0;j<partSize();j+=SEQMAX){
			ArrayList<Integer> tmp = new ArrayList<Integer>();
			for(int i=j;i<partSize()&&i-j<SEQMAX;i++){
				tmp.add(i);
			}
			sequences.add(tmp);
		}
		foundSequences = sequences;
		return sequences;
	}
	
	/* 
	public void initParameters(double[][][] transitionFunction, double[] prior, int[] partition, KernelDensityEstimator[] b){
		transitionFunction[0][0][0] = 0.5;//0.9;
		transitionFunction[0][0][1] = 0.5;//0.1;
		transitionFunction[0][1][0] = 0.5;//0.1;
		transitionFunction[0][1][1] = 0.5;//0.9;
		transitionFunction[1][0][0] = 0.5;//0.1;
		transitionFunction[1][0][1] = 0.5;//0.9;
		transitionFunction[1][1][0] = 0.5;//0.9;
		transitionFunction[1][1][1] = 0.5;//0.1;
		prior[0] = prior[1] = 0.5;
		for(int i=0;i<b.length;i++){
			b[i] = new KernelDensityEstimator(1, new KernelDensityEstimator.NormalKernel(1.0));
		}
		for(int i=0;i<partition.length;i++){
			partition[i] = Integer.parseInt(state[i]);
			b[partition[i]].add(getDataAtIDX(i));
		}
	}
	/* */
}
