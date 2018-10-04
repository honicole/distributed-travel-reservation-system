## Introduction
This project revolves around developing a Travel Reservation system with booking and management support for flights, cars and rooms. Given a single-client, single-server implementation, our goal in this milestone is to distribute the server first with Java's Remote Method Invocation API and later with the TCP protocol.

## RMI implementation
#### Overview
The server is distributed over three `ResourceManagers`, one each for flights, cars and rooms. An intermediary server called `Middleware` sits between the client and the server, relaying messages back and forth. To keep our system simple, each resource manager implements the entire interface and the middleware calls only the respective methods for each manager.

#### Customer handling
Our first design decision was to have the middleware manage the customers since it does not directly deal with any of the other resources. However, the purpose of our middleware is act to as a layer of communication and distribute the requests. With this in mind, we chose to decentralize customer handling through replication at each resource manager. Each manager maintains its own list of customers. When the client invokes a `customer`-command, the middleware calls the method on all resource managers.

#### Bundles
As each resource manager maintains a list of customers, the middleware simply calls the methods on the respective managers.

## TCP implementation
#### Overview
Our next task was to re-implement the system using the TCP protocol.

#### Architecture

In our implementation, all requests and responses are sent as serializable objects. Client requests are packaged into a `UserCommand` object containing an `id`, a `string` command and an `array` of arguments.

On both the middleware and the resource managers, a server socket is continuously listening on the port for new connections. For every incoming request, a thread selected from a thread pool processes the data.


## Additional functionality
`Research how Java 8 lambda functions/serialization can be used to simplify TCP distribution
and remote function invocation and explain in your report.`

`The CompletableFuture gives a better API for programming reactively, without forcing us to write explicit synchronization code. (we don't get this when using threads directly)
The Future interface (which is implemented by CompletableFuture) gives other additional, obvious advantages.`

Java 8 lambda functions
