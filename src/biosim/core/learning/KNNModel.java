//KNNModel.java

package biosim.core.learning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import ec.util.MersenneTwisterFast;

import biosim.core.util.FastKNN;

public class KNNModel implements LearnerAgent{
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

	protected String[] featureNames, outputNames;
	public void setFeatureNames(String[] names){ featureNames = names;}
	public String[] getFeatureNames(){ return featureNames;}
	public void setOutputNames(String[] names){ outputNames = names;}
	public String[] getOutputNames(){ return outputNames;}

	public synchronized LearnerAgent deepCopy(){
		KNNModel rv = new KNNModel();
		if(knn!=null){
			rv.knn = new FastKNN(knn);
		}
		rv.k = k;
		rv.method = method;
		if(random != null){
			rv.random = (MersenneTwisterFast)(random.clone());
		}
		rv.featureNames = new String[featureNames.length];
		System.arraycopy(featureNames,0,rv.featureNames,0,featureNames.length);
		rv.outputNames = new String[outputNames.length];
		System.arraycopy(outputNames,0,rv.outputNames,0,outputNames.length);
		return rv;
	}

	public KNNModel(){
		knn = null;
		featureNames = outputNames = null;
	}
	public KNNModel(int numFeatures, int numOutputs){
		knn = new FastKNN(numFeatures,numOutputs);
		featureNames = outputNames = null;
	}
	public KNNModel(FastKNN knn){
		this.knn=knn;
		featureNames = outputNames = null;
	}

	public double[] computeOutputs(double[] features, double[] outputs){
		if(knn == null){
			throw new RuntimeException("knn uninitialized!");
		}
		if(outputs==null){
			outputs = new double[knn.getClassDim()];
			for(int i=0;i<outputs.length;i++) outputs[i]=0.0;
		}
		double[][] neighbors = new double[k][knn.getClassDim()];
		knn.query(features,neighbors);
		switch(method){
			case SAMPLE:
				int n_index = random.nextInt(k);
				// System.out.println(n_index+" "+neighbors[0].length+" "+neighbors.length+" "+outputs.length);
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

	public void loadParameters(BufferedReader kNN_csv_data) throws IOException{
		System.out.println("[KNNModel] Loading kNN data...");
		if(knn == null){
			throw new RuntimeException("knn not initialized! (knn==null) Aborting.");
		}
		if(kNN_csv_data == null){
			throw new RuntimeException("kNN_csv_data not initialized! (kNN_csv_data==null) Aborting.");
		}
		String[] loadedNames = kNN_csv_data.readLine().split(",");
		//loadedNames should match the order of the features used in .act(...) and end with outputs
		if(knn.getSampleDim()+knn.getClassDim() != loadedNames.length){
			System.out.println("[KNNModel] WARNING! Unexpected number of features: "+loadedNames.length+" (expected "+(knn.getSampleDim()+knn.getClassDim())+")");
		}
		double[] sample = new double[knn.getSampleDim()];
		double[] classes = new double[knn.getClassDim()];
		String line = kNN_csv_data.readLine();
		while(line != null){
			String[] splitLine = line.split(",");
			for(int i=0;i<sample.length;i++){
				sample[i] = Double.parseDouble(splitLine[i]);
			}
			for(int i=0;i<classes.length;i++){
				classes[i] = Double.parseDouble(splitLine[i+sample.length]);
			}
			knn.add(sample,classes);
			line = kNN_csv_data.readLine();
		}
		System.out.println("[KNNModel] Finished loading kNN.");
	}

	public void saveParameters(BufferedWriter outf) throws IOException{
		if(featureNames == null){
			featureNames = new String[knn.getSampleDim()];
			for(int i=0;i<featureNames.length;i++){
				featureNames[i] = "feat"+(i+1);
			}
		}
		if(outputNames == null){
			outputNames = new String[knn.getClassDim()];
			for(int i=0;i<outputNames.length;i++){
				outputNames[i] = "out"+(i+1);
			}
		}
		for(int i=0;i<featureNames.length;i++){
			outf.write(featureNames[i]);
			outf.write(",");
		}
		for(int i=0;i<outputNames.length;i++){
			outf.write(outputNames[i]);
			if(i!=outputNames.length-1){
				outf.write(",");
			}
		}
		outf.write("\n");
		ArrayList<double[]> samples = knn.getSamples();
		ArrayList<double[]> classes = knn.getClasses();
		double[] samp, outs;
		for(int i=0;i<samples.size();i++){
			samp = samples.get(i);
			outs = classes.get(i);
			for(int j=0;j<samp.length;j++){
				outf.write(samp[j]+",");
			}
			for(int j=0;j<outs.length;j++){
				outf.write(outs[j]+"");
				if(j!=outs.length-1){
					outf.write(",");
				}
			}
			if(i!=samples.size()-1){
				outf.write("\n");
			}
		}
	}
	public void train(double[][] inputs, double[][] outputs){
		knn = new FastKNN(inputs[0].length,outputs[0].length);
		for(int i=0;i<inputs.length;i++){
			knn.add(inputs[i],outputs[i]);
		}
	}

	public static void main(String[] args){
		long maxMem, allocd, freed;
		Runtime runtime = Runtime.getRuntime();		
		KNNModel knnm = new KNNModel();
		knnm.setRandom(new MersenneTwisterFast());
		double[][] inputs = {{1.,2.,3.},{4.,5.,6.},{7.,8.,9.},{1.,2.,3.},{4.,5.,6.},{7.,8.,9.},{1.,2.,3.},{4.,5.,6.},{7.,8.,9.}};
		double[][] outputs = {{1.,2.,3.},{4.,5.,6.},{7.,8.,9.},{1.,2.,3.},{4.,5.,6.},{7.,8.,9.},{1.,2.,3.},{4.,5.,6.},{7.,8.,9.}};
		maxMem = runtime.maxMemory();
		allocd = runtime.totalMemory();
		freed = runtime.freeMemory();
		System.out.println(maxMem+" "+allocd+" "+freed);
		knnm.train(inputs,outputs);
		System.out.println(maxMem+" "+allocd+" "+freed);
		knnm.train(inputs,outputs);
		System.out.println(maxMem+" "+allocd+" "+freed);
		knnm.train(inputs,outputs);
		System.out.println(maxMem+" "+allocd+" "+freed);
		knnm.train(inputs,outputs);
		double[] test_out = knnm.computeOutputs(inputs[0],null);
		System.out.println("["+test_out[0]+", "+test_out[1]+", "+test_out[2]+"]");
		maxMem = runtime.maxMemory();
		allocd = runtime.totalMemory();
		freed = runtime.freeMemory();
		System.out.println(maxMem+" "+allocd+" "+freed);
	}
}