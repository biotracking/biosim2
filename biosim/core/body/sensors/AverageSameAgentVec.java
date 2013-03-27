package biosim.core.body.sensors;

import sim.util.MutableDouble2D;
public interface AverageSameAgentVec {
	public boolean getAverageSameAgentVec(MutableDouble2D rv);
	public double getAverageSameAgentVecSensorRange();
}
