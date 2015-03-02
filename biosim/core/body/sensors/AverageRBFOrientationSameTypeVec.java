package biosim.core.body.sensors;

import sim.util.MutableDouble2D;
public interface AverageRBFOrientationSameTypeVec {
	public boolean getAverageRBFOrientationSameTypeVec(MutableDouble2D rv, double sigma);
	public double getAverageRBFOrientationSameTypeVecSensorRange();
}
