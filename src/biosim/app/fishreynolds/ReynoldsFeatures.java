package biosim.app.fishreynolds;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;

import sim.util.MutableDouble2D;
import sim.engine.SimState;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractFish;
import biosim.core.body.Body;
import biosim.core.body.NotemigonusCrysoleucas;
import biosim.core.body.ReplayFish;
import biosim.core.learning.LearnerAgent;
import biosim.core.learning.ProblemSpec;
import biosim.core.sim.Environment;
import biosim.core.sim.InitiallyPlacedEnvironment;
import biosim.core.sim.RectObstacle;
import biosim.core.sim.Simulation;
import biosim.core.util.BufferedBTFData;
import biosim.core.util.BTFData;
import biosim.core.util.BTFDataLogger;

public class ReynoldsFeatures implements ProblemSpec{

	//sepX,sepY,oriX,oriY,cohX,cohY,obsX,obsY,pvelX,pvelY,pvelT
	public static final int NUM_FEATURES=11;
	//X, Y, T
	public static final int NUM_OUTPUTS=3;

	public static long timeout;

	public double sep_sigma, ori_sigma, coh_sigma, obs_sigma;

	public static Properties defaults(){
		Properties defaultProps = new Properties();
		defaultProps.setProperty("SEP_SIGMA","0.1");
		defaultProps.setProperty("ORI_SIGMA","0.2");
		defaultProps.setProperty("COH_SIGMA","1.0");
		defaultProps.setProperty("OBS_SIGMA","0.05");
		defaultProps.setProperty("TIMEOUT","5000"); //timeout is in milliseconds
		return defaultProps;
	}

	public ReynoldsFeatures(){
		loadFeatureSigma(defaults());
	}

	public void loadFeatureSigma(BufferedReader featuresSource) throws IOException{
		Properties props = new Properties(defaults());
		props.load(featuresSource);
		loadFeatureSigma(props);
	}

	public void loadFeatureSigma(Properties props){
		sep_sigma = Double.parseDouble(props.getProperty("SEP_SIGMA"));
		ori_sigma = Double.parseDouble(props.getProperty("ORI_SIGMA"));
		coh_sigma = Double.parseDouble(props.getProperty("COH_SIGMA"));
		obs_sigma = Double.parseDouble(props.getProperty("OBS_SIGMA"));
		timeout = Long.parseLong(props.getProperty("TIMEOUT"));
	}

	public int getNumFeatures(){ return NUM_FEATURES;}
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
			double[] pvel = new double[NUM_OUTPUTS];
			fishBody.getSelfVelXYT(pvel);
			double[] sensors = new double[NUM_FEATURES];
			sensors[0] = sep.x;
			sensors[1] = sep.y;
			sensors[2] = ori.x;
			sensors[3] = ori.y;
			sensors[4] = coh.x;
			sensors[5] = coh.y;
			sensors[6] = wall.x * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(obs_sigma,2)));
			sensors[7] = wall.y * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(obs_sigma,2)));
			sensors[8] = pvel[0];
			sensors[9] = pvel[1];
			sensors[10] = pvel[2];
			// let linreg do it's own thing re: bias
			// sensors[11] = 1.0;
			return sensors;
		} else {
			throw new RuntimeException(b+" is not an AbstractFish");
		}
	}

	private static final void copyInto(double[][] from,double[][] to, int offset){
		for(int i=0;i<from.length;i++){
			System.arraycopy(from[i],0,to[i],offset,from[0].length);
		}
	}

	public Dataset btf2array(BTFData btf){
		Dataset rv = new Dataset();
		try{
			int numRows = btf.loadColumn("id").length;
			rv.features = new double[numRows][NUM_FEATURES];
			rv.outputs = new double[numRows][NUM_OUTPUTS];
			//sepX,sepY,oriX,oriY,cohX,cohY,obsX,obsY,pvelX,pvelY,pvelT
			double[][] column = btf.columnAsDoubles("rbfsepvec");
			copyInto(column,rv.features,0);
			column = btf.columnAsDoubles("rbforivec");
			copyInto(column,rv.features,2);
			column = btf.columnAsDoubles("rbfcohvec");
			copyInto(column,rv.features,4);
			column = btf.columnAsDoubles("rbfwallvec");
			copyInto(column,rv.features,6);
			column = btf.columnAsDoubles("pvel");
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
		env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
		env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
		env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
		env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
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

	public BTFDataLogger getLogger(){
		return new BTFDataLogger(){
			public LinkedList<String> rbfsepvec, rbforivec, rbfcohvec, rbfwallvec, pvel, dvel, dbool;
			public void init(){
				super.init();
				rbfsepvec = new LinkedList<String>();
				rbforivec = new LinkedList<String>();
				rbfcohvec = new LinkedList<String>();
				rbfwallvec = new LinkedList<String>();
				pvel = new LinkedList<String>();
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
				rv.data.put("pvel",new ArrayList<String>(pvel));
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
							double[] prevVel = {0.0, 0.0, 0.0};
							fish.getSelfVelXYT(prevVel);
							double[] foo = {0.0, 0.0, 0.0};
							boolean bar = false;
							System.arraycopy(fish.desiredVelXYT,0,foo,0,3);
							bar = true;
							rbfsepvec.add(sepSensorVec.x+" "+sepSensorVec.y);
							rbforivec.add(oriSensorVec.x+" "+oriSensorVec.y);
							rbfcohvec.add(cohSensorVec.x+" "+cohSensorVec.y);
							rbfwallvec.add(wallSensorVec.x+" "+wallSensorVec.y);
							pvel.add(prevVel[0]+" "+prevVel[1]+" "+prevVel[2]);
							dvel.add(foo[0]+" "+foo[1]+" "+foo[2]);
							dbool.add(""+bar);
						}
					}
				}
			}
		};
	}
}