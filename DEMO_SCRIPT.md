# Demo Script (~4 minutes)

For recording with Windows' built-in recorder (Win+Alt+R) or any screen
recorder. Read the narration lines as you perform each action; pause briefly
between sections.

---

### 1. Open with the problem (20s)

**Say:** "This is an FDE L2 assessment: build an idempotent background
transaction processing service that handles duplicates, out-of-order
arrival, and retries — against a simplified banking ledger."

**Do:** Open `DESIGN.md` in the repo (or the live docs site) and scroll past
the title to the User Stories section.

### 2. Show the design (45s)

**Say:** "Before any code, I wrote 11 User Stories with Acceptance Criteria,
a high-level architecture diagram, a class diagram, four flow charts, a
sequence diagram, and a traceability matrix back to the original
requirements."

**Do:** If using the live docs site: click through the Design tab — HLD,
LLD, one flow chart, the traceability matrix. Click one requirement badge
to show it jumping back to the source Problem Statement text.

### 3. Run the engine live (45s)

**Say:** "This is a console batch job, not an API — that was a deliberate
design decision, DD-01. Here it is processing the real §9.8 test data."

**Do:**
```bash
cd transaction-service
mvn spring-boot:run
```
Let the console output scroll, then point at the final summary and the
three account balances. **Say:** "Account 201 ends at 85 — 100 credited, 40
debited, and the duplicate debit never touched the ledger. Account 203 ends
at exactly 500, not 1000 — proof the retry didn't double-apply."

### 4. Run the automated tests (45s)

**Say:** "Every one of those behaviors is backed by a Cucumber scenario —
one feature file per User Story, scenarios mapped directly to Acceptance
Criteria."

**Do:**
```bash
mvn test
```
Point at `27 Scenarios (27 passed)` in the output. Optionally open
`target/cucumber-reports/ExtentReport.html` to show the HTML report.

### 5. Show the git history (30s)

**Say:** "The build itself was iterative — design, then a basic setup, then
one User Story automated at a time, with two real defects found and fixed
along the way by actually running the code against the real data, not just
trusting the diagram."

**Do:**
```bash
git log --oneline --graph --all
```

### 6. Close (15s)

**Say:** "Source, tests, design note, release notes with the defects found
and fixed, and this walkthrough are all in the public repo linked in my
submission."

---

**Total: ~3.5–4 minutes.** Trim section 2 if running long; sections 3 and 4
(the actual proof it works) matter most.
