package biosim.app.antknn;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractAnt;
import biosim.core.body.AphaenogasterCockerelli;
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
	AbstractAnt antBody;
	FastKNN knn;
	double[] prevVel ={0.0, 0.0, 0.0};
	boolean first;
	public AntKNN(AbstractAnt b, FastKNN knn){
		antBody = b;
		this.knn = knn;
	}
	public double[] act(double time){
		double[] rv = new double[3];
		MutableDouble2D ant = new MutableDouble2D();
		boolean sawAnt = antBody.getNearestSameAgentVec(ant);
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = antBody.getNearestObstacleVec(wall);
		MutableDouble2D home = new MutableDouble2D();
		boolean sawHome = antBody.getHomeDir(home);
		double[] sensorVec = new double[9];
		double[][] nearestK = new double[10][3];
		sensorVec[0] = ant.x;
		sensorVec[1] = ant.y;
		sensorVec[2] = wall.x;
		sensorVec[3] = wall.y;
		sensorVec[4] = home.x;
		sensorVec[5] = home.y;
		sensorVec[6] = prevVel[0];
		sensorVec[7] = prevVel[1];
		sensorVec[8] = prevVel[2];
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
		return rv;
	}
	
	public static FastKNN loadKNN(BTFData btf) throws IOException{
		FastKNN knn = new FastKNN(9,3);
		String[] desiredVel = btf.loadColumn("dvel");
		String[] desiredVelBool = btf.loadColumn("dbool");
		String[] wallVec = btf.loadColumn("wallvec");
		//String[] wallBool = btf.loadColumn("wallbool");
		String[] antVec = btf.loadColumn("antvec");
		//String[] antBool = btf.loadColumn("antbool");
		String[] homeVec = btf.loadColumn("homevec");
		String[] prevVec = btf.loadColumn("pvel");
		String[] prevBoolVec = btf.loadColumn("pbool");
		int numRows = desiredVel.length;
		double[] sample = new double[9];
		double[] velclass = new double[3];
		for(int i=0;i<numRows;i++){
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
				tmp = prevVec[i].split(" ");
				sample[6] = Double.parseDouble(tmp[0]);
				sample[7] = Double.parseDouble(tmp[1]);
				sample[8] = Double.parseDouble(tmp[2]);
				knn.add(sample,velclass);
			}
		}
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
			Agent[] agents = new Agent[numAnts];
			for(int i=0;i<agents.length;i++){
				agents[i] = new AntKNN(bodies[i],knn);
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
