//DomWorldStateMachine.java

package biosim.app.domworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import sim.util.MutableDouble2D;
import sim.util.Double2D;
import ec.util.MersenneTwisterFast;

import biosim.core.agent.Agent;
import biosim.core.agent.StateMachine;
import biosim.core.body.AbstractMonkey;

public class DomWorldStateMachine extends StateMachine {
	//states
	public static int ROAM=0;
	//public static int RANDOM_WALK=0;
	public static int GROUP=1;
	public static int FLEE=2;
	public static int CHASE=3;
	public static int LOITER=4;
	public static int ENCOUNTER=5;
	public static int GO_TO_WAYPOINT=6;
	public static int NUM_STATES=7;
	//monkey behavior paramters
	public static double PERSONAL_DIST=4.0;
	public static double NEAR_DIST=20.0;
	public static double FAR_DIST=NEAR_DIST*Math.sqrt(30);//30 == number of agents. It's arbitrary.
	public static int MIN_OTHERS=3;
	public static double AVERAGE_EVENT_TIME=10.0;
	public static double FRONTAL_FOV= 120.0 * (2.0*Math.PI/360.0);
	public static double RANDOM_WALK_SPEED=0.5;
	public static double RANDOM_WALK_DIST=1.0;
	public static double ROAM_SPEED=0.5;
	public static double ROAM_DIST=2.0;
	public static double FLEE_SPEED=2.0;
	public static double FLEE_DIST=2.0;
	public static double CHASE_SPEED=1.0;
	public static double CHASE_DIST=1.0;
	public static double GROUP_SPEED=0.75;
	public static double GROUP_DIST=1.0; //The distance to travel towards another group member.
	public static double ETA = 6.0;	//derived so that the probability of A wining in a fight with B
									//where D_a = 1/30, D_b = 1.0, is roughly 0.003
	public static double HIERARCHY_STABILITY=0.9; 	//probability that a monkey entering the personal distance
													//of another monkey invokes a conflict
	public static double ROAM_OBST_DIST=0.5;
	//static config functions
	public static void invalidValue(String propName, String value){
		ETA=-1.0;
		throw new RuntimeException("invalid property value: "+propName+" = "+value);
	}

