package biosim.core.body.sensors;

import sim.util.MutableDouble2D;
public interface NearestPreyVec {
	public boolean getNearestPreyVec(MutableDouble2D rv);
	public double getNearestPreyVecSensorRange();
}
