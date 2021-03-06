package biosim.core.body;

import ec.util.MersenneTwisterFast;
import sim.util.Double2D;
import sim.util.MutableDouble2D;
import sim.util.Double3D;
import sim.util.Bag;
import sim.engine.SimState;
import biosim.core.body.sensors.*;
import biosim.core.agent.Agent;
import biosim.core.sim.Simulation;
import biosim.core.sim.Obstacle;
//debug
import biosim.core.sim.RectObstacle;


public class AphaenogasterCockerelli extends AbstractAnt {
	public double[] desiredVelXYT, previousVelXYT;
	private double xVel, yVel, tVel;
	//private Simulation sim; //most recent sim object
	
	//getters for inspectors
	public Double3D getVel(){ return new Double3D(xVel,yVel,tVel); }
	public Double3D getDesiredVel(){return (desiredVelXYT.length==3)?new Double3D(desiredVelXYT[0],desiredVelXYT[1],desiredVelXYT[2]):new Double3D();}


	public static final double SIZE=0.0086; //8.6mm long, meters
	public static final double RANGE=SIZE*3; //3 times body length, meters
	public static final double GRIP_RANGE=SIZE/2.0;
	public static final double MAX_VELOCITY_XY=SIZE; //1 bodylength, meters/second
	public static final double MAX_VELOCITY_THETA=2*Math.PI; //2pi , radians/second

	public AphaenogasterCockerelli(){
		desiredVelXYT = new double[3];
		previousVelXYT = new double[3];
		previousVelXYT[0] = previousVelXYT[1] = previousVelXYT[2] = 0.0;
	}

	public void init(){
		super.init();
		previousVelXYT[0] = previousVelXYT[1] = previousVelXYT[2] = 0.0;
	}
	
	public MersenneTwisterFast getRandom(){
		return sim.random;
	}
	
	public double getSize(){ return SIZE; }
	
