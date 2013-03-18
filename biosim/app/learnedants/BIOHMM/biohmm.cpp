#include "biohmm.h"
#include <iostream>
#include <cmath>


#define NEG_INF (-numeric_limits<double>::infinity())

using namespace std;

double elnsum(double logx, double logy){
	if(logx == NEG_INF || logy == NEG_INF){
		if(logx != NEG_INF) return logx;
		else return logy;
	} else {
		if(logx < logy){
			double tmp = logx;
			logx = logy;
			logy = tmp;
		}
		return (logx + log(1 + exp(logy - logx)));
	}
}
						
void BIOHMM::calculateLogAlpha(
	const vector<double*> sensorSeq,
	const vector<int> triggerSeq,
	double* const* const logAlpha) const{

	for(unsigned int i=0;i<numStates;i++){
		logAlpha[0][i] = priorLogProb(i)+outputLogProb(i,sensorSeq[0]);
	}
	for(unsigned int t=1;t<sensorSeq.size();t++){
		for(unsigned int j=0;j<numStates;j++){
			logAlpha[t][j] = NEG_INF;
			for(unsigned int i=0;i<numStates;i++){
				logAlpha[t][j] = elnsum(logAlpha[t][j], logAlpha[t-1][i]+transitionLogProb(i,j,triggerSeq[t-1]));
			}
			logAlpha[t][j] += outputLogProb(j,sensorSeq[t]);
		}
	}
}
void BIOHMM::calculateLogBeta(
	const vector<double*> sensorSeq,
	const vector<int> triggerSeq,
	double* const* const logBeta) const{
	
	for(int i=0;i<numStates;i++){
		logBeta[sensorSeq.size()-1][i] = 0.0;
	}
	for(int t=sensorSeq.size()-2;t>=0;t--){
		for(int i=0;i<numStates;i++){
			logBeta[t][i] = NEG_INF;
			for(int j=0;j<numStates;j++){
				double tmpLog = transitionLogProb(i,j,triggerSeq[t]);
				tmpLog += outputLogProb(j,sensorSeq[t+1]);
				tmpLog += logBeta[t+1][j];
				logBeta[t][i] = elnsum(logBeta[t][i],tmpLog);
			}
		}
	}
}
void BIOHMM::calculateLogXi(
	const vector<double*> sensorSeq,
	const vector<int> triggerSeq,
	const double* const* const logAlpha, 
	const double* const* const logBeta, 
	double* const* const* const logXi) const{
	
	double logSum;
	for(int t=0;t<sensorSeq.size();t++){
		logSum = NEG_INF;
		for(int i=0;i<numStates;i++){
			for(int j=0;j<numStates;j++){
				if(t == (sensorSeq.size()-1)){
					logXi[t][i][j] = NEG_INF;
				} else {
					logXi[t][i][j] = logAlpha[t][i];
					logXi[t][i][j] += transitionLogProb(i,j,triggerSeq[t]);
					logXi[t][i][j] += outputLogProb(j,sensorSeq[t+1]);
					logXi[t][i][j] += logBeta[t+1][j];
					logSum = elnsum(logSum,logXi[t][i][j]);
				}
			}
		}
		for(int i=0;i<numStates;i++){
			for(int j=0;j<numStates;j++){
				if(logXi[t][i][j] != NEG_INF){
					logXi[t][i][j] = logXi[t][i][j]-logSum;
				}
			}
		}
	}
}
void BIOHMM::calculateLogGamma(
	const unsigned int seqLength,
	const double* const* const logAlpha, 
	const double* const* const logBeta, 
	double* const* logGamma) const{
	
	for(int t=0;t<seqLength;t++){
		double logSum = NEG_INF, tmpls = NEG_INF;
		for(int i=0;i<numStates;i++){
			logGamma[t][i] = logAlpha[t][i]+logBeta[t][i];
			logSum = elnsum(logSum,logGamma[t][i]);
		}
		for(int i=0;i<numStates;i++){
			logGamma[t][i] = logGamma[t][i] - logSum;
		}
	}
}

