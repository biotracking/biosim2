package biosim.core.body.actuators;

public interface HolonomicDrive {
	public void setDesiredVelocity(double x, double y, double theta);
}
