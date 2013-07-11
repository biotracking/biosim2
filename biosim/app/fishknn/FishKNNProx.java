package biosim.app.fishknn;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractFish;
import biosim.core.body.NotemigonusCrysoleucas;
import biosim.core.gui.GUISimulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;
import biosim.core.util.BTFData;
import biosim.core.util.FastKNN;

import sim.util.MutableDouble2D;
import sim.util.Double3D;

import java.io.File;
import java.io.IOException;


public class FishKNNProx implements Agent{
	AbstractFish fishBody;
	public static final int FEATURES=15;//8 proximity, nearest neighbor, neighborhood CoM, avgDist bool, nearest obst.
	public static final int CLASSES=3;
	public static final int KNN_NEIGHBORS=10;
	public static final boolean MIRROR_TRAINING_DATA=true;
	public static final boolean USE_WEIGHTS=true;
	//note that we're weighting features before the distance calculation,
	//so the weights effectively wind up getting squared.
	public static final double[] FEATURE_WEIGHTS={	1.0, //prox1 
													1.0, //prox2
													1.0, //prox3
													1.0, //prox4
													1.0, //prox5
													1.0, //prox6
													1.0, //prox7
													1.0, //prox8
													0.0, //nnX
													0.0, //nnY
													1.0, //avgX
													1.0, //avgY
													Math.sqrt(10.0), //avgDist (thresholded at 3 body lengths)
													1.0, //wallX
													1.0, //wallY
												};
	FastKNN knn;
	double prevTime = 0.0;
	public FishKNNProx(AbstractFish b, FastKNN knn){
		fishBody = b;
		this.knn = knn;
	}
	public void init(){
		//System.gc();
	}
	public void finish(){
	}
	public void act(double time){
		double[] rv = new double[3];
		MutableDouble2D avgFish = new MutableDouble2D();
		MutableDouble2D nnFish = new MutableDouble2D();
		fishBody.getNearestSameAgentVec(nnFish);
		boolean sawFish = fishBody.getAverageSameAgentVec(avgFish);
		if(!sawFish) avgFish.x = avgFish.y = 0.0;
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = fishBody.getNearestObstacleVec(wall);
		if(!sawWall) wall.x = wall.y = 0.0;
		double[] features = new double[FEATURES];
		double[][] nearestK = new double[KNN_NEIGHBORS][CLASSES];
		double[] prox = null;
		prox = fishBody.getProximity(prox);
		System.arraycopy(prox,0,features,0,prox.length);
		features[8] = nnFish.x;
		features[9] = nnFish.y;
		double avgFishLen = avgFish.length();
		if(avgFishLen > 0){
			avgFish.x = avgFish.x/avgFishLen;
			avgFish.y = avgFish.y/avgFishLen;
		}
		features[10] = avgFish.x;
		features[11] = avgFish.y;
		features[12] = (avgFishLen>NotemigonusCrysoleucas.SIZE*3)?1.0:0.0;
		features[13] = wall.x;
		features[14] = wall.y;
		//System.out.println("Sensor vec: ["+sensorVec[0]+", "+sensorVec[1]+", "+sensorVec[2]+", "+sensorVec[3]+"]");
		knn.query(features,nearestK);
		//sample
		/**/ 
		int rnd_idx = fishBody.getRandom().nextInt(nearestK.length);
		for(int i=0;i<CLASSES;i++) rv[i] = nearestK[rnd_idx][i];
		fishBody.setDesiredVelocity(rv[0],rv[1],rv[2]);
	}
	
