package com.tobyrich.app.SmartPlane.util;

/**
 * @author Radu Hambasan
 * @date 05 Jun 2014
 *
 * Wrapper class for constants that are app-relevant..
 */

/* TODO: document each constant better */

public class Const {
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int RSSI_THRESHOLD_TO_CONNECT = -100;  // dB

    public static final int MAX_ROLL_ANGLE = 45;
    /* input to the rudder is between -128 and 127,
     * 0 would center it, 127 turns it fully right,
     * -128 fully left and intermediary value turn it proportionally
     */
    public static final int MAX_RUDDER_INPUT = 127;
    public static final int MAX_MOTOR_SPEED = 254;
    public static final double PITCH_ANGLE_MAX = 50;
    public static final double PITCH_ANGLE_MIN = -50;
    public static final long ANIMATION_DURATION_MILLISEC = 1000;
    /* don't let the cursor go below 80% of panel height */
    public static final float SCALE_FOR_CURSOR_RANGE = 0.8f;
    public static final double SCALE_FOR_VERT_MOVEMENT_HORIZON = 4.5;
    public static final String UNKNOWN = "Unknown";
    public static final float THROTTLE_NEEDLE_MAX_ANGLE = 48; // in degrees
    public static final float THROTTLE_NEEDLE_MIN_ANGLE = -140; // in degrees
    public static final float SIGNAL_NEEDLE_MIN_ANGLE = 0; // in degrees
    public static final float SIGNAL_NEEDLE_MAX_ANGLE = 180; // in degrees
    public static final float MAX_BLUETOOTH_STRENGTH = -20;
    public static final float MIN_BLUETOOTH_STRENGTH = -100;
    public static final float MIN_POS_SLIDER = 440;
    public static final float FUEL_NEEDLE_MIN_ANGLE = -90; // in degrees
    public static final float FUEL_NEEDLE_MAX_ANGLE = 90; // in degrees
    public static final float MAX_BATTERY_VALUE = 100; // in degrees
    /* the delay in milliseconds before task is to be executed */
    public static final long TIMER_DELAY = 500;
    /* the time in milliseconds between successive task executions */
    public static final long TIMER_PERIOD = 6000;
    public static final double RULER_MOVEMENT_SPEED = 1.4;
    public static final int RULER_MOVEMENT_HEIGHT = 200;
    /* hide the revRudder view after 3000 milliseconds */
    public static final long HIDE_REVRUDDER_DELAY = 3000;
    /* messages displayed when the charging status changes */
    public static final String IS_CHARGING = "CHARGING";
    public static final String IS_NOT_CHARGING = "IN USE";
    /* 360 degrees to be added to a negative angle, etc. TODO: find better name */
    public static final int FULL_DEGREES = 360;
}

