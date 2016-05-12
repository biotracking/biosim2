package biosim.core.sim;

import sim.engine.Steppable;
import sim.engine.SimState;

import java.io.IOException;

public interface Logger extends Steppable{
	public void step(SimState simstate);
	public void finish();
	public void init();
}
