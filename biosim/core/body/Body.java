package biosim.core.body;
import biosim.core.agent.Agent;
import sim.engine.Steppable;
import sim.engine.SimState;

public abstract class Body implements Steppable{
	protected Agent agent;
	public void setAgent(Agent a){ agent = a; }
	public abstract void step(SimState simstate);
}
