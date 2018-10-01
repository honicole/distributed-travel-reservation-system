# Server makefile
#
# Instructions for humans:
# 
# This makefile is supposed to be run from comp512-project/Server.
#
# It is supposed to:
# * Clean the previous build:
#   o Remove comp512-project/Server/java.policy
#   o Remove comp512-project/bin/Server (thus removing compiled files).
# * Make the java.policy file in comp512-project/Server.
# * Prepare the RMIInterface.jar dependency. This done via the Middleware makefile.
# * Take the Server files in comp512-project/Server and then compile them into comp512-project/bin/Server.
#

all: clean java.policy compile-server-rmi

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant {" > java.policy
	@echo "  permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface: ../Middleware/Server/Interface/IResourceManager.java
	cd ../Middleware; make -B RMIInterface.jar

compile-server-rmi: RMIInterface ../Middleware/Server/Interface/IResourceManager.java Server/Common/*.java
	@echo "Compiling server..."
	@if [ ! -d ../bin ]; then mkdir ../bin; fi
	@if [ ! -d ../bin/Server ]; then mkdir ../bin/Server; fi
	@if [ ! -d ../bin/Server/Common ]; then mkdir ../bin/Server/Common; fi
	cd ..; javac -d ./bin -Xlint:deprecation -cp .:./bin/middleware Server/Server/Common/*.java

clean:
	rm -rf ../bin/Server
	rm -f java.policy 
