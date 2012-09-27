package biosim.core.body.sensors;
import sim.util.Double2D;
import java.util.ArrayList;
public interface AgentVec{
	public ArrayList<Double2D> getAgents();
	public double getAgentVecSensorRange();
}
