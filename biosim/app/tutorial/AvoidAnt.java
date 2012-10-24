package biosim.app.tutorial;
import biosim.core.agent.Agent;
import biosim.core.body.Body;
import biosim.core.body.AbstractAnt;
import sim.util.MutableDouble2D;
public class AvoidAnt implements Agent {
	AbstractAnt antBody;
	public AvoidAnt(AbstractAnt b){
		antBody = b;
	}
	public double[] act(double time){
		double[] rv = new double[3];
		//our default is to move forward in a straight line
		rv[0] = 0.024; 	//24mm per second straight ahead
		rv[1] = 0.0;	//Ants *can* move laterally, but ours won't for now
		rv[2] = 0.0;	//no rotational velocity by default
		//get a vector towards the nearest thing so we can avoid it
		MutableDouble2D ant = new MutableDouble2D();
		boolean sawAnt = antBody.getNearestSameAgentVec(ant);
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = antBody.getNearestObstacleVec(wall);
		MutableDouble2D avoidPoint = null;
		if(sawWall && sawAnt){
			avoidPoint = (ant.lengthSq() >= wall.lengthSq())?wall:ant;
		} else if(sawWall){
			avoidPoint = wall;
		} else if(sawAnt){
			avoidPoint = ant;
		} 
		if(avoidPoint != null){
			if(avoidPoint.y > 0) rv[2] = -40.0*(Math.PI/180.0);
			else rv[2] = 40.0*(Math.PI/180.0);
		}
		return rv;
	}
}
