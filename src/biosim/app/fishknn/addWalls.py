import os, os.path, string, sys, math

def rotate(x,y,theta):
	x=float(x)
	y=float(y)
	theta=float(theta)
	xprime = (x*math.cos(theta))-(y*math.sin(theta))
	yprime = (x*math.sin(theta))+(y*math.cos(theta))
	return xprime,yprime

def vecTo(fromX,fromY,toX,toY):
	return float(toX)-float(fromX), float(toY)-float(fromY)

def addWalls(left, right, top, bot, indir=os.getcwd(), outdir=os.getcwd()):
	xpos = open(os.path.join(indir,"xpos.btf")).readlines()
	ypos = open(os.path.join(indir,"ypos.btf")).readlines()
	timage = open(os.path.join(indir,"timage.btf")).readlines()
	wallvecfile = open(os.path.join(outdir,"wallvec.btf"),"w")
	numLines = len(xpos)
	for line in xrange(numLines):
		lrDist = float(xpos[line])
		lrX,lrY = float(left),float(ypos[line])
		if lrDist > ((float(right)-float(left))/2)+float(left):
			lrDist = float(right)-lrDist
			lrX,lrY = float(right),float(ypos[line])
		tbDist = float(ypos[line])
		tbX, tbY = float(xpos[line]),float(top)
		if tbDist > ((float(bot)-float(top))/2)+float(top):
			tbDist = float(bot)-tbDist
			tbX, tbY = float(xpos[line]),float(bot)
		if lrDist < tbDist:
			wallX, wallY = rotate(lrX,lrY,-float(timage[line]))
		else:
			wallX, wallY = rotate(tbX,tbY,-float(timage[line]))
		wallvecfile.write(str(wallX)+" "+str(wallY)+"\n")
	wallvecfile.close()

if __name__=="__main__":
	if len(sys.argv) < 5 or len(sys.argv) > 7:
		print "Usage: python",sys.argv[0],"left right top bot", "[inputDir=os.getcwd()] [outputDir=os.getcwd()]"
	elif len(sys.argv) == 5:
		addWalls(float(sys.argv[1]),float(sys.argv[2]),float(sys.argv[3]),float(sys.argv[4]))
	elif len(sys.argv) == 6:
		addWalls(float(sys.argv[1]),float(sys.argv[2]),float(sys.argv[3]),float(sys.argv[4]),sys.argv[5])
	else:
		addWalls(float(sys.argv[1]),float(sys.argv[2]),float(sys.argv[3]),float(sys.argv[4]),sys.argv[5],sys.argv[6])
