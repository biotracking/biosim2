package biosim.core.body;

import biosim.core.agent.Agent;
import biosim.core.sim.Simulation;
import biosim.core.sim.Obstacle;

import ec.util.MersenneTwisterFast;
import sim.engine.Steppable;
import sim.engine.SimState;
import sim.util.MutableDouble2D;
import sim.util.Double2D;

public abstract class Body implements Steppable{
	protected Agent agent;
	protected Simulation sim;
	public String label;
	public String getLabel(){ return label; }
	public boolean notFinished = false;
	public void setAgent(Agent a){ agent = a; }
	public Agent getAgent(){ return agent; }
	public abstract MersenneTwisterFast getRandom();
	/**
	* The size of the body in meters. Used in the default portrayal as the size
	* of the displayed circle. Also used in the default Environment.configSim(...)
	* method to determine if a random location for a body is a valid starting
	* location. For Body classes that use more complicated collision detection
	* you may need to write a custom configSim(...) method.
	**/
	public abstract double getSize();
	protected abstract boolean computeNewConfiguration(MutableDouble2D newPos, MutableDouble2D newDir);
	protected boolean collisionCheck(MutableDouble2D newPos, MutableDouble2D newDir){
		int numObstacles=sim.obstacles.size();
		Double2D newPosD2D = new Double2D(newPos);//dumb dumb dumb dumb
		for(int i=0;i<numObstacles;i++){
			Obstacle o = sim.obstacles.get(i);
			if(newPos.distance(o.closestPoint(newPosD2D,sim.field2D.getObjectLocation(o)))<(getSize()/2)){
				return true;
			}
		}
		return false;
	}

	protected void move(MutableDouble2D newPos, MutableDouble2D newDir){
		sim.setObjectLocation(this,new Double2D(newPos));
		for(int i=0;i<sim.bodies.size();i++){
			if(sim.bodies.get(i)==this){
				sim.bodyOrientations.set(i,new Double2D(newDir));
				break;
			}
		}
	}

	public void init(){
		if(agent != null) agent.init();
		notFinished = true;
	}
	public void finish(){
		if(notFinished){
			if(agent != null) agent.finish();
			notFinished = false;
		}
	}
	public void step(SimState simstate){
		if(simstate instanceof Simulation){
			sim = (Simulation)simstate;
			agent.act(sim.schedule.getSteps()*sim.resolution);
			MutableDouble2D newPos = new MutableDouble2D();
			MutableDouble2D newDir = new MutableDouble2D();
			if(computeNewConfiguration(newPos,newDir)){
				if(!collisionCheck(newPos,newDir)){
					move(newPos,newDir);
				} else {
					//Allow rotation in place if it does not collide
					MutableDouble2D oldPos = new MutableDouble2D(sim.field2D.getObjectLocation(this));
					if(!collisionCheck(oldPos,newDir)){
						move(oldPos,newDir);
					}
				}
			}
		}
	}
}
