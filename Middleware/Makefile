# Middleware makefile
#
# Instructions for humans:
# 
# This makefile is supposed to be run from comp512-project/Middleware.
# The final outcome depends on the output of making Server, which depends on RMIInterface that is built here.
# It also depends on the client being built.
#
# It is supposed to:
# * Clean the previous build:
#   o Remove comp512-project/Middleware/java.policy
#   o Remove comp512-project/bin/middleware (thus removing compiled files).
#   o Remove comp512-project/Middleware/RMIInterface.jar, since it will be recreated.
# * Make the Server.
# * Make the java.policy file in comp512-project/Middleware.
# * Prepare the RMIInterface.jar dependency:
#   o This JAR file has one source class, Server.Interface.IResourceManager, which is now located in 
#     comp512-project/Middleware/Server/Interface 
#   o The new destination of the JAR file is comp512-project/Middleware.
# * Compile:
#   o Take the middleware files in comp512-project/Middleware and then compile them into comp512-project/bin/middleware.
#

all: clean makeserver java.policy compile-middleware-rmi RMIInterface.jar

java.policy: FORCE
	@echo "Creating middleware java policy"
	@echo "grant {" > java.policy
	@echo "  permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

makeserver:
	@echo "Making Server..."
	@cd ../Server; make
	@echo "Finished making Server."

compile-middleware-rmi: middleware/*.java Server/RMI/RMIResourceManager.java RMIInterface.jar
	@echo "Compiling middleware..."
	cd ..; javac -d ./bin/middleware -Xlint:deprecation -cp .:./bin:./bin/middleware \
	  Middleware/Server/Common/ResourceManager.java
	cd ..; javac -d ./bin/middleware -Xlint:deprecation -cp .:./bin:./bin/middleware \
	  Middleware/Server/RMI/RMIResourceManager.java
	cd ..; javac -d ./bin/middleware -Xlint:deprecation -cp .:./bin/middleware:./bin/middleware:./bin \
	  Middleware/middleware/*.java

RMIInterface.jar: ./Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface for middleware"
	@if [ ! -d ../bin ]; then mkdir ../bin; fi
	@if [ ! -d ../bin/middleware ]; then mkdir ../bin/middleware; fi
	@if [ ! -d ../bin/middleware/Server ]; then mkdir ../bin/middleware/Server; fi
	@if [ ! -d ../bin/middleware/Server/Interface ]; then mkdir ../bin/middleware/Server/Interface; fi
	javac -d ../bin/middleware -Xlint:deprecation Server/Interface/IResourceManager.java
	jar cvf RMIInterface.jar ../bin/middleware/Server/Interface/IResourceManager.class

clean:
	rm -rf ../bin/middleware
	rm -f RMIInterface.jar
	rm -f java.policy 
