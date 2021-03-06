package biosim.app.fishreynolds;

import sim.util.MutableDouble2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.Oriented2D;
import sim.portrayal.DrawInfo2D;

import biosim.core.sim.Simulation;
import biosim.core.body.Body;
import biosim.core.body.NotemigonusCrysoleucas;
import biosim.core.body.ReplayFish;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import java.net.URL;

import javax.imageio.ImageIO;

public class ReplayPortrayal extends OvalPortrayal2D implements Oriented2D{
	public Simulation sim;
	public Body body;

	public ReplayPortrayal(Simulation sim, Object body){
		super(Color.black,((Body)body).getSize(),false);
		this.sim = sim;
		if(body instanceof Body){
			this.body = (Body)body;
		} else {
			throw new RuntimeException("Cannot use a ReplayPortrayal for this object:"+body.getClass());
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
		if(object instanceof ReplayFish && ((ReplayFish)object).visible==false) return;
		double width = info.draw.width*body.getSize();//0.008;
		double height = info.draw.height*body.getSize();//0.008;
		int x = (int)(info.draw.x);// - width/2.0);
		int y = (int)(info.draw.y);// - height/2.0);
		int w = (int)(width);
		int h = (int)(height/3.0);
		double d = orientation2D();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		AffineTransform prevTrans = graphics.getTransform();
		graphics.translate(x,y);
		graphics.rotate(d);
		graphics.setColor(Color.gray);
		graphics.fillOval(-w/2,-h/2,w,h);
		graphics.setColor(Color.red);
		graphics.drawLine(0, 0, w/2,0);
		graphics.setTransform(prevTrans);
	}
}
