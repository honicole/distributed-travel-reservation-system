if [[ "$#" -ne 1 ]]; then
    echo "#Usage: ./run_server.sh [<server_port>]"
    exit 1
fi

./run_rmi.sh > /dev/null 2>&1
java -Djava.security.policy==java.policy -Djava.rmi.server.codebase=file:"$(pwd)/" \
 -classpath ../bin:../bin/middleware Server.TCP.TCPResourceManager $1
