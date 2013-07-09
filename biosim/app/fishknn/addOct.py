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

def addOct(indir=os.getcwd(), outdir=os.getcwd()):
	idf = open(os.path.join(indir,"id.btf")).readlines()
	xpos = open(os.path.join(indir,"xpos.btf")).readlines()
	ypos = open(os.path.join(indir,"ypos.btf")).readlines()
	timage = open(os.path.join(indir,"timage.btf")).readlines()
	timestamp = open(os.path.join(indir,"timestamp.btf")).readlines()
	octfile = open(os.path.join(outdir,"oct.btf"),"w")
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
		minDist = [-1, ]*8
		for checkLine in xrange(blockStart,blockEnd):
			if idf[curLine] == idf[checkLine]:
				continue
			tmpV = vecTo(xpos[curLine],ypos[curLine],xpos[curLine],ypos[curLine])
			tmpV = rotate(tmpV[0],tmpV[1],-float(timage[curLine]))
			tmpA = angle(tmpV[0],tmpV[1])
			#angleSlot = None
			#if -math.pi/8.0 < tmpA and tmpA <= math.pi/8.0:
			#	angleSlot = 0
			#elif math.pi/8.0 < tmpA and tmpA <= (3*math.pi)/8.0:
			#	angleSlot = 1
			#elif (3*math.pi)/8.0 < tmpA and tmpA <= (5*math.pi)/8.0:
			#	angleSlot = 2
			#elif (5*math.pi)/8.0 < tmpA and tmpA <= (7*math.pi)/8.0:
			#	angleSlot = 3
			#elif (7*math.pi)/8.0 < tmpA or tmpA <= -(7*math.pi)/8.0:
			#	angleSlot = 4
			#elif -(7*math.pi)/8.0 < tmpA and tmpA <= -(5*math.pi)/8.0:
			#	angleSlot = 5
			#elif -(5*math.pi)/8.0 < tmpA and tmpA <= -(3*math.pi)/8.0:
			#	angleSlot = 6
			#elif -(3*math.pi)/8.0 < tmpA and tmpA <= -math.pi/8.0:
			#	angleSlot = 7
			#else:
			#	angleSlot = None
			angleSlot = int(tmpA/(2.0*math.pi/8.0))
			if angleSlot < 0:
				angleSlot = 8+angleSlot
			tmpD = dSq(xpos[curLine],xpos[checkLine],ypos[curLine],ypos[checkLine])
			if minDist[angleSlot] == -1 or tmpD < minDist[angleSlot]:
				minDist[angleSlot] = tmpD
		#octfile.write(str(minDist[0])+" "+str(minDist[1])+" "+str(minDist[2])+" "+str(minDist[3])+" "+str(minDist[4])+" "+str(minDist[5])+" "+str(minDist[6])+" "+str(minDist[7])+"\n")
		curLine += 1
	octfile.close()

if __name__=="__main__":
	if len(sys.argv) > 3:
		print "Usage: python",sys.argv[0],"[inputDir=os.getcwd()] [outputDir=os.getcwd()]"
	elif len(sys.argv) == 1:
		addOct()
	elif len(sys.argv) == 2:
		addOct(sys.argv[1])
	else:
		addOct(sys.argv[1],sys.argv[2])
