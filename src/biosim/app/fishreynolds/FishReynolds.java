package biosim.app.fishreynolds;

import biosim.app.fishlr.FishLRLogger;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractFish;
import biosim.core.body.NotemigonusCrysoleucas;
import biosim.core.body.ReplayFish;
import biosim.core.gui.GUISimulation;
import biosim.core.learning.KNNModel;
import biosim.core.learning.LearnerAgent;
import biosim.core.learning.LinregModel;
import biosim.core.learning.ProblemSpec;
import biosim.core.learning.RNGConsumer;
import biosim.core.sim.InitiallyPlacedEnvironment;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;
import biosim.core.util.ArgsToProps;
import biosim.core.util.BTFData;
import biosim.core.util.FastKNN;

import sim.util.MutableDouble2D;
import sim.util.Double3D;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;


public class FishReynolds implements Agent{
	AbstractFish fishBody;
	LearnerAgent learner;
	ProblemSpec probSpec;

	public double oldTime = 0.0;
	public FishReynolds(AbstractFish b, LearnerAgent l, ProblemSpec p){
		fishBody = b;
		this.learner = l;
		this.probSpec = p;
	}
	public void init(){
	}
	public void finish(){
	}
	public void act(double time){
		double[] sensors = probSpec.computeFeatures(fishBody);
		if(learner instanceof RNGConsumer){
			if(((RNGConsumer)learner).getRandom()==null){
				((RNGConsumer)learner).setRandom(fishBody.getRandom());
			}
		}
		double[] learner_output = learner.computeOutputs(sensors,null);
		fishBody.setDesiredVelocity(learner_output[0], learner_output[1], learner_output[2]);
		oldTime = time;
	}
	
	public static ArrayList<ReplayFish> loadReplays(BTFData btf, ArrayList<Integer> ignoreTrackIDs) throws IOException{
		ArrayList<ReplayFish> rv = new ArrayList<ReplayFish>();
		String[] id = btf.loadColumn("id");
		String[] xpos = btf.loadColumn("xpos");
		String[] ypos = btf.loadColumn("ypos");
		String[] tpos = btf.loadColumn("timage");
		String[] time = btf.loadColumn("clocktime");
		for(int t=0;t<id.length;t++){
			if(t%(id.length/10)==0) System.out.println("Line #"+t);
			// if(ignoreTrackIDs.contains(Integer.parseInt(id[t].trim()))){
			// 	continue;
			// }
			int trackIdx = -1;
			for(int i=0;i<rv.size();i++){
				if(rv.get(i).trackID == Integer.parseInt(id[t].trim())){
					trackIdx = i;
					break;
				}
			}
			if(trackIdx == -1){
				rv.add(new ReplayFish());
				trackIdx = rv.size()-1;
				rv.get(trackIdx).trackID = Integer.parseInt(id[t].trim());
				rv.get(trackIdx).label = id[t].trim();
				rv.get(trackIdx).visible = false;
				rv.get(trackIdx).size = NotemigonusCrysoleucas.SIZE;
				rv.get(trackIdx).track = new ArrayList<double[]>();
			}
			double[] tmp = new double[4];
			tmp[0] = Double.parseDouble(xpos[t].trim());
			tmp[1] = Double.parseDouble(ypos[t].trim());
			tmp[2] = Double.parseDouble(tpos[t].trim());
			tmp[3] = Double.parseDouble(time[t].trim());
			rv.get(trackIdx).track.add(tmp);
		}
		return rv;
	}

	public static final double WIDTH=2.5;//2.5;
	public static final double HEIGHT=1.5;//2.5;
	
