// FastSlowFish.java
package biosim.app.fishreynolds;

import biosim.app.fishlr.FishLRLogger;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractFish;
import biosim.core.body.sensors.NeighborhoodStatistics;
import biosim.core.body.NotemigonusCrysoleucas;
import biosim.core.gui.GUISimulation;
import biosim.core.sim.InitiallyPlacedEnvironment;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;
import biosim.core.util.ArgsToProps;
import biosim.core.util.BTFData;

import sim.util.MutableDouble2D;
import sim.util.Double3D;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;


public class FastSlowFish implements Agent{
	AbstractFish fishBody;

	public double oldTime = 0.0;
	public double lastFastStart = -2.0;
	public double fastPeriod = 2.0;
	public double slowProb = 0.99;
	public double gain = 10.0;
	public double maxScaleMultiplier = 5.0;
	// public double sigma = 1.0;
	public FastSlowFish(AbstractFish b){
		fishBody = b;
	}
	public void init(){
	}
	public void finish(){
	}
	public void act(double time){
		// order of sensors: sep, ori, coh, obs, bias
		MutableDouble2D sep = new MutableDouble2D();
		MutableDouble2D ori = new MutableDouble2D();
		MutableDouble2D coh = new MutableDouble2D();
		MutableDouble2D wall = new MutableDouble2D();
		fishBody.getAverageRBFSameTypeVec(sep,0.1);
		fishBody.getAverageRBFOrientationSameTypeVec(ori,0.2);
		fishBody.getAverageRBFSameTypeVec(coh,1.0);
		fishBody.getNearestObstacleVec(wall);
		wall.multiplyIn(Math.exp(-wall.lengthSq()/(2.0*Math.pow(0.05,2))));
		NeighborhoodStatistics.VelocityStatistics velstats = new NeighborhoodStatistics.VelocityStatistics();
		fishBody.getVelocityStatistics(velstats);

		double scale=0.25;
		double bias = 0.05;
		if((fastPeriod+lastFastStart)>time){
			scale = 1.0;
		} else{
			if(fishBody.getRandom().nextDouble()<(1.0-slowProb)){
				lastFastStart = time;
				scale = 1.0;
			} else{
				scale = scale + maxScaleMultiplier*Math.max(0.0,Math.tanh( gain*(velstats.xvmean-(scale*bias)) ));
				// double sigma = 0.000001;
				// double fastProb = 1.0- ((1.0/(Math.sqrt(2.0*Math.PI*sigma*sigma))) * Math.exp(Math.pow(velstats.xvmean-0.0125,2)/(2.0*sigma*sigma)));
				// double fastProb = (1.0-slowProb)+(1.0-Math.exp(Math.max(0.0,velstats.xvmean-(scale*bias))/sigma));
				// double fastProb = (1-slowProb)+ ( (1.0-slowProb)/ (1.0+ Math.exp(-sigma*)))
				// double fastProbe= (1.0-slowProb)+Math.max(0.0,velstats.xvmean-bias*scale);
				// if (fishBody.getRandom().nextDouble() < fastProb){
				// 	lastFastStart = time;
				// 	scale = 1.0;
				// }
			}
		}
		double desiredXVel = scale*(bias + (-1.0*sep.x) + (-1.0*wall.x));
		double desiredYVel = 0.0;
		double desiredTVel = (-20.0*sep.y) + (0.1*ori.y) + (0.4*coh.y) + (-20.0*wall.y);

		fishBody.setDesiredVelocity(desiredXVel, desiredYVel, desiredTVel);
		oldTime = time;
	}
	
	public static final double WIDTH=2.5;//2.5;
	public static final double HEIGHT=1.5;//2.5;
	
	public static void main(String[] args){
		try{
			// FastKNN knn = null;
			Environment env = null;
			boolean doGui = true, logging = false, walls = true; //doKNNVis = false;
			File loggingDir = null;
			int numFish;
			String sshotdir = null;
			Properties cmdlnDefaults = new Properties();
			cmdlnDefaults.setProperty("--gui","true");
			// cmdlnDefaults.setProperty("--logging","false");
			cmdlnDefaults.setProperty("--walls","true");
			cmdlnDefaults.setProperty("--numFish","27"); //30; //initial tracked number of fish is 27
			Properties cmdlnArgs = ArgsToProps.parse(args,cmdlnDefaults);
			numFish = Integer.parseInt(cmdlnArgs.getProperty("--numFish"));
			env = new Environment(WIDTH,HEIGHT,1.0/30.0);
			//set up the environment
			if(cmdlnArgs.getProperty("--walls").equalsIgnoreCase("true")){
				env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
				env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
				env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
				env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
			} else {
				env.setToroidal(true);				
			}
			//add agents
			NotemigonusCrysoleucas[] bodies = new NotemigonusCrysoleucas[numFish];
			for(int i=0;i<bodies.length;i++){
				bodies[i] = new NotemigonusCrysoleucas();
				env.addBody(bodies[i]);
			}
		
			Agent[] agents = new Agent[numFish];
			for(int i=0; i< numFish;i++){
				agents[i] = new FastSlowFish(bodies[i]);
				bodies[i].setAgent(agents[i]);
			}
			if(cmdlnArgs.getProperty("--logging")!=null){
				FishLRLogger logger = null;
				logger = new FishLRLogger(new File(cmdlnArgs.getProperty("--logging")));
				// logger.setSigmas(reynoldsSpec.sep_sigma,reynoldsSpec.ori_sigma,reynoldsSpec.coh_sigma,reynoldsSpec.obs_sigma);
				env.addLogger(logger);				
			}
			if(cmdlnArgs.getProperty("--gui").equalsIgnoreCase("true")){
				Simulation sim = env.newSimulation();
				GUISimulation gui = new GUISimulation(sim);
				gui.screenshotDir = cmdlnArgs.getProperty("--screenshots");
				gui.setPortrayalClass(NotemigonusCrysoleucas.class, biosim.app.fishknn.FishPortrayal.class);
				gui.setDisplaySize((int)(WIDTH*500),(int)(HEIGHT*500));
				gui.createController();
			} else {
				env.runSimulation(args);
			}
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
