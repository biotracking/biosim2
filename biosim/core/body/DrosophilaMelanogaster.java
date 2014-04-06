package biosim.core.body;

import ec.util.MersenneTwisterFast;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.MutableDouble2D;
import sim.engine.SimState;
import biosim.core.body.sensors.*;
import biosim.core.agent.Agent;
import biosim.core.sim.Simulation;
import biosim.core.sim.Obstacle;


public class DrosophilaMelanogaster extends AbstractFly{
	public static final double SIZE = 0.0025; //2.5mm long
	public static final double RANGE = SIZE*3;
	public static final double MAX_VELOCITY_XY=SIZE; //1 bodylength, meters/second
	public static final double MAX_VELOCITY_THETA=2*Math.PI; //2pi , radians/second
	public double[] desiredVelXYT = new double[3];
	public double xVel, yVel, tVel;
	//private Simulation sim; //most recent sim object
	
	public double getSize(){ return SIZE;}
	
	public MersenneTwisterFast getRandom(){
		return sim.random;
	}
	
	public double getNearestSameTypeVecSensorRange(){ return RANGE; }
	public boolean getNearestSameTypeVec(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			MutableDouble2D nearestLoc = null;
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof DrosophilaMelanogaster){
					DrosophilaMelanogaster tmpFly = (DrosophilaMelanogaster)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFly);
					if(tmpFly == this) continue;
					MutableDouble2D mutTmp = new MutableDouble2D(tmpLoc);
					mutTmp.subtractIn(loc);
					mutTmp.rotate(-dir.angle());
					if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
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
	
	public double getNearestObstacleVecSensorRange(){ return RANGE; }
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
				if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
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
	
	public double getNearestPredatorVecSensorRange(){ return RANGE; }
	public boolean getNearestPredatorVec(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			MutableDouble2D nearestLoc = null;
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof AphaenogasterCockerelli){
					AphaenogasterCockerelli tmpAnt = (AphaenogasterCockerelli)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpAnt);
					MutableDouble2D mutTmp = new MutableDouble2D(tmpLoc);
					mutTmp.subtractIn(loc);
					mutTmp.rotate(-dir.angle());
					if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
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
	
	public boolean getSelfVelXYT(double[] rv){
		if(rv == null || rv.length != 3) return false;
		rv[0] = xVel;
		rv[1] = yVel;
		rv[2] = tVel;
		return true;
	}
	
	public void setDesiredVelocity(double x, double y, double theta){
		desiredVelXYT[0] = x;
		desiredVelXYT[1] = y;
		desiredVelXYT[2] = theta;
	}

	protected boolean computeNewConfiguration(MutableDouble2D newPos, MutableDouble2D newDir){
		MutableDouble2D tmp = new MutableDouble2D(desiredVelXYT[0],desiredVelXYT[1]);
		MutableDouble2D curDir = new MutableDouble2D();
		sim.getBodyOrientation(this,curDir);
		tmp.rotate(curDir.angle());
		xVel = (tmp.x<=MAX_VELOCITY_XY)?tmp.x:MAX_VELOCITY_XY;
		yVel = (tmp.y<=MAX_VELOCITY_XY)?tmp.y:MAX_VELOCITY_XY;
		tVel = (Math.abs(desiredVelXYT[2])<=MAX_VELOCITY_THETA)?desiredVelXYT[2]:Math.signum(desiredVelXYT[2])*MAX_VELOCITY_THETA;
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
	}
	/*
	public void step(SimState simstate){
		if(simstate instanceof Simulation){
			sim = (Simulation)simstate;
			if(grabbedBy == null){
				//double[] desiredVelXYT = agent.act(sim.schedule.getSteps()*sim.resolution);
				agent.act(sim.schedule.getSteps()*sim.resolution);
				MutableDouble2D tmp = new MutableDouble2D(desiredVelXYT[0],desiredVelXYT[1]);
				MutableDouble2D curDir = new MutableDouble2D();
				sim.getBodyOrientation(this,curDir);
				tmp.rotate(curDir.angle());
				double velMag = tmp.length();
				if(velMag > MAX_VELOCITY_XY){
					velXYT[0] = (tmp.x/velMag)*MAX_VELOCITY_XY;
					velXYT[1] = (tmp.y/velMag)*MAX_VELOCITY_XY;
				} else {
					velXYT[0] = tmp.x;
					velXYT[1] = tmp.y;
				}
				if(Math.abs(desiredVelXYT[2]) > MAX_VELOCITY_THETA){
					velXYT[2] = MAX_VELOCITY_THETA*Math.signum(desiredVelXYT[2]);
				} else {
					velXYT[2] = desiredVelXYT[2];
				}
				Double2D oldLoc = sim.field2D.getObjectLocation(this);
				Double2D newLoc = new Double2D(oldLoc.x+(velXYT[0]*sim.resolution),oldLoc.y+(velXYT[1]*sim.resolution));
				boolean collides = false;
				for(int i=0;i<sim.obstacles.size();i++){
					Obstacle o = sim.obstacles.get(i);
					if(newLoc.distance(o.closestPoint(newLoc,sim.field2D.getObjectLocation(o)))<SIZE){
						collides= true;
						break;
					}
				}
				if(!collides){
					sim.setObjectLocation(this,newLoc);
				} else {
					velXYT[0] = velXYT[1] = 0.0;
				}
				for(int i=0;i<sim.bodies.size();i++){
					if(sim.bodies.get(i) == this){
						MutableDouble2D oldDir = new MutableDouble2D(sim.bodyOrientations.get(i));
						MutableDouble2D newDir = new MutableDouble2D(oldDir);
						newDir.rotate(velXYT[2]*sim.resolution);
						sim.bodyOrientations.set(i,new Double2D(newDir));
						break;
					}
				}
			} else {
				sim.setObjectLocation(this,sim.field2D.getObjectLocation(grabbedBy));
			}
		} else {
			throw new RuntimeException("SimState object not an instance of "+Simulation.class.getName());
		}
	}
	*/
}
