#include <vector>

class SimpleKDE {
	public:
		SimpleKDE(int dim, double sigma);
		~SimpleKDE();
		unsigned int numSamples() const { return samples.size(); };
		void add(const double sample[], const double weight);
		void add(const double sample[]);
		void addNoCheck(const double sample[], const double weight);
		bool setWeight(const int idx, const double weight);
		double estimate(const double x[], const double bandwidth) const;
		bool getSample(double sample[], const int i) const;
		double getDist(double sample[], const int i) const;
		double getWeight(const int i) const;
		unsigned int getDim()const{ return dimensionality;};
		int getIdx(const double sample[]) const;
		void clear();
	private:
		std::vector<double*> samples;
		std::vector<double> weights;
		unsigned int dimensionality;
		double sigma;
};
