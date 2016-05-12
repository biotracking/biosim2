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

def addDvel(indir=os.getcwd(), outdir=os.getcwd()):
	idf = open(os.path.join(indir,"id.btf")).readlines()
	xpos = open(os.path.join(indir,"xpos.btf")).readlines()
	ypos = open(os.path.join(indir,"ypos.btf")).readlines()
	timage = open(os.path.join(indir,"timage.btf")).readlines()
	timestamp = open(os.path.join(indir,"timestamp.btf")).readlines()
	dvelfile = open(os.path.join(outdir,"dvel.btf"),"w")
	dboolfile = open(os.path.join(outdir,"dbool.btf"),"w")
	blockStart = 0
	blockEnd = 0
	nextBlockEnd = 0
	curLine = 0
	numLines = len(xpos)
	while blockEnd < numLines and timestamp[blockStart] == timestamp[blockEnd]:
		blockEnd += 1
	nextBlockEnd = blockEnd
	while nextBlockEnd < numLines and timestamp[blockEnd] == timestamp[nextBlockEnd]:
		nextBlockEnd += 1
	while curLine < numLines:
		if curLine % (numLines/20) == 0:
			print "Line",curLine,"of",numLines
		if curLine >= blockEnd:
			blockStart = blockEnd
			blockEnd = nextBlockEnd
			while nextBlockEnd < numLines and timestamp[nextBlockEnd] == timestamp[blockEnd]:
				nextBlockEnd += 1
		found = False
		dvelX,dvelY,dvelT = 0,0,0
		for checkLine in xrange(blockEnd,nextBlockEnd):
			if idf[curLine] == idf[checkLine]:
				dvelX,dvelY = vecTo(xpos[curLine],ypos[curLine],xpos[checkLine],ypos[checkLine])
				dvelX,dvelY = rotate(dvelX,dvelY,-float(timage[curLine]))
				timeDelta = (float(timestamp[checkLine])-float(timestamp[curLine]))/1000.0
				dvelX = dvelX/timeDelta
				dvelY = dvelY/timeDelta
				dvelT = (float(timage[checkLine])-float(timage[curLine]))/timeDelta
				found = True
				break
		dvelfile.write(str(dvelX)+" "+str(dvelY)+" "+str(dvelT)+"\n")
		dboolfile.write(str(found)+"\n")
		curLine += 1
	dvelfile.close()
	dboolfile.close()

if __name__=="__main__":
	if len(sys.argv) > 3:
		print "Usage: python",sys.argv[0],"[inputDir=os.getcwd()] [outputDir=os.getcwd()]"
	elif len(sys.argv) == 1:
		addDvel()
	elif len(sys.argv) == 2:
		addDvel(sys.argv[1])
	else:
		addDvel(sys.argv[1],sys.argv[2])
