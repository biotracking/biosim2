package biosim.core.body.sensors;

import sim.util.MutableDouble2D;
public interface AverageRBFSameTypeVec {
	public boolean getAverageRBFSameTypeVec(MutableDouble2D rv, double sigma);
	public double getAverageRBFSameTypeVecSensorRange();
}
