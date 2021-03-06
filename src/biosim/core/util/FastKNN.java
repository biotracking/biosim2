package biosim.core.util;
import biosim.core.util.annwrapper.SimpleANN;

import java.util.ArrayList;
import java.util.Scanner;
import java.io.FileReader;
import java.io.BufferedReader;

public class FastKNN{
	private ArrayList<double[]> samples,classes;
	public ArrayList<double[]> getSamples(){return samples;}
	public ArrayList<double[]> getClasses(){return classes;}
	private ArrayList<Double> weights;
	private SimpleANN kdann;
	private int sample_dim, class_dim;
	private static boolean libLoaded = false;
	private double[] featureWeights;
	private double eps=0.0;
	
	public FastKNN(FastKNN copyFrom){
		//DEEEEEEEEP COPYYYYYYYY
		this.samples = new ArrayList<double[]>(copyFrom.samples);
		this.classes = new ArrayList<double[]>(copyFrom.classes);
		this.weights = new ArrayList<Double>(copyFrom.weights);
		kdann = new SimpleANN(copyFrom.kdann);
		sample_dim = copyFrom.sample_dim;
		class_dim = copyFrom.class_dim;
		featureWeights = new double[copyFrom.featureWeights.length];
		System.arraycopy(copyFrom.featureWeights,0,featureWeights,0,featureWeights.length);
		eps = copyFrom.eps;
	}

	public FastKNN(int sample_dim,int class_dim){
		if(!libLoaded){
			System.loadLibrary("annwrapper");
			libLoaded = true;
		}
		this.sample_dim = sample_dim;
		this.class_dim = class_dim;
		kdann = new SimpleANN(sample_dim);
		samples = new ArrayList<double[]>();
		classes = new ArrayList<double[]>();
		weights = new ArrayList<Double>();
		featureWeights = new double[sample_dim];
		for(int i=0;i<featureWeights.length;i++) featureWeights[i] = 1.0;
	}
	
	public int getSampleDim(){return sample_dim;}
	public int getClassDim(){return class_dim;}
	public void add(double[] sample, double[] class_vec){
		add(sample,class_vec,1.0);
	}
	public void add(double[] sample, double[] class_vec, double weight){
		double[] tmp_c = new double[class_dim];
		double[] tmp_s = new double[sample_dim];
		double[] tmp_s_weighted = new double[sample_dim];
		System.arraycopy(class_vec,0,tmp_c,0,class_dim);
		System.arraycopy(sample,0,tmp_s,0,sample_dim);
		//System.out.print("[ ");
		for(int i=0;i<sample_dim;i++){
			tmp_s_weighted[i] = (sample[i]*featureWeights[i]);
			//System.out.print(tmp_s_weighted[i]+" ");
		}
		//System.out.println("]");
		samples.add(tmp_s);
		classes.add(tmp_c);
		weights.add(weight);
		kdann.add(tmp_s_weighted);
		/*
		System.out.print("added [");
		for(int i=0;i<tmp_s.length;i++)
			System.out.print(" "+tmp_s[i]);
		System.out.println(" ]");
		*/
	}
	
	public int numSamples(){ return samples.size(); }

	public void setEps(double eps){ 
		this.eps = eps;
		// kdann.setEpsilon(eps); 
	}

	public void setFeatureWeights(double[] f_w){
		for(int i=0;i<sample_dim;i++) featureWeights[i] = f_w[i];
		kdann = new SimpleANN(sample_dim);
		// kdann.setEpsilon(eps);
		for(int i=0;i<samples.size();i++){
			double[] tmp_s = new double[sample_dim];
			for(int j=0;j<sample_dim;j++){
				tmp_s[j] = samples.get(i)[j]*featureWeights[j];
			}
			kdann.add(tmp_s);
		}
	}
	
	public void sigmaNormalize(){
		double[] sampleAvg = new double[sample_dim];
		double[] sigma = new double[sample_dim];
		//double[] sampleStdDev = new double[sample_dim];
		for(int i=0;i<sample_dim;i++){ 
			sampleAvg[i] = 0.0;
			sigma[i] = 0.0;
		}
		for(int i=0;i<samples.size();i++){
			for(int j=0;j<sample_dim;j++){
				sampleAvg[j] += samples.get(i)[j];
			}
		}
		for(int i=0;i<sample_dim;i++) sampleAvg[i] =sampleAvg[i]/samples.size();
		for(int i=0;i<samples.size();i++){
			for(int j=0;j<sample_dim;j++){
				sigma[j] += Math.pow(samples.get(i)[j]-sampleAvg[j],2);
			}
		}
		boolean sigmaWasZero = false;
		for(int i=0;i<sample_dim;i++) {
			if(sigma[i]==0.0){
				sigmaWasZero = true;
			}
			sigma[i] = (sigma[i]==0.0)? 1.0:1.0/Math.sqrt(sigma[i]/samples.size());
		}
		if(sigmaWasZero){
			System.err.println("[FastKNN] WARNING: ignoring zero variance normalization");
			System.err.print("normalizing weights: [");
			for(int i=0;i<sample_dim;i++) System.err.print(" "+sigma[i]);
			System.err.println(" ]");
		}
		setFeatureWeights(sigma);
	}
	
