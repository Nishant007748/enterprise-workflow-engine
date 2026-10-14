# Enterprise Workflow & Task Management Engine

A full-stack task management application with an Angular Kanban board, a Scala HTTP API, and PostgreSQL persistence. Tasks can be created, listed, and moved between workflow columns; changes are stored in PostgreSQL and remain after a page refresh.

## Contents

- [Features](#features)
- [Technology stack](#technology-stack)
- [System architecture](#system-architecture)
- [Repository layout](#repository-layout)
- [Prerequisites](#prerequisites)
- [Run locally on Windows](#run-locally-on-windows)
- [Use the application](#use-the-application)
- [REST API](#rest-api)
- [Build and test](#build-and-test)
- [Configuration](#configuration)
- [Current limitations](#current-limitations)
- [Troubleshooting](#troubleshooting)

## Features

- Kanban board with **To Do**, **In Progress**, and **Done** columns.
- Create a task from the board; newly created tasks start in `TODO`.
- Drag and drop tasks between columns.
- Load and persist tasks through the backend API.
- Automatically create the PostgreSQL `tasks` table at backend startup.
- Use Angular's development proxy to forward `/api` requests to the backend.
- Manage frontend task state with NgRx actions, reducers, selectors, and effects.
- Process backend task commands with a Pekko Typed actor and asynchronous Slick database calls.

## Technology stack

| Area | Technology |
| --- | --- |
| Frontend | Angular 18, TypeScript, RxJS |
| Frontend state | NgRx Store, Effects, Entity |
| Drag and drop | Angular CDK |
| Backend | Scala 2.13, SBT |
| HTTP server and actors | Apache Pekko HTTP, Pekko Typed Actors |
| JSON | Spray JSON and Pekko HTTP Spray JSON support |
| Database access | Slick 3.4, Slick HikariCP |
| Connection pool | HikariCP |
| Database | PostgreSQL 16 |
| Local database runtime | Docker Compose |


The Angular application is served at `http://localhost:4200`. During development, its proxy forwards requests under `/api` to the backend at `http://localhost:8080`. The backend exposes task routes, sends operations to a Pekko Typed actor, and uses Slick to read or write PostgreSQL. On startup, the backend creates the task schema before binding the HTTP server.

## Repository layout

```text
enterprise-workflow-engine/
├── docker-compose.yml
├── README.md
├── sbt/
│   └── bin/
│       └── sbt.bat
├── backend-scala/
│   ├── build.sbt
│   ├── project/
│   │   └── build.properties
│   └── src/main/
│       ├── resources/
│       │   └── application.conf
│       └── scala/com/workflow/
│           ├── Main.scala
│           ├── actors/TaskStateActor.scala
│           ├── api/TaskRoutes.scala
│           ├── db/Tables.scala
│           └── domain/Task.scala
└── frontend-angular/
    ├── angular.json
    ├── package.json
    ├── proxy.conf.json
    └── src/app/
        ├── core/
        │   ├── models/task.model.ts
        │   ├── services/task.service.ts
        │   └── state/
        └── features/kanban/
```

## Prerequisites

- Windows 10/11 with PowerShell.
- A Java Development Kit installed and available as `java` in PowerShell.
- Node.js and npm.
- Docker Desktop for Windows using the Linux container engine (or another Docker Engine compatible with the Compose file).

Check that the tools are available:

```powershell
java -version
node --version
npm --version
docker info
```

`docker info` must show a **Server** section. If it reports that it cannot connect to the Docker API, start Docker Desktop and wait for its engine to finish starting.

## Run locally on Windows

Open **three separate PowerShell terminals**. In each one, first go to the repository root:

```powershell
Set-Location N:\Projects\enterprise-workflow-engine
```

### 1. Start PostgreSQL

In terminal 1:

```powershell
docker compose up -d
docker compose ps
```

Wait for the `postgres` service to be running before starting the backend. The Compose service publishes PostgreSQL on port `5432` and creates the `workflow_db` database.

### 2. Start the backend

In terminal 2:

```powershell
Set-Location N:\Projects\enterprise-workflow-engine\backend-scala
..\sbt\bin\sbt.bat run
```

Keep the terminal open. Once the database schema is ready, the backend logs the HTTP address; the API is served on port `8080`.

### 3. Start the frontend

In terminal 3:

```powershell
Set-Location N:\Projects\enterprise-workflow-engine\frontend-angular
npm start
```

Open [http://localhost:4200](http://localhost:4200) in your browser. Keep all three terminals running while using the application.

> Run each `Set-Location` from the repository root path shown above. If you are already in `backend-scala`, use `Set-Location ..\frontend-angular` to switch to the frontend; `cd frontend-angular` from the backend directory looks for a folder that does not exist.

## Use the application

1. Open `http://localhost:4200`.
2. Enter a task title in the input and select **+ Add Task** (or press Enter).
3. Confirm the task appears in **To Do**.
4. Drag the task to **In Progress** or **Done**.
5. Refresh the page and confirm that the saved task and state are loaded again.

The current board UI collects a title only. The API supports an optional description, but there is no description input on the board.

## REST API

Base URL: `http://localhost:8080`

The frontend can use the same endpoints through its proxy, for example `http://localhost:4200/api/tasks`.

| Method | Endpoint | Description | Success response |
| --- | --- | --- | --- |
| `GET` | `/api/tasks` | List all tasks | `200 OK`, JSON array |
| `POST` | `/api/tasks` | Create a task | `201 Created`, created task |
| `PUT` | `/api/tasks/{id}/state` | Update a task's state | `200 OK`, updated task |

### Task JSON

Example task response:

```json
{
  "id": 1,
  "title": "Prepare release checklist",
  "description": "Review the deployment steps",
  "state": "TODO"
}
```

For a create request, send `id` as `null`; the database generates the returned ID. `description` may be `null`. The UI uses the state values `TODO`, `IN_PROGRESS`, and `DONE`.

### PowerShell API smoke test

With the database and backend running, use PowerShell:

```powershell
# List existing tasks
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks"

# Create a task
$body = @{
    id = $null
    title = "API smoke test"
    description = "Created from PowerShell"
    state = "TODO"
} | ConvertTo-Json

$created = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/tasks" `
    -ContentType "application/json" `
    -Body $body

$created

# Move the created task to In Progress
$stateBody = @{ state = "IN_PROGRESS" } | ConvertTo-Json
Invoke-RestMethod `
    -Method Put `
    -Uri "http://localhost:8080/api/tasks/$($created.id)/state" `
    -ContentType "application/json" `
    -Body $stateBody

# Confirm the saved task and state
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks"
```

The API returns `404 Not Found` when updating the state of a task ID that does not exist.

## Build and test

### Backend compile

```powershell
Set-Location N:\Projects\enterprise-workflow-engine\backend-scala
..\sbt\bin\sbt.bat compile
```

### Frontend production build

```powershell
Set-Location N:\Projects\enterprise-workflow-engine\frontend-angular
npm run build
```

### Frontend unit tests

```powershell
Set-Location N:\Projects\enterprise-workflow-engine\frontend-angular
npm test
```

The frontend test command uses Angular CLI/Karma and may open a browser for the test runner, depending on the local environment.

## Configuration

### PostgreSQL

The local Compose service is configured in `docker-compose.yml`:

| Setting | Local value |
| --- | --- |
| Host / port | `localhost:5432` |
| Database | `workflow_db` |
| User | `workflow_user` |
| Password | `workflow_password` |

These credentials are for local development only; do not reuse them in a deployed environment.

### Backend

`backend-scala/src/main/resources/application.conf` configures Slick to connect to PostgreSQL at `jdbc:postgresql://localhost:5432/workflow_db`. The Pekko HTTP server binds to `0.0.0.0:8080` in `Main.scala`.

### Frontend proxy

`frontend-angular/proxy.conf.json` forwards `/api` requests to `http://localhost:8080`. The proxy is enabled by the Angular `serve` target in `angular.json`; use `npm start` for local development.

## Current limitations

The following are not implemented yet and should not be assumed from the project name or technology choices:

- Strict workflow transition rules: the backend currently accepts any state string in the update endpoint. It does not enforce only `TODO` → `IN_PROGRESS` → `DONE`.
- The create endpoint always sets the task state to `TODO`, regardless of the state sent in the request.
- Audit history, user accounts, authentication, authorization, or multi-tenant access.
- Task editing, deletion, ordering, or description entry in the UI.
- Automated backend tests or a committed API integration test suite.
- Production deployment configuration, secret management, database migrations, or a production web-server/proxy setup.

## Troubleshooting

| Symptom | Likely cause | What to do |
| --- | --- | --- |
| `failed to connect to the docker API` | Docker Desktop is stopped or the selected engine has not started. | Start Docker Desktop, wait for the engine, then confirm `docker info` shows a Server section. |
| `docker compose up -d` succeeds but PostgreSQL is not ready | The database container is still initializing. | Check `docker compose ps`, then wait and inspect logs with `docker compose logs postgres`. |
| PostgreSQL `Connection refused` from the backend | PostgreSQL is not running on port `5432`. | Start Docker Desktop, run `docker compose up -d`, and check `docker compose ps`. |
| Angular reports a missing `package.json` | npm was run from the repository root or backend folder. | Change to `N:\Projects\enterprise-workflow-engine\frontend-angular`, then run `npm start`. |
| Frontend loads but API calls fail | Backend is stopped, PostgreSQL is unavailable, or the frontend proxy is bypassed. | Confirm the backend started after the database; use `npm start` and check the backend terminal logs. |
| Port `8080`, `4200`, or `5432` is already in use | Another process is using the required port. | Stop that process or configure the application and proxy to use a different matching port. |
| Docker Compose warns that `version` is obsolete | The Compose file includes a legacy `version` key. | This warning is informational; Compose ignores the key and can still start the service. |
