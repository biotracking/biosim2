UNAME := $(shell uname)
#Mac
ifeq ($(UNAME), Darwin)
LIBNAME := libkdewrapper.dylib
JNI_INC := -I/System/Library/Frameworks/javaVM.framework/Headers
libannwrapper.dylib: simple_kde.o kdewrapper_wrap.o
	g++ -shared simple_kde.o kdewrapper_wrap.o -o libkdewrapper.dylib
endif
#Linux
ifeq ($(UNAME), Linux)
LIBNAME := libkdewrapper.so
JNI_INC := -I/usr/lib/jvm/java-7-openjdk-amd64/include/ -I/usr/lib/jvm/java-7-openjdk-amd64/include/linux/
#JNI_INC := -I/usr/lib/jvm/java-8-openjdk-amd64/include/ -I/usr/lib/jvm/java-8-openjdk-amd64/include/linux/
libannwrapper.so: simple_kde.o kdewrapper_wrap.o 
	g++ -shared simple_kde.o kdewrapper_wrap.o -o libkdewrapper.so
endif

all: $(LIBNAME) kde

simple_kde.o: simple_kde.h simple_kde.cpp 
	g++ -c -fPIC -O3 simple_kde.cpp 
kdewrapper_wrap.o: kdewrapper_wrap.cxx
	g++ -c -fPIC -O3 kdewrapper_wrap.cxx $(JNI_INC)
kdewrapper_wrap.cxx: kdewrapper.i
	swig -c++ -java -package biosim.core.util.kdewrapper kdewrapper.i
clean:
	rm *.o kdewrapper_wrap.cxx kdewrapper.java kdewrapperJNI.java SimpleKDE.java 
