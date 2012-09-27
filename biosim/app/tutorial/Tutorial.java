package biosim.app.tutorial;

import biosim.core.sim.Simulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Obstacle;
import biosim.core.sim.RectObstacle;
import biosim.core.body.Body;
import biosim.core.body.AphaenogasterCockerelli;
import biosim.core.agent.Agent;
public class Tutorial {
	public static final double WIDTH=0.1;
	public static final double HEIGHT=0.1;
	public static void main(String[] args){
		Simulation sim = new Simulation(args);
		//set up the environment
		int numAnts = 10;
		Environment env = new Environment(WIDTH,HEIGHT);
		env.addStaticPOI("nest",WIDTH/2,0.01);
		Obstacle[] walls = new Obstacle[4];
		walls[0] = new RectObstacle(0.01,0.1);//east wall
		env.addObstacle(walls[0], 0.09, 0.0);
		walls[1] = new RectObstacle(0.01,0.1);//west 
		env.addObstacle(walls[1], 0.0, 0.0);
		walls[2] = new RectObstacle(0.1,0.01);//north 
		env.addObstacle(walls[2],0.0,0.0);
		walls[3] = new RectObstacle(0.1,0.01);//south
		env.addObstacle(walls[3],0.0,0.09);
		//add agents
		Body[] bodies = new Body[numAnts];
		for(int i=0;i<bodies.length;i++){
			bodies[i] = new AphaenogasterCockerelli();
		}
		Agent[] agents = new Agent[numAnts];
		for(int i=0;i<agents.length;i++){
			agents[i] = new AvoidAnt(bodies[i]);
			bodies[i].setAgent(agents[i]);
			sim.addBody(bodies[i]);
		}
		sim.setEnvironment(env);
		sim.runSimulation(1000);
	}
}
