package biosim.app.domworld;

import java.util.HashMap;

import ec.util.MersenneTwisterFast;

import biosim.core.sim.Simulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Obstacle;
import biosim.core.sim.RectObstacle;
import biosim.core.body.Body;
import biosim.core.body.RhesusMacaque;
import biosim.core.agent.Agent;
import biosim.core.gui.GUISimulation;

public class SmallDomWorld {
	public static final double HEIGHT=3.0;
	public static final double WIDTH=3.0;

	public static double[][] randomTieStrength(MersenneTwisterFast rnd, int n){
		double[][] rv = new double[n][n];
		for(int i=0;i<n;i++){
			for(int j=i;j<n;j++){
				rv[i][j] = rnd.nextDouble();
				rv[j][i] = rv[i][j];
			}
		}
		return rv;
	}

	public static void main(String[] args){
		int numMonkeys = 5;
		DomWorldStateMachine.PERSONAL_DIST=0.5; //within arms reach
		DomWorldStateMachine.NEAR_DIST=3.0; //closer than corner-to-corner
		DomWorldStateMachine.MIN_OTHERS=2; //3 is more than half the monkeys
		Environment env = new Environment(WIDTH,HEIGHT,1.0/30.0);
		env.setToroidal(true);
		env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
		env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
		env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
		env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
		env.setToroidal(true);
		RhesusMacaque[] bodies = new RhesusMacaque[numMonkeys];
		for(int i=0;i<bodies.length;i++){
			bodies[i] = new RhesusMacaque();
			env.addBody(bodies[i]);
		}
		Simulation sim = env.newSimulation();
		Agent[] agents = new Agent[bodies.length];
		double[][] tieStrengths = randomTieStrength(sim.random,numMonkeys);
		for(int i=0;i<agents.length;i++){
			agents[i] = new DomWorldStateMachine(bodies[i],sim.random.nextDouble());
			//agents[i] = new DomWorldMonkey(bodies[i],sim.random.nextDouble());
			bodies[i].setAgent(agents[i]);
		}
		for(int i=0;i<agents.length;i++){
			HashMap<DomWorldStateMachine,Double> tsprefs = new HashMap<DomWorldStateMachine,Double>();
			for(int j=0;j<numMonkeys;j++){
				tsprefs.put((DomWorldStateMachine)agents[j],tieStrengths[i][j]);
			}
			((DomWorldStateMachine)agents[i]).setTieStrengths(tsprefs);
		}
		GUISimulation gui = new GUISimulation(sim);
		gui.createController();
	}
}