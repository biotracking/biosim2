package biosim.core.body.sensors;

import sim.util.MutableDouble2D;
public interface Homing {
	public boolean getHomeDir(MutableDouble2D rv);
}
