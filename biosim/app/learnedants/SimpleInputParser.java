package biosim.app.learnedants;

import biosim.core.util.BTFData;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SimpleInputParser extends BIOHMMInputParser {

	protected String[] output, switching;
	public SimpleInputParser(BTFData data){
		super(data);
		this.data = data;
		try{
			output = data.loadColumn("output");
			switching = data.loadColumn("switching");
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
	
	public double[] getDataAtIDX(int idx){
		double[] rv = new double[1];
		rv[0] = Double.parseDouble(output[idx].trim());
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
	
	public ArrayList<ArrayList<Integer>> getSequences(){
		ArrayList<Integer> tmp = new ArrayList<Integer>();
		for(int i=0;i<partSize();i++){
			tmp.add(i);
		}
		ArrayList<ArrayList<Integer>> sequences = new ArrayList<ArrayList<Integer>>();
		sequences.add(tmp);
		return sequences;
	}
}
