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
	public BufferedWriter foodVecOut, nearFoodOut, nearNestOut, stateout;
	
	public TwoStateLogger(){
		this(new File(System.getProperties().getProperty("user.dir")));
	}
	
	public TwoStateLogger(File dir){
		super(dir);
		try{
			foodVecOut = new BufferedWriter(new FileWriter(new File(parentDirectory, "foodvec.btf")));
			nearFoodOut = new BufferedWriter(new FileWriter(new File(parentDirectory,"nfood.btf")));
			nearNestOut = new BufferedWriter(new FileWriter(new File(parentDirectory, "nnest.btf")));
			stateout = new BufferedWriter(new FileWriter(new File(parentDirectory,"state.btf")));
		} catch(IOException ioe){
			System.err.println("[TwoStateLogger] Could not open "+dir+" for logging: "+ioe);
			foodVecOut = nearFoodOut = nearNestOut = stateout = null;
		}
	}
	
	public void step(SimState simstate){
		super.step(simstate);
		if(nearFoodOut == null) return;
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)simstate;
			for(int i=0;i<sim.bodies.size();i++){
				Agent agent = sim.bodies.get(i).getAgent();
				if(agent instanceof TwoStateAnt){
					TwoStateAnt tsant = (TwoStateAnt)agent;
					boolean nearFood = tsant.antBody.nearPOI("food");
					boolean nearNest = tsant.antBody.nearPOI("nest");
					MutableDouble2D foodVec = new MutableDouble2D();
					tsant.antBody.getPoiDir(foodVec,"food");
					int curState = (tsant.state - 1);
					try{
						foodVecOut.write(foodVec.x+" "+foodVec.y+"\n");
						nearFoodOut.write(nearFood+"\n");
						nearNestOut.write(nearNest+"\n");
						stateout.write(curState+"\n");
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
			foodVecOut.close();
			nearFoodOut.close();
			nearNestOut.close();
			stateout.close();
		} catch(IOException ioe){
			System.err.println("[TwoStateLogger] Error closing log files: "+ioe);
		}
	}
}
