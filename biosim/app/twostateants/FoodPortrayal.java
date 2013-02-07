package biosim.app.twostateants;

import sim.util.MutableDouble2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.Oriented2D;
import sim.portrayal.DrawInfo2D;

import biosim.core.sim.Simulation;
import biosim.core.body.Body;

import java.awt.Color;
import java.awt.Graphics2D;

public class FoodPortrayal extends OvalPortrayal2D implements Oriented2D{
	public Simulation sim;
	public Body body;
	public FoodPortrayal(Simulation sim, Object body){
		super(Color.yellow,0.0025,true);
		this.sim = sim;
		if(body instanceof Body){
			this.body = (Body)body;
		} else {
			throw new RuntimeException("Cannot use a FoodPortrayal for this object:"+body.getClass());
		}
	}
	public double orientation2D(){
		MutableDouble2D dir = new MutableDouble2D();
		if(sim.getBodyOrientation(body,dir)){
			return dir.angle();
		} else {
			throw new RuntimeException("sim.getBodyOrientation(...) failed. Sim:["+sim+"], Body:["+body+"]");
		}
	}
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info){
		super.draw(object,graphics,info);
		double width = info.draw.width*body.getSize();//0.008;
		double height = info.draw.height*body.getSize();//0.008;
		graphics.setColor(Color.red);
		double d = orientation2D();
		graphics.drawLine((int)info.draw.x, (int)info.draw.y,
			(int)(info.draw.x)+(int)(width/2 * Math.cos(d)),
			(int)(info.draw.y)+(int)(height/2* Math.sin(d)));
	}
}
