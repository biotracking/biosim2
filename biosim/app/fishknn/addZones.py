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

def addZones(outdir=os.getcwd(),indir=os.getcwd()):
	idf = open(os.path.join(indir,"id.btf")).readlines()
	xpos = open(os.path.join(indir,"xpos.btf")).readlines()
	ypos = open(os.path.join(indir,"ypos.btf")).readlines()
	timage = open(os.path.join(indir,"timage.btf")).readlines()
	timestamp = open(os.path.join(indir,"timestamp.btf")).readlines()
	zvecfile = open(os.path.join(outdir,"zonevecs.btf"),"w")
	blockStart = 0
	blockEnd = 0
	curLine = 0
	numLines = len(xpos)
	body_length=0.05 #meters
	rad_r = body_length*2
	rad_rSq = rad_r**2
	rad_o = rad_r+(body_length*2)
	rad_oSq = rad_o**2
	rad_a = rad_o+(body_length*13)
	rad_aSq = rad_a**2
	numNeighbors = {'rep':0,'ori':0,'att':0}
	vecs = {'rep':(0,0),'ori':(0,0),'att':(0,0)}
	while curLine < numLines:
		if curLine % (numLines/20) == 0:
			print "Line",curLine,"of",numLines
		if curLine >= blockEnd:
			blockStart = blockEnd
			while blockEnd < numLines and timestamp[blockEnd] == timestamp[blockStart]:
				blockEnd += 1
		numNeighbors['rep'] = 0
		numNeighbors['ori'] = 0
		numNeighbors['att'] = 0
		vecs['rep'] = (0,0)
		vecs['ori'] = (0,0)
		vecs['att'] = (0,0)
		for checkLine in xrange(blockStart,blockEnd):
			if idf[curLine] == idf[checkLine]:
				continue
			tmpVecX,tmpVecY = vecTo(xpos[curLine],ypos[curLine],xpos[checkLine],ypos[checkLine])
			tmpD = dSq(tmpVecX,0,tmpVecY,0)
			zone = None
			if rad_rSq > tmpD:
				zone = 'rep'
			elif rad_oSq > tmpD:
				zone = 'ori'
			elif rad_aSq > tmpD:
				zone = 'att'
			if not(zone is None):
				#Orientation zone is special, we need the relative heading
				#of the neighbor fish
				if zone == 'ori':
					tmpVecX,tmpVecY = rotate(1,0,timage[checkLine])
				tmpVecX, tmpVecY = rotate(tmpVecX,tmpVecY,-float(timage[curLine]))
				numNeighbors[zone] +=1
				vec = vecs[zone]
				vecs[zone] = (vec[0]+tmpVecX,vec[1]+tmpVecY)
		for key in numNeighbors:
			vec = vecs[key]
			nn = float(numNeighbors[key])
			if nn > 0:
				vecs[zone] = (vec[0]/nn,vec[1]/nn)
		zvecfile.write(str(vecs['rep'][0])+" "+str(vecs['rep'][1]))
		zvecfile.write(" "+str(vecs['ori'][0])+" "+str(vecs['ori'][1]))
		zvecfile.write(" "+str(vecs['att'][0])+" "+str(vecs['att'][1])+"\n")
		curLine += 1
	zvecfile.close()

if __name__=="__main__":
	if len(sys.argv) > 3:
		print "Usage: python",sys.argv[0],"[inputDir=os.getcwd()] [outputDir=os.getcwd()]"
	elif len(sys.argv) == 1:
		addZones()
	elif len(sys.argv) == 2:
		addZones(sys.argv[1])
	else:
		addZones(sys.argv[1],sys.argv[2])
