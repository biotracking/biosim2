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


public class FishKNN implements Agent{
	AbstractFish fishBody;
	//public static final int FEATURES=6;
	public static final int FEATURES=24;
	public static final int CLASSES=3;
	public static final int KNN_NEIGHBORS=10;
	//public static final int TURN_FEATS=3;
	//public static final int SPEED_FEATS=3;
	//public static final int TURN_CLASSES=1;
	//public static final int SPEED_CLASSES=1;
	//FastKNN turnKNN, speedKNN;
	FastKNN knn;
	double prevTime = 0.0;
	public FishKNN(AbstractFish b, FastKNN knn){
	//public FishKNN(AbstractFish b, FastKNN turnKNN, FastKNN speedKNN){
		fishBody = b;
		this.knn = knn;
		//this.turnKNN = turnKNN;
		//this.speedKNN = speedKNN;
	}
	public void init(){
	}
	public void finish(){
	}
	public void act(double time){
		double[] rv = new double[3];
		MutableDouble2D avgFish = new MutableDouble2D();
		//MutableDouble2D nnFish = new MutableDouble2D();
		//fishBody.getNearestSameAgentVec(nnFish);
		boolean sawFish = fishBody.getAverageSameAgentVec(avgFish);
		//MutableDouble2D wall = new MutableDouble2D();
		//boolean sawWall = fishBody.getNearestObstacleVec(wall);
		//double[] speedFeatures= new double[SPEED_FEATS], turnFeatures = new double[TURN_FEATS];
		double[] features = new double[FEATURES];
		//double[][] nearestTurnK = new double[5][TURN_CLASSES];
		//double[][] nearestSpeedK = new double[5][SPEED_CLASSES];
		double[][] nearestK = new double[KNN_NEIGHBORS][CLASSES];
		/*
		speedFeatures[0] = fish.x;
		speedFeatures[1] = wall.x;
		speedFeatures[2] = wall.y;
		turnFeatures[0] = fish.y;
		turnFeatures[1] = wall.x;
		turnFeatures[2] = wall.y;
		*/
		/*
		features[0] =  avgFish.x;
		features[1] =  avgFish.y;
		features[2] = 0.0;//wall.x;
		features[3] = 0.0;//wall.y;
		features[4] = nnFish.x;
		features[5] = nnFish.y;
		*/
		double[] prox = fishBody.getProximity(null);
		for(int i=0;i<prox.length;i++){
			features[i] = prox[i];
		}
		features[8] = avgFish.x;
		features[9] = avgFish.y;
		features[10] = features[8];
		features[11] = features[9];
		features[12] = features[8];
		features[13] = features[9];
		features[14] = features[8];
		features[15] = features[9];
		features[16] = features[8];
		features[17] = features[9];
		features[18] = features[8];
		features[19] = features[9];
		features[20] = features[8];
		features[21] = features[9];
		features[22] = features[8];
		features[23] = features[9];
		/*
		features[16] = wall.x;
		features[17] = wall.y;
		features[18] = features[16];
		features[19] = features[17];
		features[20] = features[16];
		features[21] = features[17];
		features[22] = features[16];
		features[23] = features[17];
		*/
		//System.out.println("Sensor vec: ["+sensorVec[0]+", "+sensorVec[1]+", "+sensorVec[2]+", "+sensorVec[3]+"]");
		/*
		turnKNN.query(turnFeatures,nearestK);
		double torq = 0.0;
		for(int k=0;k<nearestK.length;k++){
			torq += nearestK[k][0];
		}
		torq /= nearestK.length;
		speedKNN.query(speedFeatures,nearestK);
		double accel = 0.0;
		for(int k=0;k<nearestK.length;k++){
			accel += nearestK[k][0];
		}
		accel /= nearestK.length;
		
		double[] vel = {0.0,0.0,0.0};
		fishBody.getSelfVelXYT(vel);
		rv[0] = vel[0] + (accel*(time-prevTime));
		rv[1] = vel[1];
		rv[2] = vel[2] + (torq*(time-prevTime));
		
		prevTime = time;
		*/
		knn.query(features,nearestK);
		//sample
		/* */
		int rnd_idx = fishBody.getRandom().nextInt(nearestK.length);
		for(int i=0;i<CLASSES;i++) rv[i] = nearestK[rnd_idx][i];
		/* */
		//average
		/* 
		for(int i=0;i<CLASSES;i++) rv[i] = 0.0;
		for(int k=0;k<nearestK.length;k++){
			for(int i=0;i<CLASSES;i++){
				rv[i] += nearestK[k][i];
			}
		}
		for(int i=0;i<CLASSES;i++) rv[i] = rv[i]/(double)nearestK.length;
		/* */
		fishBody.setDesiredVelocity(rv[0],rv[1],rv[2]);
	}
	
