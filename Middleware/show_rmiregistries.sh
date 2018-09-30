echo "These processes are using port 1099. To kill the RMI registry, use 'kill -9 <PID>'."
lsof -w -n -i tcp:1099
