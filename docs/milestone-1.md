## Introduction
This project revolves around developing a Travel Reservation system with booking and management support for flights, cars and rooms. Given a single-client, single-server implementation, our goal in this milestone is to distribute the server first with Java's Remote Method Invocation API and later with the TCP protocol.

## RMI implementation
#### Overview
The server is distributed over three `ResourceManagers`, one each for flights, cars and rooms. An intermediary server called `Middleware` sits between the client and the server, relaying messages back and forth.

`run.sh`: ssh into different machines and run client, middleware and server

#### Customer handling
We decided to handle customers through replication at each resource manager. Each manager has its own database of customers and when an incoming command arrives at the middleware, the middleware forwards it to all managers, keeping the databases synchronized.

#### Bundles
As each resource manager maintains a list of customers, we simply call the methods on the respective manager.

## TCP implementation

#### Architecture

- Package command, args and id into `Serializable` object sent over TCP with `ObjectOutputStream`
- 

#### Concurrency
- Event listeners

## Additional functionality
