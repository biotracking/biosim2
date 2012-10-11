package biosim.core.sim;

import biosim.core.body.Body;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import ec.util.*;
import sim.util.*;

import java.util.ArrayList;

public class Simulation extends SimState{
	public Environment env;
	public Continuous2D field2D;
	public ArrayList<Body> bodies;
	public ArrayList<Obstacle> obstacles;
	public ArrayList<String> poi;
	public ArrayList<Double2D> bodyOrientations;
	public double resolution;	

	/*
	public Simulation(Environment e){
		super(System.currentTimeMillis());
		bodies = new ArrayList<Body>();
		bodyOrientations = new ArrayList<Double2D>();
		env = e;
		field2D = new Continuous2D(1,env.width,env.height);
	}
	*/
	public Simulation(long seed){
		super(seed);
		bodies = new ArrayList<Body>();
		bodyOrientations = new ArrayList<Double2D>();
		obstacles = new ArrayList<Obstacle>();
		poi = new ArrayList<String>();
		field2D = null;
		env = null;
		resolution = 0.0;
	}
	
	public void start(){
		super.start();
		//System.out.println("Starting!");
		/*
		bodies.clear();
		bodyOrientations.clear();
		field2D = new Continuous2D(fieldDiscretization,env.width, env.height);
		for(int i=0;i<env.obstacles.size();i++){
			Double2D tmp = env.obstacleLocations.get(env.obstacles.get(i));
			if(tmp == null){
				tmp = new Double2D(
			}
			field2D.setObjectLocation(env.obstacles.get(i),env.obstacleLocations.get(i));
		}
		for(int i=0;i<env.poi.size();i++){
			field2D.setObjectLocation(env.poi.get(i),env.poiLocations.get(i));
		}
		for(int i=0;i<env.initialBodies.size();i++){
			field2D.setObjectLocation(env.initialBodies.get(i),env.initialBodyLocations.get(i));
			bodies.add(env.initialBodies.get(i));
			bodyOrientations.add(env.initialBodyOrientations.get(i));
			//System.out.println("B"+i+":"+field2D.getObjectLocation(bodies.get(i)));
		}
		*/
		env.configSim(this);
		for(int i=0;i<bodies.size();i++){
			schedule.scheduleRepeating(bodies.get(i));
		}
	}

	public void finish(){
		super.finish();
		/*
		System.out.println("Finished!");
		for(int i=0;i<bodies.size();i++){
			System.out.println("B"+i+":"+field2D.getObjectLocation(bodies.get(i)));
		}
		*/
	}
	
	public boolean getBodyOrientation(Body b,MutableDouble2D rv){
		for(int i=0;i<bodies.size();i++){
			if(bodies.get(i) == b){
				Double2D dir = bodyOrientations.get(i);
				rv.x = dir.x;
				rv.y = dir.y;
				return true;
			}
		}
		return false;
	}
}
