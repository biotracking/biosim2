#include <flann/flann.hpp>
#include <vector>

class SimpleANN {
	public:
		SimpleANN(const SimpleANN& toCopy);
		SimpleANN(int numFeatures);
		~SimpleANN();
		void add(const double feats[]);
		bool query(const double feats[], int index[], int k);
		SimpleANN& operator=(const SimpleANN& rhs);
	private:
		int dim;
		flann::Index<flann::L2<double> > *searchIndex;
		std::vector<double*> cleanup;
};
