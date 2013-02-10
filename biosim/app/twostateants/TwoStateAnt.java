package biosim.app.twostateants;

import biosim.core.agent.Agent;
import biosim.core.body.Body;
import biosim.core.body.AphaenogasterCockerelli;
import biosim.core.body.DrosophilaMelanogaster;
import biosim.core.body.AbstractAnt;
import biosim.core.gui.GUISimulation;
import biosim.core.sim.Simulation;
import biosim.core.sim.Environment;
//import biosim.core.sim.Obstacle;
import biosim.core.sim.RectObstacle;

import biosim.app.tutorial.AvoidAntLogger;

import sim.util.MutableDouble2D;

import java.util.ArrayList;
public class TwoStateAnt implements Agent {
	public AbstractAnt antBody;
	public double timeNearAnt;
	public double timeAvoiding;
	double lastTime;
	public int state;
	public static final int RETURN=1;
	public static final int FORAGE=2;
	public static final double VISIT_TIME=5.0; //seconds
	public static final double VISIT_RANGE=AphaenogasterCockerelli.SIZE*2; //meters
	public static final double AVOID_TIME=10.0;
	public ArrayList<Double> collectTimes, antDists;
	double prevTime = -1.0;
	public TwoStateAnt(AbstractAnt b){
		antBody = b;
		collectTimes = new ArrayList<Double>();
		antDists = new ArrayList<Double>();
		init();
	}
	
	public void init(){
		timeAvoiding = timeNearAnt = 0.0;
		state = FORAGE;
		lastTime = 0.0;
		prevTime = -1.0;
	}
	
	public double[] act(double time){
		//double[] rv = new double[3];
		double[] rv = {0.0,0.0,0.0};
		//our default is to move forward in a straight line
		//rv[0] = 0.024; 	//24mm per second straight ahead
		//rv[1] = 0.0;	//Ants *can* move laterally, but ours won't for now
		//rv[2] = 0.0;	//no rotational velocity by default
		//get a vector towards the nearest thing so we can avoid/approach it
		MutableDouble2D ant = new MutableDouble2D();
		boolean sawAnt = antBody.getNearestSameAgentVec(ant);
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = antBody.getNearestObstacleVec(wall);
		MutableDouble2D nest = new MutableDouble2D();
		boolean sawNest = antBody.getPoiDir(nest,"nest");
		MutableDouble2D food = new MutableDouble2D();
		boolean sawFood = antBody.getNearestPreyVec(food);
		MutableDouble2D desiredVec = new MutableDouble2D();
		if(sawWall){
			desiredVec.addIn(wall.normalize().negate().multiplyIn(1.0/(Math.pow(wall.length(),2))));
		}
		if(sawAnt){
			antDists.add(ant.length());
			desiredVec.addIn(ant.normalize().negate().multiplyIn(1.0/Math.pow(ant.length(),2)));
		}
		if(!sawFood){
			food.x = (antBody.getRandom().nextDouble()-0.5)*2;//1.0;
			food.y = (antBody.getRandom().nextDouble()-0.5)*2;//0.0;
		}
		if(state  == FORAGE){
			desiredVec.addIn(food);
			rv[0] = Math.min(0.024,desiredVec.length());
			if(desiredVec.lengthSq() > 0.0){
				rv[2] = Math.acos(desiredVec.normalize().dot(new MutableDouble2D(1,0)))*Math.signum(desiredVec.angle());
			} else {
				rv[2] = 0.0;
			}
			antBody.tryToGrab();
			//rv[2] = Math.toRadians(40.0)*Math.signum(desiredVec.angle())*((Math.PI/2)-Math.abs(desiredVec.angle()))/(Math.PI/2);
			if(antBody.getGripped()){
				prevTime = time;
				state = RETURN;
			}
		}
		else if(state == RETURN){
			desiredVec.addIn(nest);
			rv[0] = Math.min(0.024,desiredVec.length());
			if(desiredVec.lengthSq() > 0.0){
				rv[2] = Math.acos(desiredVec.normalize().dot(new MutableDouble2D(1,0)))*Math.signum(desiredVec.angle());
			} else {
				rv[2] = 0.0;
			}
			//rv[2] = Math.toRadians(40.0)*Math.signum(desiredVec.angle())*((Math.PI/2)-Math.abs(desiredVec.angle()))/(Math.PI/2);			
			if(antBody.nearPOI("nest")){
				if(prevTime >= 0){
					collectTimes.add(time-prevTime);
				}
				prevTime = -1.0;
				antBody.tryToDrop();
				state = FORAGE;
			}
		}
		/*
		if(state == AVOID){
			timeAvoiding += time - lastTime;
			MutableDouble2D avoidPoint = null;
			if(sawWall){
				avoidPoint = wall;
			} else if(sawAnt){
				avoidPoint = ant;
			} 
			if(avoidPoint != null){
				if(avoidPoint.y > 0) rv[2] = Math.toRadians(-40.0);
				else rv[2] = Math.toRadians(40.0);
			}
			if(timeAvoiding > AVOID_TIME){
				timeNearAnt = 0.0;
				state = APPROACH;
			}
		} else if(state == APPROACH){
			if(sawAnt){
				if(ant.length() <= VISIT_RANGE){
					timeNearAnt += time-lastTime;
				} else {
					timeNearAnt = 0.0;
				}
				//head to the ant
				rv[0] = 0.024*(ant.length()-antBody.getSize())/antBody.getNearestSameAgentVecSensorRange();
				rv[2] = Math.toRadians(40.0)*Math.signum(ant.angle())*((Math.PI/2)-Math.abs(ant.angle()))/(Math.PI/2);
				//switch to avoid if we've visited long enough
				if(timeNearAnt > VISIT_TIME){
					timeAvoiding = 0.0;
					state = AVOID;
				}
			} else if(sawWall){
				timeAvoiding = 0.0;
				rv[2] = Math.toRadians(-40.0)*Math.signum(wall.angle());
			}
		}
		*/
		lastTime = time;
		return rv;
	}
	
