# Income statement

A small full-stack app that shows the income statement of Northwind Coffee Roasters for
any date range. The backend reads the ledger and does all the arithmetic; the frontend
shows the report.

Decisions, assumptions and the Q1 2026 net income are in [NOTES.md](NOTES.md).

## What you need

| Tool | Version used |
| --- | --- |
| Java (JDK) | 21.0.10 |
| Node.js | 23.11.0 |
| npm | 11.3.0 |

Maven does not need to be installed; `./mvnw` downloads it on first use.

Main libraries: Spring Boot 4.1.1, React 19.3, Vite 8.3, TypeScript 6.0.

## Run the backend

```sh
cd backend
./mvnw spring-boot:run
```

It listens on http://localhost:8080.

```sh
curl 'http://localhost:8080/income-statement?start=2026-01-01&end=2026-03-31'
```

## Use another ledger file

The ledger's location is the `ledger.path` setting, so another file can be used without
changing the repository:

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--ledger.path=file:/path/to/ledger.json
```

A file with mistakes in it does not stop the app. Every account and every journal entry
is validated on its own. One that fails (a missing date, an amount that is not a number,
an unknown account, debits that do not equal credits, and so on) goes into
`erroredAccounts` or `erroredEntries` with every reason, and is in no total. An errored
entry is left out as a whole. Every statement returns both lists, and the page shows them
above the table. To see it, run the supplied ledger with eight things broken:

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--ledger.path=file:src/test/resources/ledger-with-problems.json
```

## Run the frontend

```sh
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The backend must be running: the dev server forwards
`/income-statement` to http://localhost:8080.

## Run the tests

```sh
cd backend
./mvnw test
```

The frontend has no test suite: it does no arithmetic and shows the backend's amounts as
they are. It has a type check and a lint:

```sh
cd frontend
npm run build
npm run lint
```

## Where things are

```text
backend/    Spring Boot. controller → service → ledger
  src/main/resources/ledger.json   The ledger, copied unchanged from the prompt
  LedgerReader.java                Reads the file and sorts accounts and entries into valid or errored
  Ledger.java                      The valid accounts and entries, and the errored ones
  IncomeStatementService.java      The path from the dates to the numbers
frontend/   React + Vite + TypeScript
```
