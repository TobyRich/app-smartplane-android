package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.tobyrich.app.SmartPlane.util.Const;
import com.tobyrich.app.SmartPlane.util.Util;

import lib.smartlink.driver.BLESmartplaneService;

/**
 * @author Samit Vaidya
 * @date 04 March 2014
 * Refactored by: Radu Hambasan
 */

/**
 * Class in charge of the accelerometer and magnetometer callbacks
 */

public class SensorHandler implements SensorEventListener {

    private final String TAG = "SensorHandler";
    private PlaneState planeState;
    private BluetoothDelegate bluetoothDelegate;

    private SensorManager sensorManager;

    private Sensor mRotationSensor;

    private TextView hdgVal;
    private TextView throttleText;
    private ImageView throttleNeedle;
    private ImageView compass;
    private ImageView horizonImage;
    private ImageView centralRudder;
    private Switch rudderReverse;
    private Switch flAssist;

    private float[] rotationMatrix = new float[9];

    private float azimuthAngle;
    private float rollAngle;
    private float pitchAngle;

    public SensorHandler(Activity activity, BluetoothDelegate bluetoothDelegate) {
        this.bluetoothDelegate = bluetoothDelegate;
        planeState = (PlaneState) activity.getApplicationContext();

        /* The data set changes rapidly, so we need to set the views here,
         * and keep the references alive for the lifetime of the app
         */
        hdgVal = (TextView) activity.findViewById(R.id.hdgValue);
        throttleText = (TextView) activity.findViewById(R.id.throttleValue);
        compass = (ImageView) activity.findViewById(R.id.compass);
        horizonImage = (ImageView) activity.findViewById(R.id.imageHorizon);
        centralRudder = (ImageView) activity.findViewById(R.id.rulerMiddle);
        throttleNeedle = (ImageView) activity.findViewById(R.id.imgThrottleNeedle);
        rudderReverse = (Switch) activity.findViewById(R.id.rudderSwitch);
        flAssist = (Switch) activity.findViewById(R.id.flAssistSwitch);


        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            Log.e(TAG, "Couldn't get the sensor service.");
            return;
        }

        mRotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    public void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    public void registerListener() {
        if (mRotationSensor != null) {
            sensorManager.registerListener(this, mRotationSensor, Const.SENSOR_DELAY);
        } else {
            Log.e(TAG, "no Rotation Vector Sensor!");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            // Transform rotation matrix into azimuth/pitch/roll
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

            // Convert radians to degrees
            azimuthAngle = (float) (orientation[0] * Const.TO_DEGREES);
            pitchAngle = (float) (orientation[1] * Const.TO_DEGREES);
            rollAngle = (float) (orientation[2] * Const.TO_DEGREES);

        } else {
            return;  // TODO: get rotation matrix from accelerometer
        }

        BLESmartplaneService smartplaneService = bluetoothDelegate.getSmartplaneService();
        if (smartplaneService == null) {
            return;  // we can't do anything without the smartplaneService
        }

        short newRudder = (short) (rollAngle * -Const.MAX_RUDDER_INPUT / Const.MAX_ROLL_ANGLE);
        smartplaneService.setRudder(
                (short) (rudderReverse.isChecked() ? -newRudder : newRudder)
        );

        horizonImage.setRotation(-rollAngle);
        // Increase throttle when turning if flight assist is enabled
        if (flAssist.isChecked() && !planeState.isScreenLocked()) {
            double scaler = 1 - Math.cos(rollAngle * Math.PI/2 / Const.MAX_ROLL_ANGLE);
            if (scaler > 0.3) {
                scaler = 0.3;
            }
            planeState.setScaler(scaler);

            float adjustedMotorSpeed = planeState.getMotorSpeed();
            smartplaneService.setMotor((short) (adjustedMotorSpeed * Const.MAX_MOTOR_SPEED));
            Util.rotateImageView(throttleNeedle, adjustedMotorSpeed,
                    Const.THROTTLE_NEEDLE_MIN_ANGLE, Const.THROTTLE_NEEDLE_MAX_ANGLE);
            throttleText.setText((short) (adjustedMotorSpeed * 100) + "%");
        }

        float compassAngle;
        final String[] compassDir = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
           /* TODO: explain the following two members better */
        // degrees between segments in between north, south, east and west
        final int DEGREES_PER_SEGMENT = Const.FULL_DEGREES / 8;
        // degrees between N, NE, E, SE, S, SW, W , NW, N
        final float DEGREES_PER_DIRECTION = (DEGREES_PER_SEGMENT / 2);

        if (azimuthAngle < 0) {
            // scaling angle from 0 to 360
            azimuthAngle += Const.FULL_DEGREES;
        }
        compassAngle = (azimuthAngle + DEGREES_PER_DIRECTION) / DEGREES_PER_SEGMENT;
        hdgVal.setText(compassDir[(int) compassAngle]);
        compass.setRotation(azimuthAngle);

        double horizonVerticalMovement = 0.0;
        //limiting the values of pitch angle for the vertical movement of the horizon
        if (Const.PITCH_ANGLE_MIN < pitchAngle && pitchAngle < Const.PITCH_ANGLE_MAX) {
            horizonVerticalMovement = Const.SCALE_FOR_VERT_MOVEMENT_HORIZON * pitchAngle;
        } else if (pitchAngle <= Const.PITCH_ANGLE_MIN) {
            horizonVerticalMovement = Const.SCALE_FOR_VERT_MOVEMENT_HORIZON * Const.PITCH_ANGLE_MIN;
        } else if (pitchAngle >= Const.PITCH_ANGLE_MAX) {
            horizonVerticalMovement = Const.SCALE_FOR_VERT_MOVEMENT_HORIZON * Const.PITCH_ANGLE_MAX;
        }
        // translation animation, translating the image in the vertical direction
        TranslateAnimation translateHorizon = new TranslateAnimation(0, 0,
                -(float) horizonVerticalMovement, 0);
        translateHorizon.setDuration(Const.ANIMATION_DURATION_MILLISEC);

        horizonImage.startAnimation(translateHorizon);
        // ruler movement, a bit faster than horizon movement for 3D effect
        centralRudder.setY((float) (-Const.RULER_MOVEMENT_SPEED * (horizonVerticalMovement + Const.RULER_MOVEMENT_HEIGHT)));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