	public static void configure(Properties props){
		String tmp;
		//BEHAVIOR PARAMS
		tmp = props.getProperty("PERSONAL_DIST");
		if(tmp != null){
			PERSONAL_DIST = Double.parseDouble(tmp);
			if(PERSONAL_DIST < 0){
				invalidValue("PERSONAL_DIST",tmp);
			}
		}
		tmp = props.getProperty("NEAR_DIST");
		if(tmp != null){
			NEAR_DIST = Double.parseDouble(tmp);
			if(NEAR_DIST < PERSONAL_DIST){
				invalidValue("NEAR_DIST",tmp);
			}
		}
		tmp = props.getProperty("FAR_DIST");
		if(tmp != null){
			FAR_DIST = Double.parseDouble(tmp);
			if(FAR_DIST < NEAR_DIST){
				invalidValue("FAR_DIST",tmp);
			}
		}
		tmp = props.getProperty("MIN_OTHERS");
		if(tmp != null){
			MIN_OTHERS = Integer.parseInt(tmp);
			if(MIN_OTHERS < 0){
				invalidValue("MIN_OTHERS",tmp);
			}
		}
		tmp = props.getProperty("AVERAGE_EVENT_TIME");
		if(tmp != null){
			AVERAGE_EVENT_TIME = Double.parseDouble(tmp);
			if(AVERAGE_EVENT_TIME < 0){
				invalidValue("AVERAGE_EVENT_TIME",tmp);
			}
		}
		tmp = props.getProperty("FRONTAL_FOV");
		if(tmp != null){
			FRONTAL_FOV = Double.parseDouble(tmp);
			if(FRONTAL_FOV <= 0.0 || FRONTAL_FOV >= 2.0*Math.PI){
				invalidValue("FRONTAL_FOV",tmp);
			}
		}
		tmp = props.getProperty("RANDOM_WALK_SPEED");
		if(tmp != null){
			RANDOM_WALK_SPEED = Double.parseDouble(tmp);
			if(RANDOM_WALK_SPEED < 0.0){
				invalidValue("RANDOM_WALK_SPEED",tmp);
			}
		}
		tmp = props.getProperty("RANDOM_WALK_DIST");
		if(tmp != null){
			RANDOM_WALK_DIST = Double.parseDouble(tmp);
			if(RANDOM_WALK_DIST < 0.0){
				invalidValue("RANDOM_WALK_DIST",tmp);
			}
		}		tmp = props.getProperty("FLEE_SPEED");
		if(tmp != null){
			FLEE_SPEED = Double.parseDouble(tmp);
			if(FLEE_SPEED < 0.0){
				invalidValue("FLEE_SPEED",tmp);
			}
		}
		tmp = props.getProperty("FLEE_DIST");
		if(tmp != null){
			FLEE_DIST = Double.parseDouble(tmp);
			if(FLEE_DIST < 0.0){
				invalidValue("FLEE_DIST",tmp);
			}
		}
		tmp = props.getProperty("CHASE_SPEED");
		if(tmp != null){
			CHASE_SPEED = Double.parseDouble(tmp);
			if(CHASE_SPEED < 0.0){
				invalidValue("CHASE_SPEED",tmp);
			}
		}
		tmp = props.getProperty("CHASE_DIST");
		if(tmp != null){
			CHASE_DIST = Double.parseDouble(tmp);
			if(CHASE_DIST < 0.0){
				invalidValue("CHASE_DIST",tmp);
			}
		}
		tmp = props.getProperty("GROUP_SPEED");
		if(tmp != null){
			GROUP_SPEED = Double.parseDouble(tmp);
			if(GROUP_SPEED < 0.0){
				invalidValue("GROUP_SPEED",tmp);
			}
		}
		tmp = props.getProperty("GROUP_DIST");
		if(tmp != null){
			GROUP_DIST = Double.parseDouble(tmp);
			if(GROUP_DIST < 0.0){
				invalidValue("GROUP_DIST",tmp);
			}
		}
		tmp = props.getProperty("ETA");
		if(tmp != null){
			ETA = Double.parseDouble(tmp);
		}
		tmp = props.getProperty("HIERARCHY_STABILITY");
		if(tmp != null){
			HIERARCHY_STABILITY = Double.parseDouble(tmp);
			if(HIERARCHY_STABILITY < 0.0){
				invalidValue("HIERARCHY_STABILITY",tmp);
			}
		}
	}
	//instance data members
	private DomWorldStateMachine target = null, chaseTowards=null, fleeFrom=null;
	private double startLoiteringAt, stopLoiteringAt;
	private double startRandomWalkingAt, stopRandomWalkingAt;
	private double startFleeingAt, stopFleeingAt;
	private double startChasingAt, stopChasingAt;
	private double startGroupingAt, stopGroupingAt;
	private double startRoamingAt, stopRoamingAt;
	private Double2D gtwLocation;
	private Double2D roamDir;
	private double dominanceRank;
	private AbstractMonkey body;
	private HashMap<DomWorldStateMachine,Double> preferences;
	private Agent lastClosest;

	public void setTieStrengths(HashMap<DomWorldStateMachine,Double> tsprefs){
		preferences = tsprefs;
	}

