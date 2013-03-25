import os, os.path, string, sys, math
def dSq(x1,x2,y1,y2):
	return ((float(x1)-float(x2))**2) + ((float(y1)-float(y2))**2)

def rotate(x,y,theta):
	x=float(x)
	y=float(y)
	theta=float(theta)
	xprime = (x*math.cos(theta))-(y*math.sin(theta))
	yprime = (x*math.sin(theta))+(y+math.cos(theta))
	return xprime,yprime

def angle(x,y):
	return math.atan2(float(y),float(x))

def vecTo(fromX,fromY,toX,toY):
	return float(toX)-float(fromX), float(toY)-float(fromY)

def addNN(indir=os.getcwd(), outdir=os.getcwd()):
	idf = open(os.path.join(indir,"id.btf")).readlines()
	xpos = open(os.path.join(indir,"xpos.btf")).readlines()
	ypos = open(os.path.join(indir,"ypos.btf")).readlines()
	timage = open(os.path.join(indir,"timage.btf")).readlines()
	timestamp = open(os.path.join(indir,"timestamp.btf")).readlines()
	nnvecfile = open(os.path.join(outdir,"nnvec.btf"),"w")
	nnidfile = open(os.path.join(outdir,"nnid.btf"),"w")
	blockStart = 0
	blockEnd = 0
	curLine = 0
	numLines = len(xpos)
	while curLine < numLines:
		if curLine % (numLines/20) == 0:
			print "Line",curLine,"of",numLines
		if curLine >= blockEnd:
			blockStart = blockEnd
			while blockEnd < numLines and timestamp[blockEnd] == timestamp[blockStart]:
				blockEnd += 1
		minDist = -1
		minDistLine = -1
		for checkLine in xrange(blockStart,blockEnd):
			if idf[curLine] == idf[checkLine]:
				continue
			tmpD = dSq(xpos[curLine],xpos[checkLine],ypos[curLine],ypos[checkLine])
			if minDist == -1 or tmpD < minDist:
				minDist = tmpD
				minDistLine = checkLine
		if minDistLine >= 0:
			nnvecX,nnvecY = vecTo(xpos[curLine],ypos[curLine],xpos[minDistLine],ypos[minDistLine])
			nnvecX,nnvecY = rotate(nnvecX,nnvecY,-float(timage[curLine]))
			nnid = idf[minDistLine].strip()
		else:
			nnvecX = 0
			nnvecY = 0
			nnid = "-1"
		nnvecfile.write(str(nnvecX)+" "+str(nnvecY)+"\n")
		nnidfile.write(nnid+"\n")
		curLine += 1
	nnvecfile.close()
	nnidfile.close()

if __name__=="__main__":
	if len(sys.argv) > 3:
		print "Usage: python",sys.argv[0],"[inputDir=os.getcwd()] [outputDir=os.getcwd()]"
	elif len(sys.argv) == 1:
		addNN()
	elif len(sys.argv) == 2:
		addNN(sys.argv[1])
	else:
		addNN(sys.argv[1],sys.argv[2])
