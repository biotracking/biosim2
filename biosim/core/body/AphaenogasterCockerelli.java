package biosim.core.body;

import sim.util.Double2D;
import sim.util.Double3D;
import biosim.core.agent.Agent;
import biosim.core.sim.Simulation;

public class AphaenogasterCockerelli extends Body
	implements 	NearestSameAgentVec,
				Homing,
				NearestObstacleVec,
				SelfVelXYT {
	public double[] desiredVelXYT;
	private double xVel, yVel, tVel;	//velocity variables. Only kept so GUI can inspect the data.
	
	//getters for inspectors
	public Double3D getVel(){ return new Double3D(xVel,yVel,tVel); }
	public Double3D getDesiredVel(){return (desiredVelXYT.length==3)?new Double3D(desiredVelXYT[0],desiredVelXYT[1],desiredVel[2]):new Double3D();}


	public static final double SIZE=0.0086; //8.6mm long, meters
	public static final double RANGE=SIZE*3; //3 times body length, meters
	public static final double MAX_VELOCITY_XY=SIZE; //1 bodylength, meters/second
	public static final double MAX_VELOCITY_THETA=2*Math.PI; //2pi , radians/second

	public AphaenogasterCockerelli(){
		desiredVelXYT = new double[3];
		orientation = new MutableDouble2D();
	}

	public boolean getHomeDir(MutableDouble2D rv){
	
	}
	public boolean getNearestObstacleVec(MutableDouble2D rv){
	
	}
	public double getNearestObstacleVecSensorRange(){ return RANGE; }
	public boolean getNearestSameAgentVec(MutableDouble2D rv){
		
	}
	public double getNearestSameAgentVecSensorRange(){ return RANGE; }
	public boolean getSelfVelXYT(double[] rv){
		
	}

	public void step(SimState simstate){
		if(simstate instanceof Simulation){
			Simulation biosimSim = (Simulation)simstate;
			agent.act(biosimSim.schedule.getSteps());
			double velMag = Math.sqrt(Math.pow(desiredVelXYT[0],2)+Math.pow(desiredVelXYT[1],2));
			if(velMag > MAX_VELOCITY_XY){
				xVel = (desiredVelXYT[0]/velMag)*MAX_VELOCITY;
				yVel = (desiredVelXYT[1]/velMag)*MAX_VELOCITY;
			} else {
				xVel = desiredVelXYT[0];
				yVel = desiredVelXYT[1];
			}
			if(desiredVelXYT[2] > MAX_VELOCITY_THETA){
				tVel = MAX_VELOCITY_THETA;
			} else {
				tVel = desiredVelXYT[2];
			}
			Double2D oldLoc = biosimSim.field2D.getObjectLocation(this);
			Double2D newLoc = new Double2D(oldLoc.x+(xVel*biosimSim.resolution),oldLoc.y+(yVel*biosimSim.resolution));
			//for now, just check against all obstacles defined in the Environment object, since the
			//continuous field doesn't have a sense of the extent of large obstacles
			boolean collides = false;
			for(int i=0;i<biosimSim.env.obstacles.size();i++){
				Obstacle o = biosimSim.env.obstacles.get(i);
				if(o.collides(newLoc,biosimSim.field2D.getObjectLocation(o),SIZE)){
					collides = true;
					break;
				}
			}
			if(!collides){
				biosimSim.field2D.setObjectLocation(this,newLoc);
			}
			for(int i=0;i<biosimSim.bodies.size();i++){
				if(biosimSim.bodies.get(i) == this){
					MutableDouble2D oldDir = new MutableDouble2D(biosimSim.bodyOrientations.get(i));
					biosimSim.bodyOrientations.set(i,new Double2D(oldDir.rotate(tVel*biosimSim.resolution)));
					break;
				}
			}
		} else {
			throw new RuntimeException("SimState object not an instance of "+Simulation.class.getName());
		}
	}
}
