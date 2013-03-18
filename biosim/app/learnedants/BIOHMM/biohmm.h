#include <string>
#include <vector>
#include <cmath>
#include "simple_kde.h"

class BIOHMM{
	public:
		BIOHMM(unsigned int _numStates, unsigned int _numSwitches);
		
		double priorLogProb(int state) const { return log(prior[state]); };
		double transitionLogProb(int from, int to, int trigger) const {
			return log(transitions[from][to][trigger]);
		};
		double outputLogProb(int state, double sample[]) const {
			double jp = input[state].estimate(sample,inputBandwidth);
			double sp = sensors.estimate(sample,sensorBandwidth);
			double rv = log(jp)-log(sp);
			return rv;
		};

		void setPrior(double _prior[]){
			for(unsigned int i=0;i<numStates;i++)
				prior[i] = _prior[i];
		};
		void setTransition(double*** _transitions){
			for(unsigned int i=0;i<numStates;i++){
				for(unsigned int j=0;j<numStates;j++){
					for(unsigned int k=0;k<numSwitches;k++){
						transitions[i][j][k] = _transitions[i][j][k];
					}
				}
			}
		};
		void setOutputWeight(unsigned int state, double sample[], double weight){
			int idx = input[state].getIdx(sample);
			if(idx < 0) input[state].addNoCheck(sample,weight);
			else input[state].setWeight(idx,weight);
		};
		
		void calculateLogAlpha(const std::vector<double*> sensorSeq, const std::vector<int> triggerSeq, double* const* const logAlpha) const;
		void calculateLogBeta(const std::vector<double*> sensorSeq, const std::vector<int> triggerSeq, double* const* const logBeta) const;
		void calculateLogXi(const std::vector<double*> sensorSeq, const std::vector<int> triggerSeq, const double* const* const logAlpha, const double* const* const logBeta, double* const* const* const logXi) const;
		void calculateLogGamma(const unsigned int seqLength, const double* const* const logAlpha, const double* const* const logBeta, double* const* const logGamma) const;
		double loglikeForSeq(const unsigned int seqLength, const double* const* const logAlpha) const;
		double loglikeForSeq(const std::vector<double*> sensorSeq, const std::vector<int> triggerSeq) const;
		std::vector<int> viterbiSequence(const std::vector<double*> sensorSeq, const std::vector<int> triggerSeq) const;
		void train(const std::vector<std::vector<double*> > sensorSeqs, const std::vector<std::vector<int> > triggerSeqs, const double epsilon = 0.0, const int numIterations = -1);

	private:
		unsigned int numStates, numSwitches;
		double*** transitions;
		double* prior;
		SimpleKDE* input;
		double inputBandwidth;
		SimpleKDE sensors;
		double sensorBandwidth;
		
		
		
};