double BIOHMM::loglikeForSeq(const unsigned int seqLength, const double* const* const logAlpha) const{
	double rv = NEG_INF;
	for(int i=0;i<numStates;i++){
		rv = elnsum(rv,logAlpha[seqLength-1][i]);
	}
	return rv;
}

double BIOHMM::loglikeForSeq(const std::vector<double*> sensorSeq, const std::vector<int> triggerSeq) const {
	double** logAlpha = new double*[sensorSeq.size()];
	for(unsigned int t=0;t<sensorSeq.size();t++){
		logAlpha[t] = new double[numStates];
	}
	calculateLogAlpha(sensorSeq, triggerSeq, logAlpha);
	double rv = loglikeForSeq(sensorSeq.size(), logAlpha);
	for(int t=0;t<sensorSeq.size();t++){
		delete[] logAlpha[t];
	}
	delete[] logAlpha;
	return rv;
}

std::vector<int> BIOHMM::viterbiSequence(const vector<double*> sensorSeq, const vector<int> triggerSeq) const {
	unsigned int seqLength = sensorSeq.size();
	double** logDelta = new double*[seqLength];
	int** psi = new int*[seqLength];
	for(int t=0;t<seqLength;t++){
		logDelta[t] = new double[numStates];
		psi[t] = new int[numStates];
	}
	for(unsigned int i=0;i<numStates;i++){
		logDelta[0][i] = priorLogProb(i)+outputLogProb(i,sensorSeq[0]);
		psi[0][i] = 0;
	}
	for(unsigned int t=1; t<seqLength; t++){
		for(unsigned int j=0;j<numStates;j++){
			int maxState = 0;
			double max = logDelta[t-1][0], tmp;
			max += transitionLogProb(0,j,triggerSeq[t-1]);
			for(int i=1;i<numStates;i++){
				tmp = logDelta[t-1][i] + transitionLogProb(i,j,triggerSeq[t-1]);
				if(tmp > max){
					max = tmp;
					maxState = i;
				}
			}
			logDelta[t][j] = max + outputLogProb(j,sensorSeq[t]);
			psi[t][j] = maxState;
		}
	}
	int tmpMaxState = 0;
	for(int i=0;i<numStates;i++){
		if(logDelta[seqLength-1][i] > logDelta[seqLength-1][tmpMaxState]){
			tmpMaxState = i;
		}
	}
	vector<int> rv;
	int prev = tmpMaxState;
	rv.push_back(tmpMaxState);
	for(unsigned int t=seqLength-2;t>=0;t--){
		prev = psi[t+1][prev];
		rv.push_back(prev);
	}
	reverse(rv.begin(),rv.end());
	//cleanup
	for(int t=0;t<seqLength;t++){
		delete[] logDelta[t];
		delete[] psi[t];
	}
	delete[] logDelta;
	delete[] psi;
	return rv;
}

