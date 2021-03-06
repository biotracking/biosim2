//DataAsDemonstrator.java
package biosim.core.learning;

import java.io.BufferedWriter;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.Random;

import biosim.core.gui.GUISimulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.InitiallyPlacedEnvironment;
import biosim.core.util.ArgsToProps;
import biosim.core.util.BTFData;
import biosim.core.util.BufferedBTFData;
import biosim.core.util.BTFDataLogger;
import biosim.core.util.BTFSequences;
import biosim.core.learning.ProblemSpec;

import biosim.app.fishreynolds.ReynoldsFeatures;

import ec.util.MersenneTwisterFast;

public class DataAsDemonstrator{

	public static final double[][] aDoubleArray = new double[0][0];
	// public static final ExecutorService pool = Executors.newFixedThreadPool(4);

	public void kFoldCV(BTFSequences data, ProblemSpec pspec, int maxIterations, int maxThreads, File outputDirectory, int folds, boolean dontSaveLearner){
		int numCVSeqs = data.sequences.size()/folds;
		ArrayList<BTFData> allData = new ArrayList<BTFData>();
		//shuffle data
		Collections.shuffle(allData, new Random(pspec.getSeed()));
		allData.addAll(data.sequences.values());
		for(int i=0;i<folds;i++){
			ArrayList<BTFData> cvData = new ArrayList<BTFData>();
			ArrayList<BTFData> trainingData = new ArrayList<BTFData>();
			for(int j=0;j<data.sequences.size();j++){
				if(j>=i*numCVSeqs && j<(i+1)*numCVSeqs){
					cvData.add(allData.get(j));
				} else {
					trainingData.add(allData.get(j));
				}
			}
			File foldDir = new File(outputDirectory,"fold_"+i);
			foldDir.mkdir();
			System.out.println("Beginning fold "+i+" in "+foldDir);
			train(trainingData,cvData,pspec,maxIterations,maxThreads,foldDir,dontSaveLearner);
		}
	}

