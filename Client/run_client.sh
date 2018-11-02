if [[ "$#" -ne 2 ]]; then
    echo "# Usage: ./run_client.sh [<middleware_hostname> [<middleware_port>]]"
    exit 1
fi

java -Djava.security.policy==java.policy -cp ../bin/middleware:../bin Client.TCPClient $1 $2
