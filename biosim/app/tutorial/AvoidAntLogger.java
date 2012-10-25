package biosim.app.tutorial;

import biosim.core.body.Body;
import biosim.core.body.AphaenogasterCockerelli;
import biosim.core.sim.Simulation;
import biosim.core.util.BTFLogger;

import sim.engine.SimState;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class AvoidAntLogger extends BTFLogger {
	public ArrayList<String> 	wallSensor, wallSensorBool, 
								antSensor, antSensorBool,
								desiredVel, desiredVelBool,
								homeSensor;
	public AvoidAntLogger(){
		super();
		wallSensor = new ArrayList<String>();
		antSensor  = new ArrayList<String>();
		desiredVel = new ArrayList<String>();
		wallSensorBool = new ArrayList<String>();
		antSensorBool  = new ArrayList<String>();
		desiredVelBool = new ArrayList<String>();
		homeSensor = new ArrayList<String>();
	}
	public AvoidAntLogger(File dir){
		super(dir);
		wallSensor = new ArrayList<String>();
		antSensor = new ArrayList<String>();
		desiredVel = new ArrayList<String>();		
		wallSensorBool = new ArrayList<String>();
		antSensorBool  = new ArrayList<String>();
		desiredVelBool = new ArrayList<String>();
		homeSensor = new ArrayList<String>();
	}
	public void step(SimState simstate){
		super.step(simstate);
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)simstate;
			for(int i=0;i<sim.bodies.size();i++){
				Body b = sim.bodies.get(i);
				MutableDouble2D wallSensorVec = new MutableDouble2D();
				boolean wallSensorRv = false;
				MutableDouble2D antSensorVec = new MutableDouble2D();
				boolean antSensorRv = false;
				MutableDouble2D homeSensorVec = new MutableDouble2D();
				double[] foo = {0.0, 0.0, 0.0};
				boolean bar = false;
				if(b instanceof AphaenogasterCockerelli){
					AphaenogasterCockerelli ant = (AphaenogasterCockerelli)b;
					wallSensorRv = ant.getNearestObstacleVec(wallSensorVec);
					antSensorRv = ant.getNearestSameAgentVec(antSensorVec);
					ant.getHomeDir(homeSensorVec);
					System.arraycopy(ant.desiredVelXYT,0,foo,0,3);
					bar = true;
				}
				wallSensor.add(wallSensorVec.x+" "+wallSensorVec.y);
				wallSensorBool.add(""+wallSensorRv);
				antSensor.add(antSensorVec.x+" "+antSensorVec.y);
				antSensorBool.add(""+antSensorRv);
				desiredVel.add(foo[0]+" "+foo[1]+" "+foo[2]);
				desiredVelBool.add(""+bar);
				homeSensor.add(homeSensorVec.x+" "+homeSensorVec.y);

			}
		}
	}
	public void finish(){
		try{
			super.finish();
			FileWriter wallVecOut = new FileWriter(new File(parentDirectory,"wallvec.btf"));
			FileWriter wallBoolOut = new FileWriter(new File(parentDirectory,"wallbool.btf"));
			FileWriter antVecOut = new FileWriter(new File(parentDirectory,"antvec.btf"));
			FileWriter antBoolOut = new FileWriter(new File(parentDirectory,"antbool.btf"));
			FileWriter desiredVelOut = new FileWriter(new File(parentDirectory,"dvel.btf"));
			FileWriter desiredBoolOut = new FileWriter(new File(parentDirectory,"dbool.btf"));
			FileWriter homeOut = new FileWriter(new File(parentDirectory,"homevec.btf"));
			for(int i=0;i<wallSensor.size();i++){
				wallVecOut.write(wallSensor.get(i)+"\n");
				wallBoolOut.write(wallSensorBool.get(i)+"\n");
				antVecOut.write(antSensor.get(i)+"\n");
				antBoolOut.write(antSensorBool.get(i)+"\n");
				desiredVelOut.write(desiredVel.get(i)+"\n");
				desiredBoolOut.write(desiredVelBool.get(i)+"\n");
				homeOut.write(homeSensor.get(i)+"\n");
			}
			wallVecOut.close();
			wallBoolOut.close();
			antVecOut.close();
			antBoolOut.close();
			desiredVelOut.close();
			desiredBoolOut.close();
			homeOut.close();
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