	public static void main(String[] args){
		try{
			// FastKNN knn = null;
			Environment env = null;
			boolean initialPlacement = false, doGui = true, logging = false, walls = true; //doKNNVis = false;
			BufferedReader poseSrc = null;
			File loggingDir = null;
			int numFish;
			ArrayList<ReplayFish> replayFish = null;
			ArrayList<Integer> ignoreTrackIDs = new ArrayList<Integer>();
			BTFData replayBTF=null;
			String sshotdir = null;
			LinregModel lrm = null;//new FSLRMVelocityFeatures();
			KNNModel knn_model = null;
			BiasedLinearAgent bla_model = null;
			LearnerAgent learner_to_use = null;
			ReynoldsFeatures reynoldsSpec = new ReynoldsFeatures();
			Properties cmdlnDefaults = new Properties();
			cmdlnDefaults.setProperty("--modelType","LINREG");
			cmdlnDefaults.setProperty("--gui","true");
			// cmdlnDefaults.setProperty("--logging","false");
			cmdlnDefaults.setProperty("--walls","true");
			cmdlnDefaults.setProperty("--numFish","27"); //30; //initial tracked number of fish is 27
			Properties cmdlnArgs = ArgsToProps.parse(args,cmdlnDefaults);
			reynoldsSpec.configure(cmdlnArgs);
			// for(int i=0;i<args.length;i++){
			// 	if(args[i].equalsIgnoreCase("-lr")){
			// 		lrm = new LinregModel();
			// 		lrm.loadParameters(new BufferedReader(new FileReader(args[i+1])));
			// 		learner_to_use = lrm;
			// 		// loadLRCoeff(new BufferedReader(new FileReader(args[i+1])));
			// 	}
			// 	else if(args[i].equalsIgnoreCase("-spec")){
			// 		reynoldsSpec.loadFeatureSigma(new BufferedReader(new FileReader(args[i+1])));
			// 		// loadFeatureSigma(new BufferedReader(new FileReader(args[i+1])));
			// 	}
			// 	else if(args[i].equalsIgnoreCase("-placed")){
			// 		initialPlacement = true;
			// 		poseSrc = new BufferedReader(new FileReader(args[i+1]));
			// 	}
			// 	else if(args[i].equalsIgnoreCase("-nogui")){
			// 		doGui = false;
			// 	}
			// 	else if(args[i].equalsIgnoreCase("-logging")){
			// 		logging = true;
			// 		if (i<args.length-1 && args[i+1].charAt(0)!='-'){
			// 			loggingDir = new File(args[i+1]);
			// 		}
			// 	}
			// 	else if(args[i].equalsIgnoreCase("-knn")){
			// 		knn_model = new KNNModel(reynoldsSpec.getNumFeatures(),reynoldsSpec.getNumOutputs());
			// 		knn_model.loadParameters(new BufferedReader(new FileReader(args[i+1])));
			// 		learner_to_use = knn_model;
			// 	}
			// 	else if (args[i].equalsIgnoreCase("-nowalls")){
			// 		walls=false;
			// 	}
			// 	else if(args[i].equalsIgnoreCase("-replay")){
			// 		replayBTF = new BTFData();
			// 		replayBTF.loadDir(new File(args[i+1]));
			// 	}
			// 	else if(args[i].equalsIgnoreCase("-ignoreTrackIDs")){
			// 		String[] ids = args[i+1].split(",");
			// 		for(int jay=0;jay<ids.length;jay++){
			// 			ignoreTrackIDs.add(Integer.parseInt(ids[jay]));
			// 		}
			// 	} else if(args[i].equalsIgnoreCase("-screenshotDir")){
			// 		sshotdir = args[i+1];
			// 	} else if(args[i].equalsIgnoreCase("-momentum")){
			// 		bla_model = new BiasedLinearAgent();
			// 		bla_model.loadParameters(new BufferedReader(new FileReader(args[i+1])));
			// 		learner_to_use = bla_model;
			// 	} else if(args[i].equalsIgnoreCase("-method")){
			// 		knn_model.setMethod(args[i+1]);
			// 	} else if(args[i].equalsIgnoreCase("-signorm")){
			// 		knn_model.setNormFeatures(Boolean.parseBoolean(args[i+1]));
			// 	}
			// }
			numFish = Integer.parseInt(cmdlnArgs.getProperty("--numFish"));
			if(cmdlnArgs.getProperty("--placed") != null){
				InitiallyPlacedEnvironment placedEnv = new InitiallyPlacedEnvironment(WIDTH,HEIGHT,1.0/30.0);
				// ((InitiallyPlacedEnvironment)env).parseInitialPoses(btf);
				poseSrc = new BufferedReader(new FileReader(cmdlnArgs.getProperty("--placed")));
				numFish = placedEnv.parseInitialPoses(poseSrc);
				env = placedEnv;
			} else {
				env = new Environment(WIDTH,HEIGHT,1.0/30.0);
			}
			//set up the environment
			if(cmdlnArgs.getProperty("--walls").equalsIgnoreCase("true")){
				env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
				env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
				env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
				env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
			} else {
				env.setToroidal(true);				
			}
			switch(cmdlnArgs.getProperty("--modelType").toUpperCase()){
				case "LINREG":
					lrm = new LinregModel();
					learner_to_use = lrm;
					break;
				case "KNN":
					knn_model = new KNNModel(reynoldsSpec.getNumFeatures(), reynoldsSpec.getNumOutputs());
					learner_to_use = knn_model;
					break;
				case "MOMENTUM":
					bla_model = new BiasedLinearAgent();
					learner_to_use = bla_model;
					break;
				default:
					throw new RuntimeException("Unrecognized model type: "+cmdlnArgs.getProperty("--modelType"));
			}
			Properties modelProps = new Properties(cmdlnArgs);
			if(cmdlnArgs.getProperty("--modelSettings")!=null){
				modelProps.load(new FileReader(cmdlnArgs.getProperty("--modelSettings")));
			}
			learner_to_use.configure(modelProps);
			learner_to_use.loadParameters(new BufferedReader(new FileReader(cmdlnArgs.getProperty("--modelFile"))));
			if(cmdlnArgs.getProperty("--pspec")!=null){
				reynoldsSpec.configure(new BufferedReader(new FileReader(cmdlnArgs.getProperty("--pspec"))));
			}
			if(cmdlnArgs.getProperty("--ignoreTrackIDs")!=null){
				String[] ids = cmdlnArgs.getProperty("--ignoreTrackIDs").split(",");
				// for(int eye=0;eye<ids.length;eye++){
				for(String ignoreid : cmdlnArgs.getProperty("--ignoreTrackIDs").split(",")){
					// ignoreTrackIDs.add(Integer.parseInt(ids[eye]));
					ignoreTrackIDs.add(Integer.parseInt(ignoreid));
				}
			}
			//add agents
			if(cmdlnArgs.getProperty("--replay") == null){
				NotemigonusCrysoleucas[] bodies = new NotemigonusCrysoleucas[numFish];
				for(int i=0;i<bodies.length;i++){
					bodies[i] = new NotemigonusCrysoleucas();
					env.addBody(bodies[i]);
				}
			
				Agent[] agents = new Agent[numFish];
				for(int i=0; i< numFish;i++){
					agents[i] = new FishReynolds(bodies[i],learner_to_use,reynoldsSpec);
					bodies[i].setAgent(agents[i]);
				}
			} else {
				replayBTF = new BTFData();
				replayBTF.loadDir(new File(cmdlnArgs.getProperty("--replay")));
				ArrayList<Integer> allIDs = new ArrayList<Integer>();
				String[] idCol = replayBTF.loadColumn("id");
				for(int i=0;i<idCol.length;i++){
					int foo = Integer.parseInt(idCol[i]);
					if(allIDs.contains(foo)){
						continue;
					} else {
						allIDs.add(foo);
					}
				}
				replayFish = loadReplays(replayBTF,ignoreTrackIDs);
				for(int i=0;i<replayFish.size();i++){
					env.addBody(replayFish.get(i));
				}
				for(int i=0;i<ignoreTrackIDs.size();i++){
					NotemigonusCrysoleucas body = new NotemigonusCrysoleucas();
					body.label = ignoreTrackIDs.get(i).toString();
					env.addBody(body);
					Agent agent = new FishReynolds(body,learner_to_use,reynoldsSpec);
					body.setAgent(agent);
				}
			}
			if(cmdlnArgs.getProperty("--logging")!=null){
				FishLRLogger logger = null;
				logger = new FishLRLogger(new File(cmdlnArgs.getProperty("--logging")));
				logger.setSigmas(reynoldsSpec.sep_sigma,reynoldsSpec.ori_sigma,reynoldsSpec.coh_sigma,reynoldsSpec.obs_sigma);
				env.addLogger(logger);
			}
			String settingsFile = cmdlnArgs.getProperty("--settingsFile");
			try{
				OutputStream outstrm = System.out;
				if(settingsFile != null){
					outstrm = new BufferedOutputStream(new FileOutputStream(new File(settingsFile)));
				}
				String settingsComments = "FishReynolds settings\n";
				settingsComments += "Arguments: ";
				for(int i=0;i<args.length;i++){
					settingsComments += args[i]+" ";
				}
				cmdlnArgs.store(outstrm,settingsComments);
				outstrm.flush();
			} catch(IOException ioe){
				throw new RuntimeException("[DataAsDemonstrator] Failed to store settings: "+ioe);
			}

			if(cmdlnArgs.getProperty("--gui").equalsIgnoreCase("true")){
				Simulation sim = env.newSimulation();
				GUISimulation gui = new GUISimulation(sim);
				gui.screenshotDir = cmdlnArgs.getProperty("--screenshots");
				gui.setPortrayalClass(NotemigonusCrysoleucas.class, biosim.app.fishknn.FishPortrayal.class);
				gui.setPortrayalClass(ReplayFish.class, ReplayPortrayal.class);
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