	public boolean getHomeDir(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			for(int i=0;i<sim.poi.size();i++){
				if(sim.poi.get(i).equalsIgnoreCase("nest")){
					Double2D nestLoc = sim.field2D.getObjectLocation(sim.poi.get(i));
					if(sim.toroidal){
						rv.setTo(sim.field2D.tv(nestLoc,loc));
					} else {
						rv.setTo(nestLoc);
						rv.subtractIn(loc);
					}
					rv.rotate(-dir.angle());
					rv.normalize();
					return true;
				}
			}
		} 
		return false;
	}
	public boolean getPoiDir(MutableDouble2D rv, String name){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			for(int i=0;i<sim.poi.size();i++){
				if(sim.poi.get(i).equalsIgnoreCase(name)){
					Double2D nestLoc = sim.field2D.getObjectLocation(sim.poi.get(i));
					if(sim.toroidal){
						rv.setTo(sim.field2D.tv(nestLoc,loc));
					} else {
						rv.setTo(nestLoc);
						rv.subtractIn(loc);
					}
					rv.rotate(-dir.angle());
					rv.normalize();
					return true;
				}
			}
		} 
		return false;
	}
	
	public boolean nearPOI(String name){
		Double2D loc = sim.field2D.getObjectLocation(this);
		for(int i=0;i<sim.poi.size();i++){
			if(sim.poi.get(i).equalsIgnoreCase(name)){
				MutableDouble2D poiLoc = new MutableDouble2D(sim.field2D.getObjectLocation(sim.poi.get(i)));
				if(sim.toroidal){
					return (sim.field2D.tds(loc,new Double2D(poiLoc)) < this.getSize()*this.getSize());
				}else {
					return (poiLoc.subtractIn(loc).length() < this.getSize());
				}
			}
		}
		return false;
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
					if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() < -Math.PI/2) continue;
					double tmpDS = sim.field2D.tds(new Double2D(0,0),new Double2D(mutTmp));
					if(nearestObsPoint == null || nearestObsPointDS > tmpDS){
						nearestObsPoint = mutTmp;
						nearestObsPointDS = tmpDS;
					}
				} else {
					mutTmp = new MutableDouble2D(sim.obstacles.get(i).closestPoint(loc,tmpPoint));
					mutTmp.subtractIn(loc);
					mutTmp.rotate(-dir.angle());
					if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
					if(nearestObsPoint == null || nearestObsPoint.lengthSq() > mutTmp.lengthSq()){
						nearestObsPoint = mutTmp;
					}
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
	public boolean getNearestSameTypeVec(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			MutableDouble2D nearestLoc = null;
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof AphaenogasterCockerelli){
					AphaenogasterCockerelli tmpAnt = (AphaenogasterCockerelli)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpAnt);
					if(tmpAnt == this) continue;
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else {
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
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
		/*
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest= sim.field2D.getNearestNeighbors(loc,1,false,false,true,null);
			if(nearest.numObjs > 0 && nearest.objs[0] instanceof AphaenogasterCockerelli){
				AphaenogasterCockerelli nearAnt = (AphaenogasterCockerelli)nearest.objs[0];
				Double2D nearAntLoc = sim.field2D.getObjectLocation(nearAnt);
				if(loc.distance(nearAntLoc) <= RANGE){
					MutableDouble2D tmp = new MutableDouble2D(nearAntLoc);
					tmp.subtractIn(loc);	
					tmp.rotate(-dir.angle());
					if(tmp.angle() > Math.PI/2 || tmp.angle() < -Math.PI/2){
						return false;
					} else {
						rv.setTo(tmp);
						return true;
					}
				}
			}
		} 
		return false;
		*/
	}
	public double getNearestSameTypeVecSensorRange(){ return RANGE; }

	public boolean getNearestPreyVec(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			MutableDouble2D nearestLoc = null;
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof AbstractFly){
					AbstractFly tmpFly = (AbstractFly)nearest.objs[i];
					if(!tmpFly.yummy) continue;
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFly);
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else {
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
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
	public double getNearestPreyVecSensorRange(){ return RANGE; }

	
	public boolean getSelfVelXYT(double[] rv){
		if(rv.length != 3) return false;
		rv[0]=xVel;
		rv[1]=yVel;
		rv[2]=tVel;
		return true;
	}
	
	public boolean getGripped(){
		return grabbing!=null;
	}
	
	public void tryToGrab(){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			MutableDouble2D nearestLoc = null;
			AbstractFly nearestFly = null;
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof AbstractFly){
					AbstractFly tmpFly = (AbstractFly)nearest.objs[i];
					if(!tmpFly.yummy) continue;
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFly);
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else {
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
					mutTmp.rotate(-dir.angle());
					if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
					if(nearestLoc == null || nearestLoc.lengthSq() > mutTmp.lengthSq()){
						nearestLoc = mutTmp;
						nearestFly = tmpFly;
					}
				}
			}
			if(nearestLoc != null && nearestLoc.length() <= GRIP_RANGE){
				grabbing = nearestFly;
				nearestFly.grabbedBy = this;
				nearestFly.yummy = false;
			} 
		}
	}
	
	public void tryToDrop(){
		if(grabbing != null){
			grabbing.grabbedBy = null;
			grabbing = null;
		}
	}

	public void setDesiredVelocity(double x, double y, double theta){
		desiredVelXYT[0]  = x;
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
			sim= (Simulation)simstate;
			//desiredVelXYT = agent.act(sim.schedule.getSteps()*sim.resolution);
			agent.act(sim.schedule.getSteps()*sim.resolution);
			//rotate back to global frame
			MutableDouble2D tmp = new MutableDouble2D(desiredVelXYT[0],desiredVelXYT[1]);
			MutableDouble2D curDir = new MutableDouble2D();
			sim.getBodyOrientation(this,curDir);
			tmp.rotate(curDir.angle());
			double velMag = Math.sqrt(Math.pow(tmp.x,2)+Math.pow(tmp.y,2));
			if(velMag > MAX_VELOCITY_XY){
				xVel = (tmp.x/velMag)*MAX_VELOCITY_XY;
				yVel = (tmp.y/velMag)*MAX_VELOCITY_XY;
			} else {
				xVel = tmp.x;
				yVel = tmp.y;
			}
			if(Math.abs(desiredVelXYT[2]) > MAX_VELOCITY_THETA){
				tVel = MAX_VELOCITY_THETA*Math.signum(desiredVelXYT[2]);
			} else {
				tVel = desiredVelXYT[2];
			}
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
				previousVelXYT[0] = xVel;
				previousVelXYT[1] = yVel;
				sim.setObjectLocation(this,newLoc);
			} else {
				previousVelXYT[0] = previousVelXYT[1] = 0.0;
			}
			previousVelXYT[2] = tVel;
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
	*/
}
