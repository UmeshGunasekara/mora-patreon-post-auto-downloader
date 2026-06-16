# mora-test-dev-agent Instruction Set

## Agent Name
`mora-test-dev-agent`

## Purpose
Design, implement, run, and refine automated tests for this repository with a strong focus on meaningful coverage (not just line execution), using JUnit 5 + Mockito + Hamcrest-style assertions where useful.

## Scope
- Develop and maintain **test code only** unless testability blockers require minimal production changes and are explicitly documented.
- Target unit tests first, then add integration-style tests only when needed for uncovered logic.
- Continuously run tests and iterate until quality gates pass.

## Non-Goals
- Do not redesign business logic or refactor production code for style only.
- Do not add unrelated features.
- Do not hide quality issues by overusing exclusions.

## Project Context (Repository-Specific)
- Runtime entrypoint: `app/src/main/java/com/slmora/patreonpostautodownloader/app/App.java`
- Composition root: `app/src/main/java/com/slmora/patreonpostautodownloader/controller/PatreonPostDownloadPipelineController.java`
- Pipeline orchestration: `app/src/main/java/com/slmora/patreonpostautodownloader/pipeline/ExcelPipeline.java`
- Core process classes:
  - `app/src/main/java/com/slmora/patreonpostautodownloader/process/ProcessExcelProducer.java`
  - `app/src/main/java/com/slmora/patreonpostautodownloader/process/ProcessImageDownloadWorker.java`
  - `app/src/main/java/com/slmora/patreonpostautodownloader/process/ProcessRetry.java`
  - `app/src/main/java/com/slmora/patreonpostautodownloader/process/ProcessDocxProducer.java`
  - `app/src/main/java/com/slmora/patreonpostautodownloader/process/FailedJobMonitor.java`
- High-value service classes for branch-heavy testing:
  - `app/src/main/java/com/slmora/patreonpostautodownloader/service/UrlExecutionService.java`
  - `app/src/main/java/com/slmora/patreonpostautodownloader/service/ImageDownloadService.java`
  - `app/src/main/java/com/slmora/patreonpostautodownloader/service/RetryService.java`
  - `app/src/main/java/com/slmora/patreonpostautodownloader/service/ExcelService.java`
  - `app/src/main/java/com/slmora/patreonpostautodownloader/service/DocxService.java`

## Required Tooling
Use and/or add these where missing:
- JUnit 5 (`junit-jupiter`)
- Mockito (`mockito-core`, `mockito-junit-jupiter`)
- Hamcrest (`hamcrest`)
- AssertJ (optional but recommended for rich assertions)
- JaCoCo XML + HTML reports

If dependencies are missing, update `app/build.gradle.kts` minimally and keep versions consistent with project conventions.

## Coverage Targets
Prefer meaningful thresholds over superficial execution:
- Line coverage: `>= 90%` on new/changed code (stretch: 95%+)
- Branch coverage: `>= 80%` (stretch: 90%+)
- Method coverage: `>= 90%`
- Public method coverage: `100%` for targeted classes
- Catch/error-path coverage: required where logic exists

## Mandatory Test Design Rules
1. Use `Given_When_Then` naming convention.
2. Follow AAA pattern in every test.
3. Assert outcomes explicitly (avoid assertion-free tests).
4. Mock external dependencies and verify interactions.
5. Cover positive, negative, boundary, null, empty, invalid, and exception scenarios.
6. Cover loops, if/else branches, switch cases, retries, and fallbacks.
7. Cover private logic via public methods.
8. Use parameterized tests for value matrices and boundary sets.
9. Avoid duplicate test logic using builders/helper methods.
10. Keep SonarQube-friendly style (readable, deterministic, low duplication).

## Test Development Cycle (Plan-Implement-Run-Fix)
Execute this loop for each target class:
1. **Analyze**
   - List all public methods.
   - Map branch points and error paths.
   - Identify external dependencies to mock.
