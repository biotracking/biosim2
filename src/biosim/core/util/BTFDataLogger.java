package biosim.core.util;

import biosim.core.sim.Simulation;
import biosim.core.sim.Logger;

import sim.engine.SimState;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

import java.io.IOException;
import java.io.BufferedWriter;
import java.util.LinkedList;
import java.util.ArrayList;

public class BTFDataLogger implements Logger{
	//public ArrayList<String> ximage, yimage, timage, id, timestamp;
	public LinkedList<String> ximage, yimage, timage, id, timestamp, wallclock;
	public BTFDataLogger(){
		ximage = yimage = timage = id = timestamp = wallclock = null;
	}

	public BTFData getBTFData(){
		if(ximage==null) return null;
		BufferedBTFData rv = new BufferedBTFData(null);
		rv.data.put("xpos",new ArrayList<String>(ximage));
		rv.data.put("ypos",new ArrayList<String>(yimage));
		rv.data.put("timage",new ArrayList<String>(timage));
		rv.data.put("id",new ArrayList<String>(id));
		rv.data.put("clocktime",new ArrayList<String>(wallclock));
		rv.data.put("timestamp",new ArrayList<String>(timestamp));
		return rv;
	}
	
	public void init(){
		ximage = new LinkedList<String>();
		yimage = new LinkedList<String>();
		timage = new LinkedList<String>();
		id = new LinkedList<String>();
		timestamp = new LinkedList<String>();
		wallclock = new LinkedList<String>();
	}
	public void step(SimState simstate){
		if(ximage==null) return;
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)simstate;
			for(int i=0;i<sim.bodies.size();i++){
				if(sim.bodies.get(i).doNotLog) continue;
				Double2D loc = sim.field2D.getObjectLocation(sim.bodies.get(i));
				MutableDouble2D dir = new MutableDouble2D(sim.bodyOrientations.get(i));
				// ximgout.write(loc.x+"\n");
				ximage.add(""+loc.x);
				// yimgout.write(loc.y+"\n");
				yimage.add(""+loc.y);
				// timgout.write(dir.angle()+"\n");
				timage.add(""+dir.angle());
				String tmpid = sim.bodies.get(i).label;
				// idout.write(tmpid+"\n");
				id.add(tmpid);
				// timeout.write((sim.schedule.getSteps()*sim.resolution)+"\n");
				wallclock.add(""+(sim.schedule.getSteps()*sim.resolution));
				// timestampout.write(sim.schedule.getSteps()+"\n");
				timestamp.add(""+sim.schedule.getSteps());
			}
		}
	}
	public void finish(){}
}
