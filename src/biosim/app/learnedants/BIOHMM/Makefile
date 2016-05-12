UNAME := $(shell uname)
#Mac

all: biohmm

simple_kde.o: simple_kde.h simple_kde.cpp 
	g++ -c -O3 simple_kde.cpp 
biohmm.o: biohmm.cpp biohmm.h
	g++ -c -O3 biohmm.cpp
biohmm: biohmm.o simple_kde.o
	g++ -o biohmm biohmm.o simple_kde.o
clean:
	rm *.o 
