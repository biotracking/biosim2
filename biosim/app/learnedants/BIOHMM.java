package biosim.app.learnedants;

import biosim.core.util.BTFData;
import biosim.core.util.KernelDensityEstimator;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

public class BIOHMM{
	double[][][] transitionFunction;
	double[] prior;
	int[] partition;
	String[] binarySwitchNames;
	BTFData data;
	int dim, numThreads = 4;
	double kernelSigma = 1.0, bandwidth = 1.0;
	
	private String[] desiredVel, wallVec, wallBool, antVec, antBool, prevVec;
	
	public BIOHMM(int numStates, int outputDimensionality, String[] binarySwitchNames, BTFData data) throws IOException{
		this.binarySwitchNames = binarySwitchNames;
		this.data = data;
		dim = outputDimensionality;
		transitionFunction = new double[numStates][numStates][(int)Math.pow(2,binarySwitchNames.length)];
		prior = new double[numStates];
		partition = new int[data.loadColumn("id").length];
		for(int i=0;i<numStates;i++){
			for(int j=0;j<numStates;j++){
				for(int k=0;k<(int)Math.pow(2,binarySwitchNames.length);k++){
					transitionFunction[i][j][k] = 1.0/(numStates*numStates);
				}
			}
			prior[i] = 1.0/numStates;
		}
		for(int i=0;i<partition.length;i++){
			partition[i] = (int)Math.floor(i/(partition.length/numStates));
		}
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
	
	public void calculateScaledAlpha(	ArrayList<Integer> seq,
										KernelDensityEstimator[] b,
										double[][] hat_alpha,
										double[] coeff_c){
		double[] bar_alpha = new double[prior.length];
		for(int t=0;t<seq.size();t++) coeff_c[t] = 0.0;
		for(int j=0;j<prior.length;j++){
			bar_alpha[j] = prior[j]*b[j].estimate(getDataAtIDX(seq.get(0)),bandwidth);
			coeff_c[0] += bar_alpha[j];
		}
		coeff_c[0] = 1.0/coeff_c[0];
		for(int j=0;j<prior.length;j++){
			hat_alpha[0][j] = coeff_c[0] * bar_alpha[j];
		}
		//now do DP
		for(int t=1;t<seq.size();t++){
			for(int j=0;j<prior.length;j++){
				bar_alpha[j] = 0.0;
				for(int i=0;i<prior.length;i++){
					double tmp = hat_alpha[t-1][i];
					tmp = tmp*transitionFunction[i][j][getSwitchAtIDX(seq.get(t-1))];
					tmp = tmp*b[j].estimate(getDataAtIDX(seq.get(t)),bandwidth);
					bar_alpha[j] += tmp;
				}
				coeff_c[t] += bar_alpha[j];
			}
			coeff_c[t] = 1.0/coeff_c[t];
			for(int j=0;j<prior.length;j++){
				hat_alpha[t][j] = coeff_c[t] * bar_alpha[j];
			}
		}
	}
	
	public void calculateScaledBeta(	ArrayList<Integer> seq,
										KernelDensityEstimator[] b,
										double[][] hat_beta,
										double[] coeff_c){
		double[] bar_beta = new double[prior.length];
		for(int i=0;i<prior.length;i++){
			bar_beta[i] = 1.0;
			hat_beta[seq.size()-1][i] = coeff_c[seq.size()-1]*bar_beta[i];
		}
		//now do DP
		for(int t=seq.size()-2;t>=0;t--){
			for(int j=0;j<prior.length;j++){
				bar_beta[j] = 0.0;
				for(int i=0;i<prior.length;i++){
					double tmp = transitionFunction[i][j][getSwitchAtIDX(seq.get(t))];
					tmp = tmp * b[j].estimate(getDataAtIDX(seq.get(t+1)),bandwidth);
					tmp = tmp * hat_beta[t+1][i];
					bar_beta[j] += tmp;
				}
				hat_beta[t][j] = coeff_c[t]*bar_beta[j];
			}
		}
	}
	
	public void calcXiFromScaled(	ArrayList<Integer> seq,
									KernelDensityEstimator[] b,
									double[][] hat_alpha,
									double[][] hat_beta,
									double[][][] xi){
		for(int t=0;t<seq.size();t++){
			for(int i=0;i<prior.length;i++){
				for(int j=0;j<prior.length;j++){
					//the beta check goes one step past the end of the
					//sequences, so we set our probability of 
					//transitioning from i at time T to j at time T+1
					//equal to 1.0, just like we do for beta
					if(t == (seq.size()-1)){
						xi[t][i][j] = 1.0;
					} else {
						xi[t][i][j] = hat_alpha[t][i];
						xi[t][i][j] *= transitionFunction[i][j][getSwitchAtIDX(seq.get(t))];
						xi[t][i][j] *= b[j].estimate(getDataAtIDX(seq.get(t+1)),bandwidth);
						xi[t][i][j] *= hat_beta[t+1][j];
					}
				}
			}
		}
	}
	
	public void calcGammaFromXi(double[][][] xi, double[][] gamma){
		for(int t=0;t<xi.length;t++){
			for(int i=0;i<prior.length;i++){
				gamma[t][i] = 0.0;
				for(int j=0;j<prior.length;j++){
					gamma[t][i] += xi[t][i][j];
				}
			}
		}
	}
	
	public BigDecimal calculateSeqLogLikelihood(double[][] hat_alpha, double[] coeff_c){
		//According to Rabiner, the likelihood of the observation sequence under
		//a given model is just the sum of the alpha variable at time T of each
		//state. According to the errata, hat_alpha_i(t) = alpha_i(t)*C_t
		//and C_t = c_1 * c_2 * c_3 *...* c_t
		BigDecimal C_T = BigDecimal.ZERO;
		double rv = 0.0;
		for(int t=0;t<coeff_c.length;t++){
			C_T = C_T.add(new BigDecimal(Math.log(coeff_c[t])));
		}
		for(int i=0;i<prior.length;i++){
			rv = rv + hat_alpha[hat_alpha.length-1][i];
		}
		return ((new BigDecimal(Math.log(rv))).subtract(C_T));
	}
	
	public void updatePrior(double[] newPrior, double[][] gamma, int numSequences){
		for(int i=0;i<prior.length;i++){
			newPrior[i] += gamma[0][i]/numSequences;
		}
	}
	
	public void updateTransitions(	ArrayList<Integer> seq,
									double[][][] transNumerator, 
									double[][][] transDenominator,
									double[][][] xi,
									double[][] gamma){
		for(int i=0;i<prior.length;i++){
			for(int j=0;j<prior.length;j++){
				for(int k=0;k<transNumerator[i][j].length;k++){
					double numSum = 0.0, denomSum = 0.0;
					for(int t=0;t<seq.size();t++){
						if(k== getSwitchAtIDX(seq.get(t))){
							numSum += xi[t][i][j];
							denomSum += gamma[t][i];
						}
					}
					transNumerator[i][j][k] += numSum;
					transDenominator[i][j][k] +=denomSum;
				}
			}
		}		
	}
	
	public void updatePartition(ArrayList<Integer> seq, KernelDensityEstimator[] b, int[] newPartition){
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
	
	public void learn(double epsilon) throws IOException{ learn(getSequences(data), epsilon); }
	
	public void learn(final ArrayList<ArrayList<Integer>> sequences, double epsilon) throws IOException{
		boolean converged = false;
		desiredVel = data.loadColumn("dvel");
		wallVec = data.loadColumn("wallvec");
		wallBool = data.loadColumn("wallbool");
		antVec = data.loadColumn("antvec");
		antBool = data.loadColumn("antbool");
		prevVec = data.loadColumn("pvel");
		int iter = 0;
		//until transitionFunction/prior/partition has converged:
		do{
			System.out.println("Iteration "+(iter+1));
			iter++;
			final double[] newPrior = new double[prior.length];
			final double[][][] newTransition = new double[newPrior.length][newPrior.length][(int)Math.pow(2,binarySwitchNames.length)];
			final double[][][] newTransitionNumerator = new double[newPrior.length][newPrior.length][(int)Math.pow(2,binarySwitchNames.length)];
			final double[][][] newTransitionDenominator = new double[newPrior.length][newPrior.length][(int)Math.pow(2,binarySwitchNames.length)];
			for(int i=0;i<newPrior.length;i++){
				for(int j=0;j<newPrior.length;j++){
					for(int k=0;k<newTransitionNumerator[i][j].length;k++){
						newTransitionNumerator[i][j][k] = 0.0;
						newTransitionDenominator[i][j][k] = 0.0;
						newTransition[i][j][k] = 0.0;
					}
				}
			}
			System.out.println("Initializing KDE's");
			final int[] newPartition = new int[partition.length];
			final KernelDensityEstimator[] b = new KernelDensityEstimator[prior.length];
			double[] datapoint;
			for(int i=0;i<b.length;i++){
				b[i] = new KernelDensityEstimator(dim,new KernelDensityEstimator.NormalKernel(kernelSigma));
				for(int j=0;j<partition.length;j++){
					if(partition[j] == i){
						b[i].add(getDataAtIDX(j));
					}
				}
			}
			//do each sequence in parallel
			/* */
			final ArrayList<ArrayList<Integer>> tmpSeqs = new ArrayList<ArrayList<Integer>>(sequences.size());
			for(int s=0;s<sequences.size();s++) tmpSeqs.add(sequences.get(s));
			Thread[] threads = new Thread[numThreads];
			for(int th=0;th<threads.length;th++){
				threads[th] = new Thread(new Runnable(){
						public void run(){
							int seqsLeft;
							synchronized(tmpSeqs){
								seqsLeft = tmpSeqs.size();
							}
							while(seqsLeft > 0){
								ArrayList<Integer> seq;
								synchronized(tmpSeqs){
									seq = tmpSeqs.remove(0);
								}
								//do the crap
								double[][] hat_alpha = new double[seq.size()][prior.length];
								double[] coeff_c = new double[seq.size()];
								calculateScaledAlpha(seq, b, hat_alpha, coeff_c);
								double[][] hat_beta = new double[seq.size()][prior.length];
								calculateScaledBeta(seq, b, hat_beta, coeff_c);
								double[][][] xi = new double[seq.size()][prior.length][prior.length];
								calcXiFromScaled(seq,b,hat_alpha,hat_beta,xi);
								double[][] gamma = new double[seq.size()][prior.length];
								calcGammaFromXi(xi,gamma);
								synchronized(newPrior){
									updatePrior(newPrior,gamma,sequences.size());
								}
								synchronized(newTransitionNumerator){
									updateTransitions(seq,newTransitionNumerator, newTransitionDenominator, xi, gamma);
								}
								synchronized(newPartition){
									updatePartition(seq, b, newPartition);
								}
								System.out.println("Log likelihood of sequences: "+ calculateSeqLogLikelihood(hat_alpha,coeff_c));
								//end do the crap
								synchronized(tmpSeqs){
									seqsLeft = tmpSeqs.size();
								}
							}
						}
					});
				threads[th].start();
			}
			try{
				for(int th=0;th<threads.length;th++){
					threads[th].join();
				}
			}catch(InterruptedException ie){
				throw new RuntimeException(ie);
			}
			/* */
			/*
			//for each sequence:
			for(int s=0;s<sequences.size();s++){
				System.out.println("\tSequence "+s);
				ArrayList<Integer> seq = sequences.get(s);
				SingleSequenceUpdate ssu = new SingleSequenceUpdate(	this,
																		seq,
																		b,
																		newPrior,
																		newTransitionNumerator,
																		newTransitionDenominator,
																		newPartition,
																		sequences.size());
				try{
					Thread t = new Thread(ssu);
					t.start();
					t.join();
				} catch(Exception e){
					throw new RuntimeException(e);
				}
				*/
				/*
				//compute alpha_t(j), all t, all j
				System.out.println("\t Computing alpha");
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
				*/
				/*
				//Scaled variables, using equations taken from
				//"An Erratum for 'A Tutorial on Hidden Markov Models and Selected Applications
				//in Speech Recognition'"
				System.out.println("\t Computing scaled alpha");
				//double[] bar_alpha = new double[prior.length];
				double[][] hat_alpha = new double[seq.size()][prior.length];
				double[] coeff_c = new double[seq.size()];
				calculateScaledAlpha(seq,b,hat_alpha,coeff_c);
				*/
				/*
				for(int t=0;t<seq.size();t++) coeff_c[t] = 0.0;
				for(int j=0;j<prior.length;j++){
					bar_alpha[j] = prior[j]*b[j].estimate(getDataAtIDX(seq.get(0)),bandwidth);
					coeff_c[0] += bar_alpha[j];
				}
				coeff_c[0] = 1.0/coeff_c[0];
				for(int j=0;j<prior.length;j++){
					hat_alpha[0][j] = coeff_c[0] * bar_alpha[j];
				}
				//now do DP
				for(int t=1;t<sequences.get(s).size();t++){
					for(int j=0;j<prior.length;j++){
						bar_alpha[j] = 0.0;
						for(int i=0;i<prior.length;i++){
							double tmp = hat_alpha[t-1][i];
							tmp = tmp*transitionFunction[i][j][getSwitchAtIDX(seq.get(t-1))];
							tmp = tmp*b[j].estimate(getDataAtIDX(seq.get(t)),bandwidth);
							bar_alpha[j] += tmp;
						}
						coeff_c[t] += bar_alpha[j];
					}
					coeff_c[t] = 1.0/coeff_c[t];
					for(int j=0;j<prior.length;j++){
						hat_alpha[t][j] = coeff_c[t] * bar_alpha[j];
					}
				}
				*/
				/*
				//compute beta_t(i), all t, all i
				System.out.println("\t Computing beta");
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
				*/
				/*
				System.out.println("\t Computing scaled beta");
				//double[] bar_beta = new double[prior.length];
				double[][] hat_beta = new double[seq.size()][prior.length];
				calculateScaledBeta(seq,b,hat_beta,coeff_c);
				*/
				/*
				for(int i=0;i<prior.length;i++){
					bar_beta[i] = 1.0;
					hat_beta[seq.size()-1][i] = coeff_c[seq.size()-1]*bar_beta[i];
				}
				//now do DP
				for(int t=seq.size()-2;t>=0;t--){
					for(int j=0;j<prior.length;j++){
						bar_beta[j] = 0.0;
						for(int i=0;i<prior.length;i++){
							double tmp = transitionFunction[i][j][getSwitchAtIDX(seq.get(t))];
							tmp = tmp * b[j].estimate(getDataAtIDX(seq.get(t+1)),bandwidth);
							tmp = tmp * hat_beta[t+1][i];
							bar_beta[j] += tmp;
						}
						hat_beta[t][j] = coeff_c[t]*bar_beta[j];
					}
				}
				*/
				/*
				//now that we have scaled alpha and beta, we can compute
				//xi then gamma from the scaled values
				System.out.println("\t Computing xi");
				double[][][] xi = new double[seq.size()][prior.length][prior.length];
				calcXiFromScaled(seq,b,hat_alpha,hat_beta,xi);
				*/
				/*
				for(int t=0;t<seq.size();t++){
					for(int i=0;i<prior.length;i++){
						for(int j=0;j<prior.length;j++){
							//the beta check goes one step past the end of the
							//sequences, so we set our probability of 
							//transitioning from i at time T to j at time T+1
							//equal to 1.0, just like we do for beta
							if(t == (seq.size()-1)){
								xi[t][i][j] = 1.0;
							} else {
								xi[t][i][j] = hat_alpha[t][i];
								xi[t][i][j] *= transitionFunction[i][j][getSwitchAtIDX(seq.get(t))];
								xi[t][i][j] *= b[j].estimate(getDataAtIDX(seq.get(t+1)),bandwidth);
								xi[t][i][j] *= hat_beta[t+1][j];
							}
						}
					}
				}
				*/
				/*
				System.out.println("\t Computing gamma from xi");
				//compute gamma from xi
				double[][] gamma = new double[seq.size()][prior.length];
				calcGammaFromXi(xi,gamma);
				*/
				/*
				for(int t=0;t<seq.size();t++){
					for(int i=0;i<prior.length;i++){
						gamma[t][i] = 0.0;
						for(int j=0;j<prior.length;j++){
							gamma[t][i] += xi[t][i][j];
						}
					}
				}
				*/
				
				/*
				//compute gamma_t(i) for all t, for all i
				System.out.println("\t Computing gamma");
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
				System.out.println("\t Computing xi");
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
				*/
				/*
				//compute prior
				System.out.println("\t Updating prior");
				updatePrior(newPrior,gamma,sequences.size());
				*/
				/*
				for(int i=0;i<prior.length;i++){
					//remember, we're doing this over a 
					//number of sequences
					newPrior[i] += gamma[0][i]/sequences.size();
				}
				*/
				/*
				System.out.println("\t Updating transition table");
				//compute transitionFunction
				//accumulate numerator and denom seperately
				updateTransitions(seq, newTransitionNumerator, newTransitionDenominator, xi, gamma);
				*/
				/*
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
				*/
				/*
				//compute veterbi, outputFunction
				System.out.println("\t Computing viterbi");
				//since we only update one patch of data per
				//sequence, we don't have to do anything different here
				updatePartition(seq,b,newPartition);
				*/
				/*
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
				*/
				//System.out.println("\t Log likelihood of sequence: "+calculateSeqLogLikelihood(hat_alpha,coeff_c));
			//}
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
			System.out.println("Prior delta: "+priorDifference);
			System.out.println("Transition delta: "+ transitionDiff);
			System.out.println("Partition delta: "+ percentPartChanged);
			if(priorDifference < epsilon && transitionDiff < epsilon && percentPartChanged < epsilon) converged = true;
			prior = newPrior;
			transitionFunction = newTransition;
			partition = newPartition;
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
				biohmm.learn(sequences,0.01);
			} catch(Exception e){
				throw new RuntimeException(e);
			}
		}
	}
	
	public class SingleSequenceUpdate implements Runnable {
		public BIOHMM parent;
		public ArrayList<Integer> seq;
		public KernelDensityEstimator[] b;
		public double[] newPrior;
		public double[][][] newTransitionNumerator, newTransitionDenominator;
		public int[] newPartition;
		public int numSequences;
		public SingleSequenceUpdate(	BIOHMM parent, 
										ArrayList<Integer> seq, 
										KernelDensityEstimator[] b,
										double[] newPrior,
										double[][][] newTransitionNumerator,
										double[][][] newTransitionDenominator,
										int[] newPartition,
										int numSequences){ 
			this.parent = parent;
			this.seq = seq;
			this.b = b;
			this.newPrior = newPrior;
			this.newTransitionNumerator = newTransitionNumerator;
			this.newTransitionDenominator = newTransitionDenominator;
			this.newPartition = newPartition;
			this.numSequences = numSequences;
		}
		public void run(){
			double[][] hat_alpha = new double[seq.size()][parent.prior.length];
			double[] coeff_c = new double[seq.size()];
			parent.calculateScaledAlpha(seq, b, hat_alpha, coeff_c);
			double[][] hat_beta = new double[seq.size()][prior.length];
			parent.calculateScaledBeta(seq,b,hat_beta,coeff_c);
			double[][][] xi = new double[seq.size()][parent.prior.length][parent.prior.length];
			calcXiFromScaled(seq,b,hat_alpha,hat_beta,xi);
			double[][] gamma = new double[seq.size()][parent.prior.length];
			calcGammaFromXi(xi,gamma);
			synchronized(newPrior){
				parent.updatePrior(newPrior, gamma, numSequences);
			}
			synchronized(newTransitionNumerator){
				parent.updateTransitions(seq,newTransitionNumerator, newTransitionDenominator, xi, gamma);
			}
			synchronized(newPartition){
				parent.updatePartition(seq,b,newPartition);
			}
			System.out.println("Log likelihood of sequence: "+parent.calculateSeqLogLikelihood(hat_alpha,coeff_c));
		}
	}
	
}
