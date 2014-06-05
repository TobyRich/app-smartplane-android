package com.tobyrich.app.SmartPlane;

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
    public static final int MAX_RUDDER_SPEED = 127;
    public static final int MAX_MOTOR_SPEED = 254;
    public static final double PITCH_ANGLE_MAX = 50;
    public static final double PITCH_ANGLE_MIN = -50;
    public static final long ANIMATION_DURATION_MILLISEC = 1000;
    /* movement of slider only in 80% of the control panel height */
    public static final double SCALE_FOR_CONTROL_PANEL = 0.2;
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
}

