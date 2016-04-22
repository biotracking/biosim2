package biosim.app.fishreynolds;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import sim.util.MutableDouble2D;

import biosim.core.learning.LinregModel;
import biosim.core.body.AbstractFish;
import biosim.core.body.Body;

public class FSLRMVelocityFeatures extends FishSigmaLinregModel{

    static{
    	NUM_FEATURES=11;
    }

	public FSLRMVelocityFeatures(){
		this(NUM_FEATURES,3);
	}

	public FSLRMVelocityFeatures(int i, int j){
		super(i,j);
	}

	public double[] computeFeatures(Body b){
		if(b instanceof AbstractFish){
			AbstractFish fishBody = (AbstractFish)b;
			// order of sensors: sep, ori, coh, obs, bias
			MutableDouble2D sep = new MutableDouble2D();
			MutableDouble2D ori = new MutableDouble2D();
			MutableDouble2D coh = new MutableDouble2D();
			MutableDouble2D wall = new MutableDouble2D();
			fishBody.getAverageRBFSameTypeVec(sep,SEP_SIGMA);
			fishBody.getAverageRBFOrientationSameTypeVec(ori,ORI_SIGMA);
			fishBody.getAverageRBFSameTypeVec(coh,COH_SIGMA);
			fishBody.getNearestObstacleVec(wall);
			wall.multiplyIn(Math.exp(-wall.lengthSq()/(2.0*Math.pow(OBS_SIGMA,2))));
			double[] pvel = new double[3];
			fishBody.getSelfVelXYT(pvel);
			double[] sensors = new double[NUM_FEATURES+1]; // +1 for bias
			sensors[0] = sep.x;
			sensors[1] = sep.y;
			sensors[2] = ori.x;
			sensors[3] = ori.y;
			sensors[4] = coh.x;
			sensors[5] = coh.y;
			sensors[6] = wall.x * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(OBS_SIGMA,2)));
			sensors[7] = wall.y * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(OBS_SIGMA,2)));
			sensors[8] = pvel[0];
			sensors[9] = pvel[1];
			sensors[10] = pvel[2];
			sensors[11] = 1.0;
			return sensors;
		} else {
			throw new RuntimeException(b+" is not an AbstractFish");
		}
	}
}