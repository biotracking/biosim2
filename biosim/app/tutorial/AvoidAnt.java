package biosim.app.tutorial;
import biosim.core.agent.Agent;
import biosim.core.body.AbstractAnt;
import sim.util.MutableDouble2D;
public class AvoidAnt implements Agent {
	AbstractAnt antBody;
	public static double AVOID_RANGE=3.0;
	public double nextAction = -1.0;
	public AvoidAnt(AbstractAnt b){
		antBody = b;
	}
	public void init(){
	}
	public void finish(){
	}
	public void act(double time){
		//our default is to move forward in a straight line
		double forwardSpeed = 0.024; 	//24mm per second straight ahead
		double lateralSpeed = 0.0;	//Ants *can* move laterally, but ours won't for now
		double turningSpeed = 0.0;	//no rotational velocity by default
		//get a vector towards the nearest thing so we can avoid it
		MutableDouble2D ant = new MutableDouble2D();
		boolean sawAnt = antBody.getNearestSameTypeVec(ant);
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = antBody.getNearestObstacleVec(wall);
		MutableDouble2D avoidPoint = null;
		if(sawWall){
			avoidPoint = wall;
		} else if (sawAnt){
			avoidPoint = ant;
		}
		if(avoidPoint != null){
			if(avoidPoint.y > 0) turningSpeed = -40.0*(Math.PI/180.0);
			else turningSpeed = 40.0*(Math.PI/180.0);
		}
		antBody.setDesiredVelocity(forwardSpeed,lateralSpeed,turningSpeed);
	}
}
