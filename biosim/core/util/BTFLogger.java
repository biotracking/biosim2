package biosim.core.util;

import biosim.core.sim.Simulation;
import biosim.core.sim.Logger;

import sim.engine.SimState;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.util.ArrayList;

public class BTFLogger implements Logger{
	//public ArrayList<String> ximage, yimage, timage, id, timestamp;
	public BufferedWriter ximgout, yimgout, timgout, idout, timeout;
	public File parentDirectory;
	public BTFLogger(){
		this(new File(System.getProperties().getProperty("user.dir")));
	}
	public BTFLogger(File dir){
		//storing logs in memory is bad, mmkay?
		/*
		ximage = new ArrayList<String>();
		yimage = new ArrayList<String>();
		timage = new ArrayList<String>();
		id = new ArrayList<String>();
		timestamp = new ArrayList<String>();
		*/
		parentDirectory = dir;
		try{
			ximgout = new BufferedWriter(new FileWriter(new File(parentDirectory, "xpos.btf")));
			yimgout = new BufferedWriter(new FileWriter(new File(parentDirectory, "ypos.btf")));
			timgout = new BufferedWriter(new FileWriter(new File(parentDirectory, "timage.btf")));
			idout = new BufferedWriter(new FileWriter(new File(parentDirectory, "id.btf")));
			timeout = new BufferedWriter(new FileWriter(new File(parentDirectory, "clocktime.btf")));
		} catch(IOException ioe){
			System.err.println("[BTFLogger] Could not open "+dir+" for logging: "+ioe);
			ximgout = yimgout=timgout=idout=timeout = null;
		}
	}
	public void step(SimState simstate){
		if(ximgout==null) return;
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)simstate;
			for(int i=0;i<sim.bodies.size();i++){
				Double2D loc = sim.field2D.getObjectLocation(sim.bodies.get(i));
				MutableDouble2D dir = new MutableDouble2D(sim.bodyOrientations.get(i));
				try{
					ximgout.write(loc.x+"\n");
					yimgout.write(loc.y+"\n");
					timgout.write(dir.angle()+"\n");
					idout.write(i+"\n");
					timeout.write((sim.schedule.getSteps()*sim.resolution)+"\n");
					//ximage.add(""+loc.x);
					//yimage.add(""+loc.y);
					//timage.add(""+dir.angle());
					//id.add(""+i);
					//timestamp.add(""+sim.schedule.getSteps()*sim.resolution);
				} catch(IOException e){
					System.err.println("[BTFLogger] Error writing to log files: "+e);
				}
			}
		}
	}
	public void finish(){
		if(ximgout==null) return;
		try{
			/*
			FileWriter xout = new FileWriter(new File(parentDirectory,"xpos.btf"));
			FileWriter yout = new FileWriter(new File(parentDirectory,"ypos.btf"));
			FileWriter tout = new FileWriter(new File(parentDirectory,"timage.btf"));
			FileWriter idout = new FileWriter(new File(parentDirectory,"id.btf"));
			FileWriter timeout = new FileWriter(new File(parentDirectory,"clocktime.btf"));
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
			*/
			ximgout.close();
			yimgout.close();
			timgout.close();
			idout.close();
			timeout.close();
		} catch (IOException ioe){
			System.err.println("[BTFLogger] Error closing log files: "+ioe);
		}
	}
}
