# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]

java -Djava.security.policy==java.policy -cp ../bin/middleware:../bin Client.RMIClient $1 $2
