# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]

java -Djava.security.policy==java.policy -cp ../Server/RMIInterface.jar:../bin Client.RMIClient $1 $2
