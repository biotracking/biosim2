package biosim.app.learnedants;

import biosim.core.util.BTFData;
import biosim.core.util.KernelDensityEstimator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class BIOHMMInputParser {
	BTFData data;
	protected String[] desiredVel, wallVec, avoidBool, antVec, nearBool, prevVec, stateVec;
	protected int numTrackPoints;
	public static final int NUM_SENSORS = 4;
	public static final int DIM = 7;
	public static final int NUM_SWITCHES = 2;
	protected ArrayList<ArrayList<Integer>> foundSequences = null;
	public Random random = null;
	public BIOHMMInputParser(BTFData data){
		this.data = data;
		try{
			desiredVel = data.loadColumn("dvel");
			wallVec = data.loadColumn("wallvec");
			avoidBool = data.loadColumn("avoid");
			antVec = data.loadColumn("antvec");
			nearBool = data.loadColumn("near");
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
		//tmp = prevVec[idx].split(" ");
		//datapoint[7] = Double.parseDouble(tmp[0]);
		//datapoint[8] = Double.parseDouble(tmp[1]);
		//datapoint[9] = Double.parseDouble(tmp[2]);
		return datapoint;
	}
	
	public double[] getSensorsAtIDX(int idx){
		double[] datapoint = new double[NUM_SENSORS];
		String[] tmp = wallVec[idx].split(" ");
		datapoint[0] = Double.parseDouble(tmp[0]);
		datapoint[1] = Double.parseDouble(tmp[1]);
		tmp = antVec[idx].split(" ");
		datapoint[2] = Double.parseDouble(tmp[0]);
		datapoint[3] = Double.parseDouble(tmp[1]);
		return datapoint;
	}
	
	public int getSwitchAtIDX(int idx){
		int k = 0;
		if(Boolean.parseBoolean(avoidBool[idx])){
			k += 1;
		}
		k = k<<1;
		if(Boolean.parseBoolean(nearBool[idx])){
			k+=1;
		}
		return k;
	}
	
	public int partSize(){
		return numTrackPoints;
	}
	public int numSwitches(){
		return NUM_SWITCHES;
	}
	public int outputDim(){
		return DIM;
	}
	public int sensorDim(){
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
	
	public void initParameters(double[][][] transitionFunction, double[] prior, int[] partition, KernelDensityEstimator[] b){
		int numStates = prior.length;
		for(int i=0;i<numStates;i++){
			for(int j=0;j<numStates;j++){
				for(int k=0;k<(int)Math.pow(2,numSwitches());k++){
					transitionFunction[i][j][k] = 1.0/(numStates);
				}
			}
			prior[i] = 1.0/numStates;
		}
		ArrayList<ArrayList<Integer>> seqs = getSequences();
		//So, our best guess at initializing the state sequence
		//is to switch states whenever there's a change in switching
		//variables.
		/* */
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
		/* 
		for(int i=0;i<partition.length;i++){
			partition[i] = random.nextInt(prior.length);
		}
		/* */
		//For debugging, if we had the correct state sequence
		/* */
		for(int i=0;i<partition.length;i++){
			partition[i] = Integer.parseInt(stateVec[i].trim());
		}
		/* */
		//System.out.print("Initial partition:\n[");
		int errorCount = 0;
		for(int x=0;x<partition.length;x++){
			//System.out.print(partition[x]);
			b[partition[x]].add(getDataAtIDX(x));
			int ste = Integer.parseInt(stateVec[x].trim());
			if(ste != partition[x]) errorCount++;
		}
		//System.out.println("]");
		System.out.println("Error rate:"+errorCount+"/"+partition.length+" = "+( (double)errorCount/(double)partition.length));
		//throw new RuntimeException("Break");
		//for(int i=0;i<b.length;i++){
		//	for(int j=0;j<partition.length;j++){
		//		if(partition[j] == i){
		//			b[i].add(getDataAtIDX(j));
		//		}
		//	}
		//}
		
	}
}
