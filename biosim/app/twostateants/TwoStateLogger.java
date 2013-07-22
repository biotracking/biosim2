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

public class TwoStateLogger implements biosim.core.sim.Logger{
	public BufferedWriter 	ximgout, yimgout, timgout, idout, timeout, 
							foodVecOut, nearNestOut, foodInGripperOut, stateout,
							wallout, wallboolout,antout, antboolout, desiredout, 
							desiredboolout, prevout, prevboolout, homeout;
	public File parentDirectory;
	public TwoStateLogger(){
		this(new File(System.getProperties().getProperty("user.dir")));
	}
	
	public TwoStateLogger(File dir){
		try{
			foodVecOut = new BufferedWriter(new FileWriter(new File(parentDirectory, "foodvec.btf")));
			nearNestOut = new BufferedWriter(new FileWriter(new File(parentDirectory, "nnest.btf")));
			foodInGripperOut = new BufferedWriter(new FileWriter(new File(parentDirectory, "gripper.btf")));
			stateout = new BufferedWriter(new FileWriter(new File(parentDirectory,"state.btf")));
			ximgout = new BufferedWriter(new FileWriter(new File(parentDirectory, "xpos.btf")));
			yimgout = new BufferedWriter(new FileWriter(new File(parentDirectory, "ypos.btf")));
			timgout = new BufferedWriter(new FileWriter(new File(parentDirectory, "timage.btf")));
			idout = new BufferedWriter(new FileWriter(new File(parentDirectory, "id.btf")));
			timeout = new BufferedWriter(new FileWriter(new File(parentDirectory, "clocktime.btf")));			
			wallout = new BufferedWriter(new FileWriter(new File(parentDirectory,"wallvec.btf")));
			wallboolout = new BufferedWriter(new FileWriter(new File(parentDirectory,"wallbool.btf")));
			antout = new BufferedWriter(new FileWriter(new File(parentDirectory,"antvec.btf")));
			antboolout = new BufferedWriter(new FileWriter(new File(parentDirectory,"antbool.btf")));
			desiredout = new BufferedWriter(new FileWriter(new File(parentDirectory,"dvel.btf")));
			desiredboolout = new BufferedWriter(new FileWriter(new File(parentDirectory,"dbool.btf")));
			prevout = new BufferedWriter(new FileWriter(new File(parentDirectory,"pvel.btf")));
			prevboolout = new BufferedWriter(new FileWriter(new File(parentDirectory,"pbool.btf")));
			homeout = new BufferedWriter(new FileWriter(new File(parentDirectory,"homevec.btf")));
		} catch(IOException ioe){
			System.err.println("[TwoStateLogger] Could not open "+dir+" for logging: "+ioe);
			foodInGripperOut = foodVecOut = nearNestOut = stateout = null;
		}
	}

	public void init(){
		//TODO: Move all of the stuff in the constructor down here
		//so that we can log multiple runs from the console
	}
	
	public void step(SimState simstate){
		if(nearNestOut == null) return;
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)simstate;
			for(int i=0;i<sim.bodies.size();i++){
				Double2D loc = sim.field2D.getObjectLocation(sim.bodies.get(i));
				MutableDouble2D dir = new MutableDouble2D(sim.bodyOrientations.get(i));				
				Agent agent = sim.bodies.get(i).getAgent();
				if(agent instanceof TwoStateAnt){
					MutableDouble2D wallSensorVec = new MutableDouble2D();
					boolean wallSensorRv = false;
					MutableDouble2D antSensorVec = new MutableDouble2D();
					boolean antSensorRv = false;
					double[] qux = {0.0, 0.0, 0.0};
					boolean baz = false;
					MutableDouble2D homeSensorVec = new MutableDouble2D();
					double[] foo = {0.0, 0.0, 0.0};
					boolean bar = false;
					
					TwoStateAnt tsant = (TwoStateAnt)agent;
					AphaenogasterCockerelli aphaeno = (AphaenogasterCockerelli)tsant.antBody;

					wallSensorRv = tsant.antBody.getNearestObstacleVec(wallSensorVec);
					antSensorRv = tsant.antBody.getNearestSameAgentVec(antSensorVec);
					tsant.antBody.getPoiDir(homeSensorVec,"nest");
					System.arraycopy(aphaeno.desiredVelXYT,0,foo,0,3);
					bar = true;
					System.arraycopy(aphaeno.previousVelXYT,0,qux,0,3);
					baz = true;


					boolean nearNest = tsant.antBody.nearPOI("nest");
					boolean gripper = tsant.antBody.getGripped();
					MutableDouble2D foodVec = new MutableDouble2D();
					tsant.antBody.getNearestPreyVec(foodVec);
					int curState = (tsant.state - 1);
					try{
						ximgout.write(loc.x+"\n");
						yimgout.write(loc.y+"\n");
						timgout.write(dir.angle()+"\n");
						idout.write(i+"\n");
						timeout.write((sim.schedule.getSteps()*sim.resolution)+"\n");
						
						wallout.write(wallSensorVec.x+" "+wallSensorVec.y+"\n");
						wallboolout.write(wallSensorRv+"\n");
						antout.write(antSensorVec.x+" "+antSensorVec.y+"\n");
						antboolout.write(antSensorRv+"\n");
						desiredout.write(foo[0]+" "+foo[1]+" "+foo[2]+"\n");
						desiredboolout.write(bar+"\n");
						prevout.write(qux[0]+" "+qux[1]+" "+qux[2]+"\n");
						prevboolout.write(baz+"\n");
						homeout.write(homeSensorVec.x+" "+homeSensorVec.y+"\n");						
						
						foodVecOut.write(foodVec.x+" "+foodVec.y+"\n");
						nearNestOut.write(nearNest+"\n");
						stateout.write(curState+"\n");
						foodInGripperOut.write(gripper+"\n");
					} catch(IOException ioe){
						System.err.println("[TwoStateLogger] Error writing to log files: "+ioe);
					}
				}
			}
		}
	}
	
	public void finish(){
		try{
			ximgout.close();
			yimgout.close();
			timgout.close();
			idout.close();
			timeout.close();			
			
			wallout.close();
			wallboolout.close();
			antout.close();
			antboolout.close();
			desiredout.close();
			desiredboolout.close();
			prevout.close();
			prevboolout.close();
			homeout.close();
			
			foodVecOut.close();
			nearNestOut.close();
			foodInGripperOut.close();
			stateout.close();
		} catch(IOException ioe){
			System.err.println("[TwoStateLogger] Error closing log files: "+ioe);
		}
	}
}
