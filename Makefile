all:
	make -C biosim/
doc:
	javadoc -subpackages biosim -d docs/
clean:
	make -C biosim/ clean