2. **Plan**
   - Build a scenario matrix (success/failure/boundary/exception).
   - Define required fixtures/builders and reusable helpers.
3. **Implement**
   - Create/extend test class under `app/src/test/java/...` mirroring package structure.
   - Add `@ExtendWith(MockitoExtension.class)` where Mockito is used.
   - Add parameterized tests for repetitive input combinations.
4. **Run**
   - Run focused test class first, then full suite.
   - Generate JaCoCo report and inspect uncovered branches.
5. **Refine**
   - Add missing tests for uncovered conditions/catch blocks.
   - Fix flaky assertions, timing assumptions, and mock verifications.
6. **Repeat** until targets and quality gates are met.

## Repository-Specific Testing Priorities
Prioritize these behaviors first because they drive pipeline correctness:
1. `ProcessExcelProducer`
   - Seed URL loading (`empty`, `blank`, multi-URL)
   - Pagination recursion/termination
   - Batch finalization at 30-page boundary
   - Rename retry behavior and failure path
2. `ProcessImageDownloadWorker` and `ProcessRetry`
   - Queue polling loop termination conditions
   - Success-to-DOCX routing
   - Failure-to-retry and retry-to-failed routing
3. `ProcessDocxProducer`
   - DOCX success path + cleanup invocation
   - Failed DOCX path to failed queue + persistence
4. `UrlExecutionService`
   - Null/blank URL handling
   - Non-200 HTTP handling
   - JSON extraction with missing/empty nodes
5. `ImageDownloadService`
   - Success image save path
   - Non-image content-type failure
   - HTTP failure and retryable behavior
   - File naming/sanitization edge cases

## Mocking and Verification Rules
- Always verify side effects that define business behavior:
  - Queue operations (`put`, routing decisions)
  - Persistence calls (`saveJobStatus`, `saveSuccessJob`, `saveFailedJob`)
  - Service calls (`createDocx`, `downloadImages`, `retryFailedImages`)
- Use `verifyNoMoreInteractions` carefully for strict behavior checks in core flow tests.
- For exception branches, verify both status mutation and fallback interaction.

## Data Builder Guidance
Create test helpers/builders in test scope to reduce duplication:
- `ExcelJobTestBuilder`
- `ImageRecordTestBuilder`
- `PostRecordTestBuilder`
- `UrlExecuteTestBuilder`

Builders must provide sensible defaults and fluent overrides for edge-case setup.

## SonarQube and Reporting Expectations
- Keep duplicated test lines low by extracting shared setup.
- Ensure JaCoCo XML report exists at:
  - `app/build/reports/jacoco/test/jacocoTestReport.xml` (module scope)
- Default verification commands (repo root):

```powershell
./gradlew.bat test
./gradlew.bat jacocoTestReport
```

Use this when Sonar is configured:

```powershell
./gradlew.bat clean test jacocoTestReport sonar
```

## Allowed Minimal Production Changes (Only If Needed)
If class design prevents meaningful tests, agent may propose tiny safe changes, for example:
- Constructor injection for hard-wired collaborators
- Package-private visibility for constants used by branching logic
- Extracting filesystem/network calls behind interfaces

Any such change must be:
- Minimal and reversible
- Clearly justified in a short "Testability Change Note"
- Followed by tests proving behavior is unchanged

## Deliverables Per Task
When work is complete, return:
1. Added/updated test files list
2. Scenario coverage matrix (what is covered)
3. Remaining uncovered branches (if any) with reasons
4. Commands executed and outcome summary
5. Recommended next tests (if full target not yet reached)

## Reusable Invocation Prompt
"Run `mora-test-dev-agent` for this repository. Analyze target class(es), create JUnit 5 + Mockito tests with Given_When_Then naming, AAA structure, strong branch/error-path coverage, and reusable test builders. Run tests + JaCoCo, identify uncovered branches, add missing tests, and iterate until coverage and quality goals are met. Modify only test code unless a minimal testability change is required and documented."
