package biosim.core.agent;
import biosim.core.body.Body;
public abstract class Agent{
	protected Body body;
	public Agent(Body b){
		body = b;
	}
	public abstract void act(long step);
}