	private class Roam implements State{
		public String toString(){ return "ROAM";}
		public int act(double time){
			//momentum, random component (direction), follow wall if close,
			//avoid monkey as weak obstacles,
			//
			//set out in random directions, when they hit the walls they 
			//patrol the walls, generally avoid eachother weakly
			MutableDouble2D loc = new MutableDouble2D();
			MutableDouble2D dir = new MutableDouble2D();
			ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
			ArrayList<Agent> agents = new ArrayList<Agent>();
			MutableDouble2D nearestObs = new MutableDouble2D();
			boolean successObst = body.getNearestObstacleVec(nearestObs);
			boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
			boolean successAgents = body.getAllVisibleSameType(agents);
			boolean successAbsLoc = body.getAbsolutePosition(loc);
			boolean successCompass = body.getAbsoluteOrientation(dir);
			if(roamDir==null){
				roamDir = new Double2D(new MutableDouble2D(1,0).rotate(body.getRandom().nextDouble()*Math.PI*2.0));
				startRoamingAt = time;
				stopRoamingAt = time + (ROAM_DIST/ROAM_SPEED);
			}
			if(roamLoiterTimeout(time)){
				roamDir = null;
				stopLoiteringAt = -1;
				return LOITER;
			}
			if(tooClose(vecs,agents)){
				roamDir = null;
				return ENCOUNTER;
			}
			if(wonFight()){
				roamDir = null;
				return CHASE;
			}
			if(lostFight()){
				roamDir = null;
				return FLEE;
			}
			MutableDouble2D vecTowards = new MutableDouble2D(roamDir).rotate(-dir.angle());
			if((nearestObs.x+nearestObs.y)!= 0.0 && nearestObs.length()<ROAM_OBST_DIST){
				vecTowards.setTo(nearestObs);
				vecTowards.rotate(Math.PI/2.0);
				if(Math.abs(vecTowards.angle())>Math.PI/2.0){
					vecTowards.negate();
				}
			}
			body.setDesiredVelocity(ROAM_SPEED,0.0,vecTowards.angle());
			return ROAM;
		}
	}

	private class GoToWaypoint implements State{
		public String toString(){ return "GO_TO_WAYPOINT";}
		public int act(double time){
			//Pick a random spot in the arena and head there
			MutableDouble2D loc = new MutableDouble2D();
			MutableDouble2D dir = new MutableDouble2D();
			ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
			ArrayList<Agent> agents = new ArrayList<Agent>();
			MutableDouble2D nearestObs = new MutableDouble2D();
			boolean successObst = body.getNearestObstacleVec(nearestObs);
			boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
			boolean successAgents = body.getAllVisibleSameType(agents);
			boolean successAbsLoc = body.getAbsolutePosition(loc);
			boolean successCompass = body.getAbsoluteOrientation(dir);
			if(gtwLocation==null){
				MutableDouble2D size = new MutableDouble2D();
				body.getEnvironmentSize(size);
				double widthMax = size.x-(body.getSize()*1.5);
				double widthMin = body.getSize()*1.5;
				double heightMax = size.y-(body.getSize()*1.5);
				double heightMin = body.getSize()*1.5;
				double rndX = (body.getRandom().nextDouble()*(widthMax-widthMin))+widthMin;
				double rndY = (body.getRandom().nextDouble()*(heightMax-heightMin))+heightMin;
				gtwLocation = new Double2D(rndX,rndY);
			}
			if(atWaypoint(loc)){
				gtwLocation = null;
				stopLoiteringAt = -1;
				return LOITER;
			}
			if(tooClose(vecs,agents)){
				gtwLocation = null;
				return ENCOUNTER;
			}
			if(wonFight()){
				gtwLocation=null;
				return CHASE;
			}
			if(lostFight()){
				gtwLocation = null;
				return FLEE;
			}
			MutableDouble2D vecTowards = new MutableDouble2D(gtwLocation).subtractIn(loc).rotate(-loc.angle());
			body.setDesiredVelocity(RANDOM_WALK_SPEED,0,vecTowards.angle());
			return GO_TO_WAYPOINT;
		}
	}
	//RANDOM WALK behavior. 
	//Pick a random direction and head there for 
	//RANDOM_WALK_DIST meters
	private class RandomWalk implements State {
		public String toString(){ return "RANDOM_WALK";}
		public int act(double time){
			ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
			ArrayList<Agent> agents = new ArrayList<Agent>();
			MutableDouble2D nearestObs = new MutableDouble2D();
			boolean successObst = body.getNearestObstacleVec(nearestObs);
			boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
			boolean successAgents = body.getAllVisibleSameType(agents);
			double forwardSpeed = RANDOM_WALK_SPEED;
			//double forwardSpeed = (RANDOM_WALK_SPEED)*(stopRandomWalkingAt-time)/(stopRandomWalkingAt-startRandomWalkingAt);
			//get the vector to the nearest obstacle, find the vector along the plane closest to our orientation
			//add some gaussian noise
			double turnSpeed = (body.getRandom().nextDouble()-0.5)*Math.PI;
			if(successObst && (nearestObs.x != 0.0 || nearestObs.y!=0.0)){
				nearestObs.rotate(Math.PI/2.0);
				if(Math.abs(nearestObs.angle()) > (Math.PI/2.0)){
					nearestObs.negate();
				}
				nearestObs.rotate(body.getRandom().nextGaussian()* (10.0*2.0*Math.PI/360.0));
			}
			body.setDesiredVelocity(forwardSpeed,0.0,turnSpeed);
			if(lostFight())
				return FLEE;
			if(wonFight())
				return CHASE;
			if(tooClose(vecs,agents))
				return ENCOUNTER;
			if(randomWalkLoiterTimeout(time)){
				stopLoiteringAt = -1;
				//stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				//startLoiteringAt = time;
				return LOITER;
			}
			//return RANDOM_WALK;
			return -1;
		}
	}

