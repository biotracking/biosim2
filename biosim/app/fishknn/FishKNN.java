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
	public static final int FEATURES=4;
	public static final int CLASSES=3;
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
	public double[] act(double time){
		double[] rv = new double[3];
		MutableDouble2D fish = new MutableDouble2D();
		boolean sawFish = fishBody.getNearestSameAgentVec(fish);
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = fishBody.getNearestObstacleVec(wall);
		//double[] speedFeatures= new double[SPEED_FEATS], turnFeatures = new double[TURN_FEATS];
		double[] features = new double[FEATURES];
		//double[][] nearestTurnK = new double[5][TURN_CLASSES];
		//double[][] nearestSpeedK = new double[5][SPEED_CLASSES];
		double[][] nearestK = new double[5][CLASSES];
		/*
		speedFeatures[0] = fish.x;
		speedFeatures[1] = wall.x;
		speedFeatures[2] = wall.y;
		turnFeatures[0] = fish.y;
		turnFeatures[1] = wall.x;
		turnFeatures[2] = wall.y;
		*/
		features[0] = fish.x;
		features[1] = fish.y;
		features[2] = wall.x;
		features[3] = wall.y;
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
		/* 
		int rnd_idx = fishBody.getRandom().nextInt(nearestK.length);
		for(int i=0;i<CLASSES;i++) rv[i] = nearestK[rnd_idx][i];
		/* */
		//average
		/*  */
		for(int i=0;i<CLASSES;i++) rv[i] = 0.0;
		for(int k=0;k<nearestK.length;k++){
			for(int i=0;i<CLASSES;i++){
				rv[i] += nearestK[k][i];
			}
		}
		for(int i=0;i<CLASSES;i++) rv[i] = rv[i]/(double)nearestK.length;
		/* */
		return rv;
	}
	
	public static FastKNN loadKNN(BTFData btf) throws IOException{
	//public static FastKNN[] loadKNN(BTFData btf) throws IOException{
		System.out.println("[FishKNN] Loading BTF data...");
		//FastKNN turnKNN = new FastKNN(TURN_FEATS,TURN_CLASSES);
		//FastKNN speedKNN = new FastKNN(SPEED_FEATS,SPEED_CLASSES);
		FastKNN knn = new FastKNN(FEATURES,CLASSES);
		//String[] turningForce = btf.loadColumn("turningforce");
		//String[] speedingForce = btf.loadColumn("speedingforce");
		String[] wallVec = btf.loadColumn("wallvec");
		String[] nnVec = btf.loadColumn("nnvec");
		String[] dvel = btf.loadColumn("dvel");
		String[] dbool = btf.loadColumn("dbool");
		int numRows = nnVec.length;
		//double[] sampleTurn = new double[TURN_FEATS];
		//double[] sampleSpeed = new double[SPEED_FEATS];
		double[] sample = new double[FEATURES];
		//double[] turnClass = new double[1];
		//double[] speedClass = new double[1];
		double[] classes = new double[CLASSES];
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
				//tmp = nnVec[i].split(" ");
				sample[0] = Double.parseDouble(tmp[0]);
				sample[1] = Double.parseDouble(tmp[1]);
				tmp = wallVec[i].split(" ");
				sample[2] = Double.parseDouble(tmp[0]);
				sample[3] = Double.parseDouble(tmp[1]);
				//sample[0] = Double.parseDouble(tmp[0]);
				//sample[1] = Double.parseDouble(tmp[1]);
				//double wallDistSq = sample[2]*sample[2] + sample[3]*sample[3];
				//double fishSpeedSq = classes[0]*classes[0] + classes[1]*classes[1];
				//if(fishSpeedSq > Math.pow(NotemigonusCrysoleucas.SIZE*0.5,2)){
					knn.add(sample,classes);
				//}
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
			env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
			env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
			env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
			env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
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
			gui.setDisplaySize((int)(WIDTH*500),(int)(HEIGHT*500));
			gui.createController();
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
