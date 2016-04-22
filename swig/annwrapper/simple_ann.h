#include <vector>
#include "ANN/ANN.h"

class SimpleANN {
	public: 
		SimpleANN(const SimpleANN& toCopy);
		SimpleANN(int numFeatures);
		~SimpleANN();
		void add(const double feats[]);
		bool query(const double feats[], int index[], int k);
		SimpleANN& operator=(const SimpleANN& rhs);
		void setEpsilon(double epsilon){ epsilon = eps; }
	private:
		bool updateDataset;
		std::vector<double*> dataset;	
		int dim;
		ANNkd_tree *kdTree;
		double eps;
};