	//GROUP behavior.
	//Pick a person within view range and
	//head towards them for APPROACH_DIST meters.
	//Pick from visible people according to tie-strength
	private class Group implements State{
		public String toString(){ return "GROUP";}
		public int act(double time){
			ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
			ArrayList<Agent> agents = new ArrayList<Agent>();
			boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
			boolean successAgents = body.getAllVisibleSameType(agents);
			body.setDesiredVelocity(0.0,0.0,0.0);
			int tgtId = -1;
			double prefSum = 0;
			if(agents.size()<=0){
				stopLoiteringAt = -1;
				//stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				//startLoiteringAt = time;
				target = null;
				return LOITER;
			}
			for(int i=0;i<agents.size();i++){
				prefSum += preferences.get(agents.get(i));
				if(agents.get(i)==target){
					tgtId = i;
					break;
				}
			}
			if(tgtId==-1){
				int blahTemp;
				double sampleProp;
				while(tgtId==-1){
					blahTemp = body.getRandom().nextInt(agents.size());
					sampleProp = body.getRandom().nextDouble();
					if(sampleProp<(preferences.get(agents.get(blahTemp))/prefSum) ){
						tgtId = blahTemp;
					}
				}
			}
			double forwardSpeed = GROUP_SPEED;
			//double forwardSpeed = (GROUP_SPEED)*(stopGroupingAt-time)/(stopGroupingAt-startGroupingAt);
			double turnSpeed = vecs.get(tgtId).angle();
			body.setDesiredVelocity(forwardSpeed,0.0,turnSpeed);
			if(lostFight()){
				target = null;
				return FLEE;
			}
			if(wonFight()){
				target = null;
				return CHASE;
			}
			if(tooClose(vecs,agents)){
				target = null;
				return ENCOUNTER;
			}
			if(groupLoiterTimeout(time)){
				stopLoiteringAt = -1;
				//stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				//startLoiteringAt = time;
				target = null;
				return LOITER;
			}
			return GROUP;
		}
	}

