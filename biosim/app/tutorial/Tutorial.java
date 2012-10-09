package biosim.app.tutorial;

import biosim.core.sim.Simulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Obstacle;
import biosim.core.sim.RectObstacle;
import biosim.core.body.Body;
import biosim.core.body.AphaenogasterCockerelli;
import biosim.core.agent.Agent;
import biosim.core.gui.GUISimulation;
public class Tutorial {
	public static final double WIDTH=0.2;
	public static final double HEIGHT=0.2;
	public static void main(String[] args){
		//set up the environment
		int numAnts = 10;
		Environment env = new Environment(WIDTH,HEIGHT);
		Simulation sim = new Simulation(env);
		env.addStaticPOI("nest",WIDTH/2,0.02);
		Obstacle[] walls = new Obstacle[4];
		walls[0] = new RectObstacle(0.01,0.2);//east wall
		env.addObstacle(walls[0], 0.19, 0.0);
		walls[1] = new RectObstacle(0.01,0.2);//west 
		env.addObstacle(walls[1], 0.0, 0.0);
		walls[2] = new RectObstacle(0.2,0.01);//north 
		env.addObstacle(walls[2],0.0,0.0);
		walls[3] = new RectObstacle(0.2,0.01);//south
		env.addObstacle(walls[3],0.0,0.19);
		//add agents
		AphaenogasterCockerelli[] bodies = new AphaenogasterCockerelli[numAnts];
		for(int i=0;i<bodies.length;i++){
			bodies[i] = new AphaenogasterCockerelli();
			env.addBody(
				bodies[i],
				0.02+sim.random.nextDouble()*(WIDTH-0.04), 
				0.02+sim.random.nextDouble()*(HEIGHT-0.04),
				sim.random.nextDouble()*(2*Math.PI));
		}
		Agent[] agents = new Agent[numAnts];
		for(int i=0;i<agents.length;i++){
			agents[i] = new AvoidAnt(bodies[i]);
			bodies[i].setAgent(agents[i]);
		}
		sim.setResolution(1.0/30.0);
		GUISimulation gui = new GUISimulation(sim);
		gui.createController();
		//sim.runSimulation(1000);
	}
}
