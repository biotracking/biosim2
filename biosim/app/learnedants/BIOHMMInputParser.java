package biosim.app.learnedants;

import biosim.core.util.BTFData;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class BIOHMMInputParser {
	BTFData data;
	protected String[] desiredVel, wallVec, wallBool, antVec, antBool, prevVec;
	protected int numTrackPoints;
	public BIOHMMInputParser(BTFData data){
		this.data = data;
		try{
			desiredVel = data.loadColumn("dvel");
			wallVec = data.loadColumn("wallvec");
			wallBool = data.loadColumn("wallbool");
			antVec = data.loadColumn("antvec");
			antBool = data.loadColumn("antbool");
			prevVec = data.loadColumn("pvel");
			numTrackPoints = data.loadColumn("id").length;
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
	public double[] getDataAtIDX(int idx){
		double[] datapoint = new double[7];
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
	
	public int getSwitchAtIDX(int idx){
		int k = 0;
		if(Boolean.parseBoolean(wallBool[idx])){
			k += 1;
		}
		k = k<<1;
		if(Boolean.parseBoolean(antBool[idx])){
			k+=1;
		}
		return k;
	}
	
	public int partSize(){
		return numTrackPoints;
	}
	public int numSwitches(){
		return 2;
	}
	public int outputDim(){
		return 7;
	}
	public ArrayList<ArrayList<Integer>> getSequences(){
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
			return sequences;
		} catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}
}
