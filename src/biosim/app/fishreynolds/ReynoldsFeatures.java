package biosim.app.fishreynolds;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import sim.util.MutableDouble2D;
import sim.engine.SimState;
import ec.util.MersenneTwisterFast;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractFish;
import biosim.core.body.Body;
import biosim.core.body.sensors.NeighborhoodStatistics;
import biosim.core.body.NotemigonusCrysoleucas;
import biosim.core.body.ReplayFish;
import biosim.core.learning.KNNModel;
import biosim.core.learning.LearnerAgent;
import biosim.core.learning.LinregModel;
import biosim.core.learning.PerformanceMetric;
import biosim.core.learning.ProblemSpec;
import biosim.core.sim.Environment;
import biosim.core.sim.InitiallyPlacedEnvironment;
import biosim.core.sim.RectObstacle;
import biosim.core.sim.Simulation;
import biosim.core.util.BufferedBTFData;
import biosim.core.util.BTFData;
import biosim.core.util.BTFDataLogger;

public class ReynoldsFeatures implements ProblemSpec{

	// WITH VELSTATS: sepX,sepY,oriX,oriY,cohX,cohY,obsX,obsY,xvmean,xvstd,xvmax
	// NO PVEL:   sepX,sepY,oriX,oriY,cohX,cohY,obsX,obsY
	public static final int NUM_FEATURES=11;
	//X, Y, T
	public static final int NUM_OUTPUTS=3;

	public static long timeout;

	public double sep_sigma, ori_sigma, coh_sigma, obs_sigma;

	public String learner;

	// public boolean normalize_features;
	// public boolean use_pvel;
	public boolean use_walls;
	public boolean use_velstats;
	// public String combo_method;

	public long seed;
	public void setSeed(long s){seed=s;}
	public long getSeed(){return seed;}

	protected Properties learnerProps=null;
	public Properties getLearnerProps(){return learnerProps;}
	public void setLearnerProps(Properties lp){learnerProps=lp;} 

	protected MersenneTwisterFast rng;
	public MersenneTwisterFast getRNG(){
		if(rng == null){
			rng = new MersenneTwisterFast(getSeed());
		}
		return rng;
	}

	public static Properties defaults(){
		Properties defaultProps = new Properties();
		defaultProps.setProperty("SEP_SIGMA","0.1");
		defaultProps.setProperty("ORI_SIGMA","0.2");
		defaultProps.setProperty("COH_SIGMA","1.0");
		defaultProps.setProperty("OBS_SIGMA","0.05");
		defaultProps.setProperty("TIMEOUT","5000"); //timeout is in milliseconds
		defaultProps.setProperty("LEARNER","KNN");
		// defaultProps.setProperty("NORMALIZE_FEATURES","TRUE");
		// defaultProps.setProperty("USE_PVEL","FALSE");
		// defaultProps.setProperty("COMBO_METHOD","SAMPLE");
		defaultProps.setProperty("SEED",Long.toString(System.currentTimeMillis()));
		defaultProps.setProperty("USE_WALLS","TRUE");
		defaultProps.setProperty("USE_VELSTATS","FALSE");
		return defaultProps;
	}

	public ReynoldsFeatures(){
		configure(defaults());
	}

	public void configure(BufferedReader featuresSource) throws IOException{
		Properties props = new Properties();
		props.load(featuresSource);
		configure(props);
	}

	public void configure(Properties props){
		Properties tmp = props;
		props = new Properties(defaults());
		props.putAll(tmp);
		sep_sigma = Double.parseDouble(props.getProperty("SEP_SIGMA"));
		ori_sigma = Double.parseDouble(props.getProperty("ORI_SIGMA"));
		coh_sigma = Double.parseDouble(props.getProperty("COH_SIGMA"));
		obs_sigma = Double.parseDouble(props.getProperty("OBS_SIGMA"));
		timeout = Long.parseLong(props.getProperty("TIMEOUT"));
		learner = props.getProperty("LEARNER");
		// normalize_features = Boolean.parseBoolean(props.getProperty("NORMALIZE_FEATURES"));
		// use_pvel = Boolean.parseBoolean(props.getProperty("USE_PVEL"));
		use_velstats = Boolean.parseBoolean(props.getProperty("USE_VELSTATS"));
		// combo_method = props.getProperty("COMBO_METHOD");
		seed = Long.parseLong(props.getProperty("SEED"));
		use_walls = Boolean.parseBoolean(props.getProperty("USE_WALLS"));
	}

