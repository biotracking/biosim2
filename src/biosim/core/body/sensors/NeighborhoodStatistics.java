// NeighborhoodStatistics.java
package biosim.core.body.sensors;

public interface NeighborhoodStatistics {
	public class VelocityStatistics {
		public double xvmean, xvstd, xvmax;
	}
	public boolean getVelocityStatistics(VelocityStatistics rv);
}
