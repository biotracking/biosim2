#include "simple_ann.h"
#include <algorithm>
#include <iostream>

using namespace std;

SimpleANN::SimpleANN(int numFeatures){
	dim = numFeatures;
	kdTree = NULL;
	updateDataset = true;
}

SimpleANN::~SimpleANN(){
	for(int i=0;i<dataset.size();i++){
		delete[] dataset[i];
		dataset[i] = NULL;
	}
	if(kdTree != NULL){
		ANNpointArray tmp = kdTree->thePoints();
		annDeallocPts(tmp);
		delete kdTree;
	}
}

void SimpleANN::add(const double feats[]){
	double *tmp = new double[dim];
	copy(feats,feats+dim,tmp);
	dataset.push_back(tmp);
	updateDataset = true;
}


bool SimpleANN::query(const double feats[], int index[], int k){
	if(updateDataset){
		if (kdTree != NULL){ 
			ANNpointArray tmp = kdTree->thePoints();
			annDeallocPts(tmp);
			delete kdTree;
		}
		ANNpointArray dataPts = annAllocPts(dataset.size(),dim);
		for(int index=0;index<dataset.size();index++){
			for(int i=0;i<dim;i++){
				dataPts[index][i] = dataset[index][i];
			}
		}
		kdTree = new ANNkd_tree(dataPts,dataset.size(),dim);
		updateDataset = false;
	}
	if(kdTree == NULL) return false;
	ANNidxArray nnIdx = new ANNidx[k];
	ANNdistArray dists = new ANNdist[k];
	ANNpoint queryPt = annAllocPt(dim);
	for(int i=0;i<dim;i++) queryPt[i] = feats[i];
	kdTree->annkSearch(queryPt,k,nnIdx,dists,0.0);
	for(int i=0;i<k;i++){ 
		index[i] = nnIdx[i];
	}
	annDeallocPt(queryPt);
	return true;
}
