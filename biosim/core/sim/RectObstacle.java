package biosim.core.sim;

import sim.util.Double2D;

public class RectObstacle implements Obstacle {
	public double width, height;
	public RectObstacle(double width, double height){
		this.width = width; this.height = height;
	}
	public boolean collides(Double2D p, Double2D o, double radius){
		if(p.x < o.x && p.y < o.y){	//top left corner
			return (p.distanceSq(o) <= Math.pow(radius,2));
		} else if(p.x < o.x && p.y > o.y+height){ //bottom left corner
			return (p.distanceSq(new Double2D(o.x,o.y+height)) <= Math.pow(radius,2));
		} else if(p.x > o.x+width && p.y < o.y){ //top right corner
			return (p.distanceSq(new Double2D(o.x+width,o.y)) <= Math.pow(radius,2));
		} else if(p.x > o.x+widht && p.y > o.y+height){ //bottom right corner
			return (p.distanceSq(new Double2D(o.x+width,o.y+height)) <= Math.pow(radius,2));
		} else if(p.x <= o.x && p.y >= o.y && p.y <= o.y+height){ //left side
			return o.x-p.x <= radius;
		} else if(p.x >= o.x+width && p.y >= o.y && p.y <= o.y+height){ //right side
			return p.x-(o.x+width) <= radius;
		} else if(p.y <= o.y && p.x >= o.x && p.x <= o.x+width){ //top
			return o.y-p.y <= radius;
		} else if(p.y >= o.y+height && p.x >= o.x && p.x <= o.x+width){ //bottom
			return p.y-(o.y+height) <= radius;
		} else { //inside!
			return (p.x>=o.x && p.y >= o.y && p.x <= (o.x+width) && p.y <= (o.y+height));
		}
	}
}
