//DomWorldStateMachine.java

package biosim.app.domworld;

import java.util.ArrayList;

import sim.util.MutableDouble2D;

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
	public static double FRONTAL_FOV= 120.0 * (2*Math.PI/360.0);
	public static double RANDOM_WALK_SPEED=0.5;
	public static double FLEE_SPEED=2.0;
	public static double CHASE_SPEED=1.0;
	public static double GROUP_SPEED=0.75;
	//instance data members
	private DomWorldStateMachine target = null, chaseTowards=null, fleeFrom=null;
	private double startLoiteringAt, stopLoiteringAt;
	private double startRandomWalkingAt, stopRandomWalkingAt;
	private double startFleeingAt, stopFleeingAt;
	private double startChasingAt, stopChasingAt;
	private double startGroupingAt, stopGroupingAt;
	private double dominanceRank;
	private AbstractMonkey body;

	public DomWorldStateMachine(AbstractMonkey b, double domRank){
		body = b;
		dominanceRank = domRank;
		states = new State[NUM_STATES];
		//RANDOM WALK behavior. 
		//Pick a random direction and head there for 
		//RANDOM_WALK_DIST meters
		states[RANDOM_WALK] = new State() {
			public int act(double time){
				ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
				boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
				double forwardSpeed = RANDOM_WALK_SPEED;
				//double forwardSpeed = (RANDOM_WALK_SPEED)*(stopRandomWalkingAt-time)/(stopRandomWalkingAt-startRandomWalkingAt);
				double turnSpeed = (body.getRandom().nextDouble()-0.5)*Math.PI;
				body.setDesiredVelocity(forwardSpeed,0.0,turnSpeed);
				if(lostFight())
					return FLEE;
				if(wonFight())
					return CHASE;
				if(tooClose(vecs))
					return ENCOUNTER;
				if(randomWalkLoiterTimeout(time))
					return LOITER;
				return RANDOM_WALK;
			}
		};
		//GROUP behavior.
		//Pick a random person within view range and
		//head towards them for APPROACH_DIST meters
		states[GROUP] = new State() {
			public int act(double time){
				ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
				ArrayList<Agent> agents = new ArrayList<Agent>();
				boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);
				boolean successAgents = body.getAllVisibleSameType(agents);
				int tgtId = -1;
				for(int i=0;i<agents.size();i++){
					if(agents.get(i)==target){
						tgtId = i;
					}
				}
				if(tgtId==-1){
					if(agents.size()>0){
						tgtId = body.getRandom().nextInt(agents.size());
					} else{
						return RANDOM_WALK;
					}
				}
				double forwardSpeed = GROUP_SPEED;
				//double forwardSpeed = (GROUP_SPEED)*(stopGroupingAt-time)/(stopGroupingAt-startGroupingAt);
				double turnSpeed = vecs.get(tgtId).angle();
				body.setDesiredVelocity(forwardSpeed,0.0,turnSpeed);
				if(lostFight())
					return FLEE;
				if(wonFight())
					return CHASE;
				if(tooClose(vecs))
					return ENCOUNTER;
				if(groupLoiterTimeout(time))
					return LOITER;
				return GROUP;
			}
		};
		//FLEE behavior.
		//Head away from the person that is chasing
		//me for FLEE_DIST meters

		//CHASE behavior.
		//Head towards the person that I'm chasing
		//for CHASE_DIST meters

		//LOITER behavior.
		//Sit still.

		//ENCOUNTER behavior.
		//Fight!
	}

	public boolean isFarFromGroup(ArrayList<MutableDouble2D> perceivedMonkeys){
		int groupCtr = 0;
		for(int i=0;i<perceivedMonkeys.size();i++){
			double guyDist = perceivedMonkeys.get(i).length();
			boolean inFront = perceivedMonkeys.get(i).angle()<FRONTAL_FOV/2.0;
			inFront = inFront && perceivedMonkeys.get(i).angle()>-FRONTAL_FOV/2.0;
			if(guyDist <= NEAR_DIST && inFront){
				groupCtr++;
			}
		}
		if(groupCtr >MIN_OTHERS){
			return false;
		} else{
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
		stopLoiteringAt = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()));
	}
	public void finish(){
	}
}