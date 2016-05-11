//KNNModel.java

package biosim.core.learning;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import ec.util.MersenneTwisterFast;

import biosim.core.util.FastKNN;

public abstract class KNNModel implements LearnerAgent{
	protected FastKNN knn;
	public FastKNN getKNN(){
		return knn;
	}
	public void setKNN(FastKNN knn){
		this.knn = knn;
	}
	
	protected int k=3;
	public int getK(){return k;}
	public void setK(int k){this.k = k;}

	public enum ComboMethod{
		AVERAGE, SAMPLE
	}
	protected ComboMethod method=ComboMethod.SAMPLE;
	public ComboMethod getMethod(){return method;}
	public void setMethod(ComboMethod m){method=m;}	

	protected MersenneTwisterFast random=null;
	public MersenneTwisterFast getRandom(){return random;}
	public void setRandom(MersenneTwisterFast r){random=r;}

	public KNNModel(){
		knn = null;
	}
	public KNNModel(int numFeatures, int numOutputs){
		knn = new FastKNN(numFeatures,numOutputs);
	}
	public KNNModel(FastKNN knn){
		this.knn=knn;
	}

	public double[] computeOutputs(double[] features, double[] outputs){
		if(outputs==null){
			outputs = new double[knn.getSampleDim()];
			for(int i=0;i<outputs.length;i++) outputs[i]=0.0;
		}
		double[][] neighbors = new double[k][knn.getClassDim()];
		knn.query(features,neighbors);
		switch(method){
			case SAMPLE:
				int n_index = random.nextInt(k);
				System.arraycopy(neighbors[n_index],0,outputs,0,outputs.length);
				break;
			case AVERAGE:
				for(int i=0;i<k;i++){
					for(int j=0;j<outputs.length;j++){
						outputs[j] += neighbors[i][j];
					}
				}
				for(int i=0;i<outputs.length;i++){
					outputs[i] = outputs[i]/(double)k;
				}
				break;
		}
		return outputs;
	}
}