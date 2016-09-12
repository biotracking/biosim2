//KNNModel.java

package biosim.core.learning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import ec.util.MersenneTwisterFast;

import biosim.core.util.FastKNN;

public class KNNModel implements LearnerAgent, RNGConsumer{
	protected FastKNN knn;
	public FastKNN getKNN(){
		return knn;
	}
	public void setKNN(FastKNN knn){
		this.knn = knn;
	}
	
	protected boolean normFeatures;
	public boolean getNormFeatures(){return normFeatures;}
	public void setNormFeatures(boolean nfp){normFeatures = nfp;}

	protected int k=3;
	public int getK(){return k;}
	public void setK(int k){this.k = k;}

	protected double kernelBandwidth;
	public double getBandwidth(){return kernelBandwidth;}
	public void setBandwidth(double b){this.kernelBandwidth=b;}


	public enum ComboMethod{
		AVERAGE, SAMPLE, KERNEL_SMOOTH, KERNEL_SAMPLE
	}
	protected ComboMethod method=ComboMethod.SAMPLE;
	public ComboMethod getMethod(){return method;}
	public void setMethod(String m){
		if(m.equalsIgnoreCase("AVERAGE")){
			method = ComboMethod.AVERAGE;
		} else if(m.equalsIgnoreCase("SAMPLE")){
			method = ComboMethod.SAMPLE;
		} else if(m.equalsIgnoreCase("KERNEL_SMOOTH")){
			method = ComboMethod.KERNEL_SMOOTH;
		} else{
			throw new RuntimeException("[KNNModel] trying to set unrecognized combination method: "+m);
		}
	}	

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
		rv.setK(getK());
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

	public void configure(Properties settings){
		setK(Integer.parseInt(settings.getProperty("K","3")));
		setMethod(settings.getProperty("COMBO_METHOD","SAMPLE"));
		setNormFeatures(Boolean.parseBoolean(settings.getProperty("NORMALIZE_FEATURES","FALSE")));
		setBandwidth(Double.parseDouble(settings.getProperty("KERNEL_BANDWIDTH","0.01")));
	}

	public Properties getSettings(){
		Properties settings = new Properties();
		settings.setProperty("K",new Integer(getK()).toString());
		settings.setProperty("COMBO_METHOD",getMethod().toString());
		settings.setProperty("NORMALIZE_FEATURES",new Boolean(getNormFeatures()).toString());
		settings.setProperty("KERNEL_BANDWIDTH",String.format("%f",getBandwidth()));
		return settings;
	}

	public KNNModel(){
		knn = null;
		featureNames = outputNames = null;
		normFeatures = true;
	}
	public KNNModel(int numFeatures, int numOutputs){
		knn = new FastKNN(numFeatures,numOutputs);
		featureNames = outputNames = null;
	}
	public KNNModel(FastKNN knn){
		this.knn=knn;
		featureNames = outputNames = null;
	}

	private double[] neighborSqDists(double[] features, double[][] neighbors){
		double[] rv = new double[neighbors.length];
		for(int i=0;i<rv.length;i++){
			rv[i] = 0.0;
			for(int j=0;j<neighbors[i].length;j++){
				rv[i] += Math.pow(features[j]-neighbors[i][j],2);
			}
		}
		return rv;
	}

	private double[] neighborDists(double[] features, double[][] neighbors){
		double[] rv = neighborSqDists(features,neighbors);
		for(int i=0;i<rv.length;i++){
			rv[i] = Math.sqrt(rv[i]);
		}
		return rv;
	}

	protected double kernel(double u, double h){
		//Gaussian with bandwidth h
		return Math.exp(-0.5*Math.pow(u,2)/h)/(h*Math.sqrt(2.0*Math.PI));
	}

	public double[] computeOutputs(double[] features, double[] outputs){
		if(knn == null){
			throw new RuntimeException("knn uninitialized!");
		}
		if(outputs==null){
			outputs = new double[knn.getClassDim()];
			for(int i=0;i<outputs.length;i++) outputs[i]=0.0;
		}
		double[][] neighbors = new double[getK()][knn.getClassDim()];
		double[][] neighbors_features = new double[getK()][features.length];
		knn.query(features,neighbors,null,neighbors_features);
		switch(method){
			case SAMPLE:
				int n_index;
				synchronized(getRandom()){
					n_index = getRandom().nextInt(getK());
				}
				// System.out.println(n_index+" "+neighbors[0].length+" "+neighbors.length+" "+outputs.length);
				System.arraycopy(neighbors[n_index],0,outputs,0,outputs.length);
				break;
			case AVERAGE:
				for(int i=0;i<getK();i++){
					for(int j=0;j<outputs.length;j++){
						outputs[j] += neighbors[i][j];
					}
				}
				for(int i=0;i<outputs.length;i++){
					outputs[i] = outputs[i]/(double)getK();
				}
				break;
			case KERNEL_SMOOTH:
				double[] dists = neighborDists(features,neighbors_features);
				double[] kernelWeights = new double[dists.length];
				double weightSum = 0.0;
				for(int i=0;i<kernelWeights.length;i++){
					kernelWeights[i] = kernel(dists[i],getBandwidth());
					weightSum += kernelWeights[i];
				}
				for(int i=0;i<getK();i++){
					for(int j=0;j<outputs.length;j++){
						outputs[j] += neighbors[i][j]*kernelWeights[i]/weightSum;
					}
				}
				break;
			case KERNEL_SAMPLE:
				double[] sampleDists = neighborDists(features,neighbors_features);
				double[] sampleVec = new double[sampleDists.length];
				double runningSum = 0.0;
				for(int i=0;i<sampleVec.length;i++){
					sampleVec[i] = runningSum+kernel(sampleDists[i],getBandwidth());
					runningSum = sampleVec[i];
				}
				double p;
				synchronized(getRandom()){
					p = getRandom().nextDouble();
				}
				int selectedNeighbor = 0;
				while(p>sampleVec[selectedNeighbor] && selectedNeighbor<(getK()-1)) selectedNeighbor++;
				System.arraycopy(neighbors[selectedNeighbor],0,outputs,0,outputs.length);
				break;
			default:
				throw new RuntimeException("[KNNModel] Unimplemented combination method:"+method);
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
		int lnCtr = 2;
		while(line != null){
			String[] splitLine = line.split(",");
			if(splitLine.length != sample.length+classes.length){
				throw new RuntimeException("[KNNModel] Invalid line. Columns: "+splitLine.length+" Expected "+(sample.length+classes.length)+" for line #"+lnCtr+"\nLine:\n"+line);
			}
			for(int i=0;i<sample.length;i++){
				sample[i] = Double.parseDouble(splitLine[i]);
			}
			for(int i=0;i<classes.length;i++){
				classes[i] = Double.parseDouble(splitLine[i+sample.length]);
			}
			knn.add(sample,classes);
			line = kNN_csv_data.readLine();
			lnCtr++;
		}
		if(normFeatures) {
			knn.sigmaNormalize();
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
		if(normFeatures){
			knn.sigmaNormalize();
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