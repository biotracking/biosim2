package biosim.app.learnedants;

import biosim.core.util.BTFData;
import biosim.core.util.KernelDensityEstimator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class BIOHMM{
	double[][][] transitionFunction;
	double[] prior;
	int[] partition;
	String[] binarySwitchNames;
	BTFData data;
	int dim;
	double kernelSigma = 1.0, bandwidth = 1.0;
	
	private String[] desiredVel, wallVec, wallBool, antVec, antBool, prevVec;
	
	public BIOHMM(int numStates, int outputDimensionality, String[] binarySwitchNames, BTFData data) throws IOException{
		this.binarySwitchNames = binarySwitchNames;
		this.data = data;
		dim = outputDimensionality;
		transitionFunction = new double[numStates][numStates][(int)Math.pow(2,binarySwitchNames.length)];
		prior = new double[numStates];
		partition = new int[data.loadColumn("id").length];
	}
	
	private double[] getDataAtIDX(int idx){
		double[] datapoint = new double[10];
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
		tmp = prevVec[idx].split(" ");
		datapoint[7] = Double.parseDouble(tmp[0]);
		datapoint[8] = Double.parseDouble(tmp[1]);
		datapoint[9] = Double.parseDouble(tmp[2]);
		return datapoint;
	}
	
	private int getSwitchAtIDX(int idx){
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
	
	public void learn() throws IOException{ learn(getSequences(data)); }
	
	public void learn(ArrayList<ArrayList<Integer>> sequences) throws IOException{
		boolean converged = false;
		desiredVel = data.loadColumn("dvel");
		wallVec = data.loadColumn("wallvec");
		wallBool = data.loadColumn("wallbool");
		antVec = data.loadColumn("antvec");
		antBool = data.loadColumn("antbool");
		prevVec = data.loadColumn("pvel");
		//until transitionFunction/prior/partition has converged:
		do{
			double[] newPrior = new double[prior.length];
			double[][][] newTransition = new double[newPrior.length][newPrior.length][(int)Math.pow(2,binarySwitchNames.length)];
			double[][][] newTransitionNumerator = new double[newPrior.length][newPrior.length][(int)Math.pow(2,binarySwitchNames.length)];
			double[][][] newTransitionDenominator = new double[newPrior.length][newPrior.length][(int)Math.pow(2,binarySwitchNames.length)];
			for(int i=0;i<newPrior.length;i++){
				for(int j=0;j<newPrior.length;j++){
					for(int k=0;k<newTransitionNumerator[i][j].length;k++){
						newTransitionNumerator[i][j][k] = 0.0;
						newTransitionDenominator[i][j][k] = 0.0;
						newTransition[i][j][k] = 0.0;
					}
				}
			}
			int[] newPartition = new int[partition.length];
			KernelDensityEstimator[] b = new KernelDensityEstimator[prior.length];
			double[] datapoint;
			for(int i=0;i<b.length;i++){
				b[i] = new KernelDensityEstimator(dim,new KernelDensityEstimator.NormalKernel(kernelSigma));
				for(int j=0;j<partition.length;j++){
					if(partition[j] == i){
						b[i].add(getDataAtIDX(j));
					}
				}
			}
			//for each sequence:
			for(int s=0;s<sequences.size();s++){
				ArrayList<Integer> seq = sequences.get(s);
				//compute alpha_t(j), all t, all j
				double[][] alpha = new double[seq.size()][prior.length];
				for(int j=0;j<prior.length;j++){
					alpha[0][j] = prior[j]*b[j].estimate(getDataAtIDX(seq.get(0)),bandwidth);
				}
				for(int t = 1; t<sequences.get(s).size(); t++){
					for(int j=0;j<prior.length;j++){
						double sum = 0.0;
						for(int i=0;i<prior.length;i++){
							sum += alpha[t-1][i]*transitionFunction[i][j][getSwitchAtIDX(seq.get(t-1))];
						}
						sum = sum * b[j].estimate(getDataAtIDX(seq.get(t)),bandwidth);
					}
				}
				//compute beta_t(i), all t, all i
				double[][] beta = new double[seq.size()][prior.length];
				for(int i=0;i<prior.length;i++){
					beta[seq.size()-1][i] = 1.0;
				}
				for(int t = seq.size()-2;t >= 0;t--){
					for(int i=0;i<prior.length;i++){
						double sum = 0.0;
						for(int j=0;j<prior.length;j++){
							double tmp = transitionFunction[i][j][getSwitchAtIDX(seq.get(t))];
							tmp *= b[j].estimate(getDataAtIDX(seq.get(t+1)),bandwidth);
							tmp *= beta[t+1][j]; 
							sum += tmp;
						}
						beta[t][i] = sum;
					}
				}
				//compute gamma_t(i) for all t, for all i
				double[][] gamma = new double[seq.size()][prior.length];
				for(int t=0;t<seq.size();t++){
					double sum = 0.0;
					for(int j=0;j<prior.length;j++){
						sum+= alpha[t][j]*beta[t][j];
					}
					for(int i=0;i<prior.length;i++){						
						gamma[t][i] = (alpha[t][i]*beta[t][i])/(sum);
					}
				}
				//compute xi_t(i,j) for all t, for all i, for all j
				double[][][] xi = new double[seq.size()][prior.length][prior.length];
				//initialize to zero since we don't hit everything
				for(int i=0;i<prior.length;i++){
					for(int j=0;j<prior.length;j++){
						for(int t=0;t<seq.size();t++){
							xi[t][i][j] = 0.0;
						}
					}
				}
				for(int t=0;t<seq.size()-1;t++){
					double sum = 0.0;
					for(int i=0;i<prior.length;i++){
						for(int j=0;j<prior.length;j++){
							double tmp = alpha[t][i];
							tmp *= transitionFunction[i][j][getSwitchAtIDX(seq.get(t))];
							tmp *= b[j].estimate(getDataAtIDX(seq.get(t+1)),bandwidth);
							tmp *= beta[t+1][j];
							sum += tmp;
						}
					}
					for(int i=0;i<prior.length;i++){
						for(int j=0;j<prior.length;j++){
							xi[t][i][j] = alpha[t][i];
							xi[t][i][j] *= transitionFunction[i][j][getSwitchAtIDX(seq.get(t))];
							xi[t][i][j] *= b[j].estimate(getDataAtIDX(seq.get(t+1)),bandwidth);
							xi[t][i][j] *= beta[t+1][j];
							xi[t][i][j] = xi[t][i][j]/sum;
						}
					}
				}
				
				//compute prior
				for(int i=0;i<prior.length;i++){
					//remember, we're doing this over a 
					//number of sequences
					newPrior[i] += gamma[0][i]/sequences.size();
				}
				//compute transitionFunction
				//accumulate numerator and denom seperately
				for(int i=0;i<prior.length;i++){
					for(int j=0;j<prior.length;j++){
						for(int k=0;k<newTransition[i][j].length;k++){
							double numSum = 0.0, denomSum = 0.0;
							for(int t=0;t<seq.size();t++){
								if(k== getSwitchAtIDX(seq.get(t))){
									numSum += xi[t][i][j];
									denomSum += gamma[t][i];
								}
							}
							newTransitionNumerator[i][j][k] += numSum;
							newTransitionDenominator[i][j][k] +=denomSum;
						}
					}
				}
				//compute veterbi, outputFunction
				//since we only update one patch of data per
				//sequence, we don't have to do anything different here
				double[][] delta = new double[seq.size()][prior.length];
				int[][] psi = new int[seq.size()][prior.length];
				for(int i=0;i<prior.length;i++){
					delta[0][i] = prior[i]*b[i].estimate(getDataAtIDX(seq.get(0)),bandwidth);
					psi[0][i] = 0;
				}
				for(int t=1;t<seq.size();t++){
					for(int j=0;j<prior.length;j++){
						int maxState = 0;
						double max = delta[t-1][0], tmp;
						max *= transitionFunction[0][j][getSwitchAtIDX(seq.get(t-1))];
						for(int i=1;i<prior.length;i++){
							tmp = delta[t-1][i] * transitionFunction[i][j][getSwitchAtIDX(seq.get(t-1))];
							if(tmp > max){ 
								max = tmp;
								maxState = i;
							}
						}
						delta[t][j] = max * b[j].estimate(getDataAtIDX(seq.get(t)),bandwidth);
						psi[t][j] = maxState;
					}
				}
				int tmpMaxState = 0;
				for(int i=0;i<prior.length;i++){
					if(delta[seq.size()-1][i] > delta[seq.size()-1][tmpMaxState]){
						tmpMaxState = i;
					}
				}
				newPartition[seq.get(seq.size()-1)] = tmpMaxState;
				for(int t = seq.size()-2;t >=0;t--){
					newPartition[seq.get(t)] = psi[t+1][newPartition[seq.get(t+1)]];
				}
			}
			//now recombine the numerator and denominator for the transition function
			for(int i=0;i<prior.length;i++){
				for(int j=0;j<prior.length;j++){
					for(int k=0;k<newTransition[i][j].length;k++){
						newTransition[i][j][k] = newTransitionNumerator[i][j][k]/newTransitionDenominator[i][j][k];
					}
				}
			}
			//now. Compute the difference between this new model and the old one
			double priorDifference = 0.0;
			for(int i=0;i<prior.length;i++){
				priorDifference += Math.abs(prior[i] - newPrior[i]);
			}
			double transitionDiff = 0.0;
			for(int i=0;i<prior.length;i++){
				for(int j=0;j<prior.length;j++){
					for(int k=0;k<newTransition[i][j].length;k++){
						transitionDiff += Math.abs(newTransition[i][j][k] - transitionFunction[i][j][k]);
					}
				}
			}
			double percentPartChanged = 0.0;
			for(int i=0;i<partition.length;i++){
				if(partition[i] != newPartition[i]) percentPartChanged += 1.0;
			}
			percentPartChanged /= partition.length;
		} while(!converged);
	}
	
	public ArrayList<ArrayList<Integer>> getSequences(BTFData data) throws IOException{
		//parse file's into sequences
		//a sequence is an arraylist of indecies into the BTFData
		String[] antID = data.loadColumn("id");
		//String[] desiredVel = data.loadColumn("dvel");
		//String[] desiredVelBool = data.loadColumn("dbool");
		//String[] wallVec = data.loadColumn("wallvec");
		//String[] antVec = data.loadColumn("antvec");
		//String[] antBool = data.loadColumn("antbool");
		//String[] prevVec = data.loadColumn("pvel");
		//String[] prevBoolVec = data.loadColumn("pbool");
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
	}
	
	public static void main(String[] args){
		if(args.length != 1){
			System.out.println("Usage: java BIOHMM <btfDirectory>");
		} else {
			try{
				String[] names = {"antbool","wallbool"};
				BTFData btf = new BTFData();
				btf.loadDir(new File(args[0]));
				ArrayList<ArrayList<Integer>> sequences;
				BIOHMM biohmm = new BIOHMM(2,10,names,btf);
				sequences = biohmm.getSequences(btf);
				System.out.println("Num sequences:"+sequences.size());
			} catch(Exception e){
				throw new RuntimeException(e);
			}
		}
	}
	
}
