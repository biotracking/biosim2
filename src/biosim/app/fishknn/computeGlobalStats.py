import os, os.path, string, sys, math
def dSq(x1,x2,y1,y2):
	return ((float(x1)-float(x2))**2) + ((float(y1)-float(y2))**2)

def vLen(x,y):
	return math.sqrt((x*x)+(y*y))

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

def cross(ax,ay, bx,by):
	"""NOTE: Not really the cross product"""
	anglebetween = angle(bx,by)-angle(ax,ay)
	return vLen(ax,ay)*vLen(bx,by)*math.sin(anglebetween)

def computeStats(indir=os.getcwd(), outdir=os.getcwd()):
	idf = open(os.path.join(indir,"id.btf")).readlines()
	xpos = open(os.path.join(indir,"xpos.btf")).readlines()
	ypos = open(os.path.join(indir,"ypos.btf")).readlines()
	timage = open(os.path.join(indir,"timage.btf")).readlines()
	timestamp = open(os.path.join(indir,"timestamp.btf")).readlines()
	nearest = open(os.path.join(indir,"nnvec.btf")).readlines()
	wall = open(os.path.join(indir,"wallvec.btf")).readlines()
	polarfile = open(os.path.join(outdir,"polar.dat"),"w")
	angularfile = open(os.path.join(outdir,"angular.dat"),"w")
	nndistfile = open(os.path.join(outdir,"nndist.dat"),"w")
	walldistfile = open(os.path.join(outdir,"walldist.dat"),"w")
	maxdistfile = open(os.path.join(outdir,"maxdist.dat"),"w")
	#variance in nn dist
	vardistfile = open(os.path.join(outdir,"vardist.dat"),"w")
	#variance in speed (vLen(velocity))
	blockStart = 0
	blockEnd = 0
	curLine = 0
	numLines = len(xpos)
	sumvecX, sumvecY = 0.0,0.0
	comvecX, comvecY = 0.0,0.0
	numThings = 0
	numStats = 0
	avgPolarization = 0
	avgAngular = 0
	avgNNDist = 0
	avgWallDist = 0
	avgMaxDist = 0
	avgVarDist = 0
	nndist = 0
	walldist = 0
	maxdist = 0
	while curLine < numLines:
		if curLine % (numLines/20) == 0:
			print "Line", curLine, "of", numLines, "(%d%%)"%(100*(float(curLine)/float(numLines)))
		if curLine >= blockEnd:
			#write polarization
			if numThings > 0:
				polarization = 1.0/float(numThings)*vLen(sumvecX,sumvecY)
				avgPolarization += polarization
			else:
				polarization = 0
			polarfile.write(str(polarization)+"\n")
			#write angular momentum
			if numThings > 0:
				angular = 0.0
				comvecLen = vLen(comvecX,comvecY)
				comvecX = comvecX/comvecLen
				comvecY = comvecY/comvecLen
				for tmpLine in xrange(blockStart,blockEnd):
					ricX = float(xpos[tmpLine])-comvecX
					ricY = float(ypos[tmpLine])-comvecY
					ricLen = vLen(ricX,ricY)
					ricX = float(ricX)/float(ricLen)
					ricY = float(ricY)/float(ricLen)
					vi = rotate(1,0,float(timage[curLine]))
					angular += cross(ricX,ricY, vi[0],vi[1])
				angular = abs(angular/float(numThings))
				avgAngular += angular
				numStats += 1
			else:
				angular = 0
			angularfile.write(str(angular)+"\n")
			if numThings > 0:
				#write nndist
				nndist = float(nndist)/float(numThings)
				nndistfile.write(str(nndist)+"\n")
				avgNNDist += nndist
				#write walldist
				walldist = float(walldist)/float(numThings)
				walldistfile.write(str(walldist)+"\n")
				avgWallDist += walldist
				#write maxdist
				maxdistfile.write(str(maxdist)+"\n")
				avgMaxDist += maxdist
				#write vardist
				vardist = 0.0
				for tmpLine in xrange(blockStart,blockEnd):
					tmpNNX, tmpNNY = nearest[tmpLine].split()
					vardist += (vLen(float(tmpNNX),float(tmpNNY))-nndist)**2
				vardist = math.sqrt(vardist/float(numThings))
				vardistfile.write(str(vardist)+"\n")
				avgVarDist += vardist
			nndist = 0
			comvecX, comvecY = 0.0,0.0
			walldist = 0
			maxdist = 0
			blockStart = blockEnd
			while blockEnd < numLines and timestamp[blockEnd] == timestamp[blockStart]:
				blockEnd += 1
			numThings = 0
			sumvecX = sumvecY = 0
		numThings += 1
		#polarization
		#  p = 1/N * length(sum(v_i))
		vi = rotate(1,0,float(timage[curLine]))
		sumvecX += vi[0]
		sumvecY += vi[1]
		#angular momentum
		#  m = 1/N * length(sum(cross(r_ic,v_i)))
		#  r_ic = vector from group CoM to me
		comvecX += float(xpos[curLine])
		comvecY += float(ypos[curLine])
		#NN Dist
		nnX, nnY = nearest[curLine].split()
		nndist += vLen(float(nnX),float(nnY))
		#Wall Dist
		wX, wY = wall[curLine].split()
		walldist += vLen(float(wX),float(wY))
		#max dist
		curX, curY = float(xpos[curLine]), float(ypos[curLine])
		for tmpLine in xrange(blockStart,curLine):
			prevX, prevY = float(xpos[tmpLine]), float(ypos[tmpLine])
			tmpDistSq = math.sqrt(dSq(prevX,curX,prevY,curY))
			if tmpDistSq > maxdist:
				maxdist = tmpDistSq
		#iterate
		curLine += 1
	polarfile.close()
	angularfile.close()
	print "Average polarization:",avgPolarization/float(numStats)
	print "Average angular momentum:", avgAngular/float(numStats)
	print "Average nn dist:",avgNNDist/float(numStats)
	print "Average wall dist:", avgWallDist/float(numStats)
	print "Average max dist:", avgMaxDist/float(numStats)
	print "Average std. dev. of NN dist:", avgVarDist/float(numStats)

if __name__=="__main__":
	if len(sys.argv) > 3:
		print "Usage: python",sys.argv[0],"[inputDir=os.getcwd()] [outputDir=os.getcwd()]"
	elif len(sys.argv) == 1:
		computeStats()
	elif len(sys.argv) == 2:
		computeStats(sys.argv[1])
	else:
		computeStats(sys.argv[1],sys.argv[2])
