package biosim.core.body.sensors;

import sim.util.MutableDouble2D;

public interface GPS{
	public boolean getAbsolutePosition(MutableDouble2D loc);
}