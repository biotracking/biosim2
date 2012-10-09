package biosim.core.sim;
import sim.util.Double2D;

public interface Obstacle {
	public Double2D closestPoint(Double2D p, Double2D o);
}
