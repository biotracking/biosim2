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


public class FishKNNZones implements Agent{
	AbstractFish fishBody;
	public static final int FEATURES=8;//3 zones, nearest wall.
	public static final int CLASSES=3;
	public static final int KNN_NEIGHBORS=10;
	public static final boolean MIRROR_TRAINING_DATA=false;
	public static final boolean USE_WEIGHTS=true;
	//note that we're weighting features before the distance calculation,
	//so the weights effectively wind up getting squared.
	public static final double[] FEATURE_WEIGHTS={	1.0, //Zone1
													1.0, 
													1.0, //Zone2
													1.0, 
													1.0, //Zone3
													1.0, 
													1.0, //WallX
													1.0  //WallY
												};
	FastKNN knn;
	double prevTime = 0.0;
	public FishKNNZones(AbstractFish b, FastKNN knn){
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
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = fishBody.getNearestObstacleVec(wall);
		if(!sawWall) wall.x = wall.y = 0.0;
		double[] features = new double[FEATURES];
		for(int i=0;i<features.length;i++)features[i]=0.0;
		double[][] nearestK = new double[KNN_NEIGHBORS][CLASSES];
		MutableDouble2D[] zones = new MutableDouble2D[fishBody.getNumZones()];
		boolean zonesWorked = fishBody.getZoneCoMVecs(zones);
		if(zonesWorked){
			for(int i=0;i<zones.length;i++){
				features[i*2]=zones[i].x;
				features[(i*2) +1] = zones[i].y;
			}
		}
		features[6] = wall.x;
		features[7] = wall.y;
		//System.out.println("Sensor vec: ["+sensorVec[0]+", "+sensorVec[1]+", "+sensorVec[2]+", "+sensorVec[3]+"]");
		knn.query(features,nearestK);
		//sample
		/**/ 
		int rnd_idx = fishBody.getRandom().nextInt(nearestK.length);
		for(int i=0;i<CLASSES;i++) rv[i] = nearestK[rnd_idx][i];
		fishBody.setDesiredVelocity(rv[0],rv[1],rv[2]);
	}
	
	public static FastKNN loadKNN(BTFData btf) throws IOException{
		System.out.println("[FishKNNZones] Loading BTF data...");
		FastKNN knn = new FastKNN(FEATURES,CLASSES);
		if(USE_WEIGHTS){
			knn.setFeatureWeights(FEATURE_WEIGHTS);
		}
		String[] wallVec = btf.loadColumn("wallvec");
		String[] dvel = btf.loadColumn("dvel");
		String[] dbool = btf.loadColumn("dbool");
		String[] zonevecs = btf.loadColumn("zonevecs");
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
				tmp = zonevecs[i].split(" ");
				int numZones = tmp.length/2;
				if(numZones != 3){
					throw new RuntimeException("zonevecs.btf malformed, we expect 3 zones, got "+numZones);
				}
				for(int zone=0;zone<numZones;zone++){
					sample[zone*2] = Double.parseDouble(tmp[zone*2]);
					sample[(zone*2)+1] = Double.parseDouble(tmp[(zone*2)+1]);
				}
				tmp = wallVec[i].split(" ");
				MutableDouble2D wallVecMD = new MutableDouble2D(Double.parseDouble(tmp[0]),Double.parseDouble(tmp[1]));
				sample[6] = wallVecMD.length();
				sample[7] = wallVecMD.angle();
				if(wallVecMD.length() > NotemigonusCrysoleucas.RANGE){
					sample[6] = 0.0;
					sample[7] = 0.0;
				}

				flipped_sample[6] = sample[6];
				flipped_sample[7] = -sample[7];
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
