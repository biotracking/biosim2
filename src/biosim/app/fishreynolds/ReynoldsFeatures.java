package biosim.app.fishreynolds;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import sim.util.MutableDouble2D;

import biosim.core.body.AbstractFish;
import biosim.core.body.Body;
import biosim.core.learning.ProblemSpec;

public class ReynoldsFeatures implements ProblemSpec{

    //sepX,sepY,oriX,oriY,cohX,cohY,obsX,obsY,pvelX,pvelY,pvelT
    public static final int NUM_FEATURES=11;
    //X, Y, T
    public static final int NUM_OUTPUTS=3;

	public double sep_sigma, ori_sigma, coh_sigma, obs_sigma;

	public static Properties defaults(){
		Properties defaultProps = new Properties();
		defaultProps.setProperty("SEP_SIGMA","0.1");
		defaultProps.setProperty("ORI_SIGMA","0.2");
		defaultProps.setProperty("COH_SIGMA","1.0");
		defaultProps.setProperty("OBS_SIGMA","0.05");
		return defaultProps;
	}

	public ReynoldsFeatures(){
		loadFeatureSigma(defaults());
	}

    public void loadFeatureSigma(BufferedReader featuresSource) throws IOException{
    	Properties props = new Properties(defaults());
    	props.load(featuresSource);
    	loadFeatureSigma(props);
    }

    public void loadFeatureSigma(Properties props){
    	sep_sigma = Double.parseDouble(props.getProperty("SEP_SIGMA"));
    	ori_sigma = Double.parseDouble(props.getProperty("ORI_SIGMA"));
    	coh_sigma = Double.parseDouble(props.getProperty("COH_SIGMA"));
    	obs_sigma = Double.parseDouble(props.getProperty("OBS_SIGMA"));
    }

    public int getNumFeatures(){ return NUM_FEATURES;}
    public int getNumOutputs(){ return NUM_OUTPUTS;}

    public double[] computeFeatures(Body b){
        if(b instanceof AbstractFish){
            AbstractFish fishBody = (AbstractFish)b;
            // order of sensors: sep, ori, coh, obs, bias
            MutableDouble2D sep = new MutableDouble2D();
            MutableDouble2D ori = new MutableDouble2D();
            MutableDouble2D coh = new MutableDouble2D();
            MutableDouble2D wall = new MutableDouble2D();
            fishBody.getAverageRBFSameTypeVec(sep,sep_sigma);
            fishBody.getAverageRBFOrientationSameTypeVec(ori,ori_sigma);
            fishBody.getAverageRBFSameTypeVec(coh,coh_sigma);
            fishBody.getNearestObstacleVec(wall);
            wall.multiplyIn(Math.exp(-wall.lengthSq()/(2.0*Math.pow(obs_sigma,2))));
            double[] pvel = new double[NUM_OUTPUTS];
            fishBody.getSelfVelXYT(pvel);
            double[] sensors = new double[NUM_FEATURES];
            sensors[0] = sep.x;
            sensors[1] = sep.y;
            sensors[2] = ori.x;
            sensors[3] = ori.y;
            sensors[4] = coh.x;
            sensors[5] = coh.y;
            sensors[6] = wall.x * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(obs_sigma,2)));
            sensors[7] = wall.y * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(obs_sigma,2)));
            sensors[8] = pvel[0];
            sensors[9] = pvel[1];
            sensors[10] = pvel[2];
            // let linreg do it's own thing re: bias
            // sensors[11] = 1.0;
            return sensors;
        } else {
            throw new RuntimeException(b+" is not an AbstractFish");
        }
    }
}