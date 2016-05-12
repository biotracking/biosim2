package biosim.core.sim;

import sim.util.Double2D;
import sim.field.continuous.Continuous2D;

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

	public Double2D toroidalClosestPoint(Double2D p, Double2D o, Continuous2D field){
		Double2D rv;
		Double2D[] ptsToCheck = new Double2D[8];
		double tmpDS, distSq = -1.0;
		Double2D wrappedP = new Double2D(field.tx(p.x),field.ty(p.y));
		//o := the top left corner of where the obstacle is in the field
		//9 cases
		//top left
		ptsToCheck[0] = o;
		//top right
		ptsToCheck[1] = o.add(new Double2D(this.width,0));
		//bottom right
		ptsToCheck[2] = o.add(new Double2D(this.width,this.height));
		//bottom left
		ptsToCheck[3] = o.add(new Double2D(0,this.height));
		//right-left
		if( (field.ty(o.y+height)>wrappedP.y) && ((field.ty(o.y)<wrappedP.y) || (field.ty(o.y+height)<(o.y+height))) ){
			ptsToCheck[4] = new Double2D(o.x+width,wrappedP.y);
			ptsToCheck[5] = new Double2D(o.x,wrappedP.y);
		} else {
			ptsToCheck[4] = ptsToCheck[5] = null;
		}
		//top-bottom
		if( (field.tx(o.x+width)>wrappedP.x) && ((field.tx(o.x)<wrappedP.x) || (field.tx(o.x+width)<(o.x+width))) ){
			ptsToCheck[6] = new Double2D(wrappedP.x,o.y+height);
			ptsToCheck[7] = new Double2D(wrappedP.x,o.y);
		} else {
			ptsToCheck[6] = ptsToCheck[7] = null;
		}
		//inside
		if( (ptsToCheck[4] != null) && (ptsToCheck[5]!=null) && (ptsToCheck[6]!=null) && (ptsToCheck[7]!=null) ){
			return p;
		}
		rv = ptsToCheck[0];
		distSq = field.tds(ptsToCheck[0],p);
		for(int i=1;i<ptsToCheck.length;i++){
			if(ptsToCheck[i]!=null){
				tmpDS = field.tds(ptsToCheck[i],p);
				if(tmpDS < distSq){
					distSq = tmpDS;
					rv = ptsToCheck[i];
				}
			}
		}
		return rv;
	}
}
