all:
	javac *.java 
	make -C sensors/
	make -C actuators/
clean:
	rm -f *.class
	make -C sensors/ clean
	make -C actuators/ clean
