package biosim.app.learnedants;

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
import biosim.core.util.KernelDensityEstimator;

import sim.util.MutableDouble2D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;

public class LearnedAnts implements Agent{
	public static final int FEATURE_DIM = BIOHMMInputParser.sensorDim();
	public static final int NUM_SWITCHES = BIOHMMInputParser.numSwitches();
	public static final int NUM_NEIGHBORS = 10;
	public static final double KERN_BANDWIDTH = 1.0;
	double[][][] transitionMatrix;
	double[] prior;
	FastKNN[] outputFunction;
	AbstractAnt antBody;
	double[] prevVel = {0.0, 0.0, 0.0};
	int currentState = -1;
	KernelDensityEstimator.NormalKernel kernel = new KernelDensityEstimator.NormalKernel(1.0);
	public static ArrayList<Double> collectTimes = new ArrayList<Double>();
	public static ArrayList<Double> acqTimes = new ArrayList<Double>();
	public static ArrayList<Double> antDists = new ArrayList<Double>();
	public static ArrayList<Double> wallDists = new ArrayList<Double>();
	public static ArrayList<Double> foodCounts = new ArrayList<Double>();
	public static ArrayList<Double> wallTimes = new ArrayList<Double>();
	public static ArrayList<Double> antTimes = new ArrayList<Double>();
	public static ArrayList<Double> nestTimes = new ArrayList<Double>();
	public int foodCounter;
	double prevTime = -1.0, lastTime = 0.0, firstSawFood = -1.0;
	double firstSawWall = -1.0, firstSawAnt = -1.0, firstSawNest = -1.0;
	LearnedAnts[] antArray = null;
	
	public LearnedAnts(AbstractAnt b, double[] prior, double[][][] transitionMatrix, FastKNN[] knns){
		antBody = b;
		this.prior = prior;
		this.transitionMatrix = transitionMatrix;
		outputFunction = knns;
		//collectTimes = new ArrayList<Double>();
		//antDists = new ArrayList<Double>();
	}
	
	public void init(){
		currentState = -1;
		prevVel[0] = prevVel[1] = prevVel[2] = 0.0;
		prevTime = -1.0;
	}
	
	public void finish(){
		if(prevTime >=0){
			collectTimes.add(lastTime-prevTime);
		}
		if(antArray != null){
			double foodSum = 0.0;
			for(int i=0;i<antArray.length;i++){
				foodSum += antArray[i].foodCounter;
				antArray[i].foodCounter = 0;
			}
			foodCounts.add(foodSum);
		}
		
	}
	
