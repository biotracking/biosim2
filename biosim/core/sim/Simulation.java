package biosim.core.sim;

import biosim.core.body.Body;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import ec.util.*;
import sim.engine.Steppable;
import sim.util.*;

import java.util.ArrayList;

public class Simulation extends SimState{
	public Environment env;
	public Continuous2D field2D;
	public ArrayList<Body> bodies;
	public ArrayList<Obstacle> obstacles;
	public ArrayList<String> poi;
	public ArrayList<Double2D> bodyOrientations;
	public ArrayList<Logger> loggers;
	public double resolution;

	public Simulation(long seed){
		super(seed);
		bodies = new ArrayList<Body>();
		bodyOrientations = new ArrayList<Double2D>();
		obstacles = new ArrayList<Obstacle>();
		poi = new ArrayList<String>();
		loggers = new ArrayList<Logger>();
		field2D = null;
		env = null;
		resolution = 0.0;
	}
	
	public void addLogger(Logger logger){
		loggers.add(logger);
	}
	
	public void start(){
		super.start();

		bodies.clear();
		bodyOrientations.clear();
		obstacles.clear();
		poi.clear();
		
		env.configSim(this);
		for(int i=0;i<bodies.size();i++){
			schedule.scheduleRepeating(bodies.get(i));
		}
		for(int i=0;i<loggers.size();i++){
			schedule.scheduleRepeating(loggers.get(i),2,1.0);
		}
	}

	public void finish(){
		super.finish();
		for(int i=0;i<bodies.size();i++){
			bodies.get(i).finish();
		}
		/*
		System.out.println("Finished!");
		for(int i=0;i<bodies.size();i++){
			System.out.println("B"+i+":"+field2D.getObjectLocation(bodies.get(i)));
		}
		*/
		for(int i=0;i<loggers.size();i++){
			loggers.get(i).finish();
		}
			
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
