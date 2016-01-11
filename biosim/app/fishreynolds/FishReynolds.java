package biosim.app.fishreynolds;

import biosim.app.fishlr.FishLRLogger;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractFish;
import biosim.core.body.NotemigonusCrysoleucas;
import biosim.core.gui.GUISimulation;
import biosim.core.sim.InitiallyPlacedEnvironment;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;
import biosim.core.util.FastKNN;
import biosim.core.util.BTFData;

import sim.util.MutableDouble2D;
import sim.util.Double3D;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.ArrayList;


public class FishReynolds implements Agent{
	AbstractFish fishBody;
	FastKNN knn;

	public static boolean USE_KNN_INSTEAD=false;

    public static final double SEP_SIGMA=0.1;
    public static final double ORI_SIGMA=1.0; //<- is from calculated features, this is what was simmed -> 0.2;
    public static final double COH_SIGMA=1.0;
    public static final double OBS_SIGMA=1.5; //<- calced, simmed -> 0.05;
    public static final int NUM_FEATURES=8;

    public static int KNN_NEIGHBORS=10;
    public static double[] X_COMPONENTS = new double[NUM_FEATURES+1]; // +1 for bias
    public static double[] Y_COMPONENTS = new double[NUM_FEATURES+1]; // +1 for bias
    public static double[] THETA_COMPONENTS = new double[NUM_FEATURES+1]; // +1 for bias

