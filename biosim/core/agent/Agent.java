package biosim.core.agent;
import biosim.core.body.Body;
public interface Agent{
	public double[] act(double step);
	
	public void init();
	public void finish();
}
