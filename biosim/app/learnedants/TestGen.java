import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class TestGen{
	public double[][] transitionFunction;
	public double[] prior;
	public double[] sigma;
	public double[] mu;
	public double switching;
	public int curState;
	Random random;
	public TestGen(double[][] transitionFunction, double[] prior, double[] sigma, double[] mu, double switching){
		this.transitionFunction = transitionFunction;
		this.prior = prior;
		this.sigma = sigma;
		this.mu = mu;
		this.switching = switching;
		curState = -1;
		random = new Random();
	}
		
	public double[] genOutput(){
		double randNum;
		double[] rv = new double[2];
		double switched, output;
		if(curState == -1){
			double sum = 0.0;
			randNum = random.nextDouble();
			for(int i=0;i<prior.length;i++){
				if(randNum > prior[i]+sum){
					sum+= prior[i];
				} else {
					curState = i;
					break;
				}
			}
			if(curState == -1) curState = prior.length-1;
		}
		output = (sigma[curState]*random.nextGaussian())+mu[curState];
		//figure out if we're switching
		randNum = random.nextDouble();
		if(randNum <= switching){
			switched = 1.0;
			randNum = random.nextDouble();
			double sum = 0.0;
			for(int i=0;i<transitionFunction[curState].length;i++){
				if(randNum > transitionFunction[curState][i]+sum){
					sum += transitionFunction[curState][i];
				} else {
					curState = i;
					break;
				}
			}
		} else {
			switched = 0.0;
		}
		rv[0] = output;
		rv[1] = switched;
		return rv;
	}
	
	public static void main(String[] args){
		try{
			double[][] tf = { {0.1, 0.9}, {0.9, 0.1} };
			double[] pr = {0.5, 0.5};
			double[] sigma = {1.0, 2.0};
			double[] mu = {2.0, 5.0};
			double switching = 0.25;
			TestGen tg = new TestGen(tf,pr,sigma,mu,switching);
			FileWriter outf = new FileWriter(new File("output.btf"));
			FileWriter switchf = new FileWriter(new File("switching.btf"));
			for(int i=0;i<10000;i++){
				double[] rv = tg.genOutput();
				outf.write(rv[0]+"\n");
				switchf.write((int)rv[1]+"\n");
			}
			outf.close();
			switchf.close();
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
