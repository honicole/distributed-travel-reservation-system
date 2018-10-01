all: java.policy compile-middleware-rmi RMIInterface.jar

java.policy: FORCE
	@echo "Creating middleware java policy"
	@echo "grant {" > java.policy
	@echo "  permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy
	@echo `pwd`

FORCE: ;

compile-middleware-rmi: middleware/*.java Server/Interface/IResourceManager.java
	javac -d ../bin -Xlint:deprecation middleware/*.java Server/Interface/IResourceManager.java
	@if [ ! -d ../bin/middleware/Server ]; then mkdir ../bin/middleware/Server; fi
	@if [ ! -d ../bin/middleware/Server/Interface ]; then mkdir ../bin/middleware/Server/Interface; fi
	cp ../bin/Server/Interface/IResourceManager.class ../bin/middleware/Server/Interface

RMIInterface.jar: Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface for middleware"
	@echo `pwd`
	jar cvf RMIInterface.jar ../bin/middleware/Server/Interface/IResourceManager.class

clean:
	rm -f middleware/Interface/*.class middleware/*.class
	rm -f RMIInterface.jar
	rm -f java.policy 
