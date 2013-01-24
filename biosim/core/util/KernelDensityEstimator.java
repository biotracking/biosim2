package biosim.core.util;

import java.util.ArrayList;
import java.util.Scanner;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;


public class KernelDensityEstimator{
	public ArrayList<Double> weights;
	public ArrayList<double[]> samples;
	public int dimensionality;
	public Kernel kernel;
		
	public KernelDensityEstimator(int dim, Kernel kernel){
		dimensionality = dim;
		samples = new ArrayList<double[]>();
		weights = new ArrayList<Double>();
		this.kernel = kernel;
	}
	
	public int numSamples(){ return samples.size(); }
	
	public void add(double[] sample){ add(sample,1.0); }
	
	public void add(double[] sample, double weight){
		double[] tmp_s = new double[sample.length];
		System.arraycopy(sample,0,tmp_s,0,tmp_s.length);
		for(int i=0;i<samples.size();i++){
			double[] doop = samples.get(i);
			boolean diff = false;
			for(int d=0;d<dimensionality;d++){
				if(tmp_s[d] != doop[d]){
					diff = true;
					break;
				}
			}
			if(!diff){
				weights.set(i,weights.get(i)+weight);
				return;
			}
		}
		samples.add(tmp_s);
		weights.add(weight);
	}
	
	public void setWeight(double[] sample, double weight){
		for(int i=0;i<samples.size();i++){
			double[] doop = samples.get(i);
			boolean diff = false;
			for(int d=0;d<dimensionality;d++){
				if(sample[d] != doop[d]){
					diff = true;
					break;
				}
			}
			if(!diff){
				weights.set(i,weight);
				return;
			}
		}
		double[] tmp_s = new double[sample.length];
		System.arraycopy(sample,0,tmp_s,0,tmp_s.length);
		samples.add(tmp_s);
		weights.add(weight);
	}
		
	public double estimate(double[] target, double bandwidth){
		double sum=0.0, weightSum=0.0;
		double[] tmp = new double[target.length];
		for(int i=0;i<samples.size();i++){
			for(int j=0;j<target.length;j++){
				tmp[j] = (target[j]-samples.get(i)[j])/bandwidth;
			}
			sum += weights.get(i)*kernel.k(tmp);
			weightSum += weights.get(i);
		}
		if(samples.size() == 0) return 0.0;
		return (1.0/(weightSum*bandwidth))*sum;
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
				KernelDensityEstimator kde = new KernelDensityEstimator(1,new NormalKernel(0.25));
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
					double estimate = kde.estimate(tmp,1);
					System.out.println(i+" "+estimate);
				}
				System.out.println("#Done!");
			} catch(IOException ioe){
				throw new RuntimeException(ioe);
			}
		}
	}
}
