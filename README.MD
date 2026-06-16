[//]: # " Copyright (c) 2026 Eryxon and/or its affiliates. All rights reserved. "

# mora-patreon-post-auto-downloader

[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A)](https://gradle.org/)
[![Log4j2](https://img.shields.io/badge/Log4j-2.24.3-blue)](https://logging.apache.org/log4j/2.x/)
[![License](https://img.shields.io/badge/License-Proprietary-lightgrey)](#meta-data)

Automated Patreon post pipeline that fetches API data, builds batch Excel files, downloads images (not videos), and generates DOCX reports with retry and failure tracking.

## Table of Contents

- [Executive Summary](#executive-summary)
- [Problem Statement](#problem-statement)
- [Objectives](#objectives)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [Detailed Component Design](#detailed-component-design)
- [Implementation Design Pattern](#implementation-design-pattern)
- [Project Structure](#project-structure)
- [Installation](#installation)
- [Configuration Setup](#configuration-setup)
- [Running the Project](#running-the-project)
- [Workflow](#workflow)
- [Error Handling and Recovery](#error-handling-and-recovery)
- [Performance Characteristics](#performance-characteristics)
- [Results](#results)
- [Research Contributions](#research-contributions)
- [Future Enhancements](#future-enhancements)
- [References](#references)
- [Meta Data](#meta-data)
- [Revision History](#revision-history)
- [Notes](#notes)
- [Action Log](#action-log)

## Executive Summary

This project is a Java 21, Gradle-based producer-worker pipeline for large-scale Patreon post archival. It is built for content archival workflows that require:
- structured post extraction,
- downloadable media capture,
- report-ready output generation,
- and traceable failure handling.

Primary users are engineering teams and operators who need repeatable post ingestion with operational logs and retry behavior.

## Problem Statement

Manual Patreon post archiving is time-consuming and error-prone when the source is paginated and media-heavy. Teams need a reproducible way to convert API post data into artifacts suitable for analysis and reporting (Excel + DOCX), while tolerating intermittent network/media failures.

## Objectives

- Automate paginated Patreon post ingestion from seed URL inputs.
- Preserve post metadata in stable, downstream-compatible Excel format.
- Download image assets concurrently with retry control.
- Generate final DOCX artifacts from extracted post content.
- Persist status/failure logs for post-run diagnostics.

## Key Features

- URL processing and pagination via `UrlExecutionService`.
- Automated Excel batch generation via `ProcessExcelProducer` + `ExcelService`.
- Multi-threaded image downloading via `ProcessImageDownloadWorker` + `ImageDownloadService`.
- DOCX generation via `ProcessDocxProducer` + `DocxService`.
- Retry mechanism with queue-based routing via `ProcessRetry` + `RetryService`.
- Job persistence (`job-status.log`, `failed-jobs.log`) via `JobPersistenceService`.
- End-to-end pipeline processing with queue/state coordination (`PipelineQueues`, `PipelineState`).

## System Architecture

No diagram file is currently versioned in this repository (`TBD`). Current architecture is code-driven and queue-based.

```text
Seed URL File
   -> ProcessExcelProducer
      -> excelReadyQueue
         -> ProcessImageDownloadWorker
            -> docxReadyQueue (success)
            -> retryQueue (partial/failed)
               -> ProcessRetry
                  -> docxReadyQueue (retry success)
                  -> failedQueue (max retry reached)
                     -> FailedJobMonitor

docxReadyQueue
   -> ProcessDocxProducer
      -> DOCX output + success status logs
```

## Technology Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Build Tool | Gradle 9.5.1 (wrapper) |
| Framework/Libraries | Apache POI, docx4j, Jackson 3, Playwright, Guava, Lombok |
| Data/Storage | File-based artifacts (Excel, DOCX, image files, status logs) |
| Logging | Log4j2 + `com.slmora.common.logging:mora-common-logging` |
| Testing | JUnit 5 |
| Deployment/Runtime | JVM application (`application` plugin main class) |

## Detailed Component Design

### Data Collection Layer
- **Responsibility:** Read seed URLs and manage pagination traversal.
- **Input:** URL text file path from `APP.URL_INPUT_PATH`.
- **Output:** `PostRecord` batches.
- **Key files:** `app/src/main/java/com/slmora/patreonpostautodownloader/process/ProcessExcelProducer.java`, `app/src/main/java/com/slmora/patreonpostautodownloader/service/UrlExecutionService.java`

### URL Execution Layer
- **Responsibility:** Execute Patreon API calls and map JSON payload to domain records.
- **Input:** API URL.
- **Output:** `URLExecute` containing posts and next URL.
- **Key files:** `app/src/main/java/com/slmora/patreonpostautodownloader/service/UrlExecutionService.java`

### Excel Processing Layer
- **Responsibility:** Append post data to Excel files and finalize batch files.
- **Input:** `List<PostRecord>`.
- **Output:** Finalized Excel files + queued `ExcelJob`.
- **Key files:** `app/src/main/java/com/slmora/patreonpostautodownloader/service/ExcelService.java`, `app/src/main/java/com/slmora/patreonpostautodownloader/process/ProcessExcelProducer.java`

### Image Download Layer
- **Responsibility:** Read image URLs from Excel and download media concurrently.
- **Input:** `ExcelJob` from `excelReadyQueue`.
- **Output:** Updated `ImageRecord` statuses and local image files.
- **Key files:** `app/src/main/java/com/slmora/patreonpostautodownloader/process/ProcessImageDownloadWorker.java`, `app/src/main/java/com/slmora/patreonpostautodownloader/service/ImageDownloadService.java`

### DOCX Generation Layer
- **Responsibility:** Convert Excel post content to DOCX reports.
- **Input:** `ExcelJob` from `docxReadyQueue`.
- **Output:** Generated DOCX files.
- **Key files:** `app/src/main/java/com/slmora/patreonpostautodownloader/process/ProcessDocxProducer.java`, `app/src/main/java/com/slmora/patreonpostautodownloader/service/DocxService.java`

### Retry and Recovery Layer
- **Responsibility:** Reattempt failed image downloads up to configured maximum.
- **Input:** `ExcelJob` from `retryQueue`.
- **Output:** Job routed back to DOCX queue or failed queue.
- **Key files:** `app/src/main/java/com/slmora/patreonpostautodownloader/process/ProcessRetry.java`, `app/src/main/java/com/slmora/patreonpostautodownloader/service/RetryService.java`

### Monitoring Layer
- **Responsibility:** Persist job status and failed job details.
- **Input:** Success/failed jobs from stage outputs.
- **Output:** `job-status.log`, `failed-jobs.log`.
- **Key files:** `app/src/main/java/com/slmora/patreonpostautodownloader/process/FailedJobMonitor.java`, `app/src/main/java/com/slmora/patreonpostautodownloader/service/JobPersistenceService.java`

## Implementation Design Pattern

This project uses a pragmatic, code-first combination of concurrency and orchestration patterns:

- **Producer-Consumer Pipeline Pattern:**
  `ProcessExcelProducer` produces `ExcelJob` items to `excelReadyQueue`, downstream workers consume and hand off through `docxReadyQueue`, `retryQueue`, and `failedQueue`.

- **Worker Pool Pattern:**
  `ProcessImageDownloadWorker` and `ProcessDocxProducer` each use fixed thread pools sized from `PipelineConfig` to process queue items concurrently.

- **Retry Pattern with Bounded Attempts:**
  `RetryService.sendToRetryOrFailed(...)` retries failed jobs until `APP.MAX_RETRY`, then routes terminal failures to `failedQueue`.

- **Shared State Coordination Pattern:**
  `PipelineState` exposes stage-completion flags used with queue polling to support graceful drain-and-stop behavior.

- **Manual Composition Root Pattern (No DI Framework):**
  `PatreonPostDownloadPipelineController` explicitly wires services, processes, queues, and state, keeping dependencies transparent.

- **Immutable Snapshot Configuration Pattern:**
  `PipelineConfig` loads properties into an immutable snapshot model and serves static getters for thread-safe reads.

## Project Structure

```text
mora-patreon-post-auto-downloader/
|-- AGENTS.md
|-- mora-readme-agent.md
|-- README.MD
|-- settings.gradle.kts
|-- gradle.properties
|-- app/
|   |-- build.gradle.kts
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/com/slmora/patreonpostautodownloader/
|   |   |   |   |-- app/
|   |   |   |   |-- config/
|   |   |   |   |-- controller/
|   |   |   |   |-- model/
|   |   |   |   |-- pipeline/
|   |   |   |   |-- process/
|   |   |   |   `-- service/
|   |   |   `-- resources/
|   |   |       |-- app.properties
|   |   |       `-- log4j2.xml
|   |   `-- test/
|   |       `-- java/com/slmora/patreonpostautodownloader/app/AppTest.java
|   `-- libs/
|-- gradle/
|   `-- wrapper/
`-- logs/
```

## Installation

Prerequisites:
- JDK 21+
- Gradle wrapper (`gradlew.bat` included)

From repository root:

```powershell
./gradlew.bat clean build
```

## Configuration Setup

Configuration source:
- `app/src/main/resources/app.properties`

Key runtime properties:
- `APP.URL_INPUT_PATH`
- `APP.EXCEL_OUTPUT_DIR_PATH`
- `APP.IMAGE_OUTPUT_DIR_PATH`
- `APP.DOCX_OUTPUT_DIR_PATH`
- `APP.FAILD_OUTPUT_DIR_PATH`
- `APP.EXCEL_QUEUE_CAPACITY`
- `APP.DOCX_QUEUE_CAPACITY`
- `APP.RETRY_QUEUE_CAPACITY`
- `APP.FAILED_QUEUE_CAPACITY`
- `APP.PROCESS_EXCEL_THREADS`
- `APP.PROCESS_IMAGE_DOWNLOAD_THREADS`
- `APP.PROCESS_DOCX_THREADS`
- `APP.MAX_RETRY`
- `APP.PATREON_ACCESS_COOKIE=REMOVED_SECRET`

URL input file format:
- Use .txt file with just API url fetching from the Patreon browser API call. Can be listed multiple URLs for multiple runs.

Access cookie:
- Use a valid Patreon session cookie value for `APP.PATREON_ACCESS_COOKIE` to authenticate API calls. This is required for successful data retrieval.
- This one can fetch from the browser's developer tools (network tab) when making a Patreon API call while logged in. Use google chrome for better experience.

Additional logging config:
- `app/src/main/resources/log4j2.xml`
- Update `LOG_DIR` to your local path when needed.

Security note:
- Keep `APP.PATREON_ACCESS_COOKIE=REMOVED_SECRET` private and environment-specific.
- Do not commit real session cookies.

## Running the Project

Build:

```powershell
./gradlew.bat clean build
```

Test:

```powershell
./gradlew.bat test
```

Run:

```powershell
./gradlew.bat run
```

Publish local Maven artifact:

```powershell
./gradlew.bat :app:publishToMavenLocal
```

Entrypoint details:
- `app/build.gradle.kts` sets main class to `com.slmora.patreonpostautodownloader.app.App`.
- `App` starts `PatreonPostDownloadPipelineController`, which wires and runs the full pipeline.

## Workflow

1. Load `PipelineConfig` from `app.properties`.
2. Create output directories via `PatreonPostDownloadPipelineController`.
3. Start all pipeline stages via `ExcelPipeline`.
4. Produce finalized Excel jobs from paginated API records.
5. Download images and route jobs to DOCX or retry queues.
6. Retry failed image jobs until `APP.MAX_RETRY` is reached.
7. Generate DOCX reports for jobs with successful image processing.
8. Persist success/failure logs for operational traceability.

## Error Handling and Recovery

- API and I/O failures are captured per stage with Log4j logging.
- `RetryService` routes failed image jobs to `retryQueue` until `APP.MAX_RETRY`.
- Jobs exceeding retry limit are marked `FAILED` and moved to `failedQueue`.
- `FailedJobMonitor` persists detailed failure records to `failed-jobs.log`.
- Queue worker loops use poll + completion flags in `PipelineState` for graceful drain and shutdown.

## Performance Characteristics

- Producer stage: fixed thread pool sized by `APP.PROCESS_EXCEL_THREADS`.
- Image stage: fixed worker pool (`APP.PROCESS_IMAGE_DOWNLOAD_THREADS`) + virtual-thread executor in `ImageDownloadService`.
- Download concurrency throttled with `Semaphore` (`MAX_CONCURRENCY = 16`).
- DOCX stage: fixed thread pool sized by `APP.PROCESS_DOCX_THREADS`.
- Batching strategy: Excel file finalized every 30 pages in `ProcessExcelProducer`.
- Benchmarks and throughput numbers: `TBD`.

## Results

Verified outputs from implementation:
- Batch Excel files are created and finalized per date range/job id.
- Downloaded image files are persisted under configured image output path.
- DOCX files are generated from Excel content.
- Status and failure logs are persisted for post-run analysis.

Quantitative evaluation (throughput, latency, resource profile): `TBD`.

## Research Contributions

- Practical producer-worker-retry pipeline for content archival.
- Mixed concurrency model (platform worker pools + virtual-thread image execution).
- Contract-driven downstream artifact generation (Excel schema -> image fetch -> DOCX assembly).
- Fault-aware processing with queue-based retry and failed-job persistence.

## Future Enhancements

- Add architecture/sequence diagrams under a `docs/` directory.
- Externalize sensitive cookie handling to environment/secret stores.
- Add integration tests for full queue workflow.
- Add performance benchmark suite and publish baseline metrics.
- Add resumable checkpoints for in-progress Excel batches.

## References

- [Gradle User Manual](https://docs.gradle.org/)
- [Java 21 Documentation](https://docs.oracle.com/en/java/javase/21/)
- [Apache POI](https://poi.apache.org/)
- [docx4j](https://www.docx4java.org/)
- [Jackson](https://github.com/FasterXML/jackson)
- [Log4j 2](https://logging.apache.org/log4j/2.x/)

## Meta Data

### Artifact Information
- **ArtifactId :** _mora-patreon-post-auto-downloader_
- **GroupId :** _com.slmora.patreonpostautodownloader_
- **Version :** _1.0-SNAPSHOT_
- **Packaging :** _jar_

### Project Information
- **Name :** _MoraPatreonPostAutoDownloader_
- **Description :** _mora-patreon-post-auto-downloader_
- **URL :** _[slmora](http://www.slmora.com/mora-patreon-post-auto-downloader "mora-patreon-post-auto-downloader")_

### Organization Information
- **Name :** _Eryxon_
- **URL :** _[Eryxon](http://www.eryxon.com "www.eryxon.com")_

### Developers Information
1. **developer-1** :snowflake:
    - ![Profile Pic](https://avatars.githubusercontent.com/u/12097282?v=4)
    - **ID :** _SLMORA_
    - **Name :** _Umesh Gunasekara_
    - **Email :** _<umesh.gunasekara.n@gamil.com>_
    - **URL :** _[Umesh Gunasekara](http://www.umeshgunasekara.com "www.umeshgunasekara.com")_
    - **Organization :** _Eryxon_
    - **Organization URL :** _[Eryxon](http://www.eryxon.com "www.eryxon.com")_
    - **Roles :**
        + _Owner_
        + _Architect_
        + _Developer_
    - **Timezone :** _Asia/Colombo_

## Revision History

| _Date_     | _Revision_ | _Description_                                   | _Author_ |
|:-----------|:-----------|:------------------------------------------------|:---------|
| 06/11/2026 | 1.0        | Initiate gradle project                         | SLMORA   |
| 06/15/2026 | 1.1        | Professional README restructure and full update | SLMORA   |
| 06/16/2026 | 1.2        | Finalize the operations                         | SLMORA   |

- 06/11/2026 12:00 AM
    + Generate gradle project
    + [build.gradle.kts](build.gradle.kts) generated by using mora-patreon-post-auto-downloader-002-20260609001 as a reference and modified
    + [log4j2.xml](app/src/main/resources/log4j2.xml) generated by using mora-patreon-post-auto-downloader-002-20260609001 as a reference and modified
- 06/15/2026
    + Restructured `README.MD` with architecture, workflow, stack, operations, and metadata sections

## Notes

### 1. Commit
- commit comment pattern
    + `|MORALENOVO | mora-patreon-post-auto-downloader | slmora-dev-v01 | commit | UTC 2026-06-04-1200-001 | commit comment|`

## Action Log

- 2026-06-11
    + Initiate project
- 2026-06-15
    + Professional README restructure
