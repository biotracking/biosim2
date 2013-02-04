#include "simple_kde.h"
#include <algorithm>
#include <cmath>
#include <iostream>

using namespace std;

SimpleKDE::SimpleKDE(int dim, double sigma){
	this->dimensionality = dim;
	this->sigma = sigma;
}

SimpleKDE::~SimpleKDE(){
	clear();
}

void SimpleKDE::add(const double sample[]){
	add(sample,1.0);
}

void SimpleKDE::add(const double sample[], const double weight){
	if(!setWeight(sample,weight,true)){
		double *tmp = new double[dimensionality];
		copy(sample,sample+dimensionality,tmp);
		samples.push_back(tmp);
		weights.push_back(weight);
	}
}

void SimpleKDE::addNoCheck(const double sample[], const double weight){
	double *tmp = new double[dimensionality];
	copy(sample,sample+dimensionality,tmp);
	samples.push_back(tmp);
	weights.push_back(weight);
}

bool SimpleKDE::setWeight(const double sample[], const double weight, bool addTo){
	for(unsigned int i=0;i<samples.size();i++){
		double* s = samples[i];
		bool diff = false;
		for(int d=0;d<dimensionality;d++){
			if(sample[d] != s[d]){
				diff = true;
				break;
			}
		}
		if(!diff){
			weights[i] = ((addTo)?weights[i]:0)+weight;
			return true;
		}
	}
	return false;
}

double kernel(const double x[], const int dim, double sigma){
	double d = (double)dim;
	double determinant = pow(sigma,d);
	double coeff = (1.0/(pow(2*M_PI,d/2.0)*sqrt(determinant)));
	double exponent = 0.0;
	for(int i=0;i<dim;i++){
		exponent += pow(x[i],2)*(1.0/sigma);
	}
	exponent = -0.5*exponent;
	return coeff*exp(exponent);
}

double SimpleKDE::estimate(const double x[], const double bandwidth){
	double sum=0.0, weightSum = 0.0;
	double* tmp = new double[dimensionality];
	for(unsigned int i=0;i<samples.size();i++){
		for(unsigned int j=0;j<dimensionality;j++){
			tmp[j] = (x[j] - samples[i][j])/bandwidth;
		}
		sum += weights[i]*kernel(tmp,dimensionality,sigma);
		weightSum += weights[i];
	}
	if(weightSum == 0.0) return 0.0;
	return (1.0/(weightSum*bandwidth)) * sum;
}

void SimpleKDE::getSample(double sample[], int i){
	for(int j=0; j<dimensionality; j++){
		sample[j] = samples[i][j];
	}
}

double SimpleKDE::getWeight(int i){
	return weights[i];
}

void SimpleKDE::clear(){
	for(unsigned int i=0;i<samples.size();i++){
		delete[] samples[i];
		samples[i] = NULL;
	}
	samples.clear();
	weights.clear();
}	
