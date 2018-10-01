# Client makefile
#
# Instructions for humans:
# 
# This makefile is supposed to be run from comp512-project/Client.
# 
# It is supposed to:
# * Clean the previous build:
#   o Remove comp512-project/Client/java.policy
#   o Remove comp512-project/bin/Client (thus removing all compiled files).
#   o Remove comp512-project/Middleware/RMIInterface.jar, since it will be recreated.
# * Make the java.policy file in comp512-project/Client.
# * Prepare the RMIInterface.jar dependency. This done via the Middleware makefile.
# * Take the client files in comp512-project/Client and then compile them into comp512-project/bin/Client.
#


all: clean java.policy compile-client

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant {" > java.policy
	@echo "  permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy
	
FORCE: ;

RMIInterface: ../Middleware/Server/Interface/IResourceManager.java
	cd ../Middleware; make -B RMIInterface.jar

compile-client: RMIInterface
	@if [ ! -d ../bin ]; then mkdir ../bin; fi
	@if [ ! -d ../bin/Client ]; then mkdir ../bin/Client; fi
	@cd ..;	javac -d ./bin -Xlint:deprecation -cp ./Client:./Middleware Client/Client/*.java

clean:
	rm -rf ../bin/Client
	rm -f ../Middleware/RMIInterface.jar
	rm -f java.policy
