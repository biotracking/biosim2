package biosim.app.twostateants;

import biosim.app.tutorial.AvoidAntLogger;
import biosim.core.agent.Agent;
import biosim.core.body.AphaenogasterCockerelli;
import biosim.core.sim.Simulation;
import biosim.core.util.BTFLogger;

import sim.engine.SimState;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class TwoStateLogger extends AvoidAntLogger{
	public ArrayList<String> avoidTimeThresh, nearTimeThresh;
	public BufferedWriter avoidout, nearout;
	
	public TwoStateLogger(){
		this(new File(System.getProperties().getProperty("user.dir")));
	}
	
	public TwoStateLogger(File dir){
		super(dir);
		try{
			avoidout = new BufferedWriter(new FileWriter(new File(parentDirectory,"avoid.btf")));
			nearout = new BufferedWriter(new FileWriter(new File(parentDirectory, "near.btf")));
		} catch(IOException ioe){
			System.err.println("[TwoStateLogger] Could not open "+dir+" for logging: "+ioe);
			avoidout = nearout = null;
		}
	}
	
	public void step(SimState simstate){
		super.step(simstate);
		if(avoidout == null) return;
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)simstate;
			for(int i=0;i<sim.bodies.size();i++){
				Agent agent = sim.bodies.get(i).getAgent();
				if(agent instanceof TwoStateAnt){
					TwoStateAnt tsant = (TwoStateAnt)agent;
					boolean doneAvoiding = (tsant.timeAvoiding > TwoStateAnt.AVOID_TIME);
					boolean doneApproaching = (tsant.timeNearAnt > TwoStateAnt.VISIT_TIME);
					try{
						avoidout.write(doneAvoiding+"\n");
						nearout.write(doneApproaching+"\n");
					} catch(IOException ioe){
						System.err.println("[TwoStateLogger] Error writing to log files: "+ioe);
					}
				}
			}
		}
	}
	
	public void finish(){
		try{
			super.finish();
			avoidout.close();
			nearout.close();
		} catch(IOException ioe){
			System.err.println("[TwoStateLogger] Error closing log files: "+ioe);
		}
	}
}
