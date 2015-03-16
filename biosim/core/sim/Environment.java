package biosim.core.sim;

import sim.engine.MakesSimState;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

import java.util.ArrayList;

import biosim.core.body.Body;
import biosim.core.sim.Obstacle;

/**
 * Environment is a helper class for setting up the initial state of
 * a Simulation before it is run.
 * An example of typical usage is given in biosim.app.tutorial.Tutorial.
 *
 * @author Brian Hrolenok
 **/
public class Environment implements MakesSimState{
	public ArrayList<Obstacle> obstacles;
	public ArrayList<Double2D> obstacleLocations;
	public ArrayList<Body> initialBodies;
	public ArrayList<String> poi;
	public ArrayList<Double2D> poiLocations;
	public ArrayList<Logger> initialLoggers;
	public double width, height, resolution;
	double fieldDiscretizationSize;
	boolean toroidal=false;
	
	public Environment(double width, double height,double resolution){
		poi = new ArrayList<String>();
		poiLocations = new ArrayList<Double2D>();
		obstacles = new ArrayList<Obstacle>();
		obstacleLocations = new ArrayList<Double2D>();
		initialBodies = new ArrayList<Body>();
		initialLoggers = new ArrayList<Logger>();
		this.width=width;
		this.height=height;
		this.resolution = resolution;
		fieldDiscretizationSize = 0.0;
	}

	public void addLogger(Logger logger){
		initialLoggers.add(logger);
	}

	public void setFieldDiscretizationSize(double d){ fieldDiscretizationSize = d; }
	public void setToroidal(boolean t){ toroidal = t; }
	
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
		addLoggersToSim(sim);
		return sim;
	}

	public Class simulationClass(){ return Simulation.class; }
	
	public Simulation newSimulation(){
		return newSimulation(System.currentTimeMillis());
	}
	/**
	 * Convenience method for generating Simulation objects.
	 * Used primarily to generate simulation objects to the
	 * GUISimulation class. Use runSimulation(...) for non-gui
	 * runs.
	 */
	public Simulation newSimulation(long seed){
		Simulation sim = new Simulation(seed);
		sim.env = this;
		configSim(sim);
		addLoggersToSim(sim);
		return sim;
	}

	protected void addObjectsToSim(Simulation sim){
		for(int i=0;i<obstacles.size();i++){
			sim.obstacles.add(obstacles.get(i));
		}
		for(int i=0;i<poi.size();i++){
			sim.poi.add(poi.get(i));
		}
		for(int i=0;i<initialBodies.size();i++){
			sim.bodies.add(initialBodies.get(i));
			initialBodies.get(i).init();
		}
	
	}

	protected void addLoggersToSim(Simulation sim){
		for(int i=0;i<initialLoggers.size();i++){
			sim.addLogger(initialLoggers.get(i));
		}
	}

	/**
	 * Method which initializes the Simulation object's internal field2D. A Simulation 
	 * object does not initalize it's internal field2D object in its constructor 
	 * because it may not have access to an Environment object until after it has been
	 * created. By default Obstacle and POI objects are initialized at fixed locations
	 * and Body objects are initialized to random locations within the field2D that
	 * do not collide with any obstacle. Users are encouraged to override this method
	 * for more control over how object locations are initialized. 
	 */
	protected void initSimField(Simulation sim){
		for(int i=0;i<obstacles.size();i++){
			Double2D tmp = obstacleLocations.get(i);
			sim.setObjectLocation(obstacles.get(i),tmp);
		}
		for(int i=0;i<poi.size();i++){
			sim.setObjectLocation(poi.get(i),poiLocations.get(i));
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
			sim.setObjectLocation(b,tmp);
			tmp = new Double2D(sim.random.nextDouble(),sim.random.nextDouble()).normalize();
			sim.bodyOrientations.add(tmp);
		}
	
	}
	
	/**
	 * Method which sets up a simulation according to the configuration
	 * modeled by this class. This method is called by newSimulation()
	 * and runSimulation() on newly created Simulation objects, and by
	 * Simulation.start(). It in turn calls addObjectsToSim(...) to
	 * add Body, Obstacle, and POI objects to the Simulation object, and
	 * then initSimField(sim) to add these objects to its field2D.
	 * Users who want control over where Body objects are initally
	 * placed should override the initSimField method in a subclass.
	 */
	protected void configSim(Simulation sim){
		sim.resolution=resolution;
		sim.toroidal = toroidal;
		sim.field2D = new Continuous2D(fieldDiscretizationSize,width,height);
		addObjectsToSim(sim);
		initSimField(sim);
		/*
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
		*/
	}
	
	public void runSimulation(String[] args){
		Simulation.doLoop(this,args);
	}
}
