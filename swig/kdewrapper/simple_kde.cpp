#include "simple_kde.h"
#include <algorithm>
#include <cmath>
#include <iostream>

using namespace std;

SimpleKDE::SimpleKDE(int dim, double sigma){
	this->dimensionality = dim;
	this->sigma = sigma;
}

SimpleKDE::SimpleKDE(const SimpleKDE& copy){
	this->dimensionality = copy.dimensionality;
	this->sigma = copy.sigma;
	for(unsigned int i=0;i<copy.samples.size();i++){
		this->addNoCheck(copy.samples[i],copy.weights[i]);
	}
}

SimpleKDE::~SimpleKDE(){
	clear();
}

void SimpleKDE::add(const double sample[]){
	add(sample,1.0);
}

void SimpleKDE::add(const double sample[], const double weight){
	int idx = getIdx(sample);
	if(idx == -1){
		addNoCheck(sample,weight);
	} else {
		weights[idx] += weight;
	}
}

void SimpleKDE::addNoCheck(const double sample[], const double weight){
	double *tmp = new double[dimensionality];
	copy(sample,sample+dimensionality,tmp);
	samples.push_back(tmp);
	weights.push_back(weight);
}

bool SimpleKDE::setWeight(const int idx, const double weight){
	if(idx > 0 && idx < weights.size()){
		weights[idx] = weight;
		return true;
	} else {
		return false;
	}
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

double SimpleKDE::estimate(const double x[], const double bandwidth) const{
	double sum=0.0, weightSum = 0.0;
	double* tmp = new double[dimensionality];
	for(unsigned int i=0;i<samples.size();i++){
		for(unsigned int j=0;j<dimensionality;j++){
			tmp[j] = (x[j] - samples[i][j])/bandwidth;
		}
		sum += weights[i]*kernel(tmp,dimensionality,sigma);
		weightSum += weights[i];
	}
	if(weightSum == 0.0){ 
		//cout<<"weightSum==0.0"<<endl;
		return 0.0;
	}
	return (1.0/(weightSum*bandwidth)) * sum;
}

int SimpleKDE::getIdx(const double sample[]) const{
	for(unsigned int i=0;i<samples.size();i++){
		double* s = samples[i];
		bool diff = false;
		for(int d=0;d<dimensionality;d++){
			if(s[d] != sample[d]){
				diff = true;
				break;
			}
		}
		if(!diff){
			return i;
		}
	}
	return -1;
}

bool SimpleKDE::getSample(double sample[], const int i) const{
	if(i >= 0 && i < samples.size()){
		for(int j=0; j<dimensionality; j++){
			sample[j] = samples[i][j];
		}
		return true;
	} else {
		return false;
	}
}

double SimpleKDE::getDist(double sample[], const int i) const{
	if(i>=0 && i < samples.size()){
		double rv = 0;
		for(int j=0;j<dimensionality;j++){
			rv += pow(sample[j]-samples[i][j],2);
		}
		return sqrt(rv);
	} else {
		return -1.0;
	}
}

double SimpleKDE::getWeight(const int i) const{
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