	public void query(double[] sample, double[][] neighbor_classes){
		query(sample,neighbor_classes,null,null);
	}
	public void query(double[] sample, double[][] neighbor_classes, double[] weight_vec, double[][] neighbor_values){
		double[] tmp_s = new double[sample_dim];
		for(int i=0;i<sample_dim;i++) tmp_s[i] = sample[i]*featureWeights[i];
		int k = neighbor_classes.length;
		int[] neighborIdx = new int[k];
		if(!kdann.query(sample,neighborIdx,k)) System.err.println("ANN query failed!");
		for(int i=0;i<k;i++){ 
			//System.out.print("IDX: "+neighborIdx[i]+":[");
			for(int j=0;j<class_dim;j++){
				neighbor_classes[i][j] = classes.get(neighborIdx[i])[j];
				//System.out.print(" "+samples.get(neighborIdx[i])[j]);
			}
			if(weight_vec != null){
				weight_vec[i] = weights.get(neighborIdx[i]);
			}
			if(neighbor_values != null){
				for(int j=0;j<sample_dim;j++){
					neighbor_values[i][j] = samples.get(i)[j];
				}
			}
			//System.out.println("]");
		}
		//System.out.println();
		
	}
	
	/*
	public void query_point(double[] sample, double[][] neighbors){
		int k = neighbors.length;
		int[] neighborIdx = new int[k];
		if(!kdann.query(sample,neighborIdx,k)) System.err.println("Ann query failed!");
		for(int i=0;i<k;i++){
			
		}
	}
	*/

	public void printEverything(){
		for(int i=0;i<samples.size();i++){
			System.out.print("IDX:"+i+" [");
			for(int j=0;j<sample_dim;j++){
				System.out.print(" "+samples.get(i)[j]);
			}
			System.out.print("] -> [");
			for(int j=0;j<class_dim;j++){
				System.out.print(" "+classes.get(i)[j]);
			}
			System.out.println("]");
		}
	}
	
	public static void main(String[] args){
		if(args.length<2){
			System.out.println("Usage FastKNN trainfile1 [train2 train3...] testfile");
			return;
		}
		String[] training = new String[args.length-1];
		for(int i=0;i<args.length-1;i++)
			training[i] = args[i];
		String testing = args[args.length-1];
		System.out.println("Training set:");
		for(int i=0;i<training.length;i++)
			System.out.println("\t"+training[i]);
		System.out.println("\nTesting set:");
		System.out.println(testing);
		FastKNN knn = new FastKNN(2,1);
		System.out.println("KNN created");
		double[] clsses = new double[1];
		double[] foo = new double[2];
		double[][] neighbors = new double[10][1];
		try {
			System.out.println("Adding training set");
			for(int i=0;i<training.length;i++){
				BufferedReader bRead = new BufferedReader(new FileReader(training[i]));
				String line = bRead.readLine();
				while(line != null && bRead.ready()){
					line.trim();
					Scanner trainFile = new Scanner(line);
					clsses[0] = trainFile.nextInt();
					foo[0] = trainFile.nextDouble();
					foo[1] = trainFile.nextDouble();
					knn.add(foo,clsses);
					line = bRead.readLine();
				}
			}
			System.out.println("Number of samples: "+knn.samples.size());
			//knn.printEverything();
			System.out.println("Running test set");
			BufferedReader bRead = new BufferedReader(new FileReader(testing));
			String line = bRead.readLine();
			while(line != null && bRead.ready()){
				line.trim();
				Scanner testFile = new Scanner(line);
				clsses[0] = testFile.nextInt();
				foo[0] = testFile.nextDouble();
				foo[1] = testFile.nextDouble();
				knn.query(foo,neighbors);
				System.out.print("Neighbors [ ");
				for(int i=0;i<neighbors.length;i++)
					System.out.print(neighbors[i][0]+" ");
				System.out.println("] Real: "+clsses[0]);
				line = bRead.readLine();
			}
		} catch(Exception e){
			e.printStackTrace();
			System.out.println(e);
		}
		
	}
}
