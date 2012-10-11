package biosim.core.sim;

import sim.engine.MakesSimState;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

import java.util.ArrayList;

import biosim.core.body.Body;
import biosim.core.sim.Obstacle;

public class Environment implements MakesSimState{
	public ArrayList<Obstacle> obstacles;
	public ArrayList<Double2D> obstacleLocations;
	public ArrayList<Body> initialBodies;
	public ArrayList<String> poi;
	public ArrayList<Double2D> poiLocations;
	public double width, height, resolution;
	double fieldDiscretizationSize;
	
	public Environment(double width, double height,double resolution){
		poi = new ArrayList<String>();
		poiLocations = new ArrayList<Double2D>();
		obstacles = new ArrayList<Obstacle>();
		obstacleLocations = new ArrayList<Double2D>();
		initialBodies = new ArrayList<Body>();
		this.width=width;
		this.height=height;
		this.resolution = resolution;
		fieldDiscretizationSize = 0.0;
	}

	public void setFieldDiscretizationSize(double d){ fieldDiscretizationSize = d; }
	
	public void addStaticPOI(String name, double x, double y){
		poi.add(name);
		poiLocations.add(new Double2D(x,y));
	}
	
	public void addObstacle(Obstacle o, double x, double y){
		obstacles.add(o);
		obstacleLocations.add(new Double2D(x,y));
	}

	public void addBody(Body b){
		initialBodies.add(b);
	}

	public SimState newInstance(long seed,String[] args){
		Simulation sim = new Simulation(seed);
		sim.env = this;
		configSim(sim);
		return sim;
	}
	public Class simulationClass(){ return Simulation.class; }
	
	/**
	 * Convenience method for generating Simulation objects.
	 * Used primarily to generate simulation objects to the
	 * GUISimulation class. Use runSimulation(...) for non-gui
	 * runs.
	 */
	public Simulation newSimulation(){
		Simulation sim = new Simulation(System.currentTimeMillis());
		sim.env = this;
		configSim(sim);
		return sim;
	}

	protected void configSim(Simulation sim){
		sim.resolution=resolution;
		sim.bodies.clear();
		sim.bodyOrientations.clear();
		sim.obstacles.clear();
		sim.poi.clear();
		sim.field2D = new Continuous2D(fieldDiscretizationSize,width,height);
		for(int i=0;i<obstacles.size();i++){
			Double2D tmp = obstacleLocations.get(i);
			sim.field2D.setObjectLocation(obstacles.get(i),tmp);
			sim.obstacles.add(obstacles.get(i));
		}
		for(int i=0;i<poi.size();i++){
			sim.field2D.setObjectLocation(poi.get(i),poiLocations.get(i));
			sim.poi.add(poi.get(i));
		}
		for(int i=0;i<initialBodies.size();i++){
			Double2D tmp;
			Body b = initialBodies.get(i);
			boolean collides;
			do{
				tmp = new Double2D(sim.random.nextDouble()*width, sim.random.nextDouble()*height);
				collides = false;
				for(int j=0;j<obstacles.size();j++){
					Double2D objLoc = obstacleLocations.get(j);
					if(tmp.distance(obstacles.get(j).closestPoint(tmp,objLoc)) < b.getSize()){
						collides = true;
						break;
					}
				}
			} while(collides);
			sim.field2D.setObjectLocation(b,tmp);
			sim.bodies.add(b);
			tmp = new Double2D(sim.random.nextDouble(),sim.random.nextDouble()).normalize();
			sim.bodyOrientations.add(tmp);
		}
	}
	
	public void runSimulation(String[] args){
		Simulation.doLoop(this,args);
	}
}
