package biosim.core.body;

import java.util.List;
import sim.engine.SimState;
import sim.util.MutableDouble2D;
import ec.util.MersenneTwisterFast;
import biosim.core.sim.Simulation;

public class RhesusMacaque extends AbstractMonkey {
	public static double SENSOR_RANGE=10.0;
	public static double SENSOR_FOV= 300.0 * (2*Math.PI/360.0);
	public static double SIZE=1.0;

	private Simulation sim;

	public double getSize(){return SIZE;}
	public MersenneTwisterFast getRandom(){return sim.random;}
	public void setDesiredVelocity(double x, double y, double theta){

	}
	public boolean getAllVisibleSameAgentsVecs(List<MutableDouble2D> rv){
		return false;
	}
	public double getAllVisibleSameAgentsVecsSensorRange(){
		return SENSOR_RANGE;
	}
	public double getAllVisibleSameAgentsVecsFoV(){
		return SENSOR_FOV;
	}
	public void step(SimState simstate){
		if(simstate instanceof Simulation){
			sim = (Simulation)simstate;
		}
	}
}