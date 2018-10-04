if [[ "$#" -ne 7 ]]; then
  echo "Usage: bash run_middleware.sh <Port of middleware server> \
  <hostname of Flights> <Flights server port>   <hostname of Cars> <Cars server port> \
  <hostname of Rooms> <Rooms server port>"
  exit 1
fi

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:"$(pwd)/" \
 -classpath ../bin:../bin/middleware \
middleware.TCPMiddleware $1 $2 $3 $4 $5 $6 $7
