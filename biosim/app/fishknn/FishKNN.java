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
	public static final int FEATURE_DIM = 4;
	AbstractFish fishBody;
	FastKNN knn;
	double prevTime = 0.0;
	public FishKNN(AbstractFish b, FastKNN knn){
		fishBody = b;
		this.knn = knn;
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
		double[] sensorVec = new double[FEATURE_DIM];
		double[][] nearestK = new double[5][3];
		sensorVec[0] = fish.x;
		sensorVec[1] = fish.y;
		sensorVec[2] = wall.x;
		sensorVec[3] = wall.y;
		//System.out.println("Sensor vec: ["+sensorVec[0]+", "+sensorVec[1]+", "+sensorVec[2]+", "+sensorVec[3]+"]");
		knn.query(sensorVec,nearestK);
		//now, do median, average, or random selection
		//average
		for(int i=0;i<rv.length;i++) rv[i] = 0.0;
		for(int i=0;i<nearestK.length;i++){
			for(int j=0;j<nearestK[i].length;j++){
				rv[j] += nearestK[i][j];
			}
		}
		for(int i=0;i<rv.length;i++) rv[i] = rv[i]/(double)nearestK.length;
		//if(rv[0] < 0 ) System.out.println("Movin backwards!");
		//System.out.println("rv: ["+rv[0]+", "+rv[1]+", "+rv[2]+"]");
		//Now, rv contains the classes, but the classes are ACCEL
		//and we need to put a desired VEL. So get the current vel
		//and do forward euler:
		double[] vel = {0.0,0.0,0.0};
		fishBody.getSelfVelXYT(vel);
		for(int i=0;i<rv.length;i++){
			rv[i] = vel[i] + (rv[i]*(time-prevTime));
		}
		prevTime = time;
		return rv;
	}
	
	public static FastKNN loadKNN(BTFData btf) throws IOException{
		System.out.println("[FishKNN] Loading BTF data...");
		FastKNN knn = new FastKNN(FEATURE_DIM,3);
		String[] turningForce = btf.loadColumn("turningforce");
		String[] speedingForce = btf.loadColumn("speedingforce");
		String[] wallVec = btf.loadColumn("wallvec");
		String[] nnVec = btf.loadColumn("nnvec");
		int numRows = nnVec.length;
		double[] sample = new double[FEATURE_DIM];
		double[] velclass = new double[3];
		for(int i=0;i<numRows;i++){
			if(i%(numRows/10)==0) System.out.println("[FishKNN] "+i+"/"+numRows);
			String[] tmp = speedingForce[i].split(" ");
			velclass[0] = Double.parseDouble(tmp[0]);
			velclass[1] = 0.0;
			tmp = turningForce[i].split(" ");
			velclass[2] = Double.parseDouble(tmp[0]);
			tmp = nnVec[i].split(" ");
			sample[0] = Double.parseDouble(tmp[0]);
			sample[1] = Double.parseDouble(tmp[1]);
			tmp = wallVec[i].split(" ");
			sample[2] = Double.parseDouble(tmp[0]);
			sample[3] = Double.parseDouble(tmp[1]);
			knn.add(sample,velclass);
		}
		//sigmaNormalize currently broken, don't use it! (Dec 4th, 2012)
		//knn.sigmaNormalize();
		System.out.println("[FishKNN] Done!");
		return knn;
	}
	
	public static final double WIDTH=2.5;
	public static final double HEIGHT=1.4;
	
	public static void main(String[] args){
		try{
			//get some btf data and put it into a KNN
			BTFData btf = new BTFData();
			btf.loadDir(new File(args[0]));
			FastKNN knn = loadKNN(btf);
			//set up the environment
			int numFish = 30;
			Environment env = new Environment(WIDTH,HEIGHT,1.0/30.0);
			env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
			env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
			env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
			env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
			//add agents
			NotemigonusCrysoleucas[] bodies = new NotemigonusCrysoleucas[numFish];
			for(int i=0;i<bodies.length;i++){
				bodies[i] = new NotemigonusCrysoleucas();
				env.addBody(bodies[i]);
			}

			Agent[] agents = new Agent[numFish];
			for(int i=0;i<agents.length;i++){
				agents[i] = new FishKNN(bodies[i],knn);
				bodies[i].setAgent(agents[i]);
			}
			
			
			//env.runSimulation(args);
			Simulation sim = env.newSimulation();
			GUISimulation gui = new GUISimulation(sim);
			gui.createController();
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
