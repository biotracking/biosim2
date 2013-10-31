package biosim.app.domworld;

import biosim.core.sim.Simulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Obstacle;
import biosim.core.sim.RectObstacle;
import biosim.core.body.Body;
import biosim.core.body.RhesusMacaque;
import biosim.core.agent.Agent;
import biosim.core.gui.GUISimulation;

public class DomWorld {
	public static final double HEIGHT=20.0;
	public static final double WIDTH=20.0;

	public static void main(String[] args){
		int numMonkeys = 30;
		Environment env = new Environment(WIDTH,HEIGHT,1.0/30.0);
		RhesusMacaque[] bodies = new RhesusMacaque[numMonkeys];
		for(int i=0;i<bodies.length;i++){
			bodies[i] = new RhesusMacaque();
			env.addBody(bodies[i]);
		}
		Simulation sim = env.newSimulation();
		Agent[] agents = new Agent[bodies.length];
		for(int i=0;i<agents.length;i++){
			agents[i] = new DomWorldMonkey(bodies[i],sim.random.nextDouble());
			bodies[i].setAgent(agents[i]);
		}
		GUISimulation gui = new GUISimulation(sim);
		gui.createController();
	}
}