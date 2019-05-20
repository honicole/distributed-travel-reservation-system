# distributed-travel-reservation-system

Socket-based distributed system implementing two-phase locking for concurrency control, two-phase commit with shadowing for data persistence and full recovery at the middleware.

To run the RMI resource manager:

```
cd Server/
./run_server.sh [<rmi_name>] # starts a single ResourceManager
./run_servers.sh # convenience script for starting multiple resource managers
```

To run the RMI client:

```
cd Client
./run_client.sh [<server_hostname> [<server_rmi_name>]]
```
