package biosim.core.sim;

import sim.field.continuous.Continuous2D;
import sim.util.Double2D;
import sim.util.MutableDouble2D;
import java.util.ArrayList;

import biosim.core.body.Body;
import biosim.core.sim.Obstacle;

public class Environment{
	public ArrayList<String> poi;
	public ArrayList<Double2D> poiLocations;
	public ArrayList<Obstacle> obstacles;
	public ArrayList<Double2D> obstacleLocations;
	public ArrayList<Body> initialBodies;
	public ArrayList<Double2D> initialBodyLocations;
	public ArrayList<Double2D> initialBodyOrientations;
	public double width, height;
	
	public Environment(double width, double height){
		poi = new ArrayList<String>();
		poiLocations = new ArrayList<Double2D>();
		obstacles = new ArrayList<Obstacle>();
		obstacleLocations = new ArrayList<Double2D>();
		initialBodies = new ArrayList<Body>();
		initialBodyLocations = new ArrayList<Double2D>();
		initialBodyOrientations = new ArrayList<Double2D>();
		this.width=width;
		this.height=height;
	}
	
	public void addStaticPOI(String name, double x, double y){
		poi.add(name);
		poiLocations.add(new Double2D(x,y));
	}
	
	public void addObstacle(Obstacle o, double x, double y){
		obstacles.add(o);
		obstacleLocations.add(new Double2D(x,y));
	}

	public void addBody(Body b, double x, double y, double t){
		initialBodies.add(b);
		initialBodyLocations.add(new Double2D(x,y));
		initialBodyOrientations.add(new Double2D((new MutableDouble2D(1,0)).rotate(t)));
	}

}
