package biosim.app.learnedants;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractAnt;
import biosim.core.body.AphaenogasterCockerelli;
import biosim.core.gui.GUISimulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;
import biosim.core.util.BTFData;
import biosim.core.util.FastKNN;
import biosim.core.util.KernelDensityEstimator;

import sim.util.MutableDouble2D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LearnedAnts implements Agent{
	public static final int FEATURE_DIM = 6;
	public static final int NUM_SWITCHES = 1;
	public static final int NUM_NEIGHBORS = 10;
	public static final double KERN_BANDWIDTH = 1.0;
	double[][][] transitionMatrix;
	double[] prior;
	FastKNN[] outputFunction;
	AbstractAnt antBody;
	double[] prevVel = {0.0, 0.0, 0.0};
	int currentState = -1;
	KernelDensityEstimator.NormalKernel kernel = new KernelDensityEstimator.NormalKernel(1.0);
	public LearnedAnts(AbstractAnt b, double[] prior, double[][][] transitionMatrix, FastKNN[] knns){
		antBody = b;
		this.prior = prior;
		this.transitionMatrix = transitionMatrix;
		outputFunction = knns;
	}
		
	public double[] act(double time){
		//get the sensors
		double[] rv = new double[3];
		MutableDouble2D ant = new MutableDouble2D();
		boolean sawAnt = antBody.getNearestSameAgentVec(ant);
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = antBody.getNearestObstacleVec(wall);
		MutableDouble2D home = new MutableDouble2D();
		boolean sawHome = antBody.getPoiDir(home,"nest");
		//MutableDouble2D food = new MutableDouble2D();
		//boolean sawFood = antBody.getPoiDir(food,"food");
		//boolean nearNest = antBody.nearPOI("nest");
		//boolean nearFood = antBody.nearPOI("food");
		double[] sensorVec = new double[FEATURE_DIM];
		double[][] nearestK = new double[NUM_NEIGHBORS][3];
		double[][] nearestKVals = new double[NUM_NEIGHBORS][FEATURE_DIM];
		double[] nearestKWeights = new double[NUM_NEIGHBORS];
		boolean[] switches = new boolean[NUM_SWITCHES];
		sensorVec[0] = ant.x;
		sensorVec[1] = ant.y;
		sensorVec[2] = wall.x;
		sensorVec[3] = wall.y;
		sensorVec[4] = home.x;
		sensorVec[5] = home.y;
		//sensorVec[6] = food.x;
		//sensorVec[7] = food.y;
		//sensorVec[4] = prevVel[0];
		//sensorVec[5] = prevVel[1];
		//sensorVec[6] = prevVel[2];
		switches[0] = (ant.length() > 0 && ant.length() < antBody.getSize());
		//switches[1] = nearNest;
		//figure out the initial state
		if(currentState == -1){
			double sum = 0.0;
			double randNum = antBody.getRandom().nextDouble();
			for(int i=0;i<prior.length;i++){
				if(randNum > prior[i]+sum){
					sum += prior[i];
				} else {
					currentState = i;
					break;
				}
			}
			//just in case prior doesn't sum to 1
			if(currentState == -1) currentState = prior.length-1;
		}
		//System.out.println("State: "+currentState);
		//figure out the output for the current state
		outputFunction[currentState].query(sensorVec,nearestK,nearestKWeights,nearestKVals);
		double weightSum = 0.0;
		for(int i=0;i<rv.length;i++) rv[i] = 0.0;
		for(int i=0;i<nearestK.length;i++){
			double[] tmpDVec = new double[FEATURE_DIM];
			for(int j=0;j<tmpDVec.length;j++){
				tmpDVec[j] = sensorVec[j] - nearestKVals[i][j];
			}
			double weight = nearestKWeights[i];
			weight = weight*kernel.k(tmpDVec);
			for(int j=0;j<nearestK[i].length;j++){
				rv[j] += nearestK[i][j]*weight;
			}
			weightSum += weight;
		}
		if(weightSum > 0){
			for(int i=0;i<rv.length;i++){ 
				rv[i] = rv[i]/weightSum;//(double)nearestK.length;
			}
		} else {
			rv[0] = prevVel[0];
			rv[1] = prevVel[1];
			rv[2] = prevVel[2];
		}
		//System.out.println("rv[2] = "+rv[2]);
		prevVel[0] = rv[0];
		prevVel[1] = rv[1];
		prevVel[2] = rv[2];
		//figure out the next state to transition to
		int newState = -1;
		int switchingVariable = 0;
		for(int i=0;i<switches.length;i++){
			switchingVariable = switchingVariable << 1;
			if(switches[i]){
				switchingVariable += 1;
			}
		}
		double sum = 0.0;
		double randNum = antBody.getRandom().nextDouble();
		for(int i=0;i<transitionMatrix[currentState].length;i++){
			if(randNum > transitionMatrix[currentState][i][switchingVariable]+sum){
				sum += transitionMatrix[currentState][i][switchingVariable];
			} else {
				newState = i;
				break;
			}
		}
		if(newState == -1) newState = transitionMatrix[currentState].length-1;
		currentState = newState;
		//currentState = 1;
		//System.out.println("rv:"+rv[0]+" "+rv[1]+" "+rv[2]);
		return rv;
	}
	
	public void buildParameters(File parameterFile) throws IOException{
		BufferedReader fread = new BufferedReader(new FileReader(parameterFile));
		int priorInt = Integer.parseInt(fread.readLine().trim());
		prior = new double[priorInt];
		String[] priorStr = fread.readLine().split(" ");
		if(priorStr.length != priorInt){
			throw new RuntimeException("Bad parameter file: priorInt ("+priorInt+") != priorStr.length ("+priorStr.length+")");
		}
		//System.out.print("[");
		for(int i=0;i<priorStr.length;i++){
			prior[i] = Double.parseDouble(priorStr[i].trim());
			//System.out.print(" "+prior[i]);
		}
		//System.out.println(" ]");
		String[] transNumStr = fread.readLine().split(" ");
		if(transNumStr.length != 3){
			throw new RuntimeException("Bad parameter file: transNumStr.length ("+transNumStr.length+") != 3");
		}
		int transK = Integer.parseInt(transNumStr[0].trim());
		if(transK != prior.length){
			throw new RuntimeException("Bad parameter file: transNumStr[0] ("+transK+") != prior.length ("+prior.length+")");
		}
		transK = Integer.parseInt(transNumStr[1].trim());
		if(transK != prior.length){
			throw new RuntimeException("Bad parameter file: transNumStr[1] ("+transK+") != prior.length ("+prior.length+")");
		}
		transK = Integer.parseInt(transNumStr[2].trim());
		transitionMatrix = new double[prior.length][prior.length][transK];
		String[] transStr = fread.readLine().split(" ");
		for(int x=0;x<transStr.length;x++){
			int k = x % transK;
			int j = (x/transK) % prior.length;
			int i = (x/transK)/prior.length;
			transitionMatrix[i][j][k] = Double.parseDouble(transStr[x].trim());
		}
		outputFunction = new FastKNN[prior.length];
		for(int i=0;i<outputFunction.length;i++){
			outputFunction[i] = new FastKNN(FEATURE_DIM,3);
		}
		//String[] desiredVel = btf.loadColumn("dvel");
		//String[] wallVec = btf.loadColumn("wallvec");
		//String[] antVec = btf.loadColumn("antvec");
		//String[] prevVec = btf.loadColumn("pvel");
		int partitionLength = Integer.parseInt(fread.readLine().trim());
		String[] partitionStr = fread.readLine().split(" ");
		if(partitionLength != partitionStr.length){
			throw new RuntimeException("Bad parameter file: partitionLength ("+partitionLength+") != partitionStr.length ("+partitionStr.length+")");
		}
		double[] features = new double[FEATURE_DIM];
		double[] velclass = new double[3];
		String[] tmp;
		/*
		int partition;
		for(int i=0;i<partitionLength;i++){
			partition = Integer.parseInt(partitionStr[i].trim());
			tmp = antVec[i].split(" ");
			features[0] = Double.parseDouble(tmp[0]);
			features[1] = Double.parseDouble(tmp[1]);
			tmp = wallVec[i].split(" ");
			features[2] = Double.parseDouble(tmp[0]);
			features[3] = Double.parseDouble(tmp[1]);
			//tmp = prevVec[i].split(" ");
			//features[4] = Double.parseDouble(tmp[0]);
			//features[5] = Double.parseDouble(tmp[1]);
			//features[6] = Double.parseDouble(tmp[2]);
			tmp = desiredVel[i].split(" ");
			velclass[0] = Double.parseDouble(tmp[0]);
			velclass[1] = Double.parseDouble(tmp[1]);
			velclass[2] = Double.parseDouble(tmp[2]);
			
			outputFunction[partition].add(features,velclass);
		}
		*/
		String line;
		for(int i=0;i<outputFunction.length;i++){
			String stateLine = fread.readLine(); //which output it was
			int state = Integer.parseInt(stateLine);
			if(state > outputFunction.length || state < 0){
				throw new RuntimeException("Bad parameter file: state ("+state+") not in [0,"+outputFunction.length+"]");
			}
			while( !(line = fread.readLine()).isEmpty()){
				String[] sampleLine = line.split(" ");
				velclass[0] = Double.parseDouble(sampleLine[0]);
				velclass[1] = Double.parseDouble(sampleLine[1]);
				velclass[2] = Double.parseDouble(sampleLine[2]);
				for(int j = 0; j<features.length;j++){
					features[j] = Double.parseDouble(sampleLine[j+3]);
				}
				double weight = Double.parseDouble(sampleLine[features.length+3]);
				outputFunction[state].add(features,velclass,weight);
			}
			//once line.isEmpty() == true, then we've allready grabbed the
			//junk line, so just loop
		}
		
	}

	
	public static final double WIDTH=0.2;
	public static final double HEIGHT=0.2;


	public static void main(String[] args){
		try{
			File parameterFile = new File(args[0]);
			//BTFData btf = new BTFData();
			//btf.loadDir(new File(args[1]));
			int numAnts = 10;
			Environment env = new Environment(WIDTH,HEIGHT,1.0/30.0);
			env.addStaticPOI("nest",WIDTH/2,0.02);
			env.addStaticPOI("food",WIDTH/2,HEIGHT-0.02);
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
			LearnedAnts la = new LearnedAnts(bodies[0],null,null,null);
			la.buildParameters(parameterFile);
			agents[0] = la;
			bodies[0].setAgent(agents[0]);
			for(int i=1;i<agents.length;i++){
				agents[i] = new LearnedAnts(bodies[i],la.prior, la.transitionMatrix, la.outputFunction);
				bodies[i].setAgent(agents[i]);
			}
			//env.runSimulation(args);
			Simulation sim = env.newSimulation();
			GUISimulation gui = new GUISimulation(sim);
			gui.createController();
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}
}