package biosim.app.domworld;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.text.DecimalFormat;

import ec.util.MersenneTwisterFast;

import biosim.core.sim.Simulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Obstacle;
import biosim.core.sim.RectObstacle;
import biosim.core.body.Body;
import biosim.core.body.RhesusMacaque;
import biosim.core.agent.Agent;
import biosim.core.gui.GUISimulation;
import biosim.core.util.BTFLogger;

public class SmallDomWorld {
	public static double HEIGHT=3.0;
	public static double WIDTH=3.0;
	public static double RESOLUTION=1.0/30.0;
	public static boolean LOGGING=true;
	public static boolean GUI=true;
	public static boolean TOROIDAL=true;

	public static double[][] randomTieStrength(MersenneTwisterFast rnd, int n){
		double[][] rv = new double[n][n];
		for(int i=0;i<n;i++){
			for(int j=i;j<n;j++){
				rv[i][j] = rnd.nextDouble();
				rv[j][i] = rv[i][j];
			}
		}
		return rv;
	}

	public static double[][] tieStrengthFromFile(String fname) throws IOException{
		double[][] rv = null;
		BufferedReader reader = new BufferedReader(new FileReader(fname));
		reader.mark(1048576); //about a meg should be enough for the first line
		String[] line = reader.readLine().split(",");
		int n= line.length;
		rv = new double[n][n];
		reader.reset();
		for(int i=0;i<n;i++){
			line = reader.readLine().split(",");
			if(line.length != n){
				throw new IOException("Malformed tie strength matrix, n="+n+", row["+i+"]="+line.length);
			}
			for(int j=0;j<n;j++){
				rv[i][j] = Double.parseDouble(line[j]);
			}
		}
		if(reader.ready()){
			line = reader.readLine().split(",");
			if(line.length > 1){
				throw new IOException("Malformed tie strength matrix. More than n="+n+" lines. Line length: "+line.length);
			}
		}
		return rv;
	}

	public static void missingPropsError(String propName){
		throw new RuntimeException("missing property: "+propName);
	}

	public static void invalidValue(String propName, String value){
		throw new RuntimeException("invalid property value: "+propName+" = "+value);
	}

	public static Simulation defaultConfig(){
		int numMonkeys = 5;
		DomWorldStateMachine.PERSONAL_DIST=0.5; //within arms reach
		DomWorldStateMachine.NEAR_DIST=3.0; //closer than corner-to-corner
		DomWorldStateMachine.MIN_OTHERS=1; //3 is more than half the monkeys
		long seed = System.currentTimeMillis();
		Environment env = new Environment(WIDTH,HEIGHT,RESOLUTION);
		Simulation sim = env.newSimulation(seed);
		System.out.println("Seed: "+seed);
		env.setToroidal(TOROIDAL);
		env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
		env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
		env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
		env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south

		RhesusMacaque[] bodies = new RhesusMacaque[numMonkeys];
		for(int i=0;i<bodies.length;i++){
			bodies[i] = new RhesusMacaque();
			env.addBody(bodies[i]);
		}
		Agent[] agents = new Agent[bodies.length];
		double[][] tieStrengths = randomTieStrength(sim.random,numMonkeys);
		DecimalFormat df = new DecimalFormat("0.0000");
		printMat(tieStrengths,df);
		for(int i=0;i<agents.length;i++){
			double rndDom = sim.random.nextDouble();
			agents[i] = new DomWorldStateMachine(bodies[i],rndDom);
			//agents[i] = new DomWorldMonkey(bodies[i],sim.random.nextDouble());
			bodies[i].setAgent(agents[i]);
			System.out.println("Monkey "+i+" dominance: "+df.format(rndDom));
		}
		for(int i=0;i<agents.length;i++){
			HashMap<DomWorldStateMachine,Double> tsprefs = new HashMap<DomWorldStateMachine,Double>();
			for(int j=0;j<numMonkeys;j++){
				tsprefs.put((DomWorldStateMachine)agents[j],tieStrengths[i][j]);
			}
			((DomWorldStateMachine)agents[i]).setTieStrengths(tsprefs);
		}
		return sim;
	}

