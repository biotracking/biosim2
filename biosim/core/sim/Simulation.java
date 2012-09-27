package biosim.core.sim;

import biosim.core.body.Body;
import sim.engine.SimState;
import ec.util.*;

import java.util.ArrayList;

public class Simulation extends SimState{
	public Environment env;
	public Continuous2D field2D;
	public ArrayList<Body> bodies;
	public ArrayList<Double2D> bodyOrientations;
	public double resolution;	

	public Simulation(String[] args){
		super(System.currentTimeMillis());
		bodies = new ArrayList<Body>();
		bodyOrientations = new ArrayList<Double2D>();
		steps = 0;
	}

	public void setEnvironment(Environment e){
		env = e;
	}

	public void start(){
		super.start();
		bodies.clear();
		bodyOrientations.clear();
		field2D = new Continuous2D(Math.min(env.width,env.height)/100,env.width,env.height);
		for(int i=0;i<env.obstacles.size();i++){
			field2D.setObjectLocation(env.obstacles.get(i),env.obstacleLocations.get(i));
		}
		for(int i=0;i<env.poi.size();i++){
			field2D.setObjectLocation(env.poi.get(i),env.poiLocations.get(i));
		}
		for(int i=0;i<env.initialBodies.size();i++){
			field2D.setObjectLocation(initialBodies.get(i),initialBodyLocations.get(i));
			bodies.add(initialBodies.get(i));
			bodyOrientations.add(initialBodyOrientations.get(i));
		}
		for(int i=0;i<bodies.size();i++){
			schedule.scheduleRepeating(bodies.get(i));
		}
		steps = 0;
	}
	public void runSimulation(long steps){
		start();
		int step;
		do {
			if(!schedule.step(this)) break;
			step = schedule.getSteps();
		} while(step < steps);
		finish();
	}
	
	public void initializeRandomWithSeed(long seed){
		setRandom(new MersenneTwisterFast(seed));
	}
	
	public void setResolution(double secondsPerStep){
		resolution = secondsPerStep;
	}
}
