package biosim.core.body.sensors;

import sim.util.MutableDouble2D;
public interface NearestSameTypeVec {
	public boolean getNearestSameTypeVec(MutableDouble2D rv);
	public double getNearestSameTypeVecSensorRange();
}
