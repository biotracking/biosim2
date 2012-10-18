package biosim.core.util;

import biosim.core.sim.Simulation;
import biosim.core.sim.Logger;

import sim.engine.SimState;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class BTFLogger implements Logger{
	public ArrayList<String> ximage, yimage, timage, id, timestamp;
	public File parentDirectory;
	public BTFLogger(){
		this(new File(System.getProperties().getProperty("user.dir")));
	}
	public BTFLogger(File dir){
		ximage = new ArrayList<String>();
		yimage = new ArrayList<String>();
		timage = new ArrayList<String>();
		id = new ArrayList<String>();
		timestamp = new ArrayList<String>();
		parentDirectory = dir;
	}
	public void step(SimState simstate){
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)simstate;
			for(int i=0;i<sim.bodies.size();i++){
				Double2D loc = sim.field2D.getObjectLocation(sim.bodies.get(i));
				MutableDouble2D dir = new MutableDouble2D(sim.bodyOrientations.get(i));
				ximage.add(""+loc.x);
				yimage.add(""+loc.y);
				timage.add(""+dir.angle());
				id.add(""+i);
				timestamp.add(""+sim.schedule.getSteps()*sim.resolution);
			}
		}
	}
	public void finish(){
		try{
			FileWriter xout = new FileWriter(new File(parentDirectory,"ximage.btf"));
			FileWriter yout = new FileWriter(new File(parentDirectory,"yimage.btf"));
			FileWriter tout = new FileWriter(new File(parentDirectory,"timage.btf"));
			FileWriter idout = new FileWriter(new File(parentDirectory,"id.btf"));
			FileWriter timeout = new FileWriter(new File(parentDirectory,"timestamp.btf"));
			for(int i=0;i<timestamp.size();i++){
				xout.write(ximage.get(i)+"\n");
				yout.write(yimage.get(i)+"\n");
				tout.write(timage.get(i)+"\n");
				idout.write(id.get(i)+"\n");
				timeout.write(timestamp.get(i)+"\n");
			}
			xout.close();
			yout.close();
			tout.close();
			idout.close();
			timeout.close();
		} catch (IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
