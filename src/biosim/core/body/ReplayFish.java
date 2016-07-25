package biosim.core.body;

import ec.util.MersenneTwisterFast;
import sim.util.Double2D;
import sim.util.MutableDouble2D;
import sim.engine.SimState;

import biosim.core.sim.Simulation;
import biosim.core.body.sensors.NeighborhoodStatistics;

import java.util.ArrayList;

public class ReplayFish extends AbstractFish{
	public double size;
	public boolean visible;
	//A track is: [x y theta time]
	public ArrayList<double[]> track;
	// private int lastTrackIdx = 0;
	public int trackID;

	private Double2D lastPos, curPos;
	private MutableDouble2D lastDir, curDir;

	public ReplayFish(){
		doNotLog = true;
		lastPos = curPos = null;
		lastDir = curDir = null;
		visible=false;
	}

	public MersenneTwisterFast getRandom(){
		return null;
	}
	protected boolean computeNewConfiguration(MutableDouble2D newPos, MutableDouble2D newDir){
		return false;
	}
	public void step(SimState simstate){
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)simstate;
			double curTime = sim.schedule.getSteps()*sim.resolution;
			double x,y,theta;
			//find the closest time in the track
			// if(track.get(lastTrackIdx)[3] > curTime) lastTrackIdx = 0;
			if(curTime > track.get(track.size()-1)[3] || curTime < track.get(0)[3]){
				visible = false;
				return;
			}
			visible = true;
			for(int i=0; i< track.size()-1;i++){
				if(track.get(i+1)[3] > curTime){
					// if(track.get(i)[3] > curTime){
					// 	//remain invisible until our track is active
					// 	visible = false;
					// 	return;
					// }
					// visible = true;
					//interpolate between the previous point and this one
					//to handle differences between tracking rate and
					//simulation rate
					double trackStepScale = track.get(i+1)[3] - track.get(i)[3];
					double nextScale = (curTime-track.get(i)[3])/trackStepScale;
					x = track.get(i)[0]*(1-nextScale) + track.get(i+1)[0]*nextScale;
					y = track.get(i)[1]*(1-nextScale) + track.get(i+1)[1]*nextScale;
					theta = track.get(i)[2]*(1-nextScale) + track.get(i+1)[2]*nextScale;
					sim.setObjectLocation(this,new Double2D(x,y));
					lastPos = curPos;
					curPos = new Double2D(x,y);
					MutableDouble2D newDir = new MutableDouble2D(1,0);
					newDir.rotate(theta);
					lastDir = curDir;
					curDir = new MutableDouble2D(newDir);
					for(int j=0;j<sim.bodies.size();j++){
						if(sim.bodies.get(j) == this){
							sim.bodyOrientations.set(j,new Double2D(newDir));
							return;
						}
					}
					return;
				}
			}
		} else{ 
			throw new RuntimeException("SimState object not an instance of "+Simulation.class.getName());
		}
	}
	public void setSize(double size){
		this.size = size;
	}
	public double getSize(){ return size; }

	public boolean getAverageRBFOrientationSameTypeVec(MutableDouble2D rv, double sigma){ return false;}
	public double getAverageRBFOrientationSameTypeVecSensorRange(){return 0.0;}
	public boolean getAverageRBFSameTypeVec(MutableDouble2D rv, double sigma){return false;}
	public double getAverageRBFSameTypeVecSensorRange(){return 0.0;}
	public boolean getAverageSameTypeVec(MutableDouble2D rv){return false;}
	public double getAverageSameTypeVecSensorRange(){return 0.0;}
	public boolean getNearestObstacleVec(MutableDouble2D rv){return false;}
	public double getNearestObstacleVecSensorRange(){return 0.0;}
	public boolean getNearestSameTypeVec(MutableDouble2D rv){return false;}
	public double getNearestSameTypeVecSensorRange(){return 0.0;}
	public double[] getProximity(double[] rv){return rv;}
	public int getNumProximitySensors(){return 0;}
	public double getProximitySensorRange(){return 0.0;}

	public boolean getSelfVelXYT(double[] rv){
		if(rv == null || rv.length != 3) return false;
		if(lastDir == null || lastPos == null) return false;
		MutableDouble2D blah = new MutableDouble2D(curPos.x-lastPos.x,curPos.y-lastPos.y);
		blah.rotate(-lastDir.angle());
		rv[0] = blah.x/sim.resolution;
		rv[1] = blah.y/sim.resolution;
		rv[2] = curDir.angle()- lastDir.angle();
		if(rv[2] > Math.PI){
			rv[2] = rv[2]-(2.0*Math.PI);
		} else if(rv[2] < -Math.PI){
			rv[2] = rv[2]+(2.0*Math.PI);
		}
		rv[2] = rv[2]/sim.resolution;
		return true;
	}
	
	public boolean getVelocityStatistics(NeighborhoodStatistics.VelocityStatistics rv){return false;}

	public int getNumZones(){return 0;}
	public boolean getZoneCoMVecs(MutableDouble2D[] zoneVecs){return false;}
	public void getZoneRanges(double[] ranges){}
	public void setDesiredVelocity(double x, double y, double theta){}
}
