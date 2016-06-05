//DataAsDemonstrator.java
package biosim.core.learning;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import biosim.core.gui.GUISimulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.InitiallyPlacedEnvironment;
import biosim.core.util.BTFData;
import biosim.core.util.BufferedBTFData;
import biosim.core.util.BTFDataLogger;
import biosim.core.util.BTFSequences;
import biosim.core.learning.ProblemSpec;

import biosim.app.fishreynolds.ReynoldsFeatures;

import ec.util.MersenneTwisterFast;

public class DataAsDemonstrator{

	public void train(BTFSequences data, ProblemSpec pspec, LearnerAgent learner){
		MersenneTwisterFast savedRandom = null;
		ArrayList<double[]> dad_training_inputs = new ArrayList<double[]>();
		ArrayList<double[]> dad_training_outputs = new ArrayList<double[]>();
		//1. Train predictor
		BTFData seq1 = data.sequences.values().iterator().next();
		System.out.println(seq1.columns.get("id"));
		ProblemSpec.Dataset seq1_data = pspec.btf2array(seq1);
		learner.train(seq1_data.features,seq1_data.outputs);
		//2. Simulate
		int numSimSteps = seq1.numUniqueFrames();
		ArrayList<Integer> idList=seq1.getUniqueIDs();
		int activeID = idList.get(0);
		ArrayList<Integer> activeIDRows = seq1.rowIndexForID(activeID);
		// ignoreList = new ArrayList<Integer>();
		// for(int i=0;i<50;i++){ignoreList.add(i);}
		// ignoreList.add();
		Environment env = pspec.getEnvironment(learner,seq1,activeID);
		BTFDataLogger logs = pspec.getLogger();
		env.addLogger(logs);
		Simulation sim = env.newSimulation();
		sim.start();
		while(sim.schedule.getSteps()<(numSimSteps)){
			boolean rv = sim.schedule.step(sim);
			// System.out.println(sim.schedule.getSteps()+" "+rv);
		}
		sim.finish();
		BufferedBTFData logged_btf = (BufferedBTFData)(logs.getBTFData());
		BufferedBTFData fixed_btf = new BufferedBTFData(null);
		// String[] ids, xpos;
		// try{
		// 	ids = logged_btf.loadColumn("id");
		// 	xpos = logged_btf.loadColumn("xpos");
		// } catch(IOException ioe){
		// 	throw new RuntimeException("[DataAsDemonstrator] Error reading sim logs: "+ioe);
		// }
		// for(int i=0;i<ids.length;i++){
		// 	System.out.println(ids[i]+":"+xpos[i]);
		// }
		//3. Fix simulated tracks
		int trainIdxCounter = 0, trainRow;
		double curX,curY,curT, nextX,nextY,nextT, fixedDvelX,fixedDvelY,fixedDvelT, tmpT;
		String[] trainX, trainY, trainT;
		ArrayList<Integer> fixedRows = new ArrayList<Integer>();
		try{
			trainX = seq1.loadColumn("xpos");
			trainY = seq1.loadColumn("ypos");
			trainT = seq1.loadColumn("timage");			
		} catch(IOException ioe){
			throw new RuntimeException("[DataAsDemonstrator] Error reading sim logs for fixing: "+ioe);
		}
		for(int i=1;i<logged_btf.data.get("id").size()-1;i++){
			if(!(logged_btf.data.get("id").get(i).equals(""+activeID))){
				continue;
			}
			curX = Double.parseDouble(logged_btf.data.get("xpos").get(i));
			curY = Double.parseDouble(logged_btf.data.get("ypos").get(i));
			curT = Double.parseDouble(logged_btf.data.get("timage").get(i));

			trainRow = activeIDRows.get(trainIdxCounter+1);
			nextX = Double.parseDouble(trainX[trainRow]);
			nextY = Double.parseDouble(trainY[trainRow]);
			nextT = Double.parseDouble(trainT[trainRow]);

			fixedDvelX = (nextX-curX)/(sim.resolution);
			fixedDvelY = (nextY-curY)/(sim.resolution);
			tmpT = (nextT-curT);
			if(tmpT > Math.PI){
				tmpT = tmpT-(2.0*Math.PI);
			} else if(tmpT < -Math.PI){
				tmpT = tmpT+(2.0*Math.PI);
			}
			fixedDvelT = tmpT/(sim.resolution);
			// System.out.println("Time: "+logged_btf.data.get("timestamp").get(i));
			// System.out.println("Pose: "+curX+" "+curY+" "+curT);
			// System.out.println("Next pose: "+nextX+" "+nextY+" "+nextT);
			// System.out.println("old dvel: "+logged_btf.data.get("dvel").get(i));
			// System.out.println("Fixed dvel: "+fixedDvelX+" "+fixedDvelY+" "+fixedDvelT);
			logged_btf.data.get("dvel").set(i,fixedDvelX+" "+fixedDvelY+" "+fixedDvelT);
			fixedRows.add(i);
			trainIdxCounter++;
		}
		ProblemSpec.Dataset fixed_dataset = pspec.btf2array(logged_btf);		
		for(int i=0;i<fixedRows.size();i++){
			int theIdx = fixedRows.get(i);
			dad_training_inputs.add(fixed_dataset.features[theIdx]);
			dad_training_outputs.add(fixed_dataset.features[theIdx]);
		}
		// double[][] derp = dad_training_outputs.toArray(new double[0][0]);
		// System.out.println("Sample dims: "+derp.length+" rows by "+derp[0].length+" columns");
		//4. Go to step 1
		savedRandom = sim.random;
	}

	public static void main(String[] args){
		DataAsDemonstrator dad = new DataAsDemonstrator();
		BTFSequences seqs = new BTFSequences();
		seqs.loadDir(new File(args[0]));
		ProblemSpec pspec = new ReynoldsFeatures();
		KNNModel knnm = new KNNModel();
		knnm.setFeatureNames(new String[] {"sepX","sepY","oriX","oriY","cohX","cohY","wallX","wallY","pvelX","pvelY","pvelT"});
		knnm.setOutputNames(new String[] {"dvelX","dvelY","dvelT"});

		dad.train(seqs,pspec,knnm);
	}
}