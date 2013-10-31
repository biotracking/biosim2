package biosim.app.domworld;

import java.util.ArrayList;

import biosim.core.agent.Agent;
import biosim.core.body.Body;
import biosim.core.body.AbstractMonkey;
import sim.util.MutableDouble2D;

public class DomWorldMonkey implements Agent{
	AbstractMonkey body;
	public double dominanceRank, nextTime=-1.0, lastTime=0.0;
	public static double PERSONAL_DIST=4.0;
	public static double NEAR_DIST=20.0;
	public static double FAR_DIST=NEAR_DIST*Math.sqrt(30);//30 == number of agents. It's arbitrary.
	public static int MIN_OTHERS=3;
	public static double AVERAGE_EVENT_TIME=10.0;
	public static double APPROACH_SPEED=1.0;
	public static double FRONTAL_FOV= 120.0 * (2*Math.PI/360.0);
	public Agent target=null, fleeFrom=null, chaseTowards=null;
	public double targetDist=-1.0;
	public boolean isRandomWalking=false;
	public double randomTurnSpeed=0.0;
	public DomWorldMonkey(AbstractMonkey b, double domRank){
		body = b;
		dominanceRank = domRank;
	}
	public void init(){

	}
	public void finish(){

	}
	public void act(double time){
		ArrayList<MutableDouble2D> vecs = new ArrayList<MutableDouble2D>();
		ArrayList<Agent> agents = new ArrayList<Agent>();
		//PERCEIVE
		boolean successAgents = body.getAllVisibleSameType(agents);
		boolean successVecs = body.getAllVisibleSameTypeVecs(vecs);

		body.setDesiredVelocity(0,0,0);
		if(nextTime != -1.0 && nextTime > time){
			if(fleeFrom != null){
				for(int i=0;i<agents.size();i++){
					if(agents.get(i) == fleeFrom){
						if(vecs.get(i).length()<(2.0+PERSONAL_DIST)){
							body.setDesiredVelocity((2.0+PERSONAL_DIST)-vecs.get(i).length(),0,vecs.get(i).dup().negate().angle());
						} else {
							fleeFrom = null;
						}
					}
				}
			} else if(chaseTowards != null){
				for(int i=0;i<agents.size();i++){
					if(agents.get(i) == chaseTowards){
						if(vecs.get(i).length()<(1.0+PERSONAL_DIST)){
							body.setDesiredVelocity((1.0+PERSONAL_DIST)-vecs.get(i).length(),0,vecs.get(i).angle());
						} else {
							chaseTowards = null;
						}
					}
				}
			} 
			else if(target != null){
				for(int i=0;i<agents.size();i++){
					if(agents.get(i) == target){
						if(vecs.get(i).length()<targetDist){
							target = null;
						} else {
							body.setDesiredVelocity(vecs.get(i).length()-targetDist,0,vecs.get(i).angle());
						}
					}
				}
			} else if(isRandomWalking){ 
				if(time-lastTime < 1.0){
					body.setDesiredVelocity(1.0,0.0,0.0);
				} else if(time-lastTime < 2.0){
					body.setDesiredVelocity(0.0,0.0,randomTurnSpeed);
				} else {
					isRandomWalking = false;
				}
			}
			return;
		}
		isRandomWalking = false;
		chaseTowards = fleeFrom = target = null;
		double tmpRnd = body.getRandom().nextDouble();
		//System.out.println("Random: "+tmpRnd);
		//System.out.println("lg(rnd): "+Math.log(tmpRnd));
		//System.out.println("-AvgTime*lg(rnd): "+(-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble())));
		nextTime = (-AVERAGE_EVENT_TIME*Math.log(body.getRandom().nextDouble()))+time;
		//System.out.println("New event @ "+nextTime);
		lastTime = time;

		boolean upInMyFace = false;
		int nearestMonkey = -1;
		double nearestDist = -1.0;
		int groupCtr = 0;
		for(int i=0;i<vecs.size();i++){
			double guyDist = vecs.get(i).length();
			boolean inFront = vecs.get(i).angle()<FRONTAL_FOV/2.0;
			inFront = inFront && vecs.get(i).angle()>-FRONTAL_FOV/2.0;
			if(guyDist<=PERSONAL_DIST && inFront){
				upInMyFace = true;
				if(nearestDist < 0 || nearestDist > guyDist){
					nearestMonkey = i;
					nearestDist = vecs.get(i).length();
				}
			}
			if(guyDist <= NEAR_DIST && inFront){
				groupCtr++;
			}
		}
		if(upInMyFace){
			//INTERACT
			try{
				DomWorldMonkey otherGuy = (DomWorldMonkey)(agents.get(nearestMonkey));
				double chanceOfWinning = 1.0/(1+Math.exp((otherGuy.dominanceRank - this.dominanceRank)));
				double challengeChance = body.getRandom().nextDouble();
				if(challengeChance > chanceOfWinning){
					//FIGHT!
					double challengedChance = body.getRandom().nextDouble();
					if(challengedChance > 1-chanceOfWinning){
						//OK, FIGHT!
						if(body.getRandom().nextDouble() < chanceOfWinning){
							//I WON!
							chaseTowards = otherGuy;
							otherGuy.fleeFrom = this;
						} else {
							//CRAP, SUPER RUNAWAY!
							fleeFrom = otherGuy;
							otherGuy.chaseTowards = this;
						}
					} else{
						//He ran, oh well.
						otherGuy.fleeFrom = this;
					}
				} else {
					//FLEE!
					fleeFrom = otherGuy;
				}
			} catch(ClassCastException cce){
				//don't do anything, there are NON MONKEYS in this sim!
			}
		} else {
			// (NO): group?
			if(groupCtr< MIN_OTHERS){
				// (YES): GROUP
				int targetIDX = body.getRandom().nextInt(agents.size());
				target = agents.get(targetIDX);
				targetDist = vecs.get(targetIDX).length()-1.0;
			} else {
				// (NO): RANDOM WALK
				isRandomWalking=true;
				randomTurnSpeed = (body.getRandom().nextDouble()*(Math.PI))-(Math.PI/2.0);
			}
		}
		//		(NO): avoidance?
		//			(YES): AVOID
		//			(NO): RANDOM WALK
	}
}