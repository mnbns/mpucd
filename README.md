# Idempotent Background Transaction Processing Service

FDE L2 Practical Assessment. A background console service that processes
banking transaction requests (CREDIT/DEBIT/REVERSAL) idempotently, in the
face of duplicates, out-of-order arrival, and transient failures.

- **Deliverables & branches**: [`DELIVERABLES.md`](DELIVERABLES.md) — what's
  being delivered, and what `main` vs `design-baseline` each mean.
- **Design note**: [`DESIGN.md`](DESIGN.md) — User Stories, Acceptance
  Criteria, HLD, LLD, flow charts, sequence diagram, design decisions,
  traceability matrix (FR/NFR → US → AC → test case), known gaps, AI-usage notes.
- **Live, clickable version of the same design** (bidirectional links
  between every requirement and every design artifact): **[live docs
  site](https://mnbns.github.io/mpucd/)**.
- **Release notes**: [`RELEASE_NOTES.md`](RELEASE_NOTES.md) — defects found
  and fixed, final test results.
- **Test report**: [`docs/test-report/ExtentReport.html`](docs/test-report/ExtentReport.html)
  (a committed copy of the last known-good run — see below for how to generate a fresh one).

## 1. Requirements

- **JDK 17**
- **Maven 3.9+**

You do **not** need to install either yourself — a known-good copy of both
is bundled in this repo under [`Installations/`](Installations/), already
verified against this exact project.

## 2. Environment setup

### Option A — use the bundled copies (recommended, zero install)

A ready-made script points your machine at the bundled JDK 17 and Maven
instead of (or alongside) anything already installed:

```cmd
Installations\set-java-maven-path.bat
```

Run it once, from a `cmd.exe` prompt, at the repo root. It:
- Sets `JAVA_HOME` and `MAVEN_HOME` to the bundled copies under `Installations\Java` and `Installations\Maven`
- Prepends both `bin` folders to `PATH`
- Persists `JAVA_HOME`, `MAVEN_HOME`, and `PATH` for your Windows user account (`setx`), so every **new** terminal you open afterwards (cmd, PowerShell, Git Bash, VS Code's integrated terminal) picks it up automatically
- Prints `java -version` and `mvn -v` at the end so you can confirm it took effect

**Terminals already open keep their old `PATH`** — close and reopen your terminal (or just open a new tab) before running any `mvn` command below.

### Option B — set it manually for one terminal session only

If you'd rather not persist anything system-wide, set the three variables
by hand in the terminal you're about to build/test in. **Run these from the
repo root** (the folder this README is in) — they use a path relative to it;
if your terminal is somewhere else, replace `Installations` below with the
full path to this checkout's `Installations` folder.

**PowerShell**
```powershell
$env:JAVA_HOME  = (Resolve-Path ".\Installations\Java").Path
$env:MAVEN_HOME = (Resolve-Path ".\Installations\Maven").Path
$env:PATH       = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"
```

**Git Bash / WSL / macOS / Linux**
```bash
export JAVA_HOME="$(pwd)/Installations/Java"
export MAVEN_HOME="$(pwd)/Installations/Maven"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
```

**cmd.exe**
```cmd
set "JAVA_HOME=%cd%\Installations\Java"
set "MAVEN_HOME=%cd%\Installations\Maven"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"
```

Verify either option worked before continuing:

```bash
java -version    # must report 17.x
mvn -v           # "Java version: 17..." on the line Maven reports, not 1.8 or 11
```

If `mvn -v` reports a Java version other than 17 (common when a system JRE
is already on `PATH`), `JAVA_HOME` isn't actually taking effect for Maven —
re-check the variable is set in *this* terminal, not just a different one.

## 3. Build

From the repo root:

```bash
cd transaction-service

mvn compile                      # fastest sanity check: does it compile?
mvn install -DskipTests=true     # full package, tests skipped — confirms the whole build is clean
```

Both were verified clean against this exact checkout (`BUILD SUCCESS`, no errors).

## 4. Run the service

```bash
mvn spring-boot:run
```

This loads the sample batch (`src/main/resources/sample-transactions.json`,
the problem statement's §9.8 test data) and processes it in order. Every
request is schema-validated and logged individually (`target/logs/transaction-service.log`
and console); the tail of a real run looks like:

```
SUMMARY: RECEIVED=0  PROCESSING=0  PENDING_ORDER=0  RETRY_PENDING=0  PROCESSED=4  DUPLICATE=1  FAILED=1
---
BALANCE ACC-201 = 85
BALANCE ACC-202 = 0
BALANCE ACC-203 = 500
```

## 5. Run the tests

```bash
mvn test
```

Runs 11 Cucumber feature files (one per User Story, under
`src/test/resources/features/`) — **36 scenarios, 142 steps**, all automated
against the real engine classes (no mocks, no HTTP). Last verified run:
`36 Scenarios (36 passed)`, `142 Steps (142 passed)`.

### Where to look at the results

| Artifact | Path | What it's for |
|---|---|---|
| **Human-facing HTML report** | `transaction-service/target/cucumber-reports/ExtentReport.html` | Open this in any browser. Per-scenario pass/fail, step-by-step timeline, and an embedded "Step Snapshot" (masked accountId, status, retryCount) for every step — this is the one to actually read. |
| Machine-readable report | `transaction-service/target/cucumber-reports/cucumber.json` | Same data, for CI tooling or re-processing. Carries real step embeddings (not just pass/fail). |
| Test-run log | `transaction-service/target/logs/cucumber-tests.log` | Full structured log stream from the `mvn test` run, including the final `SUMMARY:` line. |
| App-run log | `transaction-service/target/logs/transaction-service.log` | Same, but from `mvn spring-boot:run`. |

To open the HTML report:

```bash
# Windows
start transaction-service\target\cucumber-reports\ExtentReport.html

# macOS
open transaction-service/target/cucumber-reports/ExtentReport.html

# Linux
xdg-open transaction-service/target/cucumber-reports/ExtentReport.html
```

or just double-click it in File Explorer / Finder. Check: the summary tile
at the top should read **36 total, 36 passed, 0 failed**; expand any
scenario to see its Given/When/Then steps and the attached Step Snapshot.

## 6. Sample input & output — and how to add your own test case

All test data lives as **plain JSON files you can open and edit directly** —
nothing is generated, hidden, or hardcoded only in Java. Two kinds:

- **`src/main/resources/sample-transactions.json`** — the one, fixed, official
  §9.8 input batch the real service and US-01 load via `TransactionSource`.
- **`src/test/resources/testdata/<US-id>/*.json`** — one file (or a few) per
  Acceptance Criterion, each loaded by its scenario's step definitions.

### Structure of a testdata JSON file

A single request:
```json
{
  "request": {
    "transactionId": "7b2e9f4c1a8d6053",
    "requestId": "4193857",
    "accountId": "6714829350",
    "transactionType": "CREDIT",
    "amount": 100.75,
    "currency": "USD",
    "sequenceNumber": 1
  },
  "expectedValidation": { "valid": false, "reasonPresent": true }
}
```

A batch of requests (for scenarios that submit several at once):
```json
{
  "requests": [ { "...one request object, same shape as above..." }, { "..." } ],
  "expectedCounts": { "PROCESSED": 2, "FAILED": 1 }
}
```

| Field | Meaning | Convention used in `testdata/` |
|---|---|---|
| `transactionId` | the business transaction's own id | 16-character hex hash |
| `requestId` | this physical attempt's id (idempotency key) | 7-digit number |
| `accountId` | which ledger account this affects | 10-digit number |
| `transactionType` | `CREDIT`, `DEBIT`, or `REVERSAL` | — |
| `amount` | the transaction amount | a float, e.g. `40.25` |
| `currency` | ISO-style 3-letter code | `"USD"` |
| `sequenceNumber` | this account's/batch's ordering slot | a small sequential integer — **not** a timestamp; `SequenceBuffer` requires an exact `lastResolved + 1` match, so this field must stay sequential or out-of-order handling breaks |
| `simulatedFailures` | *(optional)* how many leading attempts should transiently fail before succeeding | integer, default `0` |
| `expectedStatus` / `expectedFailureReasonPresent` / `expectedValidation` / `expectedCounts` | what the scenario's `Then` step checks against | varies per fixture — see the step definition it's paired with |

`sample-transactions.json` intentionally still uses the original
human-readable ids (`TXN-2001`, `REQ-1`, `ACC-201`...) rather than the
hash/numeric convention above — it's the one, official input the problem
statement itself defines, so it was left as-is rather than reformatted.
`RequestSchemaValidator` (production-side JSON validation, see
`src/main/java/.../schema/`) is deliberately convention-agnostic for this
reason: it checks for null/blank/negative/zero/degenerate-duplicate values,
not a specific id shape, so both conventions pass it.

### Try it yourself: add a new test case

1. Pick a feature file under `src/test/resources/features/` close to what
   you want to test (e.g. `US-02_input_validation.feature`).
2. Add a new JSON fixture under the matching `src/test/resources/testdata/<US-id>/`
   folder, following the table above.
3. Add a `Scenario:` (or a new row in an existing `Scenario Outline:`'s
   `Examples:` table) that points `loaded from "testdata/<US-id>/your_file.json"`.
4. If the step text you need doesn't exist yet, add one `@Given`/`@When`/`@Then`
   method to the matching class under `src/test/java/.../steps/` — most
   scenarios reuse steps that already exist.
5. `mvn test` and open the HTML report — your new scenario shows up by name,
   pass or fail, with its own Step Snapshot.

This is genuinely the fastest way to sanity-check a question like "what
happens if two requests share an accountId but not a transactionId?" —
build the fixture, wire it to one existing step, run it, read the report.

## 7. Project layout

```
transaction-service/
  src/main/java/.../model/      TransactionRequest, TransactionStatus, TransactionType
  src/main/java/.../input/      TransactionSource (interface) + InMemoryTransactionSource
  src/main/java/.../schema/     RequestSchemaValidator - JSON-shape validation before deserialization
  src/main/java/.../store/      IdempotencyStore (interface), InMemoryIdempotencyStore, AccountLedger
  src/main/java/.../ordering/   SequenceBuffer (Option A: sequence-based out-of-order handling)
  src/main/java/.../retry/      RetryPolicy, TransientProcessingException
  src/main/java/.../service/    Validator, TransactionProcessor (orchestrator), ProcessingSummary
  src/main/java/.../exception/  TransactionServiceException - shared unchecked exception hierarchy
  src/test/resources/features/  11 Cucumber .feature files, one per User Story
  src/test/resources/testdata/  JSON fixtures, one folder per User Story
  src/test/java/.../steps/      Step definitions, one file per User Story
Installations/                  Bundled JDK 17 + Maven (see section 2)
docs/                            Design review app (Problem Statement + Design tabs) - also the GitHub Pages site
DESIGN.md                        Design note (source of truth, also rendered live in docs/)
DELIVERABLES.md                  What's delivered, and what each git branch means
RELEASE_NOTES.md                 Defects found & fixed, final test results
DEMO_SCRIPT.md                   Narration + command sequence for a recorded walkthrough
```

## 8. Git branches

- **`main`** — the final, current source. Everything in sections 1–7 above
  runs against `main`.
- **`design-baseline`** — a frozen snapshot of `main` taken before the final
  gap-review implementation pass, for before/after comparison. Not meant to
  be built or run; see [`DELIVERABLES.md`](DELIVERABLES.md) for exactly what
  it captures and why.

## License

[GNU GPLv3](LICENSE)