	public static final double WIDTH=0.2;
	public static final double HEIGHT=0.2;
	public static void main(String[] args){
		//set up the environment
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
		TwoStateAnt[] agents = new TwoStateAnt[numAnts];
		for(int i=0;i<agents.length;i++){
			agents[i] = new TwoStateAnt(bodies[i]);
			bodies[i].setAgent(agents[i]);
		}
		Agent[] flyAgents = new Agent[numFlies];
		for(int i=0;i<flyAgents.length;i++){
			flyAgents[i] = new LazyFly();
			flyBodies[i].setAgent(flyAgents[i]);
		}
		env.runSimulation(args);
		//Simulation sim = env.newSimulation();
		//sim.addLogger(new TwoStateLogger());
		//GUISimulation gui = new GUISimulation(sim);
		//gui.setPortrayalClass(DrosophilaMelanogaster.class, FoodPortrayal.class);
		//gui.createController();
		int numTimes = 0;
		double cTimeAvg = 0.0;
		double cTimeStdDev = 0.0;
		for(int i=0;i<agents.length;i++){
			for(int j=0;j<agents[i].collectTimes.size();j++){
				cTimeAvg += agents[i].collectTimes.get(j);
				numTimes++;
			}
		}
		cTimeAvg = cTimeAvg/numTimes;
		for(int i=0;i<agents.length;i++){
			for(int j=0;j<agents[i].collectTimes.size();j++){
				cTimeStdDev += Math.pow(agents[i].collectTimes.get(j)-cTimeAvg,2);
			}
		}
		cTimeStdDev = Math.sqrt(cTimeStdDev/numTimes);
		int numDists = 0;
		double adAvg = 0.0;
		double adStdDev = 0.0;
		for(int i=0;i<agents.length;i++){
			for(int j=0;j<agents[i].antDists.size();j++){
				adAvg += agents[i].antDists.get(j);
				numDists++;
			}
		}
		adAvg = adAvg/numDists;
		for(int i=0;i<agents.length;i++){
			for(int j=0;j<agents[i].antDists.size();j++){
				adStdDev += Math.pow(agents[i].antDists.get(j)-adAvg,2);
			}
		}
		adStdDev = Math.sqrt(adStdDev/numDists);
		System.out.println("Collect Time avg: "+cTimeAvg+" "+cTimeStdDev+" "+numTimes);
		System.out.println("Ant dist avg: "+adAvg+" "+adStdDev+" "+numDists);		
	}

}