	public static Simulation config(Properties props){
		int numMonkeys;
		double[][] tieStrengths = null;
		Environment env = null;
		Simulation sim = null;
		DecimalFormat df = new DecimalFormat("0.0000");
		String tmp = null;
		long seed = System.currentTimeMillis();
		//EXPERIMENTAL PARAMS
		tmp = props.getProperty("seed");
		if(tmp != null){
			seed = Long.parseLong(tmp);
		}
		System.out.println("Seed: "+seed);
		tmp = props.getProperty("HEIGHT");
		if(tmp != null){
			HEIGHT= Double.parseDouble(tmp);
			if(HEIGHT < 0){
				invalidValue("HEIGHT",tmp);
			}
		}
		tmp = props.getProperty("WIDTH");
		if(tmp != null){
			WIDTH = Double.parseDouble(tmp);
			if(WIDTH < 0){
				invalidValue("WIDTH",tmp);
			}
		}
		tmp = props.getProperty("RESOLUTION");
		if(tmp != null){
			RESOLUTION = Double.parseDouble(tmp);
			if(RESOLUTION < 0.0){
				invalidValue("RESOLUTION",tmp);
			}
		}
		env = new Environment(WIDTH,HEIGHT,RESOLUTION);
		tmp = props.getProperty("numMonkeys");
		if(tmp==null){
			missingPropsError("numMonkeys");
		}
		numMonkeys = Integer.parseInt(tmp);
		if(numMonkeys <= 0){
			invalidValue("numMonkeys",tmp);
		}
		tieStrengths = new double[numMonkeys][numMonkeys];
		tmp = props.getProperty("tieStrength");
		if(tmp == null){
			missingPropsError("tieStrength");
		}
		String[] strTieStrengths = tmp.split(",");
		if(strTieStrengths.length != numMonkeys*numMonkeys){
			invalidValue("tieStrength",tmp);
		}
		for(int i=0;i<numMonkeys;i++){
			for(int j=0;j<numMonkeys;j++){
				tieStrengths[i][j] = Double.parseDouble(strTieStrengths[(i*numMonkeys)+j]);
			}
		}
		printMat(tieStrengths,df);
		tmp = props.getProperty("dominance");
		if(tmp == null){
			missingPropsError("dominance");
		}
		String[] strDominance = tmp.split(",");
		if(strDominance.length != numMonkeys){
			invalidValue("dominance",tmp);
		}
		tmp = props.getProperty("LOGGING");
		if(tmp != null){
			LOGGING = Boolean.parseBoolean(tmp);
		}
		tmp = props.getProperty("GUI");
		if(tmp != null){
			GUI = Boolean.parseBoolean(tmp);
		}
		tmp = props.getProperty("TOROIDAL");
		if(tmp != null){
			TOROIDAL = Boolean.parseBoolean(tmp);
		}
		tmp = props.getProperty("obstacles");
		if(tmp != null){
			String[] obs = tmp.split(",");
			if(obs.length % 4 != 0){
				invalidValue("obstacles",tmp);
			}
			for(int i=0;i<obs.length;i+=4){
				env.addObstacle(	new RectObstacle(Double.parseDouble(obs[i]),Double.parseDouble(obs[i+1])), 
									Double.parseDouble(obs[i+2]), Double.parseDouble(obs[i+3]));
			}
			//do some stuff
		}
		//BEHAVIOR PARAMS
		tmp = props.getProperty("PERSONAL_DIST");
		if(tmp != null){
			DomWorldStateMachine.PERSONAL_DIST = Double.parseDouble(tmp);
			if(DomWorldStateMachine.PERSONAL_DIST < RhesusMacaque.SIZE){
				invalidValue("PERSONAL_DIST",tmp);
			}
		}
		tmp = props.getProperty("NEAR_DIST");
		if(tmp != null){
			DomWorldStateMachine.NEAR_DIST = Double.parseDouble(tmp);
			if(DomWorldStateMachine.NEAR_DIST < RhesusMacaque.SIZE){
				invalidValue("NEAR_DIST",tmp);
			}
		}
		DomWorldStateMachine.FAR_DIST=DomWorldStateMachine.NEAR_DIST*Math.sqrt(numMonkeys);
		tmp = props.getProperty("MIN_OTHERS");
		if(tmp != null){
			DomWorldStateMachine.MIN_OTHERS = Integer.parseInt(tmp);
			if(DomWorldStateMachine.MIN_OTHERS < 0 || DomWorldStateMachine.MIN_OTHERS > numMonkeys){
				invalidValue("MIN_OTHERS",tmp);
			}
		}
		tmp = props.getProperty("AVERAGE_EVENT_TIME");
		if(tmp != null){
			DomWorldStateMachine.AVERAGE_EVENT_TIME = Double.parseDouble(tmp);
			if(DomWorldStateMachine.AVERAGE_EVENT_TIME < 0){
				invalidValue("AVERAGE_EVENT_TIME",tmp);
			}
		}
		tmp = props.getProperty("FRONTAL_FOV");
		if(tmp != null){
			DomWorldStateMachine.FRONTAL_FOV = Double.parseDouble(tmp);
			if(DomWorldStateMachine.FRONTAL_FOV <= 0.0 || DomWorldStateMachine.FRONTAL_FOV >= 2.0*Math.PI){
				invalidValue("FRONTAL_FOV",tmp);
			}
		}
		tmp = props.getProperty("RANDOM_WALK_SPEED");
		if(tmp != null){
			DomWorldStateMachine.RANDOM_WALK_SPEED = Double.parseDouble(tmp);
			if(DomWorldStateMachine.RANDOM_WALK_SPEED < 0.0){
				invalidValue("RANDOM_WALK_SPEED",tmp);
			}
		}
		tmp = props.getProperty("RANDOM_WALK_DIST");
		if(tmp != null){
			DomWorldStateMachine.RANDOM_WALK_DIST = Double.parseDouble(tmp);
			if(DomWorldStateMachine.RANDOM_WALK_DIST < 0.0){
				invalidValue("RANDOM_WALK_DIST",tmp);
			}
		}		tmp = props.getProperty("FLEE_SPEED");
		if(tmp != null){
			DomWorldStateMachine.FLEE_SPEED = Double.parseDouble(tmp);
			if(DomWorldStateMachine.FLEE_SPEED < 0.0){
				invalidValue("FLEE_SPEED",tmp);
			}
		}
		tmp = props.getProperty("FLEE_DIST");
		if(tmp != null){
			DomWorldStateMachine.FLEE_DIST = Double.parseDouble(tmp);
			if(DomWorldStateMachine.FLEE_DIST < 0.0){
				invalidValue("FLEE_DIST",tmp);
			}
		}
		tmp = props.getProperty("CHASE_SPEED");
		if(tmp != null){
			DomWorldStateMachine.CHASE_SPEED = Double.parseDouble(tmp);
			if(DomWorldStateMachine.CHASE_SPEED < 0.0){
				invalidValue("CHASE_SPEED",tmp);
			}
		}
		tmp = props.getProperty("CHASE_DIST");
		if(tmp != null){
			DomWorldStateMachine.CHASE_DIST = Double.parseDouble(tmp);
			if(DomWorldStateMachine.CHASE_DIST < 0.0){
				invalidValue("CHASE_DIST",tmp);
			}
		}
		tmp = props.getProperty("GROUP_SPEED");
		if(tmp != null){
			DomWorldStateMachine.GROUP_SPEED = Double.parseDouble(tmp);
			if(DomWorldStateMachine.GROUP_SPEED < 0.0){
				invalidValue("GROUP_SPEED",tmp);
			}
		}
		tmp = props.getProperty("GROUP_DIST");
		if(tmp != null){
			DomWorldStateMachine.GROUP_DIST = Double.parseDouble(tmp);
			if(DomWorldStateMachine.GROUP_DIST < 0.0){
				invalidValue("GROUP_DIST",tmp);
			}
		}
		tmp = props.getProperty("ETA");
		if(tmp != null){
			DomWorldStateMachine.ETA = Double.parseDouble(tmp);
		}
		//INITIALIZE AGENTS & BODIES
		RhesusMacaque[] bodies = new RhesusMacaque[numMonkeys];
		for(int i=0;i<bodies.length;i++){
			bodies[i] = new RhesusMacaque();
			env.addBody(bodies[i]);
		}
		Agent[] agents = new Agent[bodies.length];
		for(int i=0;i<numMonkeys;i++){
			double dom = Double.parseDouble(strDominance[i]);
			agents[i] = new DomWorldStateMachine(bodies[i],dom);
			bodies[i].setAgent(agents[i]);
			System.out.println("Monkey "+i+" dominance: "+df.format(dom));
		}
		for(int i=0;i<numMonkeys;i++){
			HashMap<DomWorldStateMachine,Double> tsprefs = new HashMap<DomWorldStateMachine,Double>();
			for(int j=0;j<numMonkeys;j++){
				tsprefs.put((DomWorldStateMachine)agents[j],tieStrengths[i][j]);
			}
			((DomWorldStateMachine)agents[i]).setTieStrengths(tsprefs);
		}
		return env.newSimulation(seed);
	}

