package biosim.core.body;

import ec.util.MersenneTwisterFast;
import sim.util.Double2D;
import sim.util.MutableDouble2D;
import sim.engine.SimState;

import biosim.core.sim.Simulation;

import java.util.ArrayList;

public class ReplayBody extends Body{
	public double size;
	public boolean visible;
	//A track is: [x y theta time]
	public ArrayList<double[]> track;
	private int lastTrackIdx = 0;
	public int trackID;

	public ReplayBody(){
		doNotLog = true;
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
			if(track.get(lastTrackIdx)[3] > curTime) lastTrackIdx = 0;
			if(curTime > track.get(track.size()-1)[3]){
				visible = false;
				return;
			}
			for(int i=lastTrackIdx; i< track.size()-1;i++){
				if(track.get(i+1)[3] > curTime){
					if(track.get(i)[3] > curTime){
						//remain invisible until our track is active
						visible = false;
						return;
					}
					visible = true;
					//interpolate between the previous point and this one
					//to handle differences between tracking rate and
					//simulation rate
					double trackStepScale = track.get(i+1)[3] - track.get(i)[3];
					double nextScale = (curTime-track.get(i)[3])/trackStepScale;
					x = track.get(i)[0]*(1-nextScale) + track.get(i+1)[0]*nextScale;
					y = track.get(i)[1]*(1-nextScale) + track.get(i+1)[1]*nextScale;
					theta = track.get(i)[2]*(1-nextScale) + track.get(i+1)[2]*nextScale;
					sim.setObjectLocation(this,new Double2D(x,y));
					MutableDouble2D newDir = new MutableDouble2D(1,0);
					newDir.rotate(theta);
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
}
