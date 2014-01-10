package biosim.app.tutorial;

import biosim.core.sim.Simulation;
import biosim.core.sim.Environment;
import biosim.core.sim.RectObstacle;
import biosim.core.body.AphaenogasterCockerelli;
import biosim.core.gui.GUISimulation;
public class Tutorial {
	public static final double WIDTH=0.2;
	public static final double HEIGHT=0.2;
	public static void main(String[] args){
		//set up the environment
		int numAnts = 10;
		Environment env = new Environment(WIDTH,HEIGHT,1.0/30.0);
		env.addStaticPOI("nest",WIDTH/2,0.02);
		env.addObstacle(new RectObstacle(0.01,0.2), 0.19,  0.0);//east wall
		env.addObstacle(new RectObstacle(0.01,0.2),  0.0,  0.0);//west
		env.addObstacle(new RectObstacle(0.2,0.01),  0.0,  0.0);//north
		env.addObstacle(new RectObstacle(0.2,0.01),  0.0, 0.19);//south
		//Create bodies
		AphaenogasterCockerelli[] bodies = new AphaenogasterCockerelli[numAnts];
		for(int i=0;i<bodies.length;i++){
			bodies[i] = new AphaenogasterCockerelli();
			env.addBody(bodies[i]);
		}
		//Add link agents to bodies
		AvoidAnt[] agents = new AvoidAnt[numAnts];
		for(int i=0;i<agents.length;i++){
			agents[i] = new AvoidAnt(bodies[i]);
			bodies[i].setAgent(agents[i]);
		}
		//Create new simulation and turn on logging
		Simulation sim = env.newSimulation();
		//sim.addLogger(new AvoidAntLogger());
		GUISimulation gui = new GUISimulation(sim);
		//display the MASON console
		gui.createController();
	}
}
