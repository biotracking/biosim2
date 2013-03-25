import os, os.path, string, sys
def px2meters(pxPerM, indir=os.getcwd(), outdir=os.getcwd()):
	ximagefile = open(os.path.join(indir,"ximage.btf"))
	yimagefile = open(os.path.join(indir,"yimage.btf"))
	xposfile = open(os.path.join(outdir,"xpos.btf"),"w")
	yposfile = open(os.path.join(outdir,"ypos.btf"),"w")
	for line in ximagefile.readlines():
		xposfile.write(str(float(line.strip())/float(pxPerM))+"\n")
	xposfile.close()
	ximagefile.close()
	for line in yimagefile.readlines():
		yposfile.write(str(float(line.strip())/float(pxPerM))+"\n")
	yposfile.close()
	yimagefile.close()

if __name__=="__main__":
	if len(sys.argv) < 2 or len(sys.argv) > 4:
		print "Usage: python",sys.argv[0],"pixles-per-meter", "[inputDir=os.getcwd()] [outputDir=os.getcwd()]"
	elif len(sys.argv) == 2:
		px2meters(float(sys.argv[1]))
	elif len(sys.argv) == 3:
		px2meters(float(sys.argv[1]),sys.argv[2])
	else:
		px2meters(float(sys.argv[2]),sys.argv[2],sys.argv[3])
