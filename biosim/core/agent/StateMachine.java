package biosim.core.agent;
import biosim.core.body.Body;


public abstract class StateMachine implements Agent {
	public interface State {
		int act(double time);
	}

	protected State[] states;
	public int nextState;
	public void act(double time){
		nextState = states[nextState].act(time);
	}
}