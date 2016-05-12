import os, os.path, string, sys
def csv2btf(fname,outdir=os.getcwd()):
	infile = open(fname)
	outnames = map(string.strip, infile.readline().split(','))
	outfiles = map(lambda name: open(os.path.join(outdir,name+".btf"),"w"),outnames)
	for line in infile.readlines():
		data = map(string.strip, line.split(','))
		for x in xrange(len(data)):
			outfiles[x].write(data[x]+"\n")
	map(lambda thing: thing.close(),outfiles)

if __name__=="__main__":
	if len(sys.argv) < 2 or len(sys.argv) > 3:
		print "Usage: python",sys.argv[0],"input.csv","[outputDir=os.getcwd()]"
	elif len(sys.argv) == 2:
		csv2btf(sys.argv[1])
	else:
		csv2btf(sys.argv[1],sys.argv[2])
