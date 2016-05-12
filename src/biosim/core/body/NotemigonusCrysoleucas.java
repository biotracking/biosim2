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
	//shadows inherited sim. Safe to remove when inherited step(...) has been tested
	//private Simulation sim;
	
	private double xVel, yVel, tVel;

	private double xVelObserved, yVelObserved, tVelObserved;

	public double[] desiredVelXYT = new double[3];

	public static final double SIZE=0.05; //7.5 to 12.5 cm...paper says they were bought at 5cm long on average
	public static final double RANGE=1.0;//Double.POSITIVE_INFINITY; //no limit on the range for now
	public static final double MAX_VELOCITY_X=3*SIZE; //3 bodylengths per second forwards/backwards
	public static final double MAX_VELOCITY_Y=SIZE/5.0; //1/5th of a bodylength per second sidways
	public static final double MAX_VELOCITY_THETA=2*Math.PI; //fish can turn quickly
	public static final int PROX_SENSORS=8;
	public static final double PROX_RANGE=4*SIZE;//Double.POSITIVE_INFINITY;
	public static final int NUM_ZONES = 3;
	public static final double REPULSION_ZONE_RANGE=SIZE*0.5;
	public static final double ORIENTATION_ZONE_RANGE=REPULSION_ZONE_RANGE+(SIZE*2);
	public static final double ATTRACTION_ZONE_RANGE=ORIENTATION_ZONE_RANGE+(SIZE*13);
	public static final int REPULSION_ZONE_IDX=0;
	public static final int ORIENTATION_ZONE_IDX=1;
	public static final int ATTRACTION_ZONE_IDX=2;

	private boolean leaderp;
	public boolean isLeader(){ return leaderp; }
	public void setLeader(boolean leader){leaderp = leader; }
	
	private double avgDensity = -1.0;
	public double getAvgDensity(){ return avgDensity; }
	public void setAvgDensity(double ad){ avgDensity = ad; }

	public boolean sameAsMe(AbstractFish other){
		if(super.sameAsMe(other)){
			return true;
		}
		else if(other instanceof ReplayFish){
			boolean res = this.label.equals( ((Integer)((ReplayFish)other).trackID).toString() );
			return res;
		} else {
			return false;
		}
	}

	public boolean getSelfVelXYT(double[] rv){
		if(rv == null || rv.length != 3) return false;
		rv[0] = xVelObserved;
		rv[1] = yVelObserved;
		rv[2] = tVelObserved;
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
				if(nearest.objs[i] instanceof AbstractFish){
					AbstractFish tmpFish = (AbstractFish)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFish);
					if(this.sameAsMe(tmpFish)) continue;
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else { 
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
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
	public double getNearestSameTypeVecSensorRange(){ return RANGE; }
	
	public double[] getProximity(double[] rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		if(rv == null || rv.length != PROX_SENSORS) rv = new double[PROX_SENSORS];
		for(int i=0;i<rv.length;i++) rv[i] = PROX_RANGE;
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof AbstractFish){
					AbstractFish tmpFish = (AbstractFish)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFish);
					if(this.sameAsMe(tmpFish)) continue;
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else {
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
					double mutTmpDist = mutTmp.length();
					if(mutTmpDist > PROX_RANGE) continue;
					mutTmp.rotate(-dir.angle());
					double mutTmpAngle = mutTmp.angle();
					int angleSlot = (int)(mutTmpAngle/(2.0*Math.PI/PROX_SENSORS));
					if(angleSlot < 0) angleSlot = PROX_SENSORS+angleSlot;
					if(rv[angleSlot] > mutTmpDist){
						rv[angleSlot] = mutTmpDist;
					}
				}
			}
		}
		return rv;
	}
	public int getNumProximitySensors(){ return PROX_SENSORS; }
	public double getProximitySensorRange(){ return PROX_RANGE; }

	public boolean getAverageSameTypeVec(MutableDouble2D rv){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		rv.x = 0;
		rv.y = 0;
		int numNeighbors = 0;
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof AbstractFish){
					AbstractFish tmpFish = (AbstractFish)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFish);
					if(this.sameAsMe(tmpFish)) continue;
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else {
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
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
				//rv = rv.normalize();
				return true;
			} else {
				rv.x = rv.y = 0.0;
				return true;
			}
			/*
			double len = rv.length();
			if(len > 0){
				rv.normalize();
			}
			return true;
			*/
		}
		rv.x = 0;
		rv.y = 0;
		return false;
		
	}
	public double getAverageSameTypeVecSensorRange(){ return RANGE; }

	public boolean getAverageRBFSameTypeVec(MutableDouble2D rv, double sigma){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		rv.x = 0;
		rv.y = 0;
		int numNeighbors = 0;
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof AbstractFish){
					AbstractFish tmpFish = (AbstractFish)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFish);
					if(this.sameAsMe(tmpFish)) continue;
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else {
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
					mutTmp.rotate(-dir.angle());
					//if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
					double weight = Math.exp(-mutTmp.lengthSq()/(2.0*Math.pow(sigma,2)));
					rv.addIn(mutTmp.multiplyIn(weight));
					numNeighbors++;
					// if(mutTmp.lengthSq() <= RANGE*RANGE){
					// 	numNeighbors++;
					// 	rv.addIn(mutTmp);
					// }
				}
			}
			if(numNeighbors > 0){
				rv.multiplyIn(1.0/(double)numNeighbors);
				//rv = rv.normalize();
				return true;
			} else {
				rv.x = rv.y = 0.0;
				return true;
			}
			/*
			double len = rv.length();
			if(len > 0){
				rv.normalize();
			}
			return true;
			*/
		}
		rv.x = 0;
		rv.y = 0;
		return false;
		
	}
	public double getAverageRBFSameTypeVecSensorRange(){ return -1; }

	public boolean getAverageRBFOrientationSameTypeVec(MutableDouble2D rv, double sigma){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D dir = new MutableDouble2D();
		rv.x = 0;
		rv.y = 0;
		int numNeighbors = 0;
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();//sim.field2D.getObjectsExactlyWithinDistance(loc,RANGE);
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof AbstractFish){
					AbstractFish tmpFish = (AbstractFish)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFish);
					if(this.sameAsMe(tmpFish)) continue;
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else {
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
					// mutTmp.rotate(-dir.angle());
					MutableDouble2D tmpDir = new MutableDouble2D();
					sim.getBodyOrientation(tmpFish,tmpDir);
					tmpDir.rotate(-dir.angle());
					//if(mutTmp.angle() > Math.PI/2 || mutTmp.angle() <-Math.PI/2) continue;
					double weight = Math.exp(-mutTmp.lengthSq()/(2.0*Math.pow(sigma,2)));
					rv.addIn(tmpDir.multiplyIn(weight));
					numNeighbors++;
					// if(mutTmp.lengthSq() <= RANGE*RANGE){
					// 	numNeighbors++;
					// 	rv.addIn(mutTmp);
					// }
				}
			}
			if(numNeighbors > 0){
				rv.multiplyIn(1.0/(double)numNeighbors);
				//rv = rv.normalize();
				return true;
			} else {
				rv.x = rv.y = 0.0;
				return true;
			}
			/*
			double len = rv.length();
			if(len > 0){
				rv.normalize();
			}
			return true;
			*/
		}
		rv.x = 0;
		rv.y = 0;
		return false;
		
	}
	public double getAverageRBFOrientationSameTypeVecSensorRange(){ return -1; }

	public boolean getZoneCoMVecs(MutableDouble2D[] zoneVecs){
		Double2D loc = sim.field2D.getObjectLocation(this);
		MutableDouble2D[] rv = new MutableDouble2D[NUM_ZONES];
		for(int i=0;i<rv.length;i++) rv[i] = new MutableDouble2D();
		int[] numNeighbors = new int[NUM_ZONES];
		for(int i=0;i<numNeighbors.length;i++) numNeighbors[i] = 0;
		MutableDouble2D dir = new MutableDouble2D();
		MutableDouble2D neighborDir = new MutableDouble2D();
		if(sim.getBodyOrientation(this,dir)){
			Bag nearest = sim.field2D.getAllObjects();
			for(int i=0;i<nearest.numObjs;i++){
				if(nearest.objs[i] instanceof AbstractFish){
					AbstractFish tmpFish = (AbstractFish)nearest.objs[i];
					Double2D tmpLoc = sim.field2D.getObjectLocation(tmpFish);
					if(this.sameAsMe(tmpFish)) continue;
					MutableDouble2D mutTmp;
					if(sim.toroidal){
						mutTmp = new MutableDouble2D(sim.field2D.tv(tmpLoc,loc));
					} else {
						mutTmp = new MutableDouble2D(tmpLoc);
						mutTmp.subtractIn(loc);
					}
					mutTmp.rotate(-dir.angle());
					int zone = -1;
					double mtlsq = mutTmp.lengthSq();
					if(mtlsq < REPULSION_ZONE_RANGE){
						zone = REPULSION_ZONE_IDX;
					} else if(mtlsq < ORIENTATION_ZONE_RANGE){
						zone = ORIENTATION_ZONE_IDX;
					} else if(mtlsq < ATTRACTION_ZONE_RANGE){
						zone = ATTRACTION_ZONE_IDX;
					}
					if(zone >= 0){
						//orientation zone is based on relative orientation,
						//not relative position
						if(zone==ORIENTATION_ZONE_IDX){
							neighborDir.zero();
							if(sim.getBodyOrientation(tmpFish,neighborDir)){
								neighborDir.rotate(-dir.angle());
								rv[zone].addIn(neighborDir);
							} else {
								System.out.println("NotemigonusCrysoleucas: neighbor failed sim.getBodyOrientation(...)");
							}
						} else {
							rv[zone].addIn(mutTmp);
						}
						numNeighbors[zone]++;
					}	
				}
			}
			//also add in obstacle vector to avoidance zone if there is one
			MutableDouble2D tmpObs = new MutableDouble2D();
			if(getNearestObstacleVec(tmpObs)){
				if(tmpObs.lengthSq()<REPULSION_ZONE_RANGE){
					rv[REPULSION_ZONE_IDX].addIn(tmpObs);
					numNeighbors[REPULSION_ZONE_IDX]++;
				}
			}
			for(int i=0;i<NUM_ZONES;i++){
				if(numNeighbors[i] > 0){
					rv[i].multiplyIn(1.0/(double)numNeighbors[i]);
				} else {
					rv[i].x = rv[i].y = 0.0;
				}
				zoneVecs[i] = rv[i];
			}
			return true;
		}
		return false;
	}
	public void getZoneRanges(double[] zoneRanges){
		zoneRanges[0] = REPULSION_ZONE_RANGE;
		zoneRanges[1] = ORIENTATION_ZONE_RANGE;
		zoneRanges[2] = ATTRACTION_ZONE_RANGE;
	}
	public int getNumZones(){ return NUM_ZONES; }

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
		xVel = (Math.abs(tmp.x)<=MAX_VELOCITY_X)?tmp.x:Math.signum(tmp.x)*MAX_VELOCITY_X;
		yVel = (Math.abs(tmp.y)<=MAX_VELOCITY_Y)?tmp.y:Math.signum(tmp.y)*MAX_VELOCITY_Y;
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

	public void step(SimState simstate){
		MutableDouble2D oldPos=null, oldDir=null;
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)(simstate);
			oldPos = new MutableDouble2D(sim.field2D.getObjectLocation(this));
			for(int i=0;i<sim.bodies.size();i++){
				if(sim.bodies.get(i)==this){
					oldDir = new MutableDouble2D(sim.bodyOrientations.get(i));
					break;
				}
			}
		}
		super.step(simstate);
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)(simstate);
			MutableDouble2D newPos = new MutableDouble2D(sim.field2D.getObjectLocation(this));
			MutableDouble2D newDir = null;
			for(int i=0;i<sim.bodies.size();i++){
				if(sim.bodies.get(i)==this){
					newDir = new MutableDouble2D(sim.bodyOrientations.get(i));
					break;
				}
			}
			xVelObserved = (oldPos.x-newPos.x)/sim.resolution;
			yVelObserved = (oldPos.y-newPos.y)/sim.resolution;
			double tDelta = newDir.angle()-oldDir.angle();
			if(tDelta > Math.PI){
				tDelta = tDelta-(2.0*Math.PI);
			} else if(tDelta < -Math.PI){
				tDelta = tDelta+(2.0*Math.PI);
			}
			tVelObserved = tDelta/sim.resolution;
		}
	}

	/*
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
	*/

}
