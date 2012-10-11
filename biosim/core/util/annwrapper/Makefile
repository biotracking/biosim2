UNAME := $(shell uname)
#Mac
ifeq ($(UNAME), Darwin)
LIBNAME := libannwrapper.dylib
JNI_INC := -I/System/Library/Frameworks/javaVM.framework/Headers
libannwrapper.dylib: simple_ann.o annwrapper_wrap.o ann/lib/libANN.a
	g++ -shared simple_ann.o annwrapper_wrap.o ann/lib/libANN.a -o libannwrapper.dylib
endif
#Linux
ifeq ($(UNAME), Linux)
LIBNAME := libannwrapper.so
libannwrapper.so: simple_ann.o annwrapper_wrap.o  ann/src/*.o
	g++ -shared simple_ann.o annwrapper_wrap.o ann/src/*.o -o libannwrapper.so
endif

all: $(LIBNAME) ann

simple_ann.o: simple_ann.h simple_ann.cpp 
	g++ -c simple_ann.cpp -Iann/include/
annwrapper_wrap.o: annwrapper_wrap.cxx
	g++ -c annwrapper_wrap.cxx -Iann/include/ $(JNI_INC)
annwrapper_wrap.cxx: annwrapper.i
	swig -c++ -java -package biosim.core.util.annwrapper annwrapper.i
clean:
	rm *.o annwrapper_wrap.cxx annwrapper.java annwrapperJNI.java SimpleANN.java 