	//FLEE behavior.
	//Head away from the person that is chasing
	//me for FLEE_DIST meters
	public class Flee implements State {
		public String toString(){return "FLEE";}
		public int act(double time){
			MutableDouble2D nearestObs = new MutableDouble2D();
			ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
			ArrayList<Agent> agents = new ArrayList<Agent>();
			boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
			boolean successAgents = body.getAllVisibleSameType(agents);
			boolean successObst = body.getNearestObstacleVec(nearestObs);
			int tgtId = -1;
			if(agents.size()<=0){
				stopLoiteringAt = -1;
				//stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				//startLoiteringAt = time;
				fleeFrom = null;
				return LOITER;
			}
			for(int i=0;i<agents.size();i++){
				if(agents.get(i)==fleeFrom){
					tgtId = i;
					break;
				}
			}
			if(tgtId==-1){
				stopLoiteringAt = -1;
				//stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				//startLoiteringAt = time;
				fleeFrom = null;
				return LOITER;
			}
			MutableDouble2D fleeVec = vecs.get(tgtId).dup().negate();
			//fleeVec.multiplyIn(1.0/fleeVec.length());
			//fleeVec.addIn(nearestObs.dup().negate().multiplyIn(1.0/nearestObs.length()));
			if(nearestObs.length() < fleeVec.length()) fleeVec.addIn(nearestObs.dup().negate());
			double forwardSpeed = GROUP_SPEED;
			double turnSpeed = fleeVec.angle();
			body.setDesiredVelocity(forwardSpeed,0.0,turnSpeed);
			if(fleeLoiterTimeout(time)){
				stopLoiteringAt = -1;
				//stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				//startLoiteringAt = time;
				fleeFrom = null;
				return LOITER;
			}
			return FLEE;
		}		
	}

	//CHASE behavior.
	//Head towards the person that I'm chasing
	//for CHASE_DIST meters
	private class Chase implements State{
		public String toString(){return "CHASE";}
		public int act(double time){
			MutableDouble2D nearestObs = new MutableDouble2D();
			ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
			ArrayList<Agent> agents = new ArrayList<Agent>();
			boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
			boolean successAgents = body.getAllVisibleSameType(agents);
			boolean successObst = body.getNearestObstacleVec(nearestObs);
			int tgtId = -1;
			if(agents.size()<=0){
				stopLoiteringAt = -1;
				//stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				//startLoiteringAt = time;
				chaseTowards = null;
				return LOITER;
			}
			for(int i=0;i<agents.size();i++){
				if(agents.get(i)==chaseTowards){
					tgtId=i;
					break;
				}
			}
			if(tgtId == -1){
				stopLoiteringAt = -1;
				//stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				//startLoiteringAt = time;
				chaseTowards = null;
				return LOITER;
			}
			MutableDouble2D chaseVec = vecs.get(tgtId).dup();
			//chaseVec.multiplyIn(1.0/Math.exp(chaseVec.length()));
			//chaseVec.addIn(nearestObs.dup().negate().multiplyIn(1.0/Math.exp(nearestObs.length())));
			chaseVec.addIn(nearestObs.dup().negate());
			double forwardSpeed = CHASE_SPEED;
			double turnSpeed = chaseVec.angle();
			body.setDesiredVelocity(forwardSpeed,0.0,turnSpeed);
			if(chaseLoiterTimeout(time)){
				body.setDesiredVelocity(0.0,0.0,-turnSpeed);
				stopLoiteringAt = -1;
				// stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				// startLoiteringAt = time;
				chaseTowards = null;
				return LOITER;
			}
			return CHASE;
		}
	}

