package biosim.app.learnedants;

import biosim.core.body.AbstractAnt;
import biosim.core.util.BTFData;
import biosim.core.util.KernelDensityEstimator;
import biosim.core.util.kdewrapper.SimpleKDE;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import sim.util.MutableDouble2D;

public class BIOHMMInputParser {
	BTFData data;
	protected String[] desiredVel, wallVec, foodVec, nearFoodBool, antVec, nestVec, nearNestBool, prevVec, gripperBool, stateVec;
	protected int numTrackPoints;
	public static final int NUM_SENSORS = 8;
	public static final int DIM = 11;
	public static final int NUM_SWITCHES = 2;
	protected ArrayList<ArrayList<Integer>> foundSequences = null;
	public Random random = null;
	public BIOHMMInputParser(BTFData data){
		this.data = data;
		try{
			desiredVel = data.loadColumn("dvel");
			wallVec = data.loadColumn("wallvec");
			foodVec = data.loadColumn("foodvec");
			//nearFoodBool = data.loadColumn("nfood");
			gripperBool = data.loadColumn("gripper");
			antVec = data.loadColumn("antvec");
			nestVec = data.loadColumn("homevec");
			nearNestBool = data.loadColumn("nnest");
			prevVec = data.loadColumn("pvel");
			stateVec = data.loadColumn("state");
			numTrackPoints = data.loadColumn("id").length;
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
		random = new Random();
	}
	public double[] getDataAtIDX(int idx){
		double[] datapoint = new double[DIM];
		String[] tmp = desiredVel[idx].split(" ");
		datapoint[0] = Double.parseDouble(tmp[0]);
		datapoint[1] = Double.parseDouble(tmp[1]);
		datapoint[2] = Double.parseDouble(tmp[2]);
		tmp = wallVec[idx].split(" ");
		datapoint[3] = Double.parseDouble(tmp[0]);
		datapoint[4] = Double.parseDouble(tmp[1]);
		tmp = antVec[idx].split(" ");
		datapoint[5] = Double.parseDouble(tmp[0]);
		datapoint[6] = Double.parseDouble(tmp[1]);
		tmp = nestVec[idx].split(" ");
		datapoint[7] = Double.parseDouble(tmp[0]);
		datapoint[8] = Double.parseDouble(tmp[1]);
		tmp = foodVec[idx].split(" ");
		datapoint[9] = Double.parseDouble(tmp[0]);
		datapoint[10] = Double.parseDouble(tmp[1]);
		//tmp = prevVec[idx].split(" ");
		//datapoint[7] = Double.parseDouble(tmp[0]);
		//datapoint[8] = Double.parseDouble(tmp[1]);
		//datapoint[9] = Double.parseDouble(tmp[2]);
		return datapoint;
	}
	
	public static double[] getSensors(AbstractAnt antBody){
		double[] sensorVec = new double[NUM_SENSORS];
		MutableDouble2D tmp = new MutableDouble2D();
		antBody.getNearestObstacleVec(tmp.zero());
		sensorVec[0] = tmp.x;
		sensorVec[1] = tmp.y;
		antBody.getNearestSameTypeVec(tmp.zero());
		sensorVec[2] = tmp.x;
		sensorVec[3] = tmp.y;
		antBody.getPoiDir(tmp,"nest");
		sensorVec[4] = tmp.x;
		sensorVec[5] = tmp.y;
		antBody.getNearestPreyVec(tmp.zero());
		sensorVec[6] = tmp.x;
		sensorVec[7] = tmp.y;
		return sensorVec;
	}
	
	public double[] getSensorsAtIDX(int idx){
		double[] datapoint = new double[NUM_SENSORS];
		String[] tmp = wallVec[idx].split(" ");
		datapoint[0] = Double.parseDouble(tmp[0]);
		datapoint[1] = Double.parseDouble(tmp[1]);
		tmp = antVec[idx].split(" ");
		datapoint[2] = Double.parseDouble(tmp[0]);
		datapoint[3] = Double.parseDouble(tmp[1]);
		tmp = nestVec[idx].split(" ");
		datapoint[4] = Double.parseDouble(tmp[0]);
		datapoint[5] = Double.parseDouble(tmp[1]);
		tmp = foodVec[idx].split(" ");
		datapoint[6] = Double.parseDouble(tmp[0]);
		datapoint[7] = Double.parseDouble(tmp[1]);
		return datapoint;
	}
	
	public int getStateAtIDX(int idx){
		int ste = Integer.parseInt(stateVec[idx].trim());
		return ste;
	}
	
	public static int getSwitch(AbstractAnt antBody){
		int k = 0;
		if(antBody.getGripped()){
			k += 1;
		}
		k = k<<1;
		if(antBody.nearPOI("nest")){
			k += 1;
		}
		return k;
	}
	
	public int getSwitchAtIDX(int idx){
		int k = 0;
		if(Boolean.parseBoolean(gripperBool[idx])){
			k += 1;
		}
		k = k<<1;
		if(Boolean.parseBoolean(nearNestBool[idx])){
			k+=1;
		}
		return k;
	}
	
	public int partSize(){
		return numTrackPoints;
	}
	public static int numSwitches(){
		return NUM_SWITCHES;
	}
	public static int outputDim(){
		return DIM;
	}
	public static int sensorDim(){
		return NUM_SENSORS;
	}
	public ArrayList<ArrayList<Integer>> getSequences(){
		if(foundSequences != null) return foundSequences;
		try{
			//parse file's into sequences
			//a sequence is an arraylist of indecies into the BTFData
			String[] antID = data.loadColumn("id");
			String[] clockTime = data.loadColumn("clocktime");
			ArrayList<ArrayList<Integer>> sequences = new ArrayList<ArrayList<Integer>>();
			ArrayList<ArrayList<Integer>> currentSeqIDX = new ArrayList<ArrayList<Integer>>();
			String oldTime = "start";
			for(int i=0;i<antID.length;i++){
				//if the current time != old time
				if(!clockTime[i].equals(oldTime)){
					//check to make sure each sequence in currentSeqIDX got updated
					//if a sequence didn't get updated, remove it from currentSeqIDX
					//and add it to sequences
					for(int j=0;j<currentSeqIDX.size();j++){
						int tmpLastSeenIDX = currentSeqIDX.get(j).get(currentSeqIDX.get(j).size()-1);
						String lastTimeSeen = clockTime[tmpLastSeenIDX];
						if(!lastTimeSeen.equals(oldTime)){
							sequences.add(currentSeqIDX.get(j));
							currentSeqIDX.remove(j);
							j--;
						}
					}
					oldTime = clockTime[i];
				}
				//get the ID of the current line. If it's not currently being
				//tracked, add it to currentSeqIDX along with a new arraylist.
				int curID = Integer.parseInt(antID[i]);
				boolean found = false;
				for(int j=0;j<currentSeqIDX.size();j++){
					int tmpIDX = currentSeqIDX.get(j).get(currentSeqIDX.get(j).size()-1);
					int thisSeqID = Integer.parseInt(antID[tmpIDX]);
					if(thisSeqID == curID){
						currentSeqIDX.get(j).add(i);
						found = true;
						break;
					}
				}
				if(!found){
					ArrayList<Integer> tmpSeqIDX = new ArrayList<Integer>();
					tmpSeqIDX.add(i);
					currentSeqIDX.add(tmpSeqIDX);
				}
			}
			//now add all the remaining active sequences
			for(int j=0;j<currentSeqIDX.size();j++){
				sequences.add(currentSeqIDX.get(j));
			}
			foundSequences = sequences;
			return sequences;
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
	
	public void initParameters(double[][][] transitionFunction, double[] prior, int[] partition, SimpleKDE[] b){
		/*
		int numStates = prior.length;
		for(int i=0;i<numStates;i++){
			for(int j=0;j<numStates;j++){
				for(int k=0;k<(int)Math.pow(2,numSwitches());k++){
					transitionFunction[i][j][k] = 1.0/(numStates);
				}
			}
			prior[i] = 1.0/numStates;
		}
		*/
		ArrayList<ArrayList<Integer>> seqs = getSequences();
		//So, our best guess at initializing the state sequence
		//is to switch states whenever there's a change in switching
		//variables.
		/* 
		boolean justChanged = false;
		int initCurState = -1;
		for(int i=0;i<seqs.size();i++){
			ArrayList<Integer> seq = seqs.get(i);
			initCurState = (initCurState+1) % prior.length;
			int initPrevK = getSwitchAtIDX(seq.get(0));
			for(int j=1;j<seq.size();j++){
				if(initPrevK != getSwitchAtIDX(seq.get(j)) &&!justChanged){
					initCurState = (initCurState+1) % prior.length;
					justChanged = true;
				} else {
					justChanged = false;
				}
				partition[seq.get(j)] = initCurState;
				initPrevK = getSwitchAtIDX(seq.get(j));
			}
		}
		/* */
		//the old way (here) was to initialize our state sequence to give an
		//equal partitioning to each state, and to give long sequences with
		//the same state.
		/* 
		for(int i=0;i<seqs.size();i++){
			ArrayList<Integer> seq = seqs.get(i);
			for(int j=0;j<seq.size();j++){
				partition[seq.get(i)] = (int)Math.floor((j/(seq.size()/numStates)));
			}
		}
		/* */
		//random state initialization.  This is least likely to produce long chains
		//of sequences. 
		/*  */
		for(int i=0;i<partition.length;i++){
			partition[i] = random.nextInt(prior.length);
		}
		/* */
		//For debugging, if we had the correct state sequence
		/*  
		for(int i=0;i<partition.length;i++){
			partition[i] = Integer.parseInt(stateVec[i].trim());
		}
		/* */
		//System.out.print("Initial partition:\n[");
		//int errorCount = 0;
		//int[] stateCount = {0,0};
		//int[] turnCount = {0,0};
		for(int x=0;x<partition.length;x++){
			//System.out.print(partition[x]);
			b[partition[x]].add(getDataAtIDX(x));
			//int ste = Integer.parseInt(stateVec[x].trim());
			//if(ste != partition[x]) errorCount++;
			//stateCount[partition[x]]++;
			//if(Double.parseDouble(desiredVel[x].split(" ")[2]) != 0.0){
			//	turnCount[partition[x]]++;
			//}
		}
		//System.out.println("]");
		//System.out.println("Error rate:"+errorCount+"/"+partition.length+" = "+( (double)errorCount/(double)partition.length));
		//System.out.println("State ratio = "+stateCount[0]+"/"+partition.length+" = "+( (double)stateCount[0]/(double)partition.length));
		//System.out.println("Ratio turn/non-turn[i] "+turnCount[0]+" "+turnCount[1]);
		//System.out.println("\t = "+((double)turnCount[0]/(double)stateCount[0])+" "+((double)turnCount[1]/(double)stateCount[1]));
		//throw new RuntimeException("Break");
		//for(int i=0;i<b.length;i++){
		//	for(int j=0;j<partition.length;j++){
		//		if(partition[j] == i){
		//			b[i].add(getDataAtIDX(j));
		//		}
		//	}
		//}
		int numStates = prior.length;
		int maxSwitchVar = (int)Math.pow(2,numSwitches());
		int[][] fromCount = new int[maxSwitchVar][numStates];
		for(int i=0;i<seqs.size();i++){
			ArrayList<Integer> seq = seqs.get(i);
			prior[partition[seq.get(0)]]++;
			for(int t=0;t<seq.size()-1;t++){
				int fromState = partition[seq.get(t)];
				int toState = partition[seq.get(t+1)];
				int switchVar = getSwitchAtIDX(seq.get(t));
				fromCount[switchVar][fromState]++;
				transitionFunction[fromState][toState][switchVar]++;
			}
		}
		for(int i=0;i<numStates;i++){
			for(int j=0;j<numStates;j++){
				for(int k=0;k<maxSwitchVar;k++){
					System.out.println("FromCount["+k+"]["+i+"] = "+fromCount[k][i]);
					if(fromCount[k][i] > 0){
						transitionFunction[i][j][k] = transitionFunction[i][j][k]/fromCount[k][i];
					} else {
						transitionFunction[i][j][k] = 1.0/numStates;
					}
				}
			}
			prior[i] = prior[i]/seqs.size();
		}
		for(int k=0; k<maxSwitchVar; k++){
			System.out.println("k="+k);
			for(int i=0;i<numStates;i++){
				System.out.print("i="+i+" ");
				for(int j=0;j<numStates;j++){
					System.out.print(transitionFunction[i][j][k]+" ");
				}
				System.out.println();
			}
			System.out.println();
		}
	}
}
