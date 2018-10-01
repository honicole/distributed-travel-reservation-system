./run_rmi.sh > /dev/null 2>&1

if [[ "$#" -ne 4 ]]; then
  echo "Usage: bash run_middleware.sh <Name of middleware server>\
  <hostname of Flights> <hostname of Cars> <hostname of Rooms>"
  exit 1
fi

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:"$(pwd)/" -classpath ../bin \
middleware.RMIMiddleware $1 $2 $3 $4
