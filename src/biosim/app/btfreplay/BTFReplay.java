package biosim.app.btfreplay;

import biosim.core.sim.Simulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Obstacle;
import biosim.core.sim.RectObstacle;
import biosim.core.body.Body;
import biosim.core.body.ReplayBody;
import biosim.core.gui.GUISimulation;
import biosim.core.util.BTFData;

import java.util.ArrayList;
import java.io.IOException;
import java.io.File;

import sim.util.Double2D;
import sim.util.MutableDouble2D;
public class BTFReplay{
	public static final double SIZE=biosim.core.body.NotemigonusCrysoleucas.SIZE;
	public static final double WIDTH=2.5;
	public static final double HEIGHT=1.5;
	public static final int MAX_TRACKS=30*30*150;
	public static ArrayList<ReplayBody> loadBTF(BTFData btf) throws IOException{
		ArrayList<ReplayBody> rv = new ArrayList<ReplayBody>();
		String[] id = btf.loadColumn("id");
		String[] xpos = btf.loadColumn("xpos");
		String[] ypos = btf.loadColumn("ypos");
		String[] tpos = btf.loadColumn("timage");
		String[] time = btf.loadColumn("clocktime");
		for(int t=0;t<id.length && t<MAX_TRACKS;t++){
			if(t%(MAX_TRACKS/10)==0) System.out.println("Line #"+t);
			int trackIdx = -1;
			for(int i=0;i<rv.size();i++){
				if(rv.get(i).trackID == Integer.parseInt(id[t].trim())){
					trackIdx = i;
					break;
				}
			}
			if(trackIdx == -1){
				rv.add(new ReplayBody());
				trackIdx = rv.size()-1;
				rv.get(trackIdx).trackID = Integer.parseInt(id[t].trim());
				rv.get(trackIdx).visible = false;
				rv.get(trackIdx).size = SIZE;
				rv.get(trackIdx).track = new ArrayList<double[]>();
			}
			double[] tmp = new double[4];
			tmp[0] = Double.parseDouble(xpos[t].trim());
			tmp[1] = Double.parseDouble(ypos[t].trim());
			tmp[2] = Double.parseDouble(tpos[t].trim());
			tmp[3] = Double.parseDouble(time[t].trim());
			rv.get(trackIdx).track.add(tmp);
		}
		return rv;
	}
	
	static class ReplayEnvironment extends Environment{
		public ReplayEnvironment(double width, double height, double resolution){
			super(width,height,resolution);
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
				MutableDouble2D tmp;
				ReplayBody rpb = (ReplayBody)(initialBodies.get(i));
				tmp = new MutableDouble2D(rpb.track.get(0)[0],rpb.track.get(0)[1]);
				sim.setObjectLocation(rpb,new Double2D(tmp));
				tmp = new MutableDouble2D(1,0);
				tmp.rotate(rpb.track.get(0)[2]);
				sim.bodyOrientations.add(new Double2D(tmp));
			}
		}
	}
	
	public static void main(String[] args){
		try{
			//set up the environment
			Environment env = new ReplayEnvironment(WIDTH,HEIGHT,1.0/30.0);
			env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
			env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
			env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
			env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
			//add agents
			BTFData btf = new BTFData();
			btf.loadDir(new File(args[0]));
			ArrayList<ReplayBody> bodies = loadBTF(btf);
			System.out.println(bodies.size()+" tracks");
			for(int i=0;i<bodies.size();i++){
				env.addBody(bodies.get(i));
			}
			//env.runSimulation(args);
			System.out.println("Creating sim...");
			Simulation sim = env.newSimulation();
			System.out.println("Creating gui...");
			GUISimulation gui = new GUISimulation(sim);
			biosim.app.fishknn.FishPortrayal.bi = null;
			gui.setPortrayalClass(ReplayBody.class, ReplayPortrayal.class);
			gui.setDisplaySize((int)(WIDTH*500),(int)(HEIGHT*500));
			System.out.println("Starting sim...");
			gui.createController();
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}
}
