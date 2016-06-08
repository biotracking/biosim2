#include "simple_ann.h"
#include <algorithm>
#include <iostream>

using namespace std;

SimpleANN::SimpleANN(const SimpleANN& toCopy) {
	dim = toCopy.dim;
	searchIndex = NULL;
	if(toCopy.searchIndex != NULL){
		searchIndex = new flann::Index<flann::L2<double> >(*(toCopy.searchIndex));
	}
}

SimpleANN& SimpleANN::operator=(const SimpleANN& rhs){
	dim = rhs.dim;
	if(searchIndex != NULL){
		delete searchIndex;
		searchIndex = NULL;
	}
	if(rhs.searchIndex != NULL){
		searchIndex = new flann::Index<flann::L2<double> >(*(rhs.searchIndex));
	}
	return *this;
}

SimpleANN::SimpleANN(int numFeatures){
	dim = numFeatures;
	searchIndex = NULL;
}

SimpleANN::~SimpleANN(){
	if(searchIndex != NULL){
		delete searchIndex;
		searchIndex = NULL;
	}
	for(unsigned int i=0;i<cleanup.size();i++){
		delete[] cleanup[i].ptr();
	}
}

void SimpleANN::add(const double feats[]){
	double *tmp = new double[dim];
	copy(feats,feats+dim,tmp);
	flann::Matrix<double> row(tmp,1,dim);
	if(searchIndex == NULL){
		searchIndex = new flann::Index<flann::L2<double> >(row,flann::KDTreeIndexParams(1));
	} else {
		searchIndex->addPoints(row);
	}
	cleanup.push_back(row);
}


bool SimpleANN::query(const double feats[], int index[], int k){
	double *tmp = new double[dim];
	copy(feats,feats+dim,tmp);
	flann::Matrix<double> qPt(tmp,1,dim);
	std::vector<std::vector<int> > indices;
	std::vector<std::vector<double> > qDists;
	searchIndex->knnSearch(qPt,indices,qDists,k,flann::SearchParams(flann::FLANN_CHECKS_UNLIMITED));
	for(int i=0;i<k;i++){
		index[i] = indices[0][i];
	}
	delete[] tmp;
	return true;
}