	//LOITER behavior.
	//Sit still.
	private class Loiter implements State {
		public String toString(){return "LOITER";}
		public int act(double time){
			ArrayList<Agent> agents = new ArrayList<Agent>();
			ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
			boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
			boolean successAgents = body.getAllVisibleSameType(agents);
			MutableDouble2D avgVec = new MutableDouble2D();
			avgVec.x = avgVec.y = 0.0;
			for(int i=0;i<vecs.size();i++){
				avgVec.addIn(vecs.get(i).dup().normalize());
			}
			body.setDesiredVelocity(0.0,0.0,avgVec.angle());
			if(stopLoiteringAt == -1){
				stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				startLoiteringAt = time;
				//System.out.println("Loitering for "+ (stopLoiteringAt-startLoiteringAt) +" seconds");
			}

			if(lostFight()){
				return FLEE;
			}
			if(wonFight()){
				return CHASE;
			}
			if(tooClose(vecs,agents)){
				return ENCOUNTER;
			}
			if(isFarFromGroup(vecs)){
				//NOTE: GROUP actually will pick an individual if none is set
				//unlike FLEE and CHASE, so all we have to do here is set
				//the timeout
				startGroupingAt = time;
				stopGroupingAt = time + (GROUP_DIST/GROUP_SPEED);
				return GROUP;
			}
			if(loiterRandomWalkTimeout(time)){
				startRandomWalkingAt = time;
				//stopRandomWalkingAt = time+(RANDOM_WALK_DIST/RANDOM_WALK_SPEED);
				stopRandomWalkingAt = time+(ROAM_DIST/ROAM_SPEED);
				//return RANDOM_WALK;
				return ROAM;
			}
			return LOITER;
		}
	}

	//ENCOUNTER behavior.
	//Fight!
	private class Encounter implements State{
		public String toString(){return "ENCOUNTER";}
		public int act(double time){
			ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
			ArrayList<Agent> agents = new ArrayList<Agent>();
			boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
			boolean successAgents = body.getAllVisibleSameType(agents);
			//If fleeFrom or chaseTowards have already been set, no
			//need to calculate anything
			if(lostFight()){
				return FLEE;
			}
			if(wonFight()){
				return CHASE;
			}
			//NO AVOIDANCE, just classic DomWorld
			//fights MUST happen.
			double closestD = -1;
			int closestId = -1;
			for(int i=0;i<vecs.size();i++){
				double guyDist = vecs.get(i).length();
				if(closestId == -1 || guyDist < closestD){
					closestId = i;
					closestD = guyDist;
				}
			}
			if(closestId == -1){
				//I guess they left
				stopLoiteringAt = -1;
				//stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
				//startLoiteringAt = time;
				return LOITER;
			}
			if(!(agents.get(closestId) instanceof DomWorldStateMachine)){
				throw new RuntimeException("Closest agent is not a monkey");
			}
			DomWorldStateMachine otherGuy = (DomWorldStateMachine)agents.get(closestId);
			//If he's chasing, or being chased, ignore it.
			if(otherGuy.nextState == CHASE || otherGuy.nextState==FLEE){
				stopLoiteringAt = -1;
				return LOITER;
			}
			double winProb = 1.0/(1+Math.exp(-ETA*(dominanceRank-otherGuy.dominanceRank)));
			double randomSample = body.getRandom().nextDouble();
			if(randomSample<winProb){
				chaseTowards = otherGuy;
				startChasingAt = time;
				stopChasingAt = time+(CHASE_DIST/CHASE_SPEED);
				otherGuy.fleeFrom = DomWorldStateMachine.this;
				otherGuy.startFleeingAt = time;
				otherGuy.stopFleeingAt = time+(FLEE_DIST/FLEE_SPEED);
				return CHASE;
			} else {
				fleeFrom = otherGuy;
				startFleeingAt = time;
				stopFleeingAt = time+(FLEE_DIST/FLEE_SPEED);
				otherGuy.chaseTowards = DomWorldStateMachine.this;
				otherGuy.startChasingAt = time;
				otherGuy.stopChasingAt = time+(CHASE_DIST/CHASE_SPEED);
				return FLEE;
			}
		}
	}

	public DomWorldStateMachine(AbstractMonkey b, double domRank){
		body = b;
		dominanceRank = domRank;
		preferences = null;
		states = new State[NUM_STATES];
		//states[RANDOM_WALK] = new RandomWalk();
		states[ROAM] = new Roam();
		states[GROUP] = new Group();
		states[FLEE] = new Flee();
		states[CHASE] = new Chase();
		states[LOITER] = new Loiter();
		states[ENCOUNTER] = new Encounter();
		//initial state is to loiter!
		stopLoiteringAt = -1;//(-AVERAGE_EVENT_TIME*Math.log(initRandom.nextDouble()));
		nextState = LOITER;
	}

