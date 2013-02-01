#include <vector>

class SimpleKDE {
	public:
		SimpleKDE(int dim, double sigma);
		~SimpleKDE();
		unsigned int numSamples(){ return samples.size(); };
		void add(const double sample[], const double weight);
		void add(const double sample[]);
		bool setWeight(const double sample[], const double weight, bool addTo = false);
		double estimate(const double x[], const double bandwidth);
		void getSample(double sample[], int i);
		double getWeight(int i);
		unsigned int getDim(){ return dimensionality;};
		void clear();
	private:
		std::vector<double*> samples;
		std::vector<double> weights;
		unsigned int dimensionality;
		double sigma;
};
