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
//        System.out.println("patreon_device_id=74a434f5-4d1e-4e02-a2b1-0404322bb60b; patreon_location_country_code=SG; patreon_locale_code=en-US; g_state={\"i_l\":0,\"i_ll\":1771685897501,\"i_b\":\"hg63iS3mDohJr3PzHn53QT0y7cHpSeYsFzB5enyww5w\",\"i_e\":{\"enable_itp_optimization\":0}}; session_id=vDto4OHc6ng9-hOic4gWtf1OwYu5Z29A2wEhzWUwuUU; stream_user_token={%22id%22:%2299404896%22%2C%22type%22:%22stream-user-token%22%2C%22token%22:%22eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiOTk0MDQ4OTYiLCJleHAiOjE3ODE3MTUyMDksImlhdCI6MTc4MDUwNTYwOX0.5…blH4PU; cf_clearance=Bppox0GylzpNH0LnyafenBxb4HmcOqXu9cPLV9S5E7k-1781408415-1.2.1.1-Ri.SI4LsPpHVpkH9Xy6NPexEuhveVhXxRJtDLykQUjqKWgLAUgIAE.vWA4o2I9uAWuLIxdC58NA9SUk2Vb6cjwo2Zcd8933P3eVeIuM_V3vrO2hkLnUg.Wn8UNEyK19a46eANu_wzEWcwg8a_JFYvA3Sd6kjBwpRn_mJ2ykbk62SBxTSj3bq.pHnRz1T_uD8JFNtxLfxdFunw1uyj8jq_VykSMU_ss3I59qo758ljetcNbj8zSxLDKORFQNha8XIuWClRApSQ3WdLfvRtYAMKsC4RqtCIpgg81PWb3kO_dYYeAK4Z_CYTdHhIPdjdn3PKkz1Ta3sjXlaU_BiQde1qjYL7WVbLmkrv3mQ7FwMTcKlTaHHC8NxLoCK3SBX5BI2eMffalm4vxuiZZkQMSwU_NWcbYT_dTEeDidOklbwHHo");
        System.out.println(PipelineConfig.getToString());
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