	public Properties getSettings(){
		Properties settings = defaults();
		settings.setProperty("SEP_SIGMA",Double.toString(sep_sigma));
		settings.setProperty("ORI_SIGMA",Double.toString(ori_sigma));
		settings.setProperty("COH_SIGMA",Double.toString(coh_sigma));
		settings.setProperty("OBS_SIGMA",Double.toString(obs_sigma));
		settings.setProperty("TIMEOUT",Long.toString(timeout));
		settings.setProperty("LEARNER",learner);
		// settings.setProperty("NORMALIZE_FEATURES",Boolean.toString(normalize_features));
		// settings.setProperty("USE_PVEL",Boolean.toString(use_pvel));
		settings.setProperty("USE_VELSTATS",Boolean.toString(use_velstats));
		// settings.setProperty("COMBO_METHOD",combo_method);
		settings.setProperty("SEED",Long.toString(seed));
		settings.setProperty("USE_WALLS",Boolean.toString(use_walls));
		settings.putAll(makeLearner().getSettings());
		return settings;
	}

	public int getNumFeatures(){ return (use_velstats)?NUM_FEATURES:NUM_FEATURES-3;}
	public int getNumOutputs(){ return NUM_OUTPUTS;}

	public double[] computeFeatures(Body b){
		if(b instanceof AbstractFish){
			AbstractFish fishBody = (AbstractFish)b;
			// order of sensors: sep, ori, coh, obs, bias
			MutableDouble2D sep = new MutableDouble2D();
			MutableDouble2D ori = new MutableDouble2D();
			MutableDouble2D coh = new MutableDouble2D();
			MutableDouble2D wall = new MutableDouble2D();
			fishBody.getAverageRBFSameTypeVec(sep,sep_sigma);
			fishBody.getAverageRBFOrientationSameTypeVec(ori,ori_sigma);
			fishBody.getAverageRBFSameTypeVec(coh,coh_sigma);
			fishBody.getNearestObstacleVec(wall);
			wall.multiplyIn(Math.exp(-wall.lengthSq()/(2.0*Math.pow(obs_sigma,2))));
			NeighborhoodStatistics.VelocityStatistics velstats = new NeighborhoodStatistics.VelocityStatistics();
			// double[] pvel = {0.0,0.0,0.0};
			// if(use_pvel){
			// 	fishBody.getSelfVelXYT(pvel);
			// }
			if(use_velstats){
				fishBody.getVelocityStatistics(velstats);
			}
			double[] sensors = new double[getNumFeatures()];
			sensors[0] = sep.x;
			sensors[1] = sep.y;
			sensors[2] = ori.x;
			sensors[3] = ori.y;
			sensors[4] = coh.x;
			sensors[5] = coh.y;
			sensors[6] = wall.x * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(obs_sigma,2)));
			sensors[7] = wall.y * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(obs_sigma,2)));
			// if(use_pvel){
			// 	sensors[8] = pvel[0];
			// 	sensors[9] = pvel[1];
			// 	sensors[10] = pvel[2];
			// }
			if(use_velstats){
				sensors[8] = velstats.xvmean;
				sensors[9] = velstats.xvstd;
				sensors[10] = velstats.xvmax;
			}
			return sensors;
		} else {
			throw new RuntimeException(b+" is not an AbstractFish");
		}
	}
	public static final void copyInto(double[][] from, double[][] to, int colOffset){
		copyInto(from,to,colOffset,0);
	}
	public static final void copyInto(double[][] from,double[][] to, int colOffset, int rowOffset){
		for(int i=0;i<from.length;i++){
			System.arraycopy(from[i],0,to[rowOffset+i],colOffset,from[0].length);
		}
	}

	public Dataset btf2array(BTFData btf){
		Dataset rv = new Dataset();
		try{
			int numRows = btf.loadColumn("id").length;
			rv.features = new double[numRows][getNumFeatures()];
			rv.outputs = new double[numRows][getNumOutputs()];
			// WITH PVEL: sepX,sepY,oriX,oriY,cohX,cohY,obsX,obsY,pvelX,pvelY,pvelT
			// NO PVEL: sepX,sepY,oriX,oriY,cohX,cohY,obsX,obsY
			double[][] column = btf.columnAsDoubles("rbfsepvec");
			copyInto(column,rv.features,0);
			column = btf.columnAsDoubles("rbforivec");
			copyInto(column,rv.features,2);
			column = btf.columnAsDoubles("rbfcohvec");
			copyInto(column,rv.features,4);
			column = btf.columnAsDoubles("rbfwallvec");
			copyInto(column,rv.features,6);
			// if(use_pvel){
			// 	column = btf.columnAsDoubles("pvel");
			// 	copyInto(column,rv.features,8);
			// }
			if(use_velstats){
				column = btf.columnAsDoubles("xvmean");
				copyInto(column,rv.features,8);
				column = btf.columnAsDoubles("xvstd");
				copyInto(column,rv.features,9);
				column = btf.columnAsDoubles("xvmax");
				copyInto(column,rv.features,10);
			}
			//dvelX, dvelY, dvelT
			column = btf.columnAsDoubles("dvel");
			copyInto(column,rv.outputs,0);
		} catch(IOException ioe){
			throw new RuntimeException("[ReynoldsFeatures] Unable to parse btf: "+ioe);
		}
		return rv;
	}

	public static final double WIDTH=2.5, HEIGHT=1.5;

	public static ArrayList<ReplayFish> loadReplays(BTFData btf, Integer ignoredTrackID) throws IOException{
		ArrayList<ReplayFish> rv = new ArrayList<ReplayFish>();
		String[] id = btf.loadColumn("id");
		String[] xpos = btf.loadColumn("xpos");
		String[] ypos = btf.loadColumn("ypos");
		String[] tpos = btf.loadColumn("timage");
		String[] time = btf.loadColumn("clocktime");
		long curTime, lastTime;
		curTime = lastTime = System.currentTimeMillis();
		for(int t=0;t<id.length;t++){
			curTime = System.currentTimeMillis();
			if(curTime-lastTime>timeout){
				System.out.println("Line #"+t);
				lastTime = curTime;
			}
			// if(ignoredTrackID == Integer.parseInt(id[t].trim())){
			// continue;
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


	public Environment getEnvironment(LearnerAgent la, BTFData replayBTF, Integer ignoredTrackID){
		InitiallyPlacedEnvironment env = new InitiallyPlacedEnvironment(WIDTH, HEIGHT, 1.0/30.0);
		if(use_walls){
			env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
			env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
			env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
			env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
		} else {
			env.setToroidal(true);
		}
		try{
			env.parseInitialPoses(replayBTF);
			if(ignoredTrackID==null){
				ArrayList<Integer> allIDs =replayBTF.getUniqueIDs();
				for(int i=0;i<allIDs.size();i++){
					NotemigonusCrysoleucas body = new NotemigonusCrysoleucas();
					body.label = allIDs.get(i).toString();
					env.addBody(body);
					Agent agent = new FishReynolds(body,la,this);
					body.setAgent(agent);
				}
			} else {
				ArrayList<ReplayFish> replayFish = loadReplays(replayBTF,ignoredTrackID);
				for(int i=0;i<replayFish.size();i++){
					env.addBody(replayFish.get(i));
				}
				NotemigonusCrysoleucas body = new NotemigonusCrysoleucas();
				body.label = ignoredTrackID.toString();
				env.addBody(body);
				Agent agent = new FishReynolds(body,la,this);
				body.setAgent(agent);
			}
		} catch(IOException ioe){
			throw new RuntimeException("[ReynoldsFeatures] could not initialize environment: "+ioe);
		}
		return env;
	}

	public LearnerAgent makeLearner(){
		LearnerAgent rv = null;
		if(learner.equalsIgnoreCase("KNN")){
			KNNModel knnm = new KNNModel();
			// knnm.setNormFeatures(normalize_features);
			// knnm.setMethod(combo_method);
			// if(use_pvel){
			// 	knnm.setFeatureNames(new String[] {"sepX","sepY","oriX","oriY","cohX","cohY","wallX","wallY","pvelX","pvelY","pvelT"}); //WITH PVEL
			// } 
			if(use_velstats){
				knnm.setFeatureNames(new String[] {"sepX","sepY","oriX","oriY","cohX","cohY","wallX","wallY","xvmean","xvstd","xvmax"}); //velstats
			}else {
				knnm.setFeatureNames(new String[] {"sepX","sepY","oriX","oriY","cohX","cohY","wallX","wallY"}); //NO VELSTATS
			}
			knnm.setOutputNames(new String[] {"dvelX","dvelY","dvelT"});
			knnm.setRandom(getRNG());
			rv = knnm;
		} else if(learner.equalsIgnoreCase("LINREG")){
			LinregModel lrm = new LinregModel(getNumFeatures(),getNumOutputs());
			rv = lrm;
		} else {
			throw new RuntimeException("[ReynoldsFeatures] Unknown learner: "+learner);
		}
		if(learnerProps != null){
			rv.configure(learnerProps);
		}
		return rv;
	}

	public ArrayList<PerformanceMetric> evaluate(ArrayList<BTFData> testSet, LearnerAgent learner, ExecutorService threadPool){
		ArrayList<PerformanceMetric> rv = new ArrayList<PerformanceMetric>();
		rv.add(averageSequenceError(testSet,learner,threadPool));
		rv.add(averageEndpointError(testSet,learner,threadPool));
		return rv;
	}

	public PerformanceMetric averageSequenceError(ArrayList<BTFData> testSet, LearnerAgent learner, ExecutorService threadPool){
		HashSet<Callable<Double> > tasks = new HashSet<Callable<Double> >();
		for(BTFData testBTF: testSet){
			Dataset testD = btf2array(testBTF);
			Callable<Double> tmp = new Callable<Double>(){
				public Double call(){
					double[] learnerOuts = new double[getNumOutputs()];
					double sumErr = 0.0;
					for(int row=0;row<testD.features.length;row++){
						learner.computeOutputs(testD.features[row],learnerOuts);
						double mse = 0.0;
						for(int col=0;col<learnerOuts.length;col++){
							mse += Math.pow(learnerOuts[col]-testD.outputs[row][col],2);
						}
						sumErr += Math.sqrt(mse);
					}
					return sumErr;
				}
			};
			tasks.add( tmp );
		}
		List<Future<Double> > results;
		double sumErr = 0.0;
		try{
			results = threadPool.invokeAll(tasks);
			for(Future<Double> f : results){
				sumErr += f.get();
			} 
		} catch(InterruptedException ie){
			threadPool.shutdown();
			throw new RuntimeException("[ReynoldsFeatures] Evaluation interrupted: "+ie);
		} catch (ExecutionException ee){
			threadPool.shutdown();
			ee.printStackTrace();
			throw new RuntimeException("[ReynoldsFeatures] ExecutionException in evaluation: "+ee);
		}
		final double computedError = sumErr/(double)testSet.size();
		return new PerformanceMetric(){
			public String toString(){return "Average 1-step error per fish: "+value();}
			public double value(){return computedError;}
		};
	}

	public PerformanceMetric averageEndpointError(ArrayList<BTFData> testSet, LearnerAgent learner, ExecutorService threadPool){
		// double avgXEPErr = 0.0, avgYEPErr = 0.0, avgTEPErr = 0.0;
		// try{
		int tmpl = -1;
		for(BTFData seq: testSet){
			int snf = seq.numUniqueFrames();
			if(tmpl < 0 || snf < tmpl){
				tmpl = snf;
			}
		}
		final int minPredictionLength = tmpl;
		HashSet<Callable<Double[]>> tasks = new HashSet<Callable<Double[]>>();
		for(BTFData seq : testSet){
			// ArrayList<BTFData.BTFDataFrame> frames = seq.splitIntoFrames();
			// int numSimSteps = frames.size();
			// int numSimSteps = seq.numUniqueFrames();
			final MersenneTwisterFast tLocalRNG = new MersenneTwisterFast(getRNG().nextLong());
			Callable<Double[]> tmp = new Callable<Double[]>(){
				public Double[] call() throws IOException{
					Double[] callableRv = new Double[3];
					ArrayList<Integer> idList = seq.getUniqueIDs();
					String[] testingX = seq.loadColumn("xpos");
					String[] testingY = seq.loadColumn("ypos");
					String[] testingTheta = seq.loadColumn("timage");
					double seqXEPErr = 0.0;
					double seqYEPErr = 0.0;
					double seqTEPErr = 0.0;
					for(int idIdx=0;idIdx<idList.size();idIdx++){
						int activeID = idList.get(idIdx);
						Environment env = getEnvironment(learner,seq,activeID);
						BTFDataLogger logs = getLogger();
						env.addLogger(logs);
						Simulation sim = env.newSimulation();
						sim.random = tLocalRNG;
						sim.start();
						while(sim.schedule.getSteps()<(minPredictionLength)){
							boolean step_success = sim.schedule.step(sim);
						}
						sim.finish();
						BufferedBTFData loggedBTF = (BufferedBTFData)(logs.getBTFData());
						ArrayList<Integer> activeIDRows = loggedBTF.rowIndexForID(activeID);
						ArrayList<Integer> testingActiveIDRows = seq.rowIndexForID(activeID);
						int lastActiveIDRow = activeIDRows.get(activeIDRows.size()-1);
						int lastTestingActiveIDRow = testingActiveIDRows.get(testingActiveIDRows.size()-1);
						String predictedX = loggedBTF.loadColumn("xpos")[lastActiveIDRow];
						String predictedY = loggedBTF.loadColumn("ypos")[lastActiveIDRow];
						String predictedTheta = loggedBTF.loadColumn("timage")[lastActiveIDRow];
						double xEPErr = (Double.parseDouble(testingX[lastTestingActiveIDRow]) - Double.parseDouble(predictedX));
						double yEPErr = (Double.parseDouble(testingY[lastTestingActiveIDRow]) - Double.parseDouble(predictedY));
						double thetaEPErr = (Double.parseDouble(testingTheta[lastTestingActiveIDRow]) - Double.parseDouble(predictedTheta));
						seqXEPErr += Math.pow(xEPErr,2);
						seqYEPErr += Math.pow(yEPErr,2);
						seqTEPErr += Math.pow(thetaEPErr,2);
					}
					// avgXEPErr += Math.sqrt(seqXEPErr)/(double)idList.size();
					// avgYEPErr += Math.sqrt(seqYEPErr)/(double)idList.size();
					// avgTEPErr += Math.sqrt(seqTEPErr)/(double)idList.size();
					callableRv[0] = Math.sqrt(seqXEPErr)/(double)idList.size();
					callableRv[1] = Math.sqrt(seqYEPErr)/(double)idList.size();
					callableRv[2] = Math.sqrt(seqTEPErr)/(double)idList.size();
					return callableRv;
				}
			};
			tasks.add(tmp);
		}
		List<Future<Double[]>> results = null;
		double tmpX=0.0, tmpY=0.0, tmpT=0.0;
		try{
			results=threadPool.invokeAll(tasks);
			for(Future<Double[]> res : results){
				Double[] tmpRv = res.get();
				tmpX += tmpRv[0];
				tmpY += tmpRv[1];
				tmpT += tmpRv[2];
			}
		} catch(InterruptedException ie){
			threadPool.shutdown();
			throw new RuntimeException("[ReynoldsFeatures] Evaluation interrupted in averageEndpointError: "+ie);
		} catch(ExecutionException ee){
			threadPool.shutdown();
			ee.printStackTrace();
			throw new RuntimeException("[ReynoldsFeatures] ExecutionException in averageEndpointError: "+ee);
		} 
		final double x = tmpX/(double)testSet.size();
		final double y = tmpY/(double)testSet.size();
		final double t = tmpT/(double)testSet.size();
		return new PerformanceMetric(){
			// final double x=avgXEPErr,y=avgYEPErr,t=avgTEPErr;
			public String toString(){return String.format("Average endpoint error per fish: x:%f y:%f t:%f, sum: %f",x,y,t,(x+y+t));}
			public double value(){return x+y+t;}
		};
		// } catch(IOException ioe){
		// 	throw new RuntimeException("[ReynoldsFeatures] IOException on averageEndpointError evaluation:"+ioe);
		// }
	}

	public BTFDataLogger getLogger(){
		return new BTFDataLogger(){
			public LinkedList<String> rbfsepvec, rbforivec, rbfcohvec, rbfwallvec, xvmean, xvstd, xvmax, dvel, dbool; //pvel
			public void init(){
				super.init();
				rbfsepvec = new LinkedList<String>();
				rbforivec = new LinkedList<String>();
				rbfcohvec = new LinkedList<String>();
				rbfwallvec = new LinkedList<String>();
				// if(use_pvel){
				// 	pvel = new LinkedList<String>();					
				// }
				if(use_velstats){
					xvmean = new LinkedList<String>();
					xvstd = new LinkedList<String>();
					xvmax = new LinkedList<String>();
				}
				dvel = new LinkedList<String>();
				dbool = new LinkedList<String>();
			}
			public BTFData getBTFData(){
				BufferedBTFData rv = (BufferedBTFData)(super.getBTFData());
				if(rv == null) return null;
				rv.data.put("rbfsepvec",new ArrayList<String>(rbfsepvec));
				rv.data.put("rbforivec",new ArrayList<String>(rbforivec));
				rv.data.put("rbfcohvec",new ArrayList<String>(rbfcohvec));
				rv.data.put("rbfwallvec",new ArrayList<String>(rbfwallvec));
				// if(use_pvel){
				// 	rv.data.put("pvel",new ArrayList<String>(pvel));
				// }
				if(use_velstats){
					rv.data.put("xvmean",new ArrayList<String>(xvmean));
					rv.data.put("xvstd", new ArrayList<String>(xvstd));
					rv.data.put("xvmax", new ArrayList<String>(xvmax));
				}
				rv.data.put("dvel",new ArrayList<String>(dvel));
				rv.data.put("dbool",new ArrayList<String>(dbool));
				return rv;
			}
			public void step(SimState simstate){
				super.step(simstate);
				if(rbfsepvec == null) return;
				if(simstate instanceof Simulation){
					Simulation sim = (Simulation)simstate;
					for(int i=0;i<sim.bodies.size();i++){
						if(sim.bodies.get(i).doNotLog){
							continue;
						}
						if(sim.bodies.get(i) instanceof NotemigonusCrysoleucas){
							NotemigonusCrysoleucas fish = (NotemigonusCrysoleucas)sim.bodies.get(i);
							MutableDouble2D sepSensorVec = new MutableDouble2D();
							MutableDouble2D oriSensorVec = new MutableDouble2D();
							MutableDouble2D cohSensorVec = new MutableDouble2D();
							MutableDouble2D wallSensorVec = new MutableDouble2D();
							fish.getAverageRBFSameTypeVec(sepSensorVec, sep_sigma);
							fish.getAverageRBFOrientationSameTypeVec(oriSensorVec, ori_sigma);
							fish.getAverageRBFSameTypeVec(cohSensorVec, coh_sigma);
							fish.getNearestObstacleVec(wallSensorVec);
							wallSensorVec.multiplyIn(Math.exp(-wallSensorVec.lengthSq()/(2.0*Math.pow(obs_sigma,2))));
							// double[] prevVel = {0.0, 0.0, 0.0};
							// if(use_pvel){
							// 	fish.getSelfVelXYT(prevVel);
							// }
							NeighborhoodStatistics.VelocityStatistics velstats = new NeighborhoodStatistics.VelocityStatistics();
							if(use_velstats){
								fish.getVelocityStatistics(velstats);
							}
							double[] foo = {0.0, 0.0, 0.0};
							boolean bar = false;
							System.arraycopy(fish.desiredVelXYT,0,foo,0,3);
							bar = true;
							rbfsepvec.add(sepSensorVec.x+" "+sepSensorVec.y);
							rbforivec.add(oriSensorVec.x+" "+oriSensorVec.y);
							rbfcohvec.add(cohSensorVec.x+" "+cohSensorVec.y);
							rbfwallvec.add(wallSensorVec.x+" "+wallSensorVec.y);
							// if(use_pvel){
							// 	pvel.add(prevVel[0]+" "+prevVel[1]+" "+prevVel[2]);
							// }
							if(use_velstats){
								xvmean.add(String.format("%f",velstats.xvmean));
								xvstd.add(String.format("%f",velstats.xvstd));
								xvmax.add(String.format("%f",velstats.xvmax));
							}
							dvel.add(foo[0]+" "+foo[1]+" "+foo[2]);
							dbool.add(""+bar);
						}
					}
				}
			}
		};
	}
}