	// public ArrayList<LearnerAgent> train(BTFSequences data, ProblemSpec pspec, int maxIterations, int maxThreads, File outputDirectory, double cvRatio){
	public ArrayList<LearnerAgent> train(ArrayList<BTFData> trainingData, ArrayList<BTFData> cvData, ProblemSpec pspec, int maxIterations, int maxThreads, File outputDirectory, boolean dontSaveLearner){
		ArrayList<double[]> dad_training_inputs = new ArrayList<double[]>();
		ArrayList<double[]> dad_training_outputs = new ArrayList<double[]>();
		ArrayList<BTFData> activeRealTrainingSequences = new ArrayList<BTFData>();
		ArrayList<LearnerAgent> rv = null;
		if(outputDirectory == null){
			rv = new ArrayList<LearnerAgent>();
		}
		//create thread pool
		int numThreads = Math.min(maxThreads,Runtime.getRuntime().availableProcessors());
		// System.out.println("Threadpool size: "+numThreads);
		final ExecutorService pool = Executors.newFixedThreadPool(numThreads);
		
		/* 		
		List<BTFData> btfValues = new ArrayList<BTFData>(data.sequences.values());
		//shuffle data 
		Collections.shuffle(btfValues, new Random(pspec.getSeed()));
		ListIterator<BTFData> seqIterator = btfValues.listIterator();//data.sequences.values().iterator();
		//pull out cross validation samples
		int numCVSeqs = (int)(data.sequences.size()*cvRatio);
		ArrayList<BTFData> cvData = new ArrayList<BTFData>();
		for(int i=0;i<numCVSeqs;i++){
			cvData.add(seqIterator.next());
		}
		*/

		//compute baseline learner
		// ListIterator<BTFData> baselineIterator = btfValues.listIterator(seqIterator.nextIndex());
		ListIterator<BTFData> baselineIterator = trainingData.listIterator();
		ArrayList<ProblemSpec.Dataset> baselineData = new ArrayList<ProblemSpec.Dataset>();
		while(baselineIterator.hasNext()){
			baselineData.add(pspec.btf2array(baselineIterator.next()));
		}
		int baselineRows = 0;
		for(ProblemSpec.Dataset d : baselineData){
			baselineRows += d.features.length;
		}
		double[][] baselineFeatures = new double[baselineRows][pspec.getNumFeatures()];
		double[][] baselineOutputs = new double[baselineRows][pspec.getNumOutputs()];
		int rowctr=0;
		for(int btfidx=0;btfidx<baselineData.size();btfidx++){
			ReynoldsFeatures.copyInto(baselineData.get(btfidx).features,baselineFeatures,0,rowctr);
			ReynoldsFeatures.copyInto(baselineData.get(btfidx).outputs,baselineOutputs,0,rowctr);
			rowctr += baselineData.get(btfidx).features.length;
		}
		LearnerAgent baselineLearner = pspec.makeLearner();
		baselineLearner.train(baselineFeatures,baselineOutputs);
		ArrayList<PerformanceMetric> baselinePerf = new ArrayList<PerformanceMetric>();
		if(cvData.size()>0){
			baselinePerf = pspec.evaluate(cvData,baselineLearner,pool);
			System.out.println("Baseline learner performance:");
			for(PerformanceMetric pm : baselinePerf){
				System.out.println(pm);
			}
			if(outputDirectory != null){
				try{
					BufferedWriter perfFile = new BufferedWriter(new FileWriter(new File(outputDirectory,"evaluation_log.txt"), true));
					perfFile.write("Baseline:\n");
					for(PerformanceMetric pm : baselinePerf){
						perfFile.write(pm+"\n");
					}
					perfFile.close();
					if(!dontSaveLearner){
						File saveTo = new File(outputDirectory,"learner_baseline.txt");
						System.out.print("Writting baseline learner to: "+saveTo);
						System.out.flush();
						BufferedWriter saveFile = new BufferedWriter(new FileWriter(saveTo));
						baselineLearner.saveParameters(saveFile);
						saveFile.close();
					}
					System.out.println(" done");
				} catch(IOException ioe){
					throw new RuntimeException("[DataAsDemonstrator] Failed writting baseline learner: "+ioe);					
				}
			}
		}


		//add initial data
		ListIterator<BTFData> dadIterator = trainingData.listIterator();
		activeRealTrainingSequences.add(dadIterator.next());
		boolean outOfData = false;
		int numRealDataPoints;

		try{
			numRealDataPoints = activeRealTrainingSequences.get(0).loadColumn("id").length;
		} catch(IOException ioe){
			throw new RuntimeException("[DataAsDemonstrator] Error getting seq sizes: "+ioe);
		}
		int iterationCounter = 0;
		while((!outOfData) && (iterationCounter<maxIterations || maxIterations<0)){
			System.out.println("Beginning iteration "+iterationCounter);
			//1. Train predictor
			//1.0 Add training data until it balances the d-a-d data, or until we run out
			while(numRealDataPoints < dad_training_inputs.size() && dadIterator.hasNext()){
				BTFData seqHolder = dadIterator.next();
				try{
					numRealDataPoints += seqHolder.loadColumn("id").length;
				} catch(IOException ioe){
					throw new RuntimeException("[DataAsDemonstrator] Error getting seq sizes (inner loop): "+ioe);
				}
				activeRealTrainingSequences.add(seqHolder);
			}
			if(!dadIterator.hasNext()) outOfData = true;
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
				ReynoldsFeatures.copyInto(dataHolder.features, combinedFeatures, 0, dataOffset);
				ReynoldsFeatures.copyInto(dataHolder.outputs, combinedOutputs, 0, dataOffset);
				dataOffset += dataHolder.features.length;
			}
			LearnerAgent learner = pspec.makeLearner();
			learner.train(combinedFeatures,combinedOutputs);
			ArrayList<PerformanceMetric> perf = new ArrayList<PerformanceMetric>();
			if(cvData.size()>0){
				perf = pspec.evaluate(cvData,learner,pool);
				for(PerformanceMetric pm : perf){
					System.out.println(pm);
				}
				// System.out.println("Average error per sequence: "+pspec.computeError(cvData,learner)/(double)cvData.size());
			}
			if(outputDirectory == null){
				rv.add(learner);
			} else {
				try{
					BufferedWriter perfFile = new BufferedWriter(new FileWriter(new File(outputDirectory,"evaluation_log.txt"), true));
					perfFile.write("Iteration "+iterationCounter+":\n");
					for(PerformanceMetric pm : perf){
						perfFile.write(pm+"\n");
					}
					perfFile.close();
					if(!dontSaveLearner){
						File saveTo = new File(outputDirectory,"learner_"+iterationCounter+".txt");
						System.out.print("Writting learner to: "+saveTo);
						System.out.flush();
						BufferedWriter saveFile = new BufferedWriter(new FileWriter(saveTo));
						learner.saveParameters(saveFile);
						saveFile.close();
					}
					System.out.println(" done");
				} catch(IOException ioe){
					throw new RuntimeException("[DataAsDemonstrator] Failed writting learner: "+ioe);
				}
			}
			//2. Kick off simulation threads
			HashSet<Callable<ArrayListDataset> > tasks = new HashSet<Callable<ArrayListDataset> >();
			// HashSet<RunSim> tasks = new HashSet<RunSim>();
			for(int seqIdx=0;seqIdx<activeRealTrainingSequences.size();seqIdx++){
				// Callable<ArrayListDataset> tmp = new RunSim(activeRealTrainingSequences.get(seqIdx),pspec,learner,null);
				final BTFData seq = activeRealTrainingSequences.get(seqIdx);
				final MersenneTwisterFast tLocalRNG = new MersenneTwisterFast(pspec.getRNG().nextLong());
				final LearnerAgent tLocalLearner = learner; //learner.deepCopy();
				final int tLocalThreadID = seqIdx;
				final ProblemSpec tLocalPspec = pspec;
				Callable<ArrayListDataset> tmp = new Callable<ArrayListDataset>(){
					public ArrayListDataset call(){
						// System.out.println("THREAD ID "+tLocalThreadID+" START");
						int numSimSteps = seq.numUniqueFrames();
						ArrayList<Integer> idList=seq.getUniqueIDs();
						ArrayListDataset rv = new ArrayListDataset();
						for(int idIdx=0;idIdx<idList.size();idIdx++){
							int activeID = idList.get(idIdx);
							ArrayList<Integer> activeIDRows = seq.rowIndexForID(activeID);
							Environment env = tLocalPspec.getEnvironment(tLocalLearner,seq,activeID);
							BTFDataLogger logs = tLocalPspec.getLogger();
							env.addLogger(logs);
							Simulation sim = env.newSimulation();
							// TODO: Come up with a way to get consistant simulations by supplying a single seed.
							// CANNOT rely on these threads to execute in the same order, so probably need to 
							// create new random's seeded from the initial seed and incremented in the same way
							// BEFORE being passed to the threadpool. 
							// Current solution:
							// Generate new rng objects from seeds generated from pspec.getRNG().nextLong();
							sim.random = tLocalRNG;
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
								if(trainIdxCounter >= activeIDRows.size()-1){
									break;
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
								logged_btf.data.get("dvel").set(i,fixedDvelX+" "+fixedDvelY+" "+fixedDvelT);
								fixedRows.add(i);
								trainIdxCounter++;
							}
							ProblemSpec.Dataset fixed_dataset = tLocalPspec.btf2array(logged_btf);
							for(int i=0;i<fixedRows.size();i++){
								int theIdx = fixedRows.get(i);
								rv.inputs.add(fixed_dataset.features[theIdx]);
								rv.outputs.add(fixed_dataset.outputs[theIdx]);
							}
						}
						// System.out.println("THREAD ID "+tLocalThreadID+" END");						
						return rv;
					}
				};
				tasks.add( tmp );
			}
			//4. Collect results
			List<Future<ArrayListDataset> > results;
			try{
				results = pool.invokeAll(tasks);
				for(Future<ArrayListDataset> f : results){
					ArrayListDataset result = f.get();
					dad_training_inputs.addAll(result.inputs);
					dad_training_outputs.addAll(result.outputs);
				}
			} catch(InterruptedException ie){
				pool.shutdown();
				throw new RuntimeException("[DataAsDemonstrator] Sims interrupted: "+ie);
			} catch(ExecutionException ee){
				pool.shutdown();
				ee.printStackTrace();
				throw new RuntimeException("[DataAsDemonstrator] ExecutionException in sim: "+ee);
			}
			//5. Goto 1
			iterationCounter++;
		}
		pool.shutdown();
		return rv;
	}

	public class ArrayListDataset {
		public ArrayList<double[]> inputs, outputs;
		public ArrayListDataset(){
			inputs = new ArrayList<double[]>();
			outputs = new ArrayList<double[]>();
		}
	}

	public static void main(String[] args){
		try{
			ReynoldsFeatures pspec = new ReynoldsFeatures();
			Properties cmdlnDefaults = new Properties(pspec.defaults());
			cmdlnDefaults.setProperty("--iterations","-1");
			cmdlnDefaults.setProperty("--threads",new Integer(Integer.MAX_VALUE).toString());
			cmdlnDefaults.setProperty("--cvFolds","10");
			cmdlnDefaults.setProperty("--learner","KNN");
			cmdlnDefaults.setProperty("--dontSaveLearner","false");
			Properties cmdlnArgs = ArgsToProps.parse(args,cmdlnDefaults);
			pspec.configure(cmdlnArgs);
			DataAsDemonstrator dad = new DataAsDemonstrator();
			BTFSequences seqs = new BTFSequences();
			// seqs.loadDir(new File(args[0]));
			seqs.loadDir(new File(cmdlnArgs.getProperty("--sequences")));
			// int maxIterations = -1;
			int maxIterations = Integer.parseInt(cmdlnArgs.getProperty("--iterations"));
			// int maxThreads = Integer.MAX_VALUE;
			int maxThreads = Integer.parseInt(cmdlnArgs.getProperty("--threads"));
			// File outputDirectory = null;
			File outputDirectory = (cmdlnArgs.getProperty("--output")==null)?null:new File(cmdlnArgs.getProperty("--output"));
			// double cvRatio = 0.1;
			int cvFolds = Integer.parseInt(cmdlnArgs.getProperty("--cvFolds"));
			pspec.learner = cmdlnArgs.getProperty("--learner");
			String learnerPropFile = cmdlnArgs.getProperty("--learnerSettings");
			Properties learnerSettings = new Properties(cmdlnArgs);
			if(learnerPropFile != null){
				learnerSettings.load(new FileReader(new File(learnerPropFile)));
			}
			pspec.setLearnerProps(learnerSettings);
			// for(int i=1;i<args.length;i++){
			// 	if(args[i].equalsIgnoreCase("--threads")){
			// 		maxThreads = Integer.parseInt(args[i+1]);
			// 	} else if(args[i].equalsIgnoreCase("--iterations")){
			// 		maxIterations = Integer.parseInt(args[i+1]);
			// 	} else if(args[i].equalsIgnoreCase("--output")){
			// 		outputDirectory = new File(args[i+1]);
			// 	} else if(args[i].equalsIgnoreCase("--cvRatio")){
			// 		cvRatio = Double.parseDouble(args[i+1]);
			// 	} else if (args[i].equalsIgnoreCase("--learner")){
			// 		pspec.learner = args[i+1];
			// 	// } else if (args[i].equalsIgnoreCase("--norm")){
			// 	// 	pspec.normalize_features = Boolean.parseBoolean(args[i+1]);
			// 	// } else if(args[i].equalsIgnoreCase("--method")){
			// 	// 	pspec.combo_method = args[i+1];
			// 	} else if(args[i].equalsIgnoreCase("--seed")){
			// 		pspec.setSeed(Long.parseLong(args[i+1]));
			// 	} 
			// 	else if(args[i].startsWith("--")) {
			// 		System.out.println("Unrecognized argument: "+args[i]);
			// 		System.out.print("Usage: java biosim.core.learning.DataAsDemonstrator <btfSequenceDir> [--threads <int>] [--iterations <int>] [--output <dir>]");
			// 		System.out.println(" [--cvRatio <double>] [--learner {knn,linreg}] [--norm {true,false}] [--method {sample,average}] [--seed <long>]");
			// 		System.exit(1);
			// 	}
			// }
			try{
				OutputStream outstrm = System.out;
				if(outputDirectory != null){
					outstrm = new BufferedOutputStream(new FileOutputStream(new File(outputDirectory,"settings.prop")));
				}
				String settingsComments = "Data-as-Demonstrator settings\n";
				settingsComments += "Arguments: ";
				for(int i=0;i<args.length;i++){
					settingsComments += args[i]+" ";
				}
				pspec.getSettings().store(outstrm,settingsComments);
				outstrm.flush();
			} catch(IOException ioe){
				throw new RuntimeException("[DataAsDemonstrator] Failed to store settings: "+ioe);
			}
			// ArrayList<LearnerAgent> learners = dad.train(seqs,pspec,maxIterations,maxThreads,outputDirectory,cvRatio);
			dad.kFoldCV(seqs,pspec,maxIterations,maxThreads,outputDirectory,cvFolds,Boolean.parseBoolean(cmdlnArgs.getProperty("--dontSaveLearner")));
			// System.out.println("#of models: "+learners.size());
			System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
			System.out.println("Allocated memory: "+ Runtime.getRuntime().totalMemory());
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