void BIOHMM::train(
	const vector<vector<double*> > sensorSeqs,
	const vector<vector<int> > triggerSeqs,
	const double epsilon,
	const int numIterations){
	
	double previousLL = NEG_INF;
	int iter = 0;
	bool outOfIters = false;
	bool converged = false;
	double*** newTransitions = new double**[numStates];
	double*** newTransitionsNum = new double**[numStates];
	double*** newTransitionsDen = new double**[numStates];
	double* newPrior = new double[numStates];
	for(int i=0;i<numStates;i++){
		newTransitions[i] = new double*[numStates];
		newTransitionsNum[i] = new double*[numStates];
		newTransitionsDen[i] = new double*[numStates];
		for(int j=0;j<numStates;j++){
			newTransitions[i][j] = new double[numSwitches];
			newTransitionsNum[i][j] = new double[numSwitches];
			newTransitionsDen[i][j] = new double[numSwitches];
		}
	}
		
	do{
		for(int i=0;i<numStates;i++){
			newPrior[i] = 0.0;
			for(int j=0;j<numStates;j++){
				for(int k=0;k<numSwitches;k++){
					newTransitions[i][j][k] = 0.0;
					newTransitionsNum[i][j][k] = 0.0;
					newTransitionsDen[i][j][k] = 0.0;
				}
			}
		}
		iter++;
		
		for(int seq=0;seq<sensorSeqs.size();seq++){
			unsigned int seqSize = sensorSeqs[seq].size();
			double** logAlpha = new double*[seqSize];
			double** logBeta = new double*[seqSize];
			double** logGamma = new double*[seqSize];
			double*** logXi = new double**[seqSize];
			for(unsigned int t=0;t<seqSize;t++){
				logAlpha[t] = new double[numStates];
				logBeta[t] = new double[numStates];
				logGamma[t] = new double[numStates];
				logXi[t] = new double*[numStates];
				for(unsigned int i=0;i<numStates;i++){
					logXi[t][i] = new double[numStates];
				}
			}
			//calculate intermediate variables
			calculateLogAlpha(sensorSeqs[seq], triggerSeqs[seq], logAlpha);
			calculateLogBeta(sensorSeqs[seq], triggerSeqs[seq], logBeta);
			calculateLogXi(sensorSeqs[seq], triggerSeqs[seq], logAlpha, logBeta, logXi);
			calculateLogGamma(seqSize, logAlpha, logBeta, logGamma);
			
			//update parameters
			//prior
			for(unsigned int i=0;i<numStates;i++){
				newPrior[i] += exp(logGamma[0][i])/sensorSeqs.size();
			}
			//transition
			for(unsigned int i=0;i<numStates;i++){
				for(unsigned int j=0;j<numStates;j++){
					for(unsigned int k=0; k<numSwitches;k++){
						double numSum = NEG_INF, denSum = NEG_INF;
						for(unsigned int t=0;t<seqSize;t++){
							if(k==triggerSeqs[seq][t]){
								numSum = elnsum(numSum,logXi[t][i][j]);
								denSum = elnsum(denSum,logGamma[t][i]);
							}
						}
						newTransitionsNum[i][j][k] = elnsum(newTransitionsNum[i][j][k], numSum);
						newTransitionsDen[i][j][k] = elnsum(newTransitionsDen[i][j][k], denSum);
					}
				}
			}
			//weights - hmmm
			
			
			
			//clean up per-sequence arrays
			for(unsigned int t=0;t<seqSize;t++){
				for(unsigned int i=0;i<numStates;i++){
					delete[] logXi[t][i];
				}
				delete[] logAlpha[t];
				delete[] logBeta[t];
				delete[] logGamma[t];
				delete[] logXi[t];
			}
			delete[] logAlpha;
			delete[] logBeta;
			delete[] logGamma;
			delete[] logXi;
		}
		//combine numerator and denominator for transition
		for(int i=0;i<numStates;i++){
			for(int j=0;j<numStates;j++){
				for(int k=0;k<numSwitches;k++){
					newTransitions[i][j][k] = exp(newTransitionsNum[i][j][k]-newTransitionsDen[i][j][k]);
				}
			}
		}
		
		
	} while(!converged && !outOfIters);
	for(int i=0;i<numStates;i++){
		for(int j=0;j<numStates;j++){
			delete[] newTransitions[i][j];
			delete[] newTransitionsNum[i][j];
			delete[] newTransitionsDen[i][j];
		}
		delete[] newTransitions[i];
		delete[] newTransitionsNum[i];
		delete[] newTransitionsDen[i];
	}
	delete[] newTransitions;
	delete[] newTransitionsNum;
	delete[] newTransitionsDen;
	delete[] newPrior;
}

int main(int argc, char* argv[]){
	return 0;
}