	public static void printMat(double[][] ts, DecimalFormat df){
		System.out.println("Tie strengths:");
		for(int i=0;i<ts.length;i++){
			System.out.print("Row "+i+": ");
			for(int j=0;j<ts[i].length;j++){
				System.out.print(df.format(ts[i][j])+" ");
			}
			System.out.println();
		}
	}

	public static Properties getParams(String fname) throws IOException{
		Properties props = new Properties();
		props.load(new FileReader(fname));
		return props;
	}

	public static void main(String[] args){
		Simulation sim = null;
		if(args.length>0){
			Properties props = new Properties();
			try{
				props.load(new FileReader(args[0]));
			} catch(Exception e){
				throw new RuntimeException(e);
			}
			sim = config(props);
		} else {
			sim = defaultConfig();
		}
		/*
		RhesusMacaque[] bodies = new RhesusMacaque[numMonkeys];
		for(int i=0;i<bodies.length;i++){
			bodies[i] = new RhesusMacaque();
			env.addBody(bodies[i]);
		}
		Simulation sim = env.newSimulation();
		Agent[] agents = new Agent[bodies.length];
		double[][] tieStrengths = randomTieStrength(sim.random,numMonkeys);
		DecimalFormat df = new DecimalFormat("0.0000");
		printMat(tieStrengths,df);
		*/
		/*
		DecimalFormat df = new DecimalFormat("0.0000");
		System.out.println("Tie strengths:");
		for(int i=0;i<tieStrengths.length;i++){
			System.out.print("Row "+i+": ");
			for(int j=0;j<tieStrengths[i].length;j++){
				System.out.print(df.format(tieStrengths[i][j])+" ");
			}
			System.out.println();
		}
		*/
		/*
		for(int i=0;i<agents.length;i++){
			double rndDom = sim.random.nextDouble();
			agents[i] = new DomWorldStateMachine(bodies[i],rndDom);
			//agents[i] = new DomWorldMonkey(bodies[i],sim.random.nextDouble());
			bodies[i].setAgent(agents[i]);
			System.out.println("Monkey "+i+" dominance: "+df.format(rndDom));
		}
		for(int i=0;i<agents.length;i++){
			HashMap<DomWorldStateMachine,Double> tsprefs = new HashMap<DomWorldStateMachine,Double>();
			for(int j=0;j<numMonkeys;j++){
				tsprefs.put((DomWorldStateMachine)agents[j],tieStrengths[i][j]);
			}
			((DomWorldStateMachine)agents[i]).setTieStrengths(tsprefs);
		}
		*/
		if(LOGGING){
			sim.addLogger(new BTFLogger());
		}
		if(GUI){
			GUISimulation gui = new GUISimulation(sim);
			gui.createController();
		}
	}
}