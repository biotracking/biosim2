package biosim.core.body.sensors;

import sim.util.MutableDouble2D;
public interface NearestObstacleVec {
	public boolean getNearestObstacleVec(MutableDouble2D rv);
	public double getNearestObstacleVecSensorRange();
}
