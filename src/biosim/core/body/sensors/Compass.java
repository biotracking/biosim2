package biosim.core.body.sensors;

import sim.util.MutableDouble2D;

public interface Compass {
	public boolean getAbsoluteOrientation(MutableDouble2D dir);
}