package biosim.core.body;

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
	public double[] desiredVelXYT;
	private double xVel, yVel, tVel;
	private Simulation sim; //most recent sim object
	
	//getters for inspectors
	public Double3D getVel(){ return new Double3D(xVel,yVel,tVel); }
	public Double3D getDesiredVel(){return (desiredVelXYT.length==3)?new Double3D(desiredVelXYT[0],desiredVelXYT[1],desiredVelXYT[2]):new Double3D();}


	public static final double SIZE=0.0086; //8.6mm long, meters
	public static final double RANGE=SIZE*3; //3 times body length, meters
	public static final double MAX_VELOCITY_XY=SIZE; //1 bodylength, meters/second
	public static final double MAX_VELOCITY_THETA=2*Math.PI; //2pi , radians/second

	public AphaenogasterCockerelli(){
		desiredVelXYT = new double[3];
	}

	public double getSize(){ return SIZE; }
	
	public boolean getHomeDir(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			for(int i=0;i<sim.poi.size();i++){
				if(sim.poi.get(i).equalsIgnoreCase("nest")){
					Double2D nestLoc = sim.field2D.getObjectLocation(sim.poi.get(i));
					rv.setTo(nestLoc);
					rv.subtractIn(loc);
					rv.rotate(-dir.angle());
					rv.normalize();
					return true;
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
	public double getNearestObstacleVecSensorRange(){ return RANGE; }
	public boolean getNearestSameAgentVec(MutableDouble2D rv){
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
	public double getNearestSameAgentVecSensorRange(){ return RANGE; }
	public boolean getSelfVelXYT(double[] rv){
		if(rv.length != 3) return false;
		rv[0]=xVel;
		rv[1]=yVel;
		rv[2]=tVel;
		return true;
	}

	public void step(SimState simstate){
		if(simstate instanceof Simulation){
			sim= (Simulation)simstate;
			desiredVelXYT = agent.act(sim.schedule.getSteps()*sim.resolution);
			//rotate back to global frame
			MutableDouble2D tmp = new MutableDouble2D(desiredVelXYT[0],desiredVelXYT[1]);
			MutableDouble2D curDir = new MutableDouble2D();
			sim.getBodyOrientation(this,curDir);
			tmp.rotate(curDir.angle());
			/*
			desiredVelXYT[0] = tmp.x;
			desiredVelXYT[1] = tmp.y;
			double velMag = Math.sqrt(Math.pow(desiredVelXYT[0],2)+Math.pow(desiredVelXYT[1],2));
			if(velMag > MAX_VELOCITY_XY){
				xVel = (desiredVelXYT[0]/velMag)*MAX_VELOCITY_XY;
				yVel = (desiredVelXYT[1]/velMag)*MAX_VELOCITY_XY;
			} else {
				xVel = desiredVelXYT[0];
				yVel = desiredVelXYT[1];
			}
			if(desiredVelXYT[2] > MAX_VELOCITY_THETA){
				tVel = MAX_VELOCITY_THETA;
			} else {
				tVel = desiredVelXYT[2];
			}
			*/
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
				if(newLoc.distance(o.closestPoint(newLoc,sim.field2D.getObjectLocation(o)))<SIZE){
					collides = true;
					//System.out.println("Collision!");
					break;
				}
			}
			if(!collides){
				sim.field2D.setObjectLocation(this,newLoc);
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
