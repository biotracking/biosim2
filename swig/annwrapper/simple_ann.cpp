#include "simple_ann.h"
#include <algorithm>
#include <iostream>

using namespace std;

SimpleANN::SimpleANN(const SimpleANN& toCopy) {
	dim = toCopy.dim;
	// kdTree = NULL;
	// updateDataset = true;
	// eps = toCopy.eps;
	for(unsigned int i=0;i<toCopy.dataset.size();i++){
		double *tmpCpy = new double[dim];
		double *tmp = toCopy.dataset[i];
		
		//for(int j=0;j<dim;j++){
		//	tmpCpy[j] = tmp[j];
		//}
		copy(tmp,tmp+dim,tmpCpy);
		dataset.push_back(tmpCpy);
	}
}

SimpleANN& SimpleANN::operator=(const SimpleANN& rhs){
	// index.buildIndex();
	for(int i=0;i<dataset.size();i++){
		delete[] dataset[i];
		dataset[i] = NULL;
	}
	dataset.clear();
	// if(kdTree != NULL){
	// 	ANNpointArray tmp = kdTree->thePoints();
	// 	annDeallocPts(tmp);
	// 	delete kdTree;
	// 	kdTree = NULL;
	// }
	dim = rhs.dim;
	// kdTree = NULL;
	// updateDataset = true;
	// eps = rhs.eps;
	for(unsigned int i=0;i<rhs.dataset.size();i++){
		double *tmpCpy = new double[dim];
		double *tmp = rhs.dataset[i];
		//for(int j=0;j<dim;j++){
		//	tmpCpy[j] = tmp[j];
		//}
		copy(tmp,tmp+dim,tmpCpy);
		dataset.push_back(tmpCpy);
	}
	return *this;
}

SimpleANN::SimpleANN(int numFeatures){
	// index.buildIndex();
	dim = numFeatures;
	// kdTree = NULL;
	// updateDataset = true;
	// eps = 0.0;
}

SimpleANN::~SimpleANN(){
	for(int i=0;i<dataset.size();i++){
		delete[] dataset[i];
		dataset[i] = NULL;
	}
	dataset.clear();
	// if(kdTree != NULL){
	// 	ANNpointArray tmp = kdTree->thePoints();
	// 	annDeallocPts(tmp);
	// 	delete kdTree;
	// 	kdTree = NULL;
	// }
	//annClose();

}

void SimpleANN::add(const double feats[]){
	double *tmp = new double[dim];
	//cout<<"[ ";
	//for(int i=0;i<dim;i++){
	//	tmp[i] = feats[i];
		//cout<<tmp[i]<<" ";
	//}
	//cout<<"]"<<endl;
	copy(feats,feats+dim,tmp);
	dataset.push_back(tmp);
	// updateDataset = true;
}


bool SimpleANN::query(const double feats[], int index[], int k){
	// if(updateDataset){
	// 	if (kdTree != NULL){ 
	// 		ANNpointArray tmp = kdTree->thePoints();
	// 		annDeallocPts(tmp);
	// 		delete kdTree;
	// 		kdTree = NULL;
	// 	} 
	// 	int numPts = dataset.size();
	// 	ANNpointArray dataPts = annAllocPts(numPts,dim);
	// 	for(int index=0;index<numPts;index++){
	// 		copy(dataset[index],dataset[index]+dim,dataPts[index]);
	// 		//for(int i=0;i<dim;i++){
	// 		//	dataPts[index][i] = dataset[index][i];
	// 		//}
	// 	}
	// 	kdTree = new ANNkd_tree(dataPts,numPts,dim);
	// 	updateDataset = false;
	// 	//cout<<"SimpleANN: Rebuilt tree"<<endl;
	// }
	// if(kdTree == NULL) return false;
	// ANNidxArray nnIdx = new ANNidx[k];
	// ANNdistArray dists = new ANNdist[k];
	// ANNpoint queryPt = annAllocPt(dim);
	// //for(int i=0;i<dim;i++) queryPt[i] = feats[i];
	// copy(feats,feats+dim,queryPt);
	// kdTree->annkSearch(queryPt,k,nnIdx,dists,eps);
	// //for(int i=0;i<k;i++){ 
	// //	index[i] = nnIdx[i];
	// //}
	// copy(nnIdx,nnIdx+k,index);
	// annDeallocPt(queryPt);
	// delete[] nnIdx;
	// delete[] dists;
	flann::Matrix<double> firstRow(dataset[0],1,dim);
	flann::Index<flann::L2<double> > searchIndex(firstRow,flann::KDTreeIndexParams(1));
	for(int i=1;i<dataset.size();i++){
		flann::Matrix<double> row(dataset[i],1,dim);
		searchIndex.addPoints(row,2.0f*dataset.size()); //rebuild_threshold set really high so we don't rebuild frequently
	}
	// flann::Matrix<double> searchData(ref,dataset.size(),dim);
	// searchIndex.buildIndex(searchData);
	searchIndex.buildIndex();
	double *tmp = new double[dim];
	copy(feats,feats+dim,tmp);
	flann::Matrix<double> qPt(tmp,1,dim);
	std::vector<std::vector<int> > indices;
	std::vector<std::vector<double> > qDists;
	searchIndex.knnSearch(qPt,indices,qDists,k,flann::SearchParams(flann::FLANN_CHECKS_UNLIMITED));
	for(int i=0;i<k;i++){
		index[i] = indices[0][i];
	}
	delete[] tmp;
	return true;
}
