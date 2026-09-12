# Enterprise Workflow & Task Management Engine

## 1. Problem Statement
Enterprise organizations require high-throughput workflow engines capable of orchestrating complex business processes, managing strict state transition rules, maintaining audit trails, and executing reactive user interactions without thread blocking or UI race conditions.

**CORE PARADIGM:** This project solves state consistency across concurrent web users by combining an Actor-based State Machine in Scala (Apache Pekko) on the backend with an immutable NgRx state store in Angular on the frontend.

## 2. Essential System Functionalities

| Domain Module | Core Functionality | Technical Implementation |
| :--- | :--- | :--- |
| **Task State Machine** | Enforces strict workflow rules (TODO -> IN_PROGRESS -> DONE). Blocks invalid jumps. | Pekko Typed Actor with message ask-pattern. |
| **Relational Persistence** | Asynchronous non-blocking storage, connection pooling & auto schema creation. | Slick 3.x FRP ORM over HikariCP and PostgreSQL driver. |
| **Interactive Kanban Board** | Multi-column drag-and-drop workflow UI with immediate visual status updates. | Angular CDK Drag-and-Drop, RxJS pipelines & Material. |
| **Reactive State Store** | Centralized immutability store preventing UI race conditions during rapid moves. | NgRx Store, Actions, Reducers, Effects & Selectors. |

## 3. System Architecture

The application uses a decoupled monorepo layout where Scala handles high-concurrency API tasks and state logic, while Angular presents a single-page application interfacing over proxied REST APIs.

### Directory Layout

```text
workflow-engine/
├── docker-compose.yml              # PostgreSQL Database Container
├── backend-scala/                  # Scala Backend Project
│   ├── build.sbt                   # SBT Build & Dependencies
│   └── src/main/
│       ├── resources/
│       │   └── application.conf    # Pekko & Postgres Configuration
│       └── scala/com/workflow/
│           ├── Main.scala          # Application Server & Schema Setup
│           ├── domain/Task.scala   # Scala Case Classes & Domain Models
│           ├── db/Tables.scala     # Slick Database Table Mappings
│           ├── actors/TaskStateActor.scala # Pekko State Machine Actor
│           └── api/TaskRoutes.scala # Pekko HTTP REST Endpoint Routes
└── frontend-angular/               # Angular 18 Frontend Project
    ├── proxy.conf.json             # Angular Dev Proxy Configuration
    └── src/app/
        ├── core/
        │   ├── models/task.model.ts # TypeScript Data Definitions
        │   └── state/               # NgRx State Management Modules
        │       ├── task.actions.ts
        │       ├── task.reducer.ts
        │       ├── task.effects.ts
        │       └── task.selectors.ts
        └── features/
            └── kanban/             # Angular Material CDK Board Component
```
## 4. System starts with following 

1. **Start Database Container:** `docker compose up -d`
2. **Start Scala Pekko HTTP Backend:** `cd backend-scala; sbt run`
3. **Start Angular Frontend:** `cd frontend-angular; npm start`

## 5. Troubleshooting & Error Resolution Guide

| Symptom / Error | Root Cause | Solution |
| :--- | :--- | :--- |
| `org.postgresql.util.PSQLException: Connection refused` | PostgreSQL Docker container is not running. | Run `docker compose up -d` |
| `XMLHttpRequest blocked by CORS policy` | Angular app is bypassing proxy config. | Run Angular with `npm start` which uses proxy.conf.json |
| `Pekko AskTimeoutException` | TaskStateActor failed during message processing. | Check backend terminal logs for database connection issues. |
| `SlickException: Could not obtain connection` | HikariCP connection pool exhausted. | Avoid blocking synchronous code inside `db.run()` futures. |
