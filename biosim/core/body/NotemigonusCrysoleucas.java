package biosim.core.body;

import biosim.core.body.sensors.*;
import biosim.core.agent.Agent;
import biosim.core.sim.Simulation;
import biosim.core.sim.Obstacle;
import biosim.core.sim.RectObstacle;

import ec.util.MersenneTwisterFast;
import sim.engine.SimState;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

public class NotemigonusCrysoleucas extends AbstractFish {
	private Simulation sim;
	
	private double xVel, yVel, tVel;

	public double[] desiredVelXYT = new double[3];

	public static final double SIZE=0.10; //7.5 to 12.5 cm
	public static final double RANGE=Double.POSITIVE_INFINITY; //no limit on the range for now
	public static final double MAX_VELOCITY_X=3*SIZE; //3 bodylengths per second forwards/backwards
	public static final double MAX_VELOCITY_Y=SIZE/5.0; //1/5th of a bodylength per second sidways
	public static final double MAX_VELOCITY_THETA=2*Math.PI; //fish can turn quickly
	public static final int PROX_SENSORS=8;
	public static final double PROX_RANGE=Double.POSITIVE_INFINITY;
	
	public boolean getSelfVelXYT(double[] rv){
		if(rv == null || rv.length != 3) return false;
		rv[0] = xVel;
		rv[1] = yVel;
		rv[2] = tVel;
		return true;
	}

	public double getSize(){ return SIZE; }
	
	public MersenneTwisterFast getRandom(){
		return sim.random;
	}
	