	public boolean isFarFromGroup(ArrayList<MutableDouble2D> perceivedMonkeys){
		int groupCtr = 0;
		for(int i=0;i<perceivedMonkeys.size();i++){
			double guyDist = perceivedMonkeys.get(i).length();
			//System.out.println("Dir to monkey: "+perceivedMonkeys.get(i).angle());
			//System.out.println("Dist to monkey: "+perceivedMonkeys.get(i));
			boolean inFront = perceivedMonkeys.get(i).angle()<FRONTAL_FOV/2.0;
			inFront = inFront && perceivedMonkeys.get(i).angle()>-FRONTAL_FOV/2.0;
			if(guyDist <= NEAR_DIST && inFront){
				groupCtr++;
			}
		}
		if(groupCtr >MIN_OTHERS){
			return false;
		} else{
			//System.out.println("NEAR_DIST: "+NEAR_DIST);
			//System.out.println("Group: "+groupCtr+" "+perceivedMonkeys.size());
			return true;
		}
	}

	public boolean tooClose(ArrayList<MutableDouble2D> perceivedMonkeys, ArrayList<Agent> perceivedAgents){
		MutableDouble2D closestMonkey = null;
		Agent closestMonkeyAgent = null;;
		double closestD = -1;
		for(int i=0;i<perceivedMonkeys.size();i++){
			if(!(perceivedAgents.get(i) instanceof DomWorldStateMachine)) continue;
			if(((DomWorldStateMachine)perceivedAgents.get(i)).nextState == CHASE) continue;
			if(((DomWorldStateMachine)perceivedAgents.get(i)).nextState == FLEE) continue;
			double guyDist = perceivedMonkeys.get(i).length();
			boolean inFront = perceivedMonkeys.get(i).angle()<FRONTAL_FOV/2.0;
			inFront = inFront && perceivedMonkeys.get(i).angle()>-FRONTAL_FOV/2.0;
			if(guyDist <= PERSONAL_DIST && inFront){
				if(closestMonkey==null || guyDist < closestD){
					closestMonkey = perceivedMonkeys.get(i);
					closestD = guyDist;
					closestMonkeyAgent = perceivedAgents.get(i);
				}
			}
		}
		if(lastClosest == closestMonkeyAgent || body.getRandom().nextDouble()<HIERARCHY_STABILITY) return false;
		lastClosest = closestMonkeyAgent;
		return (closestMonkey!=null)&&(closestMonkey.length()<PERSONAL_DIST);
	}

	public boolean lostFight(){
		return fleeFrom!=null;
	}

	public boolean wonFight(){
		return chaseTowards!=null;
	}

	public boolean loiterRandomWalkTimeout(double time){
		return time>=stopLoiteringAt;
	}

	public boolean randomWalkLoiterTimeout(double time){
		return time>=stopRandomWalkingAt;
	}

	public boolean fleeLoiterTimeout(double time){
		return time>=stopFleeingAt;
	}

	public boolean chaseLoiterTimeout(double time){
		return time>=stopChasingAt;
	}

	public boolean groupLoiterTimeout(double time){
		return time >=stopGroupingAt;
	}

	public boolean atWaypoint(MutableDouble2D pos){
		return pos.distance(gtwLocation)==0.0;
	}

	public boolean roamLoiterTimeout(double time){
		return time >= stopRoamingAt;
	}

	public void init(){
		nextState=LOITER;
		fleeFrom = null;
		chaseTowards = null;
		startRandomWalkingAt = stopRandomWalkingAt = -1; 
		startFleeingAt = stopFleeingAt = -1;
		startChasingAt = stopChasingAt = -1;
		startGroupingAt = stopGroupingAt = -1;
		startLoiteringAt = 0;
		stopLoiteringAt = -1;//(-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()));
	}
	public void finish(){
	}
}