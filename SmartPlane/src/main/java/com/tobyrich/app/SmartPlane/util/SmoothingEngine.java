package com.tobyrich.app.SmartPlane.util;

/**
 * Smoothing engine based on a circular buffer.
 * It computes the average over the last half second in an efficient manner.
 *
 * @author Radu Hambasan <radu.hambasan@tobyrich.com>
 * @date 07 Jul 2014
 */
public class SmoothingEngine {
    private static final int MICROS_IN_SEC = 1000000;
    // Average over half a second
    private static final int BUFFER_SIZE = (MICROS_IN_SEC / Const.SENSOR_DELAY) / 2;

    private final float[][] buffer = new float[BUFFER_SIZE][3];
    private int currElement = -1;

    private float sum0 = 0;
    private float sum1 = 0;
    private float sum2 = 0;

    /**
     * @param input an array of 3 elements (smoothed individually)
     * @return an array of 3 smoothed values corresponding to the input values
     */
    public float[] smooth(float[] input) {
        if (input.length != 3) {
            throw new IllegalArgumentException("smooth only works on an array of 3 elements");
        }

        ++currElement;
        currElement %= BUFFER_SIZE;

        float old0 = buffer[currElement][0];
        float old1 = buffer[currElement][1];
        float old2 = buffer[currElement][2];

        sum0 = sum0 - old0 + input[0];
        sum1 = sum1 - old1 + input[1];
        sum2 = sum2 - old2 + input[2];

        buffer[currElement][0] = input[0];
        buffer[currElement][1] = input[1];
        buffer[currElement][2] = input[2];

        float[] result = new float[3];
        result[0] = sum0 / BUFFER_SIZE;
        result[1] = sum1 / BUFFER_SIZE;
        result[2] = sum2 / BUFFER_SIZE;

        return result;
    }

}
