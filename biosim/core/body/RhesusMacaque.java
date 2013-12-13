package biosim.core.body;

import java.util.List;
import sim.engine.SimState;
import sim.util.Bag;
import sim.util.MutableDouble2D;
import sim.util.Double2D;
import ec.util.MersenneTwisterFast;

import biosim.core.sim.Simulation;
import biosim.core.agent.Agent;

public class RhesusMacaque extends AbstractMonkey {
	public static double SENSOR_RANGE=50.0;
	public static double SENSOR_FOV= 360.0 * (2*Math.PI/360.0);
	public static double SIZE=0.25;
	public static double MAX_VELOCITY_X=2.0;
	public static double MAX_VELOCITY_Y=1.0;
	public static double MAX_VELOCITY_THETA= 2*Math.PI;
	public static double PROX_RANGE=SENSOR_RANGE;
	public static double PROX_SENSORS=100;

	private double[] desiredVelXYT = {0.0,0.0,0.0};
	private String text = " ";
	public void setTextDisplay(String displayText){ text = displayText; }
	public String getTextDisplay(){ return text;}

	public double getSize(){return SIZE;}
	public MersenneTwisterFast getRandom(){return sim.random;}
	public void setDesiredVelocity(double x, double y, double theta){
		if(Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(theta)){
			throw new RuntimeException("Tried to set NaN velocity");
		}
		desiredVelXYT[0] = x;
		desiredVelXYT[1] = y;
		desiredVelXYT[2] = theta;
	}

	public boolean getAbsoluteOrientation(MutableDouble2D dir){
		return sim.getBodyOrientation(this,dir);
	}

	public boolean getAbsolutePosition(MutableDouble2D loc){
		Double2D rv = sim.field2D.getObjectLocation(this);
		if(rv == null) return false;
		loc.setTo(rv);
		return true;
	}

	public boolean getEnvironmentSize(MutableDouble2D size){
		Double2D rv = sim.field2D.getDimensions();
		size.setTo(rv);
		return true;
	}

	public boolean getAllVisibleSameType(List<Agent> rv){
		if(rv == null) return false;
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof RhesusMacaque){
					RhesusMacaque tmpMonkey = (RhesusMacaque)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpMonkey);
					if(tmpMonkey == this) continue;
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else {
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
					mutTmp.rotate(-dir.angle());
					if(mutTmp.angle() > SENSOR_FOV/2 || mutTmp.angle() <-SENSOR_FOV/2) continue;
					if(mutTmp.length() > SENSOR_RANGE) continue;
					rv.add(tmpMonkey.agent);
				}
			}
			return true;
		}
		return false;
	}
	public double getAllVisibleSameTypeSensorRange(){
		return SENSOR_RANGE;
	}
	public double getAllVisibleSameTypeSensorFoV(){
		return SENSOR_FOV;
	}
	public boolean getAllVisibleSameTypeVecs(List<MutableDouble2D> rv){
		if(rv == null) return false;
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof RhesusMacaque){
					RhesusMacaque tmpMonkey = (RhesusMacaque)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpMonkey);
					if(tmpMonkey == this) continue;
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else {
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
					mutTmp.rotate(-dir.angle());
					if(mutTmp.angle() > SENSOR_FOV/2 || mutTmp.angle() <-SENSOR_FOV/2) continue;
					if(mutTmp.length() > SENSOR_RANGE) continue;
					rv.add(mutTmp);
				}
			}
			return true;
		}
		return false;
	}
	public double getAllVisibleSameTypeVecsSensorRange(){
		return SENSOR_RANGE;
	}
	public double getAllVisibleSameTypeVecsSensorFoV(){
		return SENSOR_FOV;
	}

	public boolean getNearestObstacleVec(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			MutableDouble2D nearestObsPoint = null;
			double nearestObsPointDS = -1.0;
			for(int i=0;i<sim.obstacles.size();i++){
				Double2D tmpPoint = sim.field2D.getObjectLocation(sim.obstacles.get(i));
				MutableDouble2D mutTmp;
				if(sim.toroidal){
					mutTmp = new MutableDouble2D(sim.obstacles.get(i).toroidalClosestPoint(loc,tmpPoint,sim.field2D));
					mutTmp = new MutableDouble2D(sim.field2D.tv(new Double2D(mutTmp),loc));
					mutTmp.rotate(-dir.angle());
					//if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() < -Math.PI/2) continue;
					double tmpDS = sim.field2D.tds(new Double2D(0,0),new Double2D(mutTmp));
					if(nearestObsPoint == null || nearestObsPointDS > tmpDS){
						nearestObsPoint = mutTmp;
						nearestObsPointDS = tmpDS;
					}					
				} else {
					mutTmp = new MutableDouble2D(sim.obstacles.get(i).closestPoint(loc,tmpPoint));
					mutTmp.subtractIn(loc);
					mutTmp.rotate(-dir.angle());
					//if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
					if(nearestObsPoint == null || nearestObsPoint.lengthSq() > mutTmp.lengthSq()){
						nearestObsPoint = mutTmp;
					}
				}
			}
			if(nearestObsPoint != null && nearestObsPoint.length() <= SENSOR_RANGE){
				rv.setTo(nearestObsPoint);
				return true;
			} 
		}
		return false;
	}
	public double getNearestObstacleVecSensorRange(){ return SENSOR_RANGE; }

	protected boolean computeNewConfiguration(MutableDouble2D newPos, MutableDouble2D newDir){
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
		newDir.setTo(curDir);
		newDir.rotate(tVel*sim.resolution);
		return true;
	}
}