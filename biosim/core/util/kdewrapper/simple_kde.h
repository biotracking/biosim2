#include <vector>

class SimpleKDE {
	public:
		SimpleKDE(int dim, double sigma);
		~SimpleKDE();
		unsigned int numSamples(){ return samples.size(); };
		void add(const double sample[], const double weight);
		void add(const double sample[]);
		void addNoCheck(const double sample[], const double weight);
		bool setWeight(const int idx, const double weight);
		double estimate(const double x[], const double bandwidth);
		bool getSample(double sample[], const int i);
		double getWeight(const int i);
		unsigned int getDim(){ return dimensionality;};
		int getIdx(const double sample[]);
		void clear();
	private:
		std::vector<double*> samples;
		std::vector<double> weights;
		unsigned int dimensionality;
		double sigma;
};
