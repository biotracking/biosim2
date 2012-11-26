package biosim.core.util;
import biosim.core.util.annwrapper.SimpleANN;

import java.util.ArrayList;
import java.util.Scanner;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;


public class KernelDensityEstimator{
	private ArrayList<double[]> samples;
	private SimpleANN kdann;
	private int dimensionality;
	private Kernel kernel;
	
	static { System.loadLibrary("annwrapper"); }
	
	public KernelDensityEstimator(int dim, Kernel kernel){
		dimensionality = dim;
		kdann = new SimpleANN(dim);
		samples = new ArrayList<double[]>();
		this.kernel = kernel;
	}
	
	public void add(double[] sample){
		double[] tmp_s = new double[sample.length];
		System.arraycopy(sample,0,tmp_s,0,tmp_s.length);
		samples.add(tmp_s);
		kdann.add(sample);
	}
	
	public double estimate(double[] target, double bandwidth){
		double sum=0.0;
		double[] tmp = new double[target.length];
		for(int i=0;i<samples.size();i++){
			for(int j=0;j<target.length;j++){
				tmp[j] = (target[j]-samples.get(i)[j])/bandwidth;
			}
			sum += kernel.k(tmp);
		}
		return (1.0/(samples.size()*bandwidth))*sum;
	}
	
	public double adaptive_bandwidth_estimate(double[] target, double bandwidth, int k){
		double sum=0.0, d_jk=0.0;
		double[] tmp = new double[target.length];
		double[] kth_neighbor, current_sample;
		int[] neighborIDX = new int[k];
		for(int i=0;i<samples.size();i++){
			current_sample = samples.get(i);
			if(!kdann.query(current_sample, neighborIDX,k)) System.out.println("ANN query failed");
			kth_neighbor = samples.get(neighborIDX[k-1]);
			for(int j=0;j<current_sample.length;j++){
				d_jk += Math.pow(current_sample[j] - kth_neighbor[j],2);
			}
			d_jk = Math.sqrt(d_jk);
			for(int j=0;j<target.length;j++){
				tmp[j] = (target[j] - current_sample[j])/(bandwidth*d_jk);
			}
			sum += kernel.k(tmp)*(1.0/bandwidth*d_jk);
		}
		return (1.0/(samples.size()))*sum;
	}
	
	public static class NormalKernel implements Kernel{
		private double sigma;
		public NormalKernel(double sigma){
			this.sigma = sigma;
		}
		public double k(double[] point){
			double dim = (double)point.length;
			double determinant = Math.pow(sigma,dim);
			double coeff = (1.0/(Math.pow(2*Math.PI,dim/2.0)*Math.sqrt(determinant)));
			double exponent = 0.0;
			for(int i=0;i<dim;i++){
				exponent += Math.pow(point[i],2)*(1.0/sigma);
			}
			exponent = -0.5*exponent;
			return coeff*Math.exp(exponent);
		}
	}
	
	public static void main(String[] args){
		if(args.length != 1){
			System.out.println("Usage: java KernelDensityEstimator <SamplesFilename>");
		} else {
			try{
				KernelDensityEstimator kde = new KernelDensityEstimator(1,new NormalKernel(1));
				System.out.println("#Adding data");
				BufferedReader bRead = new BufferedReader(new FileReader(args[0]));
				String line = bRead.readLine();
				double[] tmp = new double[1];
				while(line != null && bRead.ready()){
					line.trim();
					Scanner scan = new Scanner(line);
					tmp[0] = scan.nextDouble();
					kde.add(tmp);
					line = bRead.readLine();
				}
				System.out.println("#Number of samples: "+kde.samples.size());
				System.out.println("#Generating estimated output, range:[-5,15), step size:0.01");
				for(double i = -5.0;i<15.0;i+= 0.01){
					tmp[0] = i;
					//double estimate = kde.estimate(tmp,1);
					double estimate = kde.adaptive_bandwidth_estimate(tmp,1,5);
					System.out.println(i+" "+estimate);
				}
				System.out.println("#Done!");
			} catch(IOException ioe){
				throw new RuntimeException(ioe);
			}
		}
	}
}
