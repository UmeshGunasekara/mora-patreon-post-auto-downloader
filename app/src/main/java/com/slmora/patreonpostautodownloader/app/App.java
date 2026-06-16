/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 7/10/2023 12:00 AM
 */
package com.slmora.patreonpostautodownloader.app;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.controller.PatreonPostDownloadPipelineController;

import java.io.IOException;

/**
 * The {@code App} class is created for starting the Patreon post auto downloader
 * application from the Gradle {@code application} plugin or a direct Java launch.
 * <p>
 * This class keeps startup work intentionally small. It logs the resolved
 * {@link PipelineConfig}, creates the {@link PatreonPostDownloadPipelineController},
 * and delegates the complete producer-worker pipeline execution to that controller.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Acts as the runtime entry point configured by the Gradle application target.</li>
 *     <li>Logs startup configuration and pipeline lifecycle messages through {@link MoraLogger}.</li>
 *     <li>Delegates Patreon URL pagination, Excel generation, image downloads, retries, and DOCX creation to the pipeline controller.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineConfig}<br>
 * 2 - {@link PatreonPostDownloadPipelineController}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link App#main(String[])}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The pipeline depends on environment-backed configuration values loaded by {@link PipelineConfig}.</li>
 *     <li>Any {@link IOException} raised while creating or running the controller is logged and not rethrown by this entry point.</li>
 * </ul>
 *
 * @author: SLMORA
 * @since 1.0
 *
 * <h4>Revision History</h4>
 * <blockquote><pre>
 * <br>Version      Date            Editor              Note
 * <br>-------------------------------------------------------
 * <br>1.0          7/10/2023      SLMORA                Initial Code
 * </pre></blockquote>
 */
public class App
{
    /**
     * Class-scoped logger used for application startup, shutdown, and fatal
     * pipeline initialization errors.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(App.class);

    /**
     * <h3>Start Patreon post download pipeline</h3>
     * Starts the application pipeline by logging configuration, constructing the
     * pipeline controller, and invoking the controller lifecycle.
     * <p>
     * The method is the only Java launcher entry point for this application. It
     * does not parse command-line options; runtime behavior is driven by
     * {@link PipelineConfig} and the services wired by
     * {@link PatreonPostDownloadPipelineController}.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Writes the resolved pipeline configuration to the application log before controller creation.</li>
     *     <li>Creates a new {@link PatreonPostDownloadPipelineController} for the current process execution.</li>
     *     <li>Logs the pipeline start and end lifecycle messages around {@link PatreonPostDownloadPipelineController#execute()}.</li>
     *     <li>Logs any {@link IOException} produced by controller initialization or execution.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * App.main(new String[0]);
     * }</pre>
     *
     * @param args command-line arguments passed by the Java launcher; currently unused by this implementation
     *
     * @implNote The entry point keeps orchestration outside this class so the
     * application startup boundary remains separate from the pipeline process
     * coordination logic.
     * @apiNote Ensure the required environment variables referenced by
     * {@link PipelineConfig} are available before launching the application.
     * @since 1.0
     *
     * @see PatreonPostDownloadPipelineController#execute()
     * @see PipelineConfig
     */
    public static void main(String[] args)
    {
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().threadId(),
                Thread.currentThread().getStackTrace()),"PipelineConfig {}", PipelineConfig.getToString());
        try {
            PatreonPostDownloadPipelineController pipeline = new PatreonPostDownloadPipelineController();
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().threadId(),
                    Thread.currentThread().getStackTrace()),"Starting the Patreon Post Download Pipeline");
            // Delegate orchestration to the controller so startup remains focused on lifecycle concerns.
            pipeline.execute();
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().threadId(),
                    Thread.currentThread().getStackTrace()),"Ending the Patreon Post Download Pipeline");
        } catch (IOException e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().threadId(),
                    Thread.currentThread().getStackTrace()), e);
        }
    }
}
