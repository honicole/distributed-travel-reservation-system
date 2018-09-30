all: java.policy compile-server-rmi RMIInterface.jar

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant {" > java.policy
	@echo "  permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface.jar: ../Middleware/Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface"
	javac ../Middleware/Server/Interface/IResourceManager.java
	jar cvf RMIInterface.jar ../Middleware/Server/Interface/IResourceManager.class

compile-server-rmi: Server/RMI/*.java ../Middleware/Server/Interface/IResourceManager.java Server/Common/*.java
	javac -d ../bin -Xlint:deprecation Server/RMI/*.java ../Middleware/Server/Interface/IResourceManager.java Server/Common/*.java

clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class
	rm -f RMIInterface.jar
	rm -f java.policy 
