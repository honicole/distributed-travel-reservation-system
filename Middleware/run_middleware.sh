./run_rmi.sh > /dev/null 2>&1

if [[ "$#" -ne 7 ]]; then
  echo "Usage: bash run_middleware.sh <Name of middleware server> \
  <hostname of Flights> <Flights server name>   <hostname of Cars> <Cars server name> \
  <hostname of Rooms> <Rooms server name>"
  exit 1
fi

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:"$(pwd)/" \
 -classpath ../bin:../bin/middleware \
middleware.RMIMiddleware $1 $2 $3 $4 $5 $6 $7
