package biosim.app.tutorial;
import biosim.core.agent.Agent;
import biosim.core.body.Body;
import biosim.core.body.AphaenogasterCockerelli;
public class AvoidAnt extends Agent {
	public AvoidAnt(Body b){
		super(b);
	}
	public void act(){
		AphaenogasterCockerelli antBody = (AphaenogasterCockerelli)body;
	}
}
