package biosim.core.body.sensors;

public interface Proximity{
	public double[] getProximity(double[] rv);
	public int getNumSensors();
	public double getProximitySensorRange();
}
