import os, os.path, string, sys, math
def dSq(x1,x2,y1,y2):
	return ((float(x1)-float(x2))**2) + ((float(y1)-float(y2))**2)

def rotate(x,y,theta):
	x=float(x)
	y=float(y)
	theta=float(theta)
	xprime = (x*math.cos(theta))-(y*math.sin(theta))
	yprime = (x*math.sin(theta))+(y*math.cos(theta))
	return xprime,yprime

def angle(x,y):
	return math.atan2(float(y),float(x))

def vecTo(fromX,fromY,toX,toY):
	return float(toX)-float(fromX), float(toY)-float(fromY)

def addAverageNN(outdir=os.getcwd(),indir=os.getcwd(), maxD=None):
	idf = open(os.path.join(indir,"id.btf")).readlines()
	xpos = open(os.path.join(indir,"xpos.btf")).readlines()
	ypos = open(os.path.join(indir,"ypos.btf")).readlines()
	timage = open(os.path.join(indir,"timage.btf")).readlines()
	timestamp = open(os.path.join(indir,"timestamp.btf")).readlines()
	nnvecfile = open(os.path.join(outdir,"avgnnvec.btf"),"w")
	blockStart = 0
	blockEnd = 0
	curLine = 0
	numLines = len(xpos)
	if maxD is None:
		maxDSq = None
	else:
		maxDSq = maxD**2
	while curLine < numLines:
		if curLine % (numLines/20) == 0:
			print "Line",curLine,"of",numLines
		if curLine >= blockEnd:
			blockStart = blockEnd
			while blockEnd < numLines and timestamp[blockEnd] == timestamp[blockStart]:
				blockEnd += 1
		numNeighbors=0
		nnvecX,nnvecY = 0,0
		for checkLine in xrange(blockStart,blockEnd):
			if idf[curLine] == idf[checkLine]:
				continue
			tmpVecX,tmpVecY = vecTo(xpos[curLine],ypos[curLine],xpos[checkLine],ypos[checkLine])
			tmpD = dSq(tmpVecX,0,tmpVecY,0)
			if (maxDSq is None) or (maxDSq >= tmpD):
				tmpVecX, tmpVecY = rotate(tmpVecX,tmpVecY,-float(timage[curLine]))
				numNeighbors +=1
				nnvecX += tmpVecX
				nnvecY += tmpVecY
		if numNeighbors > 0:
			nnvecX = nnvecX/numNeighbors
			nnvecY = nnvecY/numNeighbors
		nnvecfile.write(str(nnvecX)+" "+str(nnvecY)+"\n")
		curLine += 1
	nnvecfile.close()

if __name__=="__main__":
	if len(sys.argv) > 4:
		print "Usage: python",sys.argv[0],"[inputDir=os.getcwd()] [outputDir=os.getcwd()] [maxD=None] "
	elif len(sys.argv) == 1:
		addAverageNN()
	elif len(sys.argv) == 2:
		addAverageNN(sys.argv[1])
	elif len(sys.argv) == 3:
		addAverageNN(sys.argv[1],sys.argv[2])
	else:
		addAverageNN(sys.argv[1],sys.argv[2],float(sys.argv[3]))
