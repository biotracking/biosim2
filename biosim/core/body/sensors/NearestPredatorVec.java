package biosim.core.body.sensors;

import sim.util.MutableDouble2D;
public interface NearestPredatorVec {
	public boolean getNearestPredatorVec(MutableDouble2D rv);
	public double getNearestPredatorVecSensorRange();
}
