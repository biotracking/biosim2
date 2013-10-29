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

	protected boolean computeNewConfiguration(MutableDouble2D newPos, MutableDouble2D newDir){
		/*
		MutableDouble2D tmp = new MutableDouble2D(desiredVelXYT[0],desiredVelXYT[1]);
		MutableDouble2D curDir = new MutableDouble2D();
		sim.getBodyOrientation(this,curDir);
		tmp.rotate(curDir.angle());
		double xVel = (tmp.x<=MAX_VELOCITY_X)?tmp.x:MAX_VELOCITY_X;
		double yVel = (tmp.y<=MAX_VELOCITY_Y)?tmp.y:MAX_VELOCITY_Y;
		double tVel = (Math.abs(desiredVelXYT[2])<=MAX_VELOCITY_THETA)?desiredVelXYT[2]:Math.signum(desiredVelXYT[2])*MAX_VELOCITY_THETA;
		Double2D oldPos = sim.field2D.getObjectLocation(this);
		newPos.x = oldPos.x+(xVel*sim.resolution);
		newPos.y = oldPos.y+(yVel*sim.resolution);
		for(int i=0;i<sim.bodies.size();i++){
			if(sim.bodies.get(i)==this){
				newDir.setTo(sim.bodyOrientations.get(i));
				newDir.rotate(tVel*sim.resolution);
				break;
			}
		}
		return true;
		*/
		return false;
	}

	public void step(SimState simstate){
		if(simstate instanceof Simulation){
			sim = (Simulation)simstate;
		}
	}
}