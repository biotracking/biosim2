import os, os.path, string, sys, math
import numpy

def getBeta(indir=os.getcwd(), maxD=None):
	#speedingforce.btf turningforce.btf wallvec.btf dvel.btf avgnnvec.btf
	#        0                1            2  3      4 5 6      7   8
	filtered_data = open(os.path.join(indir,"acceldata_filtered.dat")).readlines()
	numLines  = len(filtered_data)
	#dvel = open(os.path.join(indir,"dvel.btf")).readlines()
	#avgnnvec = open(os.path.join(indir,"avgnnvec.btf")).readlines()
	#wallvec = open(os.path.join(indir,"wallvec.btf")).readlines()
	#numLines = len(wallvec)
	desired_fbspeed = numpy.empty(numLines) #numpy.array(tuple(float(line.split()[0]) for line in dvel))
	desired_turnspeed = numpy.empty(numLines) #numpy.array(tuple(float(line.split()[2]) for line in dvel))
	input_vec = numpy.empty((numLines,4))
	for x in xrange(numLines):
		input_vec[x,0] = float(filtered_data[x].split()[7])
		input_vec[x,1] = float(filtered_data[x].split()[8])
		input_vec[x,2] = float(filtered_data[x].split()[2])
		input_vec[x,3] = float(filtered_data[x].split()[3])
		desired_fbspeed[x] = float(filtered_data[x].split()[4])
		desired_turnspeed[x] = float(filtered_data[x].split()[6])
		#input_vec[x,0] = float(avgnnvec[x].split()[0])
		#input_vec[x,1] = float(avgnnvec[x].split()[1])
		#input_vec[x,2] = float(wallvec[x].split()[0])
		#input_vec[x,3] = float(wallvec[x].split()[1])
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
