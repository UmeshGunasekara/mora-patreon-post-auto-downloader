# AGENTS.md

## Purpose
- This repo is a Java 21 Gradle app that pulls Patreon post JSON, writes batch Excel files, downloads images, and produces DOCX reports.
- The runtime entrypoint is `app/src/main/java/com/slmora/patreonpostautodownloader/app/App.java`, which delegates to `PatreonPostDownloadPipelineController`.
- The Gradle `application.mainClass` points to `com.slmora.patreonpostautodownloader.app.App`, and `./gradlew.bat run` starts the full pipeline.

## Architecture (producer-worker pipeline)
- Process A (`ProcessExcelProducer`) reads seed URL(s) from a local file, paginates Patreon API links, and appends rows into a temp Excel.
- Every 30 pages it renames the temp Excel and enqueues an `ExcelJob` to `excelReadyQueue`.
- Process B (`ProcessImageDownloadWorker`) reads jobs, parses image URLs from Excel (`ExcelService.readImageRecords`), downloads images (`ImageDownloadService`), then pushes to `docxReadyQueue` or retry/failed.
- Retry path (`RetryProcess` + `RetryService`) retries failed image downloads up to `maxRetry`.
- Process C (`ProcessDocxProducer`) builds DOCX from Excel+images (`DocxService`) and persists success/failure logs.
- `FailedJobMonitor` drains `failedQueue` and writes detailed failure logs.
- Queue/state coordination lives in `pipeline/PipelineQueues.java` and `pipeline/PipelineState.java` (poll + finished flags, no framework/DI).

## Critical configuration and local environment assumptions
- `app/src/main/resources/app.properties` expects environment-backed placeholders (for example `${URL_INPUT_PATH}`, `${EXCEL_OUTPUT_DIR_PATH}`, `${PATREON_ACCESS_COOKIE}`); ensure those variables are defined.
- `PipelineConfig` loads `app.properties` plus a hard-coded `.env` path (`D:\SLMORAWorkSpace\IntelliJProjects\slmora-project\mora-patreon-post-auto-downloader\.env`), so local path assumptions still exist.
- `UrlExecutionService` sends Patreon `Cookie` from `PipelineConfig.getPatreonAccessCookie()` (`APP.PATREON_ACCESS_COOKIE`); treat this as sensitive and environment-specific.
- Logging writes to an absolute path in `app/src/main/resources/log4j2.xml` (`D:/SLMORAWorkSpace/.../logs`).

## Build, test, run
- Verified in this workspace: `./gradlew.bat test` succeeds.
- Preferred commands from repo root:
  - `./gradlew.bat clean build`
  - `./gradlew.bat test`
  - `./gradlew.bat run` (runs `com.slmora.patreonpostautodownloader.app.App` and starts the pipeline)
  - `./gradlew.bat :app:publishToMavenLocal` (publishes configured `mavenJava` publication)
- Configuration cache is enabled in `gradle.properties`; avoid non-cacheable task patterns when editing build logic.

## Project-specific coding patterns to preserve
- Plain Java orchestration with manual wiring in `PatreonPostDownloadPipelineController`; do not introduce Spring/DI unless explicitly requested.
- Concurrency style is mixed by design: platform thread pools for process stages, virtual-thread pool inside `ImageDownloadService` for image fetches.
- Data model uses Lombok `@Data` for DTO-like classes (`ExcelJob`, `ImageRecord`, `PostRecord`, `URLExecute`).
- `ExcelService` and `DocxService` depend on exact column names (`id`, `published_at`, `title`, `content_json_string`, `large_url`, `thumb_url`); keep column contract stable.
- File naming and downstream matching are coupled: Excel temp/final naming in `ProcessExcelProducer` must stay compatible with regex in `DocxService.getFinalDocxFileName`.

## Integration points
- Patreon JSON parsing: `UrlExecutionService.extractPosts` maps `data[]`, `included[]`, and `links.next`.
- Excel I/O: Apache POI (`poi-ooxml`).
- DOCX generation: docx4j (`WordprocessingMLPackage`) with JSON-to-text extraction from `content_json_string`.
- Logging API: `com.slmora.common.logging:mora-common-logging` + Log4j2 config in resources.

## Existing AI guidance sources scanned
- Glob scan for standard AI instruction files returned: `AGENTS.md`.

