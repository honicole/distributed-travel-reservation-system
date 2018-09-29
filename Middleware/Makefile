all: java.policy compile-middleware-rmi

java.policy: FORCE
	@echo "Creating middleware java policy"
	@echo "grant {" > java.policy
	@echo "  permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface.jar: middleware/Interface/IResourceManager.java
	@echo "Compiling RMI middleware interface"
	javac middleware/Interface/IResourceManager.java
	jar cvf RMIInterface.jar middleware/Interface/IResourceManager.class

compile-middleware-rmi: middleware/*.java middleware/Interface/IResourceManager.java
	javac -d ../bin -Xlint:deprecation middleware/*.java middleware/Interface/IResourceManager.java

clean:
	rm -f middleware/Interface/*.class middleware/*.class
	rm -f RMIInterface.jar
	rm -f java.policy 