	public static FastKNN loadKNN(BTFData btf) throws IOException{
	//public static FastKNN[] loadKNN(BTFData btf) throws IOException{
		System.out.println("[FishKNN] Loading BTF data...");
		//FastKNN turnKNN = new FastKNN(TURN_FEATS,TURN_CLASSES);
		//FastKNN speedKNN = new FastKNN(SPEED_FEATS,SPEED_CLASSES);
		FastKNN knn = new FastKNN(FEATURES,CLASSES);
		//String[] turningForce = btf.loadColumn("turningforce");
		//String[] speedingForce = btf.loadColumn("speedingforce");
		//String[] wallVec = btf.loadColumn("wallvec");
		//String[] nnVec = btf.loadColumn("nnvec");
		String[] avgNNVec = btf.loadColumn("avgnnvec");
		String[] dvel = btf.loadColumn("dvel");
		String[] dbool = btf.loadColumn("dbool");
		String[] oct = btf.loadColumn("oct");
		int numRows = dvel.length;
		//double[] sampleTurn = new double[TURN_FEATS];
		//double[] sampleSpeed = new double[SPEED_FEATS];
		double[] sample = new double[FEATURES];
		double[] flipped_sample = new double[FEATURES];
		//double[] turnClass = new double[1];
		//double[] speedClass = new double[1];
		double[] classes = new double[CLASSES];
		double[] flipped_classes = new double[CLASSES];
		for(int i=0;i<numRows;i++){
			if(i%(numRows/10)==0) System.out.println("[FishKNN] "+i+"/"+numRows);
			/*
			String[] tmp = speedingForce[i].split(" ");
			speedClass[0] = Double.parseDouble(tmp[0]);
			tmp = turningForce[i].split(" ");
			turnClass[0] = Double.parseDouble(tmp[0]);
			tmp = nnVec[i].split(" ");
			sampleSpeed[0] = Double.parseDouble(tmp[0]);
			sampleTurn[0] = Double.parseDouble(tmp[1]);
			tmp = wallVec[i].split(" ");
			sampleSpeed[1] = Double.parseDouble(tmp[0]);
			sampleSpeed[2] = Double.parseDouble(tmp[1]);
			sampleTurn[1] = sampleSpeed[1];
			sampleTurn[2] = sampleSpeed[2];
			speedKNN.add(sampleSpeed,speedClass);
			turnKNN.add(sampleTurn,turnClass);
			*/
			String[] tmp = dbool[i].split(" ");
			if(Boolean.parseBoolean(tmp[0])){
				tmp = dvel[i].split(" ");
				classes[0] = Double.parseDouble(tmp[0]);
				classes[1] = Double.parseDouble(tmp[1]);
				classes[2] = Double.parseDouble(tmp[2]);
				flipped_classes[0] = classes[0];
				flipped_classes[1] = -classes[1];
				flipped_classes[2] = -classes[2];
				/*
				tmp = avgNNVec[i].split(" ");
				sample[0] =  Double.parseDouble(tmp[0]);
				sample[1] =  Double.parseDouble(tmp[1]);
				tmp = wallVec[i].split(" ");
				sample[2] = Double.parseDouble(tmp[0]);
				sample[3] = Double.parseDouble(tmp[1]);
				tmp = nnVec[i].split(" ");
				sample[4] = Double.parseDouble(tmp[0]);
				sample[5] = Double.parseDouble(tmp[1]);
				*/
				tmp = oct[i].split(" ");
				for(int oh = 0; oh < 8; oh++){
					sample[oh] = Double.parseDouble(tmp[oh]);
					flipped_sample[8-oh] = sample[oh];
				}
				tmp = avgNNVec[i].split(" ");
				sample[8] = Double.parseDouble(tmp[0]);
				sample[9] = Double.parseDouble(tmp[1]);
				sample[10] = sample[8];
				sample[11] = sample[9];
				sample[12] = sample[8];
				sample[13] = sample[9];
				sample[14] = sample[8];
				sample[15] = sample[9];
				sample[16] = sample[8];
				sample[17] = sample[9];
				sample[18] = sample[8];
				sample[19] = sample[9];
				sample[20] = sample[8];
				sample[21] = sample[9];
				sample[22] = sample[8];
				sample[23] = sample[9];
				
				/*
				tmp = wallVec[i].split(" ");
				sample[16] = Double.parseDouble(tmp[0]);
				sample[17] = Double.parseDouble(tmp[1]);
				sample[18] = sample[16];
				sample[19] = sample[17];
				sample[20] = sample[16];
				sample[21] = sample[17];
				sample[22] = sample[16];
				sample[23] = sample[17];
				*/
				flipped_sample[8] = sample[8];
				flipped_sample[9] = -sample[9];
				flipped_sample[10] = flipped_sample[8];
				flipped_sample[11] = flipped_sample[9];
				flipped_sample[12] = flipped_sample[8];
				flipped_sample[13] = flipped_sample[9];
				flipped_sample[14] = flipped_sample[8];
				flipped_sample[15] = flipped_sample[9];
				flipped_sample[16] = flipped_sample[8];
				flipped_sample[17] = flipped_sample[9];
				flipped_sample[18] = flipped_sample[8];
				flipped_sample[19] = flipped_sample[9];
				flipped_sample[20] = flipped_sample[8];
				flipped_sample[21] = flipped_sample[9];
				flipped_sample[22] = flipped_sample[8];
				flipped_sample[23] = flipped_sample[9];
				/*
				flipped_sample[16] = sample[16];
				flipped_sample[17] = -sample[17];
				flipped_sample[18] = flipped_sample[16];
				flipped_sample[19] = flipped_sample[17];
				flipped_sample[20] = flipped_sample[16];
				flipped_sample[21] = flipped_sample[17];
				flipped_sample[22] = flipped_sample[16];
				flipped_sample[23] = flipped_sample[17];
				*/
				knn.add(sample,classes);
				knn.add(flipped_sample,flipped_classes);
			}
		}
		//sigmaNormalize currently broken, don't use it! (Dec 4th, 2012)
		//knn.sigmaNormalize();
		System.out.println("[FishKNN] Done!");
		//FastKNN[] rv = {turnKNN,speedKNN};
		//return rv;
		return knn;
	}
	
