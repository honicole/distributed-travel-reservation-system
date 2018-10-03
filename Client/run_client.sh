# Usage: ./run_client.sh [<server_hostname> [<server_port>]]

java -Djava.security.policy==java.policy -cp ../bin/middleware:../bin Client.TCPClient $1 $2
