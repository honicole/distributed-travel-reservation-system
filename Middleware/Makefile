all: java.policy compile-middleware-rmi RMIInterface.jar

java.policy: FORCE
	@echo "Creating middleware java policy"
	@echo "grant {" > java.policy
	@echo "  permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

compile-middleware-rmi: middleware/*.java middleware/Interface/IResourceManager.java
	javac -d ../bin -Xlint:deprecation middleware/*.java middleware/Interface/IResourceManager.java

RMIInterface.jar: middleware/Interface/IResourceManager.java
	@echo "Compiling RMI middleware interface"
	@echo `pwd`
	jar cvf RMIInterface.jar ../bin/middleware/Interface/IResourceManager.class

clean:
	rm -f middleware/Interface/*.class middleware/*.class
	rm -f RMIInterface.jar
	rm -f java.policy 