	public static final double WIDTH=2.5;//2.5;
	public static final double HEIGHT=1.5;//2.5;
	
	public static void main(String[] args){
		try{
			//get some btf data and put it into a KNN
			BTFData btf = new BTFData();
			btf.loadDir(new File(args[0]));
			//FastKNN[] knns = loadKNN(btf);
			FastKNN knn = loadKNN(btf);
			//set up the environment
			int numFish = 30;
			Environment env = new Environment(WIDTH,HEIGHT,1.0/30.0);
			//env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
			//env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
			//env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
			//env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
			env.setToroidal(true);
			//add agents
			NotemigonusCrysoleucas[] bodies = new NotemigonusCrysoleucas[numFish];
			for(int i=0;i<bodies.length;i++){
				bodies[i] = new NotemigonusCrysoleucas();
				env.addBody(bodies[i]);
			}

			Agent[] agents = new Agent[numFish];
			for(int i=0;i<agents.length;i++){
				//agents[i] = new FishKNN(bodies[i],knns[0],knns[1]);
				agents[i] = new FishKNN(bodies[i],knn);
				bodies[i].setAgent(agents[i]);
			}
			
			//env.runSimulation(args);
			Simulation sim = env.newSimulation();
			GUISimulation gui = new GUISimulation(sim);
			gui.setPortrayalClass(NotemigonusCrysoleucas.class, FishPortrayal.class);
			gui.setDisplaySize((int)(WIDTH*500),(int)(HEIGHT*500));
			gui.createController();
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