	public boolean getNearestObstacleVec(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			MutableDouble2D nearestObsPoint = null;
			for(int i=0;i<sim.obstacles.size();i++){
				Double2D tmpPoint = sim.field2D.getObjectLocation(sim.obstacles.get(i));
				MutableDouble2D mutTmp = new MutableDouble2D(sim.obstacles.get(i).closestPoint(loc,tmpPoint));
				mutTmp.subtractIn(loc);
				mutTmp.rotate(-dir.angle());
				//if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
				if(nearestObsPoint == null || nearestObsPoint.lengthSq() > mutTmp.lengthSq()){
					nearestObsPoint = mutTmp;
				}
			}
			if(nearestObsPoint != null && nearestObsPoint.length() <= RANGE){
				rv.setTo(nearestObsPoint);
				return true;
			} 
		}
		return false;
	}
	public double getNearestObstacleVecSensorRange(){ return RANGE; }
	
	public boolean getNearestSameAgentVec(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			MutableDouble2D nearestLoc = null;
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof NotemigonusCrysoleucas){
					NotemigonusCrysoleucas tmpFish = (NotemigonusCrysoleucas)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFish);
					if(tmpFish == this) continue;
					MutableDouble2D mutTmp = new MutableDouble2D(tmpLoc);
					mutTmp.subtractIn(loc);
					mutTmp.rotate(-dir.angle());
					//if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
					if(nearestLoc == null || nearestLoc.lengthSq() > mutTmp.lengthSq()){
						nearestLoc = mutTmp;
					}
				}
			}
			if(nearestLoc != null && nearestLoc.length() <= RANGE){
				rv.setTo(nearestLoc);
				return true;
			} 
		}
		return false;
	}
	public double getNearestSameAgentVecSensorRange(){ return RANGE; }
	
	public double[] getProximity(double[] rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(rv == null || rv.length != PROX_SENSORS) rv = new double[PROX_SENSORS];
		for(int i=0;i<rv.length;i++) rv[i] = -1.0;
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof NotemigonusCrysoleucas){
					NotemigonusCrysoleucas tmpFish = (NotemigonusCrysoleucas)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFish);
					if(tmpFish == this) continue;
					MutableDouble2D mutTmp = new MutableDouble2D(tmpLoc);
					mutTmp.subtractIn(loc);
					double mutTmpDist = mutTmp.length();
					mutTmp.rotate(-dir.angle());
					double mutTmpAngle = mutTmp.angle();
					int angleSlot = (int)(mutTmpAngle/(2.0*Math.PI/PROX_SENSORS));
					if(angleSlot < 0) angleSlot = PROX_SENSORS+angleSlot;
					if(rv[angleSlot] == -1.0 || rv[angleSlot] > mutTmpDist){
						rv[angleSlot] = mutTmpDist;
					}
				}
			}
		}
		return rv;
	}
	public int getNumSensors(){ return PROX_SENSORS; }
	public double getProximitySensorRange(){ return PROX_RANGE; }

	public boolean getAverageSameAgentVec(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		rv.x = 0;
		rv.y = 0;
		int numNeighbors = 0;
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof NotemigonusCrysoleucas){
					NotemigonusCrysoleucas tmpFish = (NotemigonusCrysoleucas)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFish);
					if(tmpFish == this) continue;
					MutableDouble2D mutTmp = new MutableDouble2D(tmpLoc);
					mutTmp.subtractIn(loc);
					mutTmp.rotate(-dir.angle());
					//if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
					if(mutTmp.lengthSq() <= RANGE*RANGE){
						numNeighbors++;
						rv.addIn(mutTmp);
					}
				}
			}
			if(numNeighbors > 0){
				rv.multiplyIn(1.0/(double)numNeighbors);
				return true;
			} 
		}
		rv.x = 0;
		rv.y = 0;
		return false;
		
	}
	public double getAverageSameAgentVecSensorRange(){ return RANGE; }

	public void setDesiredVelocity(double x, double y, double theta){
		desiredVelXYT[0] = x;
		desiredVelXYT[1] = y;
		desiredVelXYT[2] = theta;
	}

	public void step(SimState simstate){
		if(simstate instanceof Simulation){
			sim= (Simulation)simstate;
			//double[] desiredVelXYT = agent.act(sim.schedule.getSteps()*sim.resolution);
			agent.act(sim.schedule.getSteps()*sim.resolution);
			//rotate back to global frame
			MutableDouble2D tmp = new MutableDouble2D(desiredVelXYT[0],desiredVelXYT[1]);
			MutableDouble2D curDir = new MutableDouble2D();
			sim.getBodyOrientation(this,curDir);
			tmp.rotate(curDir.angle());
			
			xVel = (tmp.x<=MAX_VELOCITY_X)?tmp.x:MAX_VELOCITY_X;
			yVel = (tmp.y<=MAX_VELOCITY_Y)?tmp.y:MAX_VELOCITY_Y;
			tVel = (Math.abs(desiredVelXYT[2])<=MAX_VELOCITY_THETA)?desiredVelXYT[2]:Math.signum(desiredVelXYT[2])*MAX_VELOCITY_THETA;
			Double2D oldLoc = sim.field2D.getObjectLocation(this);
			Double2D newLoc = new Double2D(oldLoc.x+(xVel*sim.resolution),oldLoc.y+(yVel*sim.resolution));
			//for now, just check against all obstacles defined in the Environment object, since the
			//continuous field doesn't have a sense of the extent of large obstacles
			boolean collides = false;
			for(int i=0;i<sim.obstacles.size();i++){
				Obstacle o = sim.obstacles.get(i);
				if(newLoc.distance(o.closestPoint(newLoc,sim.field2D.getObjectLocation(o)))<(SIZE/2)){
					collides = true;
					//System.out.println("Collision!");
					break;
				}
			}
			if(!collides){
				sim.setObjectLocation(this,newLoc);
			} 
			for(int i=0;i<sim.bodies.size();i++){
				if(sim.bodies.get(i) == this){
					MutableDouble2D oldDir = new MutableDouble2D(sim.bodyOrientations.get(i));
					MutableDouble2D newDir = new MutableDouble2D(oldDir);
					newDir.rotate(tVel*sim.resolution);
					sim.bodyOrientations.set(i,new Double2D(newDir));
					break;
				}
			}
		} else {
			throw new RuntimeException("SimState object not an instance of "+Simulation.class.getName());
		}
	}

}
