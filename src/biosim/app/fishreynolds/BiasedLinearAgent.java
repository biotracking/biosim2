// BiasedLinearAgent.java

package biosim.app.fishreynolds;

import biosim.core.learning.LinregModel;
import biosim.core.learning.RNGConsumer;

import ec.util.MersenneTwisterFast;

import java.util.Properties;

public class BiasedLinearAgent extends LinregModel implements RNGConsumer{

	protected MersenneTwisterFast random=null;
	public MersenneTwisterFast getRandom(){return random;}
	public void setRandom(MersenneTwisterFast r){random=r;}

	protected double sigma=0.01;
	public double getSigma(){return sigma;}
	public void setSigma(double s){sigma = s;}

	protected double mean=0.1;
	public double getMean(){return mean;}
	public void setMean(double m){mean = m;}

	public double[] computeOutputs(double[] features,double[] outputs){
		outputs = super.computeOutputs(features,outputs);
		outputs[0] = outputs[0]+ (mean+random.nextGaussian()*sigma);
		return outputs;
	}

	public void configure(Properties settings){
		super.configure(settings);
		setSigma(Double.parseDouble(settings.getProperty("SIGMA","0.01")));
		setMean(Double.parseDouble(settings.getProperty("MEAN","0.1")));
	}
}