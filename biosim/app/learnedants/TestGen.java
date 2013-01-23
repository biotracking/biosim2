import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class TestGen{
	public double[][][] transitionFunction;
	public double[] prior;
	public double[] sigma;
	public double[] mu;
	public double switching;
	public int curState;
	Random random;
	public TestGen(double[][][] transitionFunction, double[] prior, double[] sigma, double[] mu, double switching){
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
		double[] rv = new double[4];
		double switched, output, lastState = curState;
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
			lastState = curState;
		}
		//generate input
		double input = (random.nextInt(5)+1)*10;
		output = (sigma[curState]*random.nextGaussian())+mu[curState];
		//figure out if we're switching
		randNum = random.nextDouble();
		if(randNum <= switching){
			switched = 1.0;
		} else {
			switched = 0.0;
		}
		
		randNum = random.nextDouble();
		double sum = 0.0;
		for(int i=0;i<transitionFunction[(int)switched][curState].length;i++){
			if(randNum > transitionFunction[(int)switched][curState][i]+sum){
				sum += transitionFunction[(int)switched][curState][i];
			} else {
				curState = i;
				break;
			}
		}
		
		
		rv[0] = output;
		rv[1] = switched;
		rv[2] = lastState;
		return rv;
	}
	
	public static void main(String[] args){
		try{
			double[][][] tf = {{ {0.9, 0.1}, {0.1, 0.9} }, { {0.1, 0.9}, {0.9, 0.1} }};
			double[] pr = {0.5, 0.5};
			double[] sigma = {1.0, 2.0};
			double[] mu = {2.0, 5.0};
			double switching = 0.25;
			TestGen tg = new TestGen(tf,pr,sigma,mu,switching);
			FileWriter outf = new FileWriter(new File("output.btf"));
			FileWriter switchf = new FileWriter(new File("switching.btf"));
			FileWriter statef = new FileWriter(new File("state.btf"));
			for(int i=0;i<10000;i++){
				double[] rv = tg.genOutput();
				outf.write(rv[0]+"\n");
				switchf.write((int)rv[1]+"\n");
				statef.write((int)rv[2]+"\n");
			}
			outf.close();
			switchf.close();
			statef.close();
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
