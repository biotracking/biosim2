import os, os.path, string, sys, math
import numpy

def getBeta(indir=os.getcwd(), maxD=None):
	dvel = open(os.path.join(indir,"dvel.btf")).readlines()
	avgnnvec = open(os.path.join(indir,"avgnnvec.btf")).readlines()
	wallvec = open(os.path.join(indir,"wallvec.btf")).readlines()
	desired_fbspeed = numpy.array(tuple(float(line.split()[0]) for line in dvel))
	desired_turnspeed = numpy.array(tuple(float(line.split()[2]) for line in dvel))
	numLines = len(wallvec)
	input_vec = numpy.empty((numLines,4))
	for x in xrange(numLines):
		input_vec[x,0] = float(avgnnvec[x].split()[0])
		input_vec[x,1] = float(avgnnvec[x].split()[1])
		input_vec[x,2] = float(wallvec[x].split()[0])
		input_vec[x,3] = float(wallvec[x].split()[1])
	inp_t = input_vec.transpose()
	fbspeedB = numpy.dot(numpy.linalg.inv(numpy.dot(inp_t,input_vec)),numpy.dot(inp_t,desired_fbspeed))
	turnspeedB = numpy.dot(numpy.linalg.inv(numpy.dot(inp_t,input_vec)),numpy.dot(inp_t,desired_turnspeed))
	return fbspeedB,turnspeedB

if __name__=="__main__":
	if len(sys.argv) > 2:
		print "Usage: python",sys.argv[0],"[indir=os.getcwd()]"
	elif len(sys.argv) == 1:
		betas = getBeta()
	else:
		betas = getBeta(sys.argv[1])
	print "Front-back speed betas:",betas[0]
	print "Turn speed betas:", betas[1]
