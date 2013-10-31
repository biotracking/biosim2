package biosim.core.sim;
import sim.util.Double2D;
import sim.field.continuous.Continuous2D;

public interface Obstacle {
	public Double2D closestPoint(Double2D p, Double2D o);
	public Double2D toroidalClosestPoint(Double2D p, Double2D o, Continuous2D field);
}