	public void act(double time){
		//get the sensors
		double[] rv = new double[3];
		MutableDouble2D ant = new MutableDouble2D();
		boolean sawAnt = antBody.getNearestSameTypeVec(ant);
		if(sawAnt){
			antDists.add(ant.length());
			if(firstSawAnt == -1){
				firstSawAnt = time;
			}
		} else {
			if(firstSawAnt >= 0 && firstSawAnt < time){
				antTimes.add(time - firstSawAnt);
			}
			firstSawAnt = -1;
		}
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = antBody.getNearestObstacleVec(wall);
		if(sawWall){
			wallDists.add(wall.length());
			if(firstSawWall == -1){
				firstSawWall = time;
			}
		} else {
			if(firstSawWall >=0 && firstSawWall < time){
				wallTimes.add(time - firstSawWall);
			}
			firstSawWall = -1;
		}
		MutableDouble2D home = new MutableDouble2D();
		boolean sawHome = antBody.getPoiDir(home,"nest");
		MutableDouble2D food = new MutableDouble2D();
		boolean sawFood = antBody.getNearestPreyVec(food);
		if(!sawFood){
			firstSawFood = -1.0;
		} else if(firstSawFood == -1.0){
			firstSawFood = time;
		}
		boolean nearNest = antBody.nearPOI("nest");
		if(nearNest){
			if(firstSawNest == -1){
				firstSawNest = time;
			}
		} else {
			if(firstSawNest >= 0 && firstSawNest < time){
				nestTimes.add(time-firstSawNest);
			}
			firstSawNest = -1;
		}
		boolean gripper = antBody.getGripped();
		//boolean nearFood = antBody.nearPOI("food");
		double[] sensorVec = BIOHMMInputParser.getSensors(antBody);// new double[FEATURE_DIM];//
		double[][] nearestK = new double[NUM_NEIGHBORS][3];
		double[][] nearestKVals = new double[NUM_NEIGHBORS][FEATURE_DIM];
		double[] nearestKWeights = new double[NUM_NEIGHBORS];
		/* 
		boolean[] switches = new boolean[NUM_SWITCHES];
		sensorVec[0] = wall.x;
		sensorVec[1] = wall.y;
		sensorVec[2] = ant.x;
		sensorVec[3] = ant.y;
		sensorVec[4] = home.x;
		sensorVec[5] = home.y;
		sensorVec[6] = food.x;
		sensorVec[7] = food.y;
		//sensorVec[4] = prevVel[0];
		//sensorVec[5] = prevVel[1];
		//sensorVec[6] = prevVel[2];
		switches[0] = gripper;//(ant.length() > 0 && ant.length() < antBody.getSize());
		switches[1] = nearNest;
		int switchingVariable = 0;
		for(int i=0;i<switches.length;i++){
			switchingVariable = switchingVariable << 1;
			if(switches[i]){
				switchingVariable += 1;
			}
		}
		/* */
		int switchingVariable = BIOHMMInputParser.getSwitch(antBody);
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
		//System.out.println("STATE"+currentState+" ("+food.x+", "+food.y+")->");
		outputFunction[currentState].query(sensorVec,nearestK,nearestKWeights,nearestKVals);
		double weightSum = 0.0;
		for(int i=0;i<nearestK.length;i++){
			double weight = 1.0;
			/* 
			double[] tmpDVec = new double[FEATURE_DIM];
			for(int j=0;j<tmpDVec.length;j++){
				tmpDVec[j] = sensorVec[j] - nearestKVals[i][j];
			}
			weight = nearestKWeights[i];
			weight = weight*kernel.k(tmpDVec);
			/* */
			weightSum += weight;
		}
		double tmpSum = 0.0;
		double kRandNum = antBody.getRandom().nextDouble();
		int neighIdx = nearestK.length-1;
		for(int i=0;i<nearestK.length;i++){
			if(kRandNum > (1/weightSum)+tmpSum){//(nearestKWeights[i]/weightSum)+tmpSum){
				tmpSum += (1/weightSum);//nearestKWeights[i]/weightSum;
			} else {
				neighIdx = i;
				break;
			}
		}
		rv[0] = nearestK[neighIdx][0];
		rv[1] = nearestK[neighIdx][1];
		rv[2] = nearestK[neighIdx][2];
		/*
		double weightSum = 0.0;
		for(int i=0;i<rv.length;i++) rv[i] = 0.0;
		for(int i=0;i<nearestK.length;i++){
			double[] tmpDVec = new double[FEATURE_DIM];
			//System.out.print("{");
			for(int j=0;j<tmpDVec.length;j++){
				//System.out.print(" "+nearestKVals[i][j]);
				tmpDVec[j] = sensorVec[j] - nearestKVals[i][j];
			}
			//System.out.println(" }");
			double weight = nearestKWeights[i];
			weight = weight*kernel.k(tmpDVec);
			//System.out.print("[");
			//double weight = 1.0;
			for(int j=0;j<nearestK[i].length;j++){
				//System.out.print(" "+nearestK[i][j]);
				rv[j] += nearestK[i][j]*weight;
			}
			//System.out.println(" ] * "+weight);
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
		*/
		//System.out.println("rv[2] = "+rv[2]);
		prevVel[0] = rv[0];
		prevVel[1] = rv[1];
		prevVel[2] = rv[2];
		//figure out the next state to transition to
		int newState = -1;
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
		if(!gripper){
			antBody.tryToGrab();
			if(antBody.getGripped()){
				prevTime =time;
				if(firstSawFood >= 0 && firstSawFood <= time){
					acqTimes.add(time-firstSawFood);
				}
			} else {
				prevTime = -1.0;
			}
		}
		if(nearNest){
			antBody.tryToDrop();
			if(prevTime >= 0){
				collectTimes.add(time - prevTime);
				foodCounter++;
			}
			prevTime = -1.0;
		}
		lastTime = time;
		antBody.setDesiredVelocity(rv[0],rv[1],rv[2]);
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
			System.out.println("output "+i+" :"+outputFunction[i].numSamples()+" samples");
			//outputFunction[i].sigmaNormalize();
		}
		
	}

	
	public static final double WIDTH=0.2;
	public static final double HEIGHT=0.2;

	public static double timeAvg(ArrayList<Double> times){
		double timeAvg = 0.0;
		for(int j=0;j<times.size();j++){
			timeAvg += times.get(j);
		}
		timeAvg = timeAvg/times.size();
		return timeAvg;
	}
	
	public static double timeStdDev(ArrayList<Double> times, double avg){
		double timeStdDev = 0.0;
		for(int j=0;j<times.size();j++){
			timeStdDev += Math.pow(avg - times.get(j),2);
		}
		timeStdDev = Math.sqrt(timeStdDev/times.size());
		return timeStdDev;
	}

	public static void main(String[] args){
		try{
			File parameterFile = new File(args[0]);
			//BTFData btf = new BTFData();
			//btf.loadDir(new File(args[1]));
			int numAnts = 10;
			int numFlies = 10;
			Environment env = new Environment(WIDTH,HEIGHT,1.0/30.0);
			env.addStaticPOI("nest",WIDTH/2,0.02);
			//env.addStaticPOI("food",WIDTH/2,HEIGHT-0.02);
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
			LearnedAnts[] agents = new LearnedAnts[numAnts];
			LearnedAnts la = new LearnedAnts(bodies[0],null,null,null);
			la.buildParameters(parameterFile);
			agents[0] = la;
			bodies[0].setAgent(agents[0]);
			for(int i=1;i<agents.length;i++){
				agents[i] = new LearnedAnts(bodies[i],la.prior, la.transitionMatrix, la.outputFunction);
				bodies[i].setAgent(agents[i]);
			}
			agents[0].antArray = agents;
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
			
			int numTimes = collectTimes.size();//0;
			double cTimeAvg = timeAvg(collectTimes); //0.0;
			double cTimeStdDev = timeStdDev(collectTimes,cTimeAvg);//0.0;

			int numTimesAcq = acqTimes.size();//0;
			double acqTimesAvg = timeAvg(acqTimes);//0.0;
			double acqTimesStdDev = timeStdDev(acqTimes,acqTimesAvg);//0.0;

			int numTimesWall = wallTimes.size();//0;
			double wallTimesAvg = timeAvg(wallTimes);//0.0;
			double wallTimesStdDev = timeStdDev(wallTimes,wallTimesAvg);//0.0;

			int numTimesAnt = antTimes.size();//0;
			double antTimesAvg = timeAvg(antTimes);//0.0;
			double antTimesStdDev = timeStdDev(antTimes,antTimesAvg);//0.0;
			
			int numTimesNest = nestTimes.size();
			double nestTimesAvg = timeAvg(nestTimes);
			double nestTimesStdDev = timeStdDev(nestTimes,nestTimesAvg);
			
			int numDists = antDists.size();//0;
			double adAvg = timeAvg(antDists);//0.0;
			double adStdDev = timeStdDev(antDists,adAvg);//0.0;

			int numDistsWall = wallDists.size();
			double wdAvg = timeAvg(wallDists);
			double wdStdDev = timeStdDev(wallDists,wdAvg);
			
			double foodAvgPerRun = timeAvg(foodCounts);//0.0;
			double foodStdDevPerRun = timeStdDev(foodCounts,foodAvgPerRun);//0.0;

			Collections.sort(collectTimes);
			Collections.sort(antDists);
			
			System.out.println("Collect Time avg: "+cTimeAvg+" "+cTimeStdDev+" "+numTimes);
			System.out.println("Ant dist avg: "+adAvg+" "+adStdDev+" "+numDists);
			System.out.println("Collect time median: "+collectTimes.get(collectTimes.size()/2));
			System.out.println("Ant dist median: "+antDists.get(antDists.size()/2));
			System.out.println("Avg food collected per run: "+foodAvgPerRun+" "+foodStdDevPerRun+" "+foodCounts.size());
			System.out.println("Wall dist avg: "+wdAvg+" "+wdStdDev+" "+numDistsWall);
			System.out.println("Acquire Time avg: "+acqTimesAvg+" "+acqTimesStdDev+" "+numTimesAcq);
			System.out.println("Wall time avg: "+wallTimesAvg+" "+wallTimesStdDev+" "+numTimesWall);
			System.out.println("Ant time avg: "+antTimesAvg+" "+antTimesStdDev+" "+numTimesAnt);
			System.out.println("Nest time avg: "+nestTimesAvg+" "+nestTimesStdDev+" "+numTimesNest);
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}
}
