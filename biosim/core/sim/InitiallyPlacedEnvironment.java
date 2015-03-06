package biosim.core.sim;

import java.util.ArrayList;
import java.io.IOException;

import sim.util.Double2D;
import sim.util.MutableDouble2D;

import biosim.core.util.BTFData;

public class InitiallyPlacedEnvironment extends Environment{
	public ArrayList<Double2D> initialPositions = new ArrayList<Double2D>();
	public ArrayList<Double2D> initialOrientations = new ArrayList<Double2D>();

	public InitiallyPlacedEnvironment(double width, double height,double resolution){
		super(width,height,resolution);
	}
	public void addInitialPose(Double2D p, Double2D dir){
		initialPositions.add(p);
		initialOrientations.add(dir);
	}
	public void parseInitialPoses(BTFData btf) throws IOException{
		String[] xpos = btf.loadColumn("xpos");
		String[] ypos = btf.loadColumn("ypos");
		String[] timage = btf.loadColumn("timage");
		String[] clocktime = btf.loadColumn("clocktime");
		double firstTime = Double.parseDouble(clocktime[0]);
		for(int i=0; Double.parseDouble(clocktime[i]) == firstTime; i++){
			initialPositions.add(new Double2D(Double.parseDouble(xpos[i]), Double.parseDouble(ypos[i])));
			MutableDouble2D tmp = new MutableDouble2D(1,0);
			tmp.rotate(Double.parseDouble(timage[i]));
			initialOrientations.add(new Double2D(tmp));
		}

	}
	protected void initSimField(Simulation sim){
		for(int i=0;i<obstacles.size();i++){
			Double2D tmp = obstacleLocations.get(i);
			sim.setObjectLocation(obstacles.get(i),tmp);
		}
		for(int i=0;i<poi.size();i++){
			sim.setObjectLocation(poi.get(i),poiLocations.get(i));
		}
		for(int i=0;i<initialBodies.size();i++){
			sim.setObjectLocation(initialBodies.get(i),initialPositions.get(i));
			sim.bodyOrientations.add(initialOrientations.get(i));
		}
	}
}