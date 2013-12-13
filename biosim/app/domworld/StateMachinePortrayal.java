package biosim.app.domworld;

import sim.util.MutableDouble2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.Oriented2D;
import sim.portrayal.DrawInfo2D;

import biosim.core.sim.Simulation;
import biosim.core.body.Body;
import biosim.core.body.actuators.TextDisplay;
import biosim.core.gui.portrayals.BasicBodyPortrayal;

import java.awt.Color;
import java.awt.Graphics2D;

public class StateMachinePortrayal extends BasicBodyPortrayal{
	//public static final boolean DEBUG=true;
	public StateMachinePortrayal(Simulation sim, Object body){
		super(sim,body);
		if(!(body instanceof TextDisplay)){
			throw new RuntimeException("Cannot use a StateMachinePortrayal for this object:"+body.getClass());
		}
	}
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info){
		super.draw(object,graphics,info);
		//if(DEBUG) return;
		graphics.setColor(Color.blue);
		double xPos = info.draw.x - body.getSize()*info.draw.width/2.0;
		double yPos = info.draw.y - body.getSize()*info.draw.height/2.0;
		graphics.drawString(((TextDisplay)body).getTextDisplay(),(int)xPos,(int)yPos);
		//System.out.println("STATE:"+((TextDisplay)body).getTextDisplay());
	}
}
