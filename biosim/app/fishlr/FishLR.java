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


public class FishLR implements Agent{
	AbstractFish fishBody;
	public static final double[] FB_BETA = {0.00831176, -0.0259669, 
											-0.00091077, 0.00584585};//{0.0087933, -0.03805837};
	public static final double[] LR_BETA = {-0.06239644, 0.10435975, 
											-0.10492971,  0.10604713};//{0.00396506, -0.11737089};
	public FishLR(AbstractFish b){
		fishBody = b;
	}
	public void init(){
	}
	public void finish(){
	}
	public double[] act(double time){
		double[] rv = new double[3];
		MutableDouble2D avgFish = new MutableDouble2D();
		MutableDouble2D nnFish = new MutableDouble2D();
		boolean sawFish = fishBody.getAverageSameAgentVec(avgFish);
		fishBody.getAverageSameAgentVec(nnFish);
		double[] sensors = new double[FB_BETA.length];
		sensors[0] = avgFish.x;
		sensors[1] = avgFish.y;
		sensors[2] = nnFish.x;
		sensors[3] = nnFish.y;
		double fbSpeed = 0.0, turnSpeed = 0.0;
		for(int i=0;i<sensors.length;i++){
			fbSpeed+= FB_BETA[i]*sensors[i];
			turnSpeed += LR_BETA[i]*sensors[i];
		}
		rv[0] = fbSpeed;
		rv[1] = 0.0;
		rv[2] = turnSpeed;
		return rv;
	}
	
	public static final double WIDTH=2.5;//2.5;
	public static final double HEIGHT=1.5;//2.5;
	
	public static void main(String[] args){
		//set up the environment
		int numFish = 2;//30;
		Environment env = new Environment(WIDTH,HEIGHT,1.0/30.0);
		/* */
		env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
		env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
		env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
		env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
		/**/
		env.setToroidal(true);
		//add agents
		NotemigonusCrysoleucas[] bodies = new NotemigonusCrysoleucas[numFish];
		for(int i=0;i<bodies.length;i++){
			bodies[i] = new NotemigonusCrysoleucas();
			env.addBody(bodies[i]);
		}
	
		Agent[] agents = new Agent[numFish];
		for(int i=0;i<agents.length;i++){
			agents[i] = new FishLR(bodies[i]);
			bodies[i].setAgent(agents[i]);
		}
		
		
		//env.runSimulation(args);
		Simulation sim = env.newSimulation();
		GUISimulation gui = new GUISimulation(sim);
		gui.setDisplaySize((int)(WIDTH*500),(int)(HEIGHT*500));
		gui.createController();
	}
}
