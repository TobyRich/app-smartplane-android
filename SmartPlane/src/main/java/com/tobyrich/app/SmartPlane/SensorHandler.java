/*

Copyright (c) 2014, TobyRich GmbH
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/
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
import android.widget.TextView;

import com.tobyrich.app.SmartPlane.util.Const;
import com.tobyrich.app.SmartPlane.util.SmoothingEngine;
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
    private AppState appState;
    private BluetoothDelegate bluetoothDelegate;

    private SensorManager sensorManager;

    private Sensor mRotationSensor;
    private Sensor mAccelerometerSensor;
    private Sensor mMagnetometerSensor;

    private TextView hdgVal;
    private TextView throttleText;
    private ImageView throttleNeedle;
    private ImageView compass;
    private ImageView horizonImage;
    private ImageView centralRudder;

    SmoothingEngine smoothingEngine = new SmoothingEngine();
    // data that needs to be available across multiple calls
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];

    public SensorHandler(Activity activity, BluetoothDelegate bluetoothDelegate) {
        this.bluetoothDelegate = bluetoothDelegate;
        appState = (AppState) activity.getApplicationContext();

        /* The data set changes rapidly, so we need to set the views here,
         * and keep the references alive for the lifetime of the app
         */
        hdgVal = (TextView) activity.findViewById(R.id.hdgValue);
        throttleText = (TextView) activity.findViewById(R.id.throttleValue);
        compass = (ImageView) activity.findViewById(R.id.compass);
        horizonImage = (ImageView) activity.findViewById(R.id.imageHorizon);
        centralRudder = (ImageView) activity.findViewById(R.id.rulerMiddle);
        throttleNeedle = (ImageView) activity.findViewById(R.id.imgThrottleNeedle);

        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            Log.e(TAG, "Couldn't get the sensor service.");
            return;
        }

        mRotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mAccelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    public void registerListener() {
        if (mRotationSensor != null) {
            sensorManager.registerListener(this, mRotationSensor, Const.SENSOR_DELAY);
        } else {
            Log.w(TAG, "No rotation sensor. Falling back on acc & compass.");

            if (mAccelerometerSensor != null) {
                sensorManager.registerListener(this, mAccelerometerSensor, Const.SENSOR_DELAY);
            } else {
                Log.w(TAG, "No accelerometer found (fatal).");
            }

            if (mMagnetometerSensor != null) {
                sensorManager.registerListener(this, mMagnetometerSensor, Const.SENSOR_DELAY);
            } else {
                Log.w(TAG, "No compass found.");
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] angles = getAnglesFromSensor(event);
        if (angles == null) {
            return;  // not enough data
        }
        float azimuthAngle = angles[0];
        float pitchAngle = angles[1];
        float rollAngle = angles[2];

        short newRudder = (short) (rollAngle * -Const.MAX_RUDDER_INPUT / Const.MAX_ROLL_ANGLE);
        if (appState.isFlAssistEnabled()) {
            // limit rudder for left turn
            if (newRudder < 0) {
                newRudder = (short) (newRudder * Const.SCALE_LEFT_RUDDER);
            }
            // cutoff if needed
            if (newRudder < Const.SCALE_LEFT_RUDDER * -Const.MAX_RUDDER_INPUT) {
                newRudder = (short) (Const.SCALE_LEFT_RUDDER * -Const.MAX_RUDDER_INPUT);
            }
        }
        @SuppressWarnings("SpellCheckingInspection")
        BLESmartplaneService smartplaneService = bluetoothDelegate.getSmartplaneService();
        if (smartplaneService != null) {
            smartplaneService.setRudder(
                    (short) (appState.rudderReversed ? -newRudder : newRudder)
            );
        }
        horizonImage.setRotation(-rollAngle);
        // Increase throttle when turning if flight assist is enabled
        if (appState.isFlAssistEnabled() && !appState.screenLocked) {
            double scaler = 1 - Math.cos(rollAngle * Math.PI/2 / Const.MAX_ROLL_ANGLE);
            if (scaler > 0.3) {
                scaler = 0.3;
            }
            appState.setScaler(scaler);

            float adjustedMotorSpeed = appState.getAdjustedMotorSpeed();
            Util.rotateImageView(throttleNeedle, adjustedMotorSpeed,
                    Const.THROTTLE_NEEDLE_MIN_ANGLE, Const.THROTTLE_NEEDLE_MAX_ANGLE);
            throttleText.setText((short) (adjustedMotorSpeed * 100) + "%");
            if (smartplaneService != null) {
                smartplaneService.setMotor((short) (adjustedMotorSpeed * Const.MAX_MOTOR_SPEED));
            }
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
        centralRudder.setY((float) (-Const.RULER_MOVEMENT_SPEED *
                (horizonVerticalMovement + Const.RULER_MOVEMENT_HEIGHT)));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /* We handle three main cases:
     * -high-end phones have ROTATION_VECTOR sensor, which fuses together accelerometer,
     * gyroscope and compass (no smoothing necessary).
     * -low-end phones have only accelerometer and compass, in which case we need to compute
     * the orientation based on acceleration (not so precise).
     * -some phones may not even have compass
     * For phones without ROTATION_VECTOR sensor, smoothing is necessary
     */
    private float[] getAnglesFromSensor(SensorEvent event) {
        if (event.values.length < 3) {
            return null;  // we need at least 3 values
        }
        // On some devices, event.values.length > 3, which would cause an Exception
        float[] safeValues = new float[3];
        if (event.values.length > 3) {
            System.arraycopy(event.values, 0, safeValues, 0, 3);
        } else {
            safeValues = event.values;
        }

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, safeValues);

            // Transform rotation matrix into azimuth/pitch/roll
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

            float[] angles = new float[3];
            // Convert radians to degrees
            angles[0] = (float) (orientation[0] * Const.TO_DEGREES);  // azimuth
            angles[1] = (float) (orientation[1] * Const.TO_DEGREES);  // pitch
            angles[2] = (float) (orientation[2] * Const.TO_DEGREES);  // roll

            return angles;
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER &&
                mMagnetometerSensor == null) {
            float[] smoothValues = smoothingEngine.smooth(safeValues);
            // acceleration on the three axes
            float x = smoothValues[0];
            float y = smoothValues[1];
            float z = smoothValues[2];

            float[] angles = new float[3];
            angles[0] = 0;  // azimuth
            /* get euler angles from acceleration */
            // the pitch angle increases too fast without compass, so we need to scale it down
            final float REDUCE_PITCH = 0.5f;
            angles[1] = (float) (-Math.atan2(y, Math.sqrt(x * x + z * z)) * Const.TO_DEGREES * REDUCE_PITCH);
            // noinspection SuspiciousNameCombination
            angles[2] = (float) (-Math.atan2(x, Math.sqrt(y * y + z * z)) * Const.TO_DEGREES);

            return angles;
        } else if (mMagnetometerSensor != null){
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    mGravity = safeValues;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mGeomagnetic = safeValues;
                    break;
                default:
                    break;
            }
            float[] rotationMatrix = new float[9];
            float[] inclinationMatrix = new float[9];
            // if the device is in free fall, getRotationMatrix returns false
            if (!SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix,
                    mGravity, mGeomagnetic)) {
                return null;
            }

            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

            // smooth the orientation
            float[] smoothOrientation = smoothingEngine.smooth(orientation);

            float[] angles = new float[3];
            angles[0] = (float) (smoothOrientation[0] * Const.TO_DEGREES);
            angles[1] = (float) (smoothOrientation[1] * Const.TO_DEGREES);
            angles[2] = (float) (smoothOrientation[2] * Const.TO_DEGREES);

            return angles;
        }

        return null;
    }
}
