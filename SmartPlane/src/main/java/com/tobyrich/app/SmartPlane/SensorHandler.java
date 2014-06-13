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
    private BluetoothDelegate bluetoothDelegate;

    private SensorManager sensorManager;

    private Sensor mRotationSensor;

    TextView hdgVal;
    ImageView compass;
    ImageView horizonImage;
    ImageView centralRudder;
    Switch rudderSwitch;

    private float[] rotationMatrix = new float[9];

    private float azimuthAngle;
    private float rollAngle;
    private float pitchAngle;

    public SensorHandler(Activity activity, BluetoothDelegate bluetoothDelegate) {
        this.bluetoothDelegate = bluetoothDelegate;

        /* The data set changes rapidly, so we need to set the views here,
         * and keep the references alive for the lifetime of the app
         */
        hdgVal = (TextView) activity.findViewById(R.id.hdgValue);
        compass = (ImageView) activity.findViewById(R.id.compass);
        horizonImage = (ImageView) activity.findViewById(R.id.imageHorizon);
        centralRudder = (ImageView) activity.findViewById(R.id.rulerMiddle);
        rudderSwitch = (Switch) activity.findViewById(R.id.rudderSwitch);

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
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) { //if the device has rotation vector sensor

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            // Transform rotation matrix into azimuth/pitch/roll
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

            // Convert radians to degrees
            azimuthAngle = (float) (orientation[0] * Const.TO_DEGREES);
            pitchAngle = (float) (orientation[1] * Const.TO_DEGREES);
            rollAngle = (float) (orientation[2] * Const.TO_DEGREES);

        }

        BLESmartplaneService smartplaneService = bluetoothDelegate.getSmartplaneService();
        if (smartplaneService != null) {
            short newRudder = (short) (rollAngle * -Const.MAX_RUDDER_INPUT / Const.MAX_ROLL_ANGLE);
            smartplaneService.setRudder(
                    (short) (rudderSwitch.isChecked() ? -newRudder : newRudder)
            );
        }

        horizonImage.setRotation(-rollAngle);

        /* Ignore small changes in the angles*/

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
