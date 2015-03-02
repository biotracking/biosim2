package biosim.app.fishknn;

import sim.util.MutableDouble2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.Oriented2D;
import sim.portrayal.DrawInfo2D;

import biosim.core.sim.Simulation;
import biosim.core.body.Body;
import biosim.core.body.NotemigonusCrysoleucas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import java.net.URL;

import javax.imageio.ImageIO;

public class FishPortrayal extends OvalPortrayal2D implements Oriented2D{
	public Simulation sim;
	public Body body;
	public static BufferedImage bi;
	public static double AVG_DIST = -1.0;
	public static double STD_DEV_DIST = -1.0;
	static {
		URL icnLoc = FishPortrayal.class.getResource("fishicn.png");
		if(icnLoc != null){
			try{
				bi = ImageIO.read(icnLoc);
				Graphics2D g2 = bi.createGraphics();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.dispose();
			} catch(Exception e){
				System.out.println("Couldn't load fish icon:"+e);
				bi = null;
			}
		} else {
			bi = null;
		}
	}
	public FishPortrayal(Simulation sim, Object body){
		super(Color.black,((Body)body).getSize(),false);
		this.sim = sim;
		if(body instanceof Body){
			this.body = (Body)body;
		} else {
			throw new RuntimeException("Cannot use a FishPortrayal for this object:"+body.getClass());
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
		double width = info.draw.width*body.getSize();//0.008;
		double height = info.draw.height*body.getSize();//0.008;
		int x = (int)(info.draw.x);// - width/2.0);
		int y = (int)(info.draw.y);// - height/2.0);
		int w = (int)(width);
		int h = (int)(height/3.0);
		double d = orientation2D();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if(bi == null){
			AffineTransform prevTrans = graphics.getTransform();
			graphics.translate(x,y);
			graphics.rotate(d);
			graphics.setColor(Color.black);
			graphics.fillOval(-w/2,-h/2,w,h);
			graphics.setColor(Color.red);
			graphics.drawLine(0, 0, w/2,0);
			graphics.setTransform(prevTrans);
		} else {
			AffineTransform imgTrans = new AffineTransform();
			imgTrans.translate(x,y);
			imgTrans.rotate(d);
			imgTrans.translate(-width/2.0,-height/6.0);
			imgTrans.scale(width/bi.getWidth(),height/(bi.getHeight()*3));
			graphics.drawImage(bi,imgTrans,null);
		}
		if( body instanceof NotemigonusCrysoleucas){
			if(((NotemigonusCrysoleucas)body).isLeader()){
				graphics.setColor(Color.blue);
				graphics.fillOval(x-(int)(width/8),y-(int)(height/8),(int)(width/4),(int)(height/4));
			}  else if(((NotemigonusCrysoleucas)body).getAvgDensity()>=0){
				Color c;
				double avgDist = ((NotemigonusCrysoleucas)body).getAvgDensity();
				// double howGreen = Math.exp(-((NotemigonusCrysoleucas)body).getAvgDensity());
				// c = new Color((float)(1.0f-howGreen), (float)howGreen, 0.0f)
				if(avgDist < AVG_DIST){
					float howGreen = (float)Math.max(0.0, (avgDist-(AVG_DIST-STD_DEV_DIST))/(STD_DEV_DIST));
					c = new Color(1.0f-howGreen, 1.0f, 1.0f-howGreen);
				} else {
					double max = AVG_DIST+STD_DEV_DIST;
					float howRed = (float)Math.min(1.0,(avgDist - AVG_DIST)/(STD_DEV_DIST));
					c = new Color(1.0f,1.0f-howRed,1.0f-howRed);
				}
				graphics.setColor(c);
				graphics.fillOval(x-(int)(width/8),y-(int)(height/8),(int)(width/4),(int)(height/4));
			}
		}
	}
}
