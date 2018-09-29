all: java.policy compile-client

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant {" > java.policy
	@echo "  permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java
	make -C ../Server/ RMIInterface.jar

compile-client: ../Server/RMIInterface.jar
	javac -Xlint:deprecation -cp ../Server/RMIInterface.jar Client/*.java

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f java.policy
