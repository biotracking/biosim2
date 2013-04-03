package biosim.core.body;
import biosim.core.agent.Agent;
import ec.util.MersenneTwisterFast;
import sim.engine.Steppable;
import sim.engine.SimState;

public abstract class Body implements Steppable{
	protected Agent agent;
	public boolean notFinished = false;
	public void setAgent(Agent a){ agent = a; }
	public Agent getAgent(){ return agent; }
	public abstract void step(SimState simstate);
	public abstract MersenneTwisterFast getRandom();
	/**
	* The size of the body in meters. Used in the default portrayal as the size
	* of the displayed circle. Also used in the default Environment.configSim(...)
	* method to determine if a random location for a body is a valid starting
	* location. For Body classes that use more complicated collision detection
	* you may need to write a custom configSim(...) method.
	**/
	public abstract double getSize();
	
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
}
