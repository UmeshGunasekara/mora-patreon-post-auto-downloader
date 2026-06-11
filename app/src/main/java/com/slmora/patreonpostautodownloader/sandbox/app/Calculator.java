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
 *  This Class created for project initial class Calculator<br>
 *  Code<br>
 *
 *  Methods
 *  <ul>
 *      <li>{@link #add(Integer, Integer)}</li>
 *      <li>{@link #getAreaOfCircle(Double)}</li>
 *      <li>{@link #division(Integer, Integer)}</li>
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
public class Calculator
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(Calculator.class);

    /**
     * Get sum of given two integers.
     *
     * @param input1    the input integer 1.
     * @param input2    the input integer 2.
     * @return          the int value of sum of given two Integer values.
     *
     * @since           1.0
     */
    public int add(Integer input1, Integer input2)
    {
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().getId(),
                Thread.currentThread().getStackTrace()),"Calculator - add() method is called");
        return input1+input2;
    }

    /**
     * Get area of the circle for given radius.
     * To get π value use {@link Math#PI} and to calculate power use {@link Math#pow(double, double)}
     *
     * @param radius    the radius of the circle.
     * @return          the double value of area of circle with given radius.
     *
     * @since           1.0
     */
    public double getAreaOfCircle(Double radius)
    {
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().getId(),
                Thread.currentThread().getStackTrace()),"Calculator - getAreaOfCircle() method is called");
        return Math.PI*Math.pow(radius,2);
//        return 0;
    }

    /**
     * Get division of given two integers.
     *
     * @param input1                the input integer 1 as dividend.
     * @param input2                the input integer 2 as divisor.
     * @return                      the integer portion of division of given two Integer values.
     * @throws ArithmeticException  if an error occurs with divisor value is 0.
     *
     * @since                       1.0
     */
    public int division(Integer input1, Integer input2)
    {
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().getId(),
                Thread.currentThread().getStackTrace()),"Calculator - division() method is called");
        return input1/input2;
    }
}
