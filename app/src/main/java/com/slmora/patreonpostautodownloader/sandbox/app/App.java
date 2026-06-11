/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/11/2026 12:00 AM
 */
package com.slmora.patreonpostautodownloader.sandbox.app;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;

/**
 *  This Class created for project initial class App<br>
 *  Code<br>
 *
 *  Methods
 *  <ul>
 *      <li>{@link #getGreeting()}</li>
 *      <li>....</li>
 *  </ul>
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

    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args)
    {
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().getId(),
                Thread.currentThread().getStackTrace()),"The App main() method is called");

        Calculator cal = new Calculator();

        try {
            int divide = cal.division(100,2);
            LOGGER.warn(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()),"Warning message - Can make exception with division by 0");
            System.out.println("Division of 100 and 2 is : "+divide);
        } catch (Exception e) {
//            LOGGER.error(ExceptionUtils.getStackTrace(e));
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()), e);
            e.printStackTrace();
        }

    }

}
