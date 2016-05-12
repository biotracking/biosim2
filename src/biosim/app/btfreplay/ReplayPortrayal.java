package biosim.app.btfreplay;

import java.awt.Graphics2D;
import sim.portrayal.DrawInfo2D;
import biosim.core.sim.Simulation;
import biosim.core.body.ReplayBody;

public class ReplayPortrayal extends biosim.app.fishknn.FishPortrayal{
	public ReplayPortrayal(Simulation sim, Object body){
		super(sim,body);
	}
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info){
		if(body instanceof ReplayBody){
			if(!((ReplayBody)body).visible){
				return;
			}
		}
		super.draw(object,graphics,info);
	}
}
