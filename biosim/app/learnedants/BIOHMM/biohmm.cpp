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
	double** logAlpha) const{

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
	double** logBeta) const{

	
}
void BIOHMM::calculateLogXi(
	const vector<double*> sensorSeq,
	const vector<int> triggerSeq,
	const double** logAlpha, 
	const double** logBeta, 
	double** logXi) const{
	
	
}
void BIOHMM::calculateLogGamma(
	const vector<double*> sensorSeq,
	const vector<int> triggerSeq,
	const double** logAlpha, 
	const double** logBeta, 
	double** logGamma) const{
	
	
}

int main(int argc, char* argv[]){
	return 0;
}
