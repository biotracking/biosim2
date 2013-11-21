//DomWorldStateMachine.java

package biosim.app.domworld;

import java.util.ArrayList;
import java.util.HashMap;

import sim.util.MutableDouble2D;
import ec.util.MersenneTwisterFast;

import biosim.core.agent.Agent;
import biosim.core.agent.StateMachine;
import biosim.core.body.AbstractMonkey;

public class DomWorldStateMachine extends StateMachine {
	//states
	public static int RANDOM_WALK=0;
	public static int GROUP=1;
	public static int FLEE=2;
	public static int CHASE=3;
	public static int LOITER=4;
	public static int ENCOUNTER=5;
	public static int NUM_STATES=6;
	//monkey behavior paramters
	public static double PERSONAL_DIST=4.0;
	public static double NEAR_DIST=20.0;
	public static double FAR_DIST=NEAR_DIST*Math.sqrt(30);//30 == number of agents. It's arbitrary.
	public static int MIN_OTHERS=3;
	public static double AVERAGE_EVENT_TIME=10.0;
	public static double FRONTAL_FOV= 120.0 * (2.0*Math.PI/360.0);
	public static double RANDOM_WALK_SPEED=0.5;
	public static double RANDOM_WALK_DIST=1.0;
	public static double FLEE_SPEED=2.0;
	public static double FLEE_DIST=5.0;
	public static double CHASE_SPEED=1.0;
	public static double CHASE_DIST=1.0;
	public static double GROUP_SPEED=0.75;
	public static double GROUP_DIST=1.0; //The distance to travel towards another group member.
	public static double ETA = 6.0;	//derived so that the probability of A wining in a fight with B
									//where D_a = 1/30, D_b = 1.0, is roughly 0.003
	//instance data members
	private DomWorldStateMachine target = null, chaseTowards=null, fleeFrom=null;
	private double startLoiteringAt, stopLoiteringAt;
	private double startRandomWalkingAt, stopRandomWalkingAt;
	private double startFleeingAt, stopFleeingAt;
	private double startChasingAt, stopChasingAt;
	private double startGroupingAt, stopGroupingAt;
	private double dominanceRank;
	private AbstractMonkey body;
	private HashMap<DomWorldStateMachine,Double> preferences;

	public void setTieStrengths(HashMap<DomWorldStateMachine,Double> tsprefs){
		preferences = tsprefs;
	}

	public DomWorldStateMachine(AbstractMonkey b, double domRank){
		body = b;
		dominanceRank = domRank;
		preferences = null;
		states = new State[NUM_STATES];
		//RANDOM WALK behavior. 
		//Pick a random direction and head there for 
		//RANDOM_WALK_DIST meters
		states[RANDOM_WALK] = new State() {
			public String toString(){ return "RANDOM_WALK";}
			public int act(double time){
				ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
				MutableDouble2D nearestObs = new MutableDouble2D();
				boolean successObst = body.getNearestObstacleVec(nearestObs);
				boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
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
				if(tooClose(vecs))
					return ENCOUNTER;
				if(randomWalkLoiterTimeout(time)){
					stopLoiteringAt = -1;
					//stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
					//startLoiteringAt = time;
					return LOITER;
				}
				return RANDOM_WALK;
			}
		};
		//GROUP behavior.
		//Pick a person within view range and
		//head towards them for APPROACH_DIST meters.
		//Pick from visible people according to tie-strength
		states[GROUP] = new State() {
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
				if(tooClose(vecs)){
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
		};
		//FLEE behavior.
		//Head away from the person that is chasing
		//me for FLEE_DIST meters
		states[FLEE] = new State(){
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
				fleeVec.addIn(nearestObs.dup().negate());
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
		};
		//CHASE behavior.
		//Head towards the person that I'm chasing
		//for CHASE_DIST meters
		states[CHASE] = new State(){
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
		};
		//LOITER behavior.
		//Sit still.
		states[LOITER] = new State(){
			public String toString(){return "LOITER";}
			public int act(double time){
				ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
				boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
				body.setDesiredVelocity(0.0,0.0,0.0);
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
				if(tooClose(vecs)){
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
					stopRandomWalkingAt = time+(RANDOM_WALK_DIST/RANDOM_WALK_SPEED);
					return RANDOM_WALK;
				}
				return LOITER;
			}
		};
		//ENCOUNTER behavior.
		//Fight!
		states[ENCOUNTER] = new State(){
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
		};
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

	public boolean tooClose(ArrayList<MutableDouble2D> perceivedMonkeys){
		MutableDouble2D closestMonkey = null;
		double closestD = -1;
		for(int i=0;i<perceivedMonkeys.size();i++){
			double guyDist = perceivedMonkeys.get(i).length();
			boolean inFront = perceivedMonkeys.get(i).angle()<FRONTAL_FOV/2.0;
			inFront = inFront && perceivedMonkeys.get(i).angle()>-FRONTAL_FOV/2.0;
			if(guyDist <= PERSONAL_DIST && inFront){
				if(closestMonkey==null || guyDist < closestD){
					closestMonkey = perceivedMonkeys.get(i);
					closestD = guyDist;
				}
			}
		}
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