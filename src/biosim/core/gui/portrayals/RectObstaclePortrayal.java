package biosim.core.gui.portrayals;

import sim.portrayal.DrawInfo2D;

import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;

import java.awt.Color;
import java.awt.Graphics2D;

public class RectObstaclePortrayal extends sim.portrayal.SimplePortrayal2D {
	public RectObstacle obst;
	public RectObstaclePortrayal(RectObstacle o){
		super();
		obst = o;
	}
	
	public void draw(Object obj, Graphics2D graphics, DrawInfo2D info){
		super.draw(obj,graphics,info);
		double width = info.draw.width*obst.width;
		double height = info.draw.height*obst.height;
		graphics.setColor(new Color(200,200,200));
		graphics.fillRect((int)info.draw.x, (int)info.draw.y,(int)width,(int)height);
	}
}
