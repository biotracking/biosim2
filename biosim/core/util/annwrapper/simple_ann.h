#include <vector>
#include "ANN/ANN.h"

class SimpleANN {
	public: 
		SimpleANN(int numFeatures);
		~SimpleANN();
		void add(const double feats[]);
		bool query(const double feats[], int index[], int k);
	private:
		bool updateDataset;
		std::vector<double*> dataset;	
		int dim;
		ANNkd_tree *kdTree;
};
