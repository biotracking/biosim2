package biosim.core.body;
import biosim.core.agent.Agent;
import sim.engine.Steppable;
import sim.engine.SimState;

public abstract class Body implements Steppable{
	protected Agent agent;
	public void setAgent(Agent a){ agent = a; }
	public abstract void step(SimState simstate);
	/**
	* The size of the body in meters. Used in the default portrayal as the size
	* of the displayed circle. Also used in the default Environment.configSim(...)
	* method to determine if a random location for a body is a valid starting
	* location. For Body classes that use more complicated collision detection
	* you may need to write a custom configSim(...) method.
	**/
	public abstract double getSize();
}
