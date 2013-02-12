package biosim.app.antknn;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractAnt;
import biosim.core.body.AphaenogasterCockerelli;
import biosim.core.body.DrosophilaMelanogaster;
import biosim.core.gui.GUISimulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;
import biosim.core.util.BTFData;
import biosim.core.util.FastKNN;

import sim.util.MutableDouble2D;

import java.io.File;
import java.io.IOException;


public class AntKNN implements Agent{
	public static final int FEATURE_DIM = 6;
	AbstractAnt antBody;
	FastKNN knn;
	double[] prevVel ={0.0, 0.0, 0.0};
	boolean first;
	public AntKNN(AbstractAnt b, FastKNN knn){
		antBody = b;
		this.knn = knn;
	}
	public void init(){
		prevVel[0] = prevVel[1] = prevVel[2] = 0.0;
	}
	public void finish(){
	}
	public double[] act(double time){
		//System.out.println("Ant body:"+antBody);
		double[] rv = new double[3];
		MutableDouble2D ant = new MutableDouble2D();
		boolean sawAnt = antBody.getNearestSameAgentVec(ant);
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = antBody.getNearestObstacleVec(wall);
		MutableDouble2D home = new MutableDouble2D();
		boolean sawHome = antBody.getPoiDir(home,"nest");
		MutableDouble2D food = new MutableDouble2D();
		boolean sawFood = antBody.getNearestPreyVec(food);
		double[] sensorVec = new double[FEATURE_DIM];
		double[][] nearestK = new double[5][3];
		sensorVec[0] = ant.x;
		sensorVec[1] = ant.y;
		sensorVec[2] = wall.x;
		sensorVec[3] = wall.y;
		//System.out.println("Sensor vec: ["+sensorVec[0]+", "+sensorVec[1]+", "+sensorVec[2]+", "+sensorVec[3]+"]");
		sensorVec[4] = home.x;
		sensorVec[5] = home.y;
		//sensorVec[6] = food.x;
		//sensorVec[7] = food.y;
		//sensorVec[4] = prevVel[0];
		//sensorVec[5] = prevVel[1];
		//sensorVec[6] = prevVel[2];
		//sensorVec[7] = home.x;
		//sensorVec[8] = home.y;
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
		prevVel[0] = rv[0];
		prevVel[1] = rv[1];
		prevVel[2] = rv[2];
		//System.out.println("rv: ["+rv[0]+", "+rv[1]+", "+rv[2]+"]");
		return rv;
	}
	
	public static FastKNN loadKNN(BTFData btf) throws IOException{
		System.out.println("[AntKNN] Loading BTF data...");
		FastKNN knn = new FastKNN(FEATURE_DIM,3);
		String[] desiredVel = btf.loadColumn("dvel");
		String[] desiredVelBool = btf.loadColumn("dbool");
		String[] wallVec = btf.loadColumn("wallvec");
		String[] wallBool = btf.loadColumn("wallbool");
		String[] antVec = btf.loadColumn("antvec");
		String[] antBool = btf.loadColumn("antbool");
		String[] homeVec = btf.loadColumn("homevec");
		String[] foodVec = btf.loadColumn("foodvec");
		String[] prevVec = btf.loadColumn("pvel");
		String[] prevBoolVec = btf.loadColumn("pbool");
		int numRows = desiredVel.length;
		double[] sample = new double[FEATURE_DIM];
		double[] velclass = new double[3];
		for(int i=0;i<numRows;i++){
			if(i%(numRows/10)==0) System.out.println("[AntKNN] "+i+"/"+numRows);
			if(Boolean.parseBoolean(desiredVelBool[i])){
				String[] tmp = desiredVel[i].split(" ");
				velclass[0] = Double.parseDouble(tmp[0]);
				velclass[1] = Double.parseDouble(tmp[1]);
				velclass[2] = Double.parseDouble(tmp[2]);
				tmp = antVec[i].split(" ");
				sample[0] = Double.parseDouble(tmp[0]);
				sample[1] = Double.parseDouble(tmp[1]);
				tmp = wallVec[i].split(" ");
				sample[2] = Double.parseDouble(tmp[0]);
				sample[3] = Double.parseDouble(tmp[1]);
				tmp = homeVec[i].split(" ");
				sample[4] = Double.parseDouble(tmp[0]);
				sample[5] = Double.parseDouble(tmp[1]);
				//tmp = foodVec[i].split(" ");
				//sample[6] = Double.parseDouble(tmp[0]);
				//sample[7] = Double.parseDouble(tmp[1]);
				//tmp = prevVec[i].split(" ");
				//sample[4] = Double.parseDouble(tmp[0]);
				//sample[5] = Double.parseDouble(tmp[1]);
				//sample[6] = Double.parseDouble(tmp[2]);
				//tmp = homeVec[i].split(" ");
				//sample[7] = Double.parseDouble(tmp[0]);
				//sample[8] = Double.parseDouble(tmp[1]);
				knn.add(sample,velclass);
			}
		}
		//sigmaNormalize currently broken, don't use it! (Dec 4th, 2012)
		//knn.sigmaNormalize();
		System.out.println("[AntKNN] Done!");
		return knn;
	}
	
	public static final double WIDTH=0.2;
	public static final double HEIGHT=0.2;
	
	public static void main(String[] args){
		try{
			//get some btf data and put it into a KNN
			BTFData btf = new BTFData();
			btf.loadDir(new File(args[0]));
			FastKNN knn = loadKNN(btf);
			//set up the environment
			int numAnts = 10;
			int numFlies = 10;
			Environment env = new Environment(WIDTH,HEIGHT,1.0/30.0);
			env.addStaticPOI("nest",WIDTH/2,0.02);
			env.addObstacle(new RectObstacle(0.01,0.2), 0.19,  0.0);//east wall
			env.addObstacle(new RectObstacle(0.01,0.2),  0.0,  0.0);//west
			env.addObstacle(new RectObstacle(0.2,0.01),  0.0,  0.0);//north
			env.addObstacle(new RectObstacle(0.2,0.01),  0.0, 0.19);//south
			//add agents
			AphaenogasterCockerelli[] bodies = new AphaenogasterCockerelli[numAnts];
			for(int i=0;i<bodies.length;i++){
				bodies[i] = new AphaenogasterCockerelli();
				env.addBody(bodies[i]);
			}
			DrosophilaMelanogaster[] flyBodies = new DrosophilaMelanogaster[numFlies];
			for(int i=0;i<flyBodies.length;i++){
				flyBodies[i] = new DrosophilaMelanogaster();
				env.addBody(flyBodies[i]);
			}

			Agent[] agents = new Agent[numAnts];
			for(int i=0;i<agents.length;i++){
				agents[i] = new AntKNN(bodies[i],knn);
				bodies[i].setAgent(agents[i]);
			}
			
			Agent[] flyAgents = new Agent[numFlies];
			for(int i=0;i<flyAgents.length;i++){
				flyAgents[i] = new biosim.app.twostateants.LazyFly();
				flyBodies[i].setAgent(flyAgents[i]);
			}
			
			//env.runSimulation(args);
			Simulation sim = env.newSimulation();
			GUISimulation gui = new GUISimulation(sim);
			gui.setPortrayalClass(DrosophilaMelanogaster.class, biosim.app.twostateants.FoodPortrayal.class);
			gui.createController();
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
