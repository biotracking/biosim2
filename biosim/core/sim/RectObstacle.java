package biosim.core.sim;

import sim.util.Double2D;

public class RectObstacle implements Obstacle {
	public double width, height;
	public RectObstacle(double width, double height){
		this.width = width; this.height = height;
	}
	public Double2D closestPoint(Double2D p, Double2D o){
		if(p.x < o.x && p.y < o.y){	//top left corner
			return o; 
		} else if(p.x < o.x && p.y > o.y+height){ //bottom left corner
			return new Double2D(o.x,o.y+height); 
		} else if(p.x > o.x+width && p.y < o.y){ //top right corner
			return new Double2D(o.x+width,o.y); 
		} else if(p.x > o.x+width && p.y > o.y+height){ //bottom right corner
			return new Double2D(o.x+width,o.y+height); 
		} else if(p.x <= o.x && p.y >= o.y && p.y <= o.y+height){ //left side
			return new Double2D(o.x,p.y);
		} else if(p.x >= o.x+width && p.y >= o.y && p.y <= o.y+height){ //right side
			return new Double2D(o.x+width,p.y);
		} else if(p.y <= o.y && p.x >= o.x && p.x <= o.x+width){ //top
			return new Double2D(p.x,o.y);
		} else if(p.y >= o.y+height && p.x >= o.x && p.x <= o.x+width){ //bottom
			return new Double2D(p.x,o.y+height);
		} else { //inside!
			return p;
		}
	}
}
