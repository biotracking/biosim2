package biosim.core.sim;

public interface Obstacle {
	public boolean collides(Double2D p, Double2D o, double radius);
}
