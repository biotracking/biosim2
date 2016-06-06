//DataAsDemonstrator.java
package biosim.core.learning;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;

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

	private void addDataToList(ProblemSpec.Dataset data, ArrayList<double[]> inputs, ArrayList<double[]> outputs){
		for(int dataRow = 0; dataRow < data.features.length; dataRow++){
			inputs.add(data.features[dataRow]);
			outputs.add(data.outputs[dataRow]);
		}
	}

	public static final double[][] aDoubleArray = new double[0][0];

	public void train(BTFSequences data, ProblemSpec pspec, LearnerAgent learner, int maxIterations){
		MersenneTwisterFast savedRandom = null;
		ArrayList<double[]> dad_training_inputs = new ArrayList<double[]>();
		ArrayList<double[]> dad_training_outputs = new ArrayList<double[]>();
		ArrayList<BTFData> activeRealTrainingSequences = new ArrayList<BTFData>();
		Iterator<BTFData> seqIterator = data.sequences.values().iterator();
		activeRealTrainingSequences.add(seqIterator.next());
		boolean outOfData = false;
		int numRealDataPoints;
		try{
			numRealDataPoints = activeRealTrainingSequences.get(0).loadColumn("id").length;
		} catch(IOException ioe){
			throw new RuntimeException("[DataAsDemonstrator] Error getting seq sizes: "+ioe);
		}

		//1. Train predictor
		//1.0 Add training data until it balances the d-a-d data, or until we run out
		while(numRealDataPoints < dad_training_inputs.size() && seqIterator.hasNext()){
			BTFData seqHolder = seqIterator.next();
			try{
				numRealDataPoints += seqHolder.loadColumn("id").length;
			} catch(IOException ioe){
				throw new RuntimeException("[DataAsDemonstrator] Error getting seq sizes (inner loop): "+ioe);
			}
			activeRealTrainingSequences.add(seqHolder);
		}
		if(!seqIterator.hasNext()) outOfData = true;
		//1.1 Make a combined double[][], and put the d-a-d data into it
		int totalRows = numRealDataPoints+dad_training_inputs.size();
		int fCols = pspec.getNumFeatures();
		int oCols = pspec.getNumOutputs();
		double[][] combinedFeatures = new double[totalRows][fCols];
		double[][] combinedOutputs = new double[totalRows][oCols];
		if(dad_training_inputs.size() > 0){
			ReynoldsFeatures.copyInto(dad_training_inputs.toArray(aDoubleArray), combinedFeatures,0);
			ReynoldsFeatures.copyInto(dad_training_outputs.toArray(aDoubleArray), combinedOutputs, 0);
		}
		//1.2 loop through all the real data and add it to the combined array
		int dataOffset = dad_training_inputs.size();
		for(int i=0;i<activeRealTrainingSequences.size();i++){
			ProblemSpec.Dataset dataHolder = pspec.btf2array(activeRealTrainingSequences.get(i));
			ReynoldsFeatures.copyInto(dataHolder.features, combinedFeatures, dataOffset);
			ReynoldsFeatures.copyInto(dataHolder.outputs, combinedOutputs, dataOffset);
			dataOffset += dataHolder.features.length;
		}
		learner.train(combinedFeatures,combinedOutputs);

		//2. Kick of simulation threads
		for(int seqIdx=0;seqIdx<activeRealTrainingSequences.size();seqIdx++){
			RunSim rs = new RunSim(activeRealTrainingSequences.get(seqIdx),pspec,learner,null);
			ArrayListDataset rv = rs.call();
			dad_training_inputs.addAll(rv.inputs);
			dad_training_outputs.addAll(rv.outputs);
		}
		// int numSimSteps = seq1.numUniqueFrames();
		// ArrayList<Integer> idList=seq1.getUniqueIDs();
		// int activeID = idList.get(0);
		// ArrayList<Integer> activeIDRows = seq1.rowIndexForID(activeID);
		// Environment env = pspec.getEnvironment(learner,seq1,activeID);
		// BTFDataLogger logs = pspec.getLogger();
		// env.addLogger(logs);
		// Simulation sim = env.newSimulation();
		// sim.start();
		// while(sim.schedule.getSteps()<(numSimSteps)){
		// 	boolean rv = sim.schedule.step(sim);
		// 	// System.out.println(sim.schedule.getSteps()+" "+rv);
		// }
		// sim.finish();
		// BufferedBTFData logged_btf = (BufferedBTFData)(logs.getBTFData());
		// BufferedBTFData fixed_btf = new BufferedBTFData(null);

		//3. Fix simulated tracks
		// int trainIdxCounter = 0, trainRow;
		// double curX,curY,curT, nextX,nextY,nextT, fixedDvelX,fixedDvelY,fixedDvelT, tmpT;
		// String[] trainX, trainY, trainT;
		// ArrayList<Integer> fixedRows = new ArrayList<Integer>();
		// try{
		// 	trainX = seq1.loadColumn("xpos");
		// 	trainY = seq1.loadColumn("ypos");
		// 	trainT = seq1.loadColumn("timage");			
		// } catch(IOException ioe){
		// 	throw new RuntimeException("[DataAsDemonstrator] Error reading sim logs for fixing: "+ioe);
		// }
		// for(int i=1;i<logged_btf.data.get("id").size()-1;i++){
		// 	if(!(logged_btf.data.get("id").get(i).equals(""+activeID))){
		// 		continue;
		// 	}
		// 	curX = Double.parseDouble(logged_btf.data.get("xpos").get(i));
		// 	curY = Double.parseDouble(logged_btf.data.get("ypos").get(i));
		// 	curT = Double.parseDouble(logged_btf.data.get("timage").get(i));

		// 	trainRow = activeIDRows.get(trainIdxCounter+1);
		// 	nextX = Double.parseDouble(trainX[trainRow]);
		// 	nextY = Double.parseDouble(trainY[trainRow]);
		// 	nextT = Double.parseDouble(trainT[trainRow]);

		// 	fixedDvelX = (nextX-curX)/(sim.resolution);
		// 	fixedDvelY = (nextY-curY)/(sim.resolution);
		// 	tmpT = (nextT-curT);
		// 	if(tmpT > Math.PI){
		// 		tmpT = tmpT-(2.0*Math.PI);
		// 	} else if(tmpT < -Math.PI){
		// 		tmpT = tmpT+(2.0*Math.PI);
		// 	}
		// 	fixedDvelT = tmpT/(sim.resolution);
		// 	// System.out.println("Time: "+logged_btf.data.get("timestamp").get(i));
		// 	// System.out.println("Pose: "+curX+" "+curY+" "+curT);
		// 	// System.out.println("Next pose: "+nextX+" "+nextY+" "+nextT);
		// 	// System.out.println("old dvel: "+logged_btf.data.get("dvel").get(i));
		// 	// System.out.println("Fixed dvel: "+fixedDvelX+" "+fixedDvelY+" "+fixedDvelT);
		// 	logged_btf.data.get("dvel").set(i,fixedDvelX+" "+fixedDvelY+" "+fixedDvelT);
		// 	fixedRows.add(i);
		// 	trainIdxCounter++;
		// }
		// ProblemSpec.Dataset fixed_dataset = pspec.btf2array(logged_btf);		
		// for(int i=0;i<fixedRows.size();i++){
		// 	int theIdx = fixedRows.get(i);
		// 	dad_training_inputs.add(fixed_dataset.features[theIdx]);
		// 	dad_training_outputs.add(fixed_dataset.features[theIdx]);
		// }
		// // double[][] derp = dad_training_outputs.toArray(new double[0][0]);
		// // System.out.println("Sample dims: "+derp.length+" rows by "+derp[0].length+" columns");

		//4. Go to step 1
		// savedRandom = sim.random;
	}

	public class ArrayListDataset {
		public ArrayList<double[]> inputs, outputs;
		public ArrayListDataset(){
			inputs = new ArrayList<double[]>();
			outputs = new ArrayList<double[]>();
		}
	}

	public class RunSim implements Callable {
		public BTFData seq;
		public ProblemSpec pspec;
		public LearnerAgent learner;
		public MersenneTwisterFast savedRandom;
		public RunSim(BTFData seq, ProblemSpec pspec, LearnerAgent learner, MersenneTwisterFast savedRandom){
			this.seq = seq;
			this.pspec = pspec;
			this.learner = learner;
			this.savedRandom = savedRandom;
		}
		public ArrayListDataset call(){
			int numSimSteps = seq.numUniqueFrames();
			ArrayList<Integer> idList=seq.getUniqueIDs();
			ArrayListDataset rv = new ArrayListDataset();
			for(int idIdx=0;idIdx<idList.size();idIdx++){
				int activeID = idList.get(idIdx);
				ArrayList<Integer> activeIDRows = seq.rowIndexForID(activeID);
				Environment env = pspec.getEnvironment(learner,seq,activeID);
				BTFDataLogger logs = pspec.getLogger();
				env.addLogger(logs);
				Simulation sim = env.newSimulation();
				// TODO: Come up with a way to get consistant simulations by supplying a single seed.
				// CANNOT rely on these threads to execute in the same order, so probably need to 
				// create new random's seeded from the initial seed and incremented in the same way
				// BEFORE being passed to the threadpool. 
				// sim.random = savedRandom;
				sim.start();
				while(sim.schedule.getSteps()<(numSimSteps)){
					boolean step_success = sim.schedule.step(sim);
					// System.out.println(sim.schedule.getSteps()+" "+rv);
				}
				sim.finish();
				BufferedBTFData logged_btf = (BufferedBTFData)(logs.getBTFData());
				int trainIdxCounter = 0, trainRow;
				double curX,curY,curT, nextX,nextY,nextT, fixedDvelX,fixedDvelY,fixedDvelT, tmpT;
				String[] trainX, trainY, trainT;
				ArrayList<Integer> fixedRows = new ArrayList<Integer>();
				try{
					trainX = seq.loadColumn("xpos");
					trainY = seq.loadColumn("ypos");
					trainT = seq.loadColumn("timage");			
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
					rv.inputs.add(fixed_dataset.features[theIdx]);
					rv.outputs.add(fixed_dataset.features[theIdx]);
				}
				// double[][] derp = dad_training_outputs.toArray(new double[0][0]);
				// System.out.println("Sample dims: "+derp.length+" rows by "+derp[0].length+" columns");
			}
			return rv;
		}
	}

	public static void main(String[] args){
		DataAsDemonstrator dad = new DataAsDemonstrator();
		BTFSequences seqs = new BTFSequences();
		seqs.loadDir(new File(args[0]));
		ProblemSpec pspec = new ReynoldsFeatures();
		KNNModel knnm = new KNNModel();
		knnm.setFeatureNames(new String[] {"sepX","sepY","oriX","oriY","cohX","cohY","wallX","wallY","pvelX","pvelY","pvelT"});
		knnm.setOutputNames(new String[] {"dvelX","dvelY","dvelT"});

		dad.train(seqs,pspec,knnm,-1);
	}
}