package biosim.app.fishlr;

import biosim.core.body.Body;
import biosim.core.body.NotemigonusCrysoleucas;
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

public class FishLRLogger extends BTFLogger {
	public BufferedWriter sepout, oriout, cohout, wwallout, desiredout, desiredboolout;
	double sepSigma, cohSigma, oriSigma, obsSigma;

	public void setSigmas(double sep, double ori, double coh, double obs){
		sepSigma = sep;
		oriSigma = ori;
		cohSigma = coh;
		obsSigma = obs;
	}

	public FishLRLogger(){
		this(new File(System.getProperties().getProperty("user.dir")));
	}
	public FishLRLogger(File dir){
		super(dir);
		tmpFilePrefix = "FishLRLogger-";
	}
	public void initFiles() throws IOException {
		super.initFiles();
		sepout = new BufferedWriter(new FileWriter(new File(tmpDir,"rbfsepvec.btf")));
		oriout = new BufferedWriter(new FileWriter(new File(tmpDir,"rbforivec.btf")));
		cohout = new BufferedWriter(new FileWriter(new File(tmpDir,"rbfcohvec.btf")));
		wwallout = new BufferedWriter(new FileWriter(new File(tmpDir,"rbfwallvec.btf")));
		desiredout = new BufferedWriter(new FileWriter(new File(tmpDir,"dvel.btf")));
		desiredboolout = new BufferedWriter(new FileWriter(new File(tmpDir,"dbool.btf")));
	}

	public void nullFiles(){
		super.nullFiles();
		wwallout = cohout = oriout = sepout = desiredout = desiredboolout = null;
	}

	public void closeFiles() throws IOException{
		super.closeFiles();
		sepout.close();
		cohout.close();
		oriout.close();
		wwallout.close();
		desiredout.close();
		desiredboolout.close();
	}
		
	public void step(SimState simstate){
		super.step(simstate);
		if(sepout == null) return;
		if(simstate instanceof Simulation){
			Simulation sim = (Simulation)simstate;
			for(int i=0;i<sim.bodies.size();i++){
				Body b = sim.bodies.get(i);
				if(b instanceof NotemigonusCrysoleucas){
					NotemigonusCrysoleucas fish = (NotemigonusCrysoleucas)b;
					MutableDouble2D sepSensorVec = new MutableDouble2D();
					MutableDouble2D oriSensorVec = new MutableDouble2D();
					MutableDouble2D cohSensorVec = new MutableDouble2D();
					MutableDouble2D wwallSensorVec = new MutableDouble2D();
					fish.getAverageRBFSameTypeVec(sepSensorVec, sepSigma);
					fish.getAverageRBFOrientationSameTypeVec(oriSensorVec, oriSigma);
					fish.getAverageRBFSameTypeVec(cohSensorVec, cohSigma);
					fish.getNearestObstacleVec(wwallSensorVec);
					wwallSensorVec.multiplyIn(Math.exp(-wwallSensorVec.lengthSq()/(2.0*Math.pow(obsSigma,2))));
					double[] foo = {0.0, 0.0, 0.0};
					boolean bar = false;
					System.arraycopy(fish.desiredVelXYT,0,foo,0,3);
					bar = true;
					try{
						sepout.write(sepSensorVec.x+" "+sepSensorVec.y+"\n");
						oriout.write(oriSensorVec.x+" "+oriSensorVec.y+"\n");
						cohout.write(cohSensorVec.x+" "+cohSensorVec.y+"\n");
						wwallout.write(wwallSensorVec.x+" "+wwallSensorVec.y+"\n");						
						desiredout.write(foo[0]+" "+foo[1]+" "+foo[2]+"\n");
						desiredboolout.write(bar+"\n");
					} catch(IOException ioe){
						System.err.println("[FishLRLogger] Error writing to log files: "+ioe);
					}
				}

			}
		}
	}
}
