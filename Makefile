# This makefile makes the entire project with one command, namely the Client, Middleware, Server, and ResourceManagers.

all: clean makeall

# TODO: Add ResourceManagers here later
makeall:
	@echo "Building Comp512 project..."
	@cd Client; make
	@cd Middleware; make
	@cd Server; make

clean:
	rm -rf bin
