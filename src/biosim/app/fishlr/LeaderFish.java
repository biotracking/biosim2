package biosim.app.fishlr;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractFish;
import biosim.core.body.NotemigonusCrysoleucas;
import biosim.core.gui.GUISimulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;

import sim.util.MutableDouble2D;
import sim.util.Double3D;

import java.io.File;
import java.io.IOException;


public class LeaderFish implements Agent{
	AbstractFish fishBody;
	public LeaderFish(AbstractFish b){
		fishBody = b;
	}
	public void init(){
		if(fishBody instanceof NotemigonusCrysoleucas){
			((NotemigonusCrysoleucas)fishBody).setLeader(true);
		}
	}
	public void finish(){
	}
	public void act(double time){
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = fishBody.getNearestObstacleVec(wall);
		double fbSpeed = NotemigonusCrysoleucas.SIZE, turnSpeed = 0.0;
		if(sawWall && wall.length() < 2.5*NotemigonusCrysoleucas.SIZE){
			if(wall.y > 0) turnSpeed = -40*(Math.PI/180.0);
			else turnSpeed = 40*(Math.PI/180.0);
		}
		fishBody.setDesiredVelocity(fbSpeed,0.0,turnSpeed);
	}
	
}
