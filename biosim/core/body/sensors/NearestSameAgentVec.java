package biosim.core.body.sensors;

import sim.util.MutableDouble2D;
public interface NearestSameAgentVec {
	public boolean getNearestSameAgentVec(MutableDouble2D rv);
	public double getNearestSameAgentVecSensorRange();
}