	public double oldTime = 0.0;
	public FishReynolds(AbstractFish b, FastKNN knn){
		fishBody = b;
		this.knn = knn;
	}
	public void init(){
	}
	public void finish(){
	}
	public void act(double time){
		double[][] nearestK_classes = new double[KNN_NEIGHBORS][3];
		double[][] nearestK_features = new double[KNN_NEIGHBORS][NUM_FEATURES];
		// order of sensors: sep, ori, coh, obs, bias
		MutableDouble2D sep = new MutableDouble2D();
		MutableDouble2D ori = new MutableDouble2D();
		MutableDouble2D coh = new MutableDouble2D();
		MutableDouble2D wall = new MutableDouble2D();
		fishBody.getAverageRBFSameTypeVec(sep,SEP_SIGMA);
		fishBody.getAverageRBFOrientationSameTypeVec(ori,ORI_SIGMA);
		fishBody.getAverageRBFSameTypeVec(coh,COH_SIGMA);
		fishBody.getNearestObstacleVec(wall);
		wall.multiplyIn(Math.exp(-wall.lengthSq()/(2.0*Math.pow(OBS_SIGMA,2))));
		double[] sensors = new double[NUM_FEATURES+1]; // +1 for bias
		double[] features = new double[NUM_FEATURES];
		sensors[0] = sep.x;
		sensors[1] = sep.y;
		sensors[2] = ori.x;
		sensors[3] = ori.y;
		sensors[4] = coh.x;
		sensors[5] = coh.y;
		sensors[6] = wall.x * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(OBS_SIGMA,2)));
		sensors[7] = wall.y * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(OBS_SIGMA,2)));
		sensors[8] = 1.0;
		double xvel = 0.0, yvel = 0.0, tvel = 0.0;
		for(int i=0;i<sensors.length;i++){
			xvel += X_COMPONENTS[i]*sensors[i];
			yvel += Y_COMPONENTS[i]*sensors[i];
			tvel += THETA_COMPONENTS[i]*sensors[i];
			if(i<features.length){
				features[i] = sensors[i];
			}
		}
		if(knn != null || USE_KNN_INSTEAD){
			knn.query(features,nearestK_classes,null,nearestK_features);
			double avgDist = 0.0;
			for(int i=0;i<KNN_NEIGHBORS;i++){
				double tmpD = 0.0;
				for(int j=0;j<X_COMPONENTS.length-1;j++){
					tmpD += Math.pow(nearestK_features[i][j]-features[j],2);
				}
				avgDist += Math.sqrt(tmpD);
			}
			avgDist = avgDist/(double)KNN_NEIGHBORS;
			// Ok, next we're going to actually use kNN instead of LR, JUST BECAUSE
			if(USE_KNN_INSTEAD){
				int rnd_idx = fishBody.getRandom().nextInt(nearestK_classes.length);
				xvel = nearestK_classes[rnd_idx][0];
				yvel = nearestK_classes[rnd_idx][1];
				tvel = nearestK_classes[rnd_idx][2];
			}
			// System.out.println(avgDist);
			((NotemigonusCrysoleucas)fishBody).setAvgDensity(avgDist);
		}
		// System.out.println("yvel:"+yvel);
		fishBody.setDesiredVelocity(xvel, yvel, tvel);
		oldTime = time;
	}
	
	public static void loadLRCoeff(BufferedReader lrCoeffSrc) throws IOException {
		ArrayList<String> coeffLines = new ArrayList<String>();
		for(String line=null; lrCoeffSrc.ready();){
			line = lrCoeffSrc.readLine();
			coeffLines.add(line);
		}
		int readNumCoeffs = coeffLines.size();
		if(NUM_FEATURES != readNumCoeffs){
			System.out.println("[FishReynolds] WARNING! Number of parsed LR coefficients ("+readNumCoeffs+") different from NUM_FEATURES ("+NUM_FEATURES+")");
		}
		X_COMPONENTS = new double[readNumCoeffs];
		Y_COMPONENTS = new double[readNumCoeffs];
		THETA_COMPONENTS = new double[readNumCoeffs];
		for(int i=0;i<readNumCoeffs;i++){
			String[] tmp = coeffLines.get(i).split(" ");
			X_COMPONENTS[i] = Double.parseDouble(tmp[0]);
			Y_COMPONENTS[i] = Double.parseDouble(tmp[1]);
			THETA_COMPONENTS[i] = Double.parseDouble(tmp[2]);
		}
	}

	public static FastKNN loadKNN(BufferedReader kNN_csv_data) throws IOException{
		System.out.println("[FishReynolds] Loading kNN data...");
		FastKNN knn = new FastKNN(NUM_FEATURES,3);
		String[] loadedNames = kNN_csv_data.readLine().split(",");
		//these should match the order of the features used in .act(...) and end with dvel's
		String[] featNames = {	"sepX", "sepY",
								"oriX","oriY",
								"cohX","cohY",
								"wallX","wallY",
								"dvelX","dvelY","dvelT"};
		if(featNames.length != loadedNames.length){
			System.out.println("[FishReynolds] WARNING! Unexpected number of features: "+loadedNames.length+" (expected "+featNames.length+")");
		}
		int[] featIndexes = new int[featNames.length];
		for(int i =0; i<featNames.length;i++){
			boolean found = false;
			for(int j=0; j<loadedNames.length;j++){
				if(loadedNames[j].equalsIgnoreCase(featNames[i])){
					featIndexes[i] = j;
					found = true;
					break;
				}
			}
			if(!found){
				throw new RuntimeException("[FishReynolds] Could not find feature named ["+featNames[i]+"] while loading kNN data.");
			}
		}
		double[] sample = new double[NUM_FEATURES];
		double[] classes = new double[3];
		String line = kNN_csv_data.readLine();
		while(line != null){
			String[] splitLine = line.split(",");
			for(int i=0;i<NUM_FEATURES;i++){
				sample[i] = Double.parseDouble(splitLine[featIndexes[i]]);
			}
			for(int i=0;i<3;i++){
				classes[i] = Double.parseDouble(splitLine[featIndexes[i+NUM_FEATURES]]);
			}
			knn.add(sample,classes);
			line = kNN_csv_data.readLine();
		}
		System.out.println("[FishReynolds] Finished loading kNN.");
		return knn;
	}

	public static final double WIDTH=2.5;//2.5;
	public static final double HEIGHT=1.5;//2.5;
	
	public static void main(String[] args){
		try{
			FastKNN knn = null;
			Environment env = null;
			boolean doKNNVis = false, initialPlacement = false, doGui = true, logging = false, walls = true;
			BufferedReader poseSrc = null;
			File loggingDir = null;
			int numFish = 27; //30; //initial tracked number of fish is 27
			for(int i=0;i<args.length;i++){
				//System.err.println(args[i]);
				if(args[i].equalsIgnoreCase("-vis")){
					doKNNVis=true;
					if(knn == null){
						knn = loadKNN(new BufferedReader(new FileReader(args[i+1])));
					}
				}
				else if(args[i].equalsIgnoreCase("-lr")){
					loadLRCoeff(new BufferedReader(new FileReader(args[i+1])));
				}
				else if(args[i].equalsIgnoreCase("-placed")){
					initialPlacement = true;
					poseSrc = new BufferedReader(new FileReader(args[i+1]));
				}
				else if(args[i].equalsIgnoreCase("-nogui")){
					doGui = false;
				}
				else if(args[i].equalsIgnoreCase("-logging")){
					logging = true;
					if (i<args.length-1 && args[i+1].charAt(0)!='-'){
						loggingDir = new File(args[i+1]);
					}
				}
				else if(args[i].equalsIgnoreCase("-knn")){
					USE_KNN_INSTEAD = true;
					if(knn == null){
						knn = loadKNN(new BufferedReader(new FileReader(args[i+1])));
					}
				}
				else if (args[i].equalsIgnoreCase("-nowalls")){
					walls=false;
				}
			}
			if(initialPlacement){
				env = new InitiallyPlacedEnvironment(WIDTH,HEIGHT,1.0/30.0);
				// ((InitiallyPlacedEnvironment)env).parseInitialPoses(btf);
				numFish = ((InitiallyPlacedEnvironment)env).parseInitialPoses(poseSrc);
			} else {
				env = new Environment(WIDTH,HEIGHT,1.0/30.0);
			}
			//set up the environment
			if(walls){
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
				agents[i] = new FishReynolds(bodies[i],knn);
				bodies[i].setAgent(agents[i]);
			}
						
			if(logging){
				FishLRLogger logger = null;
				if(loggingDir == null){
					logger = new FishLRLogger();
				} else {
					logger = new FishLRLogger(loggingDir);
				}
				logger.setSigmas(SEP_SIGMA,ORI_SIGMA,COH_SIGMA,OBS_SIGMA);
				env.addLogger(logger);				
			}
			if(doGui){
				Simulation sim = env.newSimulation();
				GUISimulation gui = new GUISimulation(sim);
				gui.setPortrayalClass(NotemigonusCrysoleucas.class, biosim.app.fishknn.FishPortrayal.class);
				biosim.app.fishknn.FishPortrayal.AVG_DIST = 0.0449592693977;
				biosim.app.fishknn.FishPortrayal.STD_DEV_DIST = 0.0235436856268;
				biosim.app.fishknn.FishPortrayal.bi=null;
				// gui.setDisplaySize((int)(WIDTH*380),(int)(HEIGHT*380));
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
