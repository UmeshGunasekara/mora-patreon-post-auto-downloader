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
import com.slmora.patreonpostautodownloader.controller.PatreonPostDownloadPipelineController;

import java.io.IOException;

/**
 * Application entry point for the Patreon post auto downloader.
 * <p>
 * This class performs the minimal startup work required by the Gradle
 * application target: it creates the pipeline controller, logs the pipeline
 * lifecycle, and delegates execution to the downloader pipeline.
 * </p>
 *
 * <p>Methods:</p>
 * <ul>
 *     <li>{@link #main(String[])} - starts the Patreon post download pipeline.</li>
 * </ul>
 *
 * @since   1.0
 *
 * <blockquote><pre>
 * <br>Version      Date            Editor              Note
 * <br>-------------------------------------------------------
 * <br>1.0          7/10/2023      SLMORA                Initial Code
 * </pre></blockquote>
 */
public class App
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(App.class);

    /**
     * Starts the Patreon post download pipeline.
     *
     * @param args command-line arguments passed by the Java launcher; currently unused
     */
    public static void main(String[] args)
    {
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
