# Distributed Casino Platform

An end-to-end distributed gaming system featuring an Android frontend and a multithreaded Java backend, using TCP sockets, MapReduce workflows, and dynamic hashing. 

## Architecture

The system consists of the following components, communicating exclusively over **TCP sockets**:

- **Master** — Multithreaded TCP server that handles player search/bet requests and manager operations. Routes game data to the appropriate Worker node using a hash function `H(GameName) mod NumberOfNodes`, and coordinates MapReduce queries via the Reducer.
- **Workers** — Multithreaded TCP servers that store game data and statistics in memory (no database, no disk persistence except optional game logos), and process bet outcomes.
- **Reducer** — Multithreaded TCP server that aggregates intermediate MapReduce results from Workers into final results (e.g. total profit/loss per game, per provider, per player).
- **SecureRandomGenerator** — Independent multithreaded TCP server that generates random numbers for bet outcomes using a producer-consumer model with a bounded buffer, and authenticates each value via `sha256(number + secret)`.
- **Android App / ConsoleClient** — Communicates with the Master over TCP to search for games, apply filters, and place bets.

## Key Features

- **Manager operations** (console app): add/remove/edit games, view total profit/loss per game, per provider, and per player
- **Player operations**: browse and filter available games by popularity (stars), bet limits ($ / $$ / $$$), and risk level (low / medium / high); play games; rate games (1–5 stars)
- **MapReduce-based aggregation** for profit/loss calculations per provider and per player, implemented from scratch (no external libraries)
- **Distributed data storage**: games are hashed and distributed across Worker nodes, all data kept in memory
- **Producer-consumer model** for random number generation, using a bounded buffer between the SecureRandomGenerator and Workers
- **Concurrency control** for simultaneous bets on the same game, implemented using `synchronized` and `wait`/`notify` (no `java.util.concurrent`)
- **Configurable number of Worker nodes**
- Risk-based payout system using predefined multiplier tables (Low / Medium / High) and Jackpot values

## Built With

- Java (Master, Workers, Reducer, SecureRandomGenerator)
- Plain `ServerSocket` / TCP Sockets for all inter-component communication (no HTTP frameworks)
- Custom MapReduce implementation
- Android SDK

## How to Run

1. Open a terminal in the project's `code` folder and compile:

```bash
javac *.java
```

2. Start the components in the following order:

```bash
java SecureRandomGenerator
java Reducer
java Master
java Worker 6001
java Worker 6002
java Worker 6003
java ConsoleClient
```

Each `Worker` is started with its own port number as an argument, and any number of Workers can be launched this way.

## Academic context

Developed as a group project for the "Distributed Systems" course, AUEB, Spring Semester 2025-2026.