	public static FastKNN loadKNN(BTFData btf) throws IOException{
		System.out.println("[FishKNNProx] Loading BTF data...");
		FastKNN knn = new FastKNN(FEATURES,CLASSES);
		if(USE_WEIGHTS){
			knn.setFeatureWeights(FEATURE_WEIGHTS);
		}
		String[] wallVec = btf.loadColumn("wallvec");
		String[] nnVec = btf.loadColumn("nnvec");
		String[] avgNNVec = btf.loadColumn("avgnnvec");
		String[] dvel = btf.loadColumn("dvel");
		String[] dbool = btf.loadColumn("dbool");
		String[] oct = btf.loadColumn("oct");
		//String[] zonevecs = btf.loadColumn("zonevecs");
		int numRows = dvel.length;
		double[] sample = new double[FEATURES];
		double[] flipped_sample = new double[FEATURES];
		double[] classes = new double[CLASSES];
		double[] flipped_classes = new double[CLASSES];
		for(int i=0;i<numRows;i++){
			//if(i%(numRows/10)==0) System.out.println("[FishKNNProx] "+i+"/"+numRows);
			String[] tmp = dbool[i].split(" ");
			if(Boolean.parseBoolean(tmp[0])){
				tmp = dvel[i].split(" ");
				classes[0] = Double.parseDouble(tmp[0]);
				classes[1] = Double.parseDouble(tmp[1]);
				classes[2] = Double.parseDouble(tmp[2]);
				flipped_classes[0] = classes[0];
				flipped_classes[1] = -classes[1];
				flipped_classes[2] = -classes[2];
				tmp = oct[i].split(" ");
				for(int oh = 0; oh < 8; oh++){
					sample[oh] = Double.parseDouble(tmp[oh]);
					if(sample[oh] > NotemigonusCrysoleucas.PROX_RANGE || sample[oh] < 0){
						sample[oh] = NotemigonusCrysoleucas.PROX_RANGE;
					}
					if( oh > 0 ){
						flipped_sample[8-oh] = sample[oh];
					} else {
						flipped_sample[oh] = sample[oh];
					}
				}
				tmp = nnVec[i].split(" ");
				MutableDouble2D nnVecMD = new MutableDouble2D(Double.parseDouble(tmp[0]),Double.parseDouble(tmp[1]));
				double tmpDist = nnVecMD.length();
				sample[8] = nnVecMD.x;
				sample[9] = nnVecMD.y;
				tmp = avgNNVec[i].split(" ");
				MutableDouble2D avgNNVecMD = new MutableDouble2D(Double.parseDouble(tmp[0]),Double.parseDouble(tmp[1]));
				tmpDist = avgNNVecMD.length();
				if(tmpDist > 0){
					avgNNVecMD.x = avgNNVecMD.x/tmpDist;
					avgNNVecMD.y = avgNNVecMD.y/tmpDist;
				} else {
					avgNNVecMD.x = avgNNVecMD.y = 0.0;
				}
				sample[10] = avgNNVecMD.x;
				sample[11] = avgNNVecMD.y;
				//sample[12] = (tmpDist>NotemigonusCrysoleucas.SIZE)?1.0:0.0;
				sample[12] = (tmpDist>NotemigonusCrysoleucas.SIZE*3)?1.0:0.0;
				tmp = wallVec[i].split(" ");
				MutableDouble2D wallVecMD = new MutableDouble2D(Double.parseDouble(tmp[0]),Double.parseDouble(tmp[1]));
				sample[13] = wallVecMD.x;
				sample[14] = wallVecMD.y;
				if(wallVecMD.length() > NotemigonusCrysoleucas.RANGE){
					sample[13] = 0.0;
					sample[14] = 0.0;
				}

				flipped_sample[8] = sample[8];
				flipped_sample[9] = -sample[9];
				flipped_sample[10] = sample[10];
				flipped_sample[11] = -sample[11];
				flipped_sample[12] = sample[12];
				flipped_sample[13] = sample[13];
				flipped_sample[14] = -sample[14];
				knn.add(sample,classes);
				if(MIRROR_TRAINING_DATA){
					knn.add(flipped_sample,flipped_classes);
				}
			}
		}
		System.out.println("[FishKNNProx] Done!");
		//FastKNN[] rv = {turnKNN,speedKNN};
		//return rv;
		return knn;
	}
}
