UNAME := $(shell uname)

#Mac
ifeq ($(UNAME), Darwin)
LIBNAME := libannwrapper.dylib
JNI_INC := -I/System/Library/Frameworks/javaVM.framework/Headers
libannwrapper.dylib: simple_ann.o annwrapper_wrap.o
	g++ -D_REENTRANT -shared simple_ann.o annwrapper_wrap.o -o libannwrapper.dylib
endif
#Linux
ifeq ($(UNAME), Linux)
LIBNAME := libannwrapper.so
#JNI_INC := -I/usr/lib/jvm/java-1.7.0-openjdk-amd64/include/ -I/usr/lib/jvm/java-1.7.0-openjdk-amd64/include/linux/
JNI_INC := -I/usr/lib/jvm/java-1.8.0-openjdk-amd64/include/ -I/usr/lib/jvm/java-1.8.0-openjdk-amd64/include/linux/
libannwrapper.so: simple_ann.o annwrapper_wrap.o 
	g++ -O3 -shared -D_REENTRANT simple_ann.o annwrapper_wrap.o -o libannwrapper.so
endif

all: $(LIBNAME)

simple_ann.o: simple_ann.h simple_ann.cpp 
	g++ -c -O3 -fPIC -D_REENTRANT simple_ann.cpp
annwrapper_wrap.o: annwrapper_wrap.cxx
	g++ -c -O3 -fPIC -D_REENTRANT annwrapper_wrap.cxx $(JNI_INC)
annwrapper_wrap.cxx: annwrapper.i
	swig -c++ -java -package biosim.core.util.annwrapper annwrapper.i
clean:
	rm *.o annwrapper_wrap.cxx annwrapper.java annwrapperJNI.java SimpleANN.java 
