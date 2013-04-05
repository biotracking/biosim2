package biosim.core.body.sensors;

public interface Proximity{
	public double[] getProximity(double[] rv);
	public int getNumProximitySensors();
	public double getProximitySensorRange();
}
