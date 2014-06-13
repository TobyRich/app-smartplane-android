package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
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

public class PanelTouchListener implements View.OnTouchListener {
    private Activity activity;
    private PlaneState planeState;
    private BluetoothDelegate bluetoothDelegate;

    public PanelTouchListener(Activity activity, BluetoothDelegate bluetoothDelegate) {
        this.activity = activity;
        this.planeState = (PlaneState) activity.getApplicationContext();
        this.bluetoothDelegate = bluetoothDelegate;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_MOVE) {
            // digest the event
            return true;
        }

        ImageView controlPanel = (ImageView) activity.findViewById(R.id.imgPanel);
        ImageView slider = (ImageView) activity.findViewById(R.id.throttleCursor);
        ImageView throttleNeedle = (ImageView) activity.findViewById(R.id.imgThrottleNeedle);
        TextView throttleText = (TextView) activity.findViewById(R.id.throttleValue);

        /* The current height of the control panel */
        final float panelHeight = controlPanel.getHeight();
        final float eventYValue = event.getY();

        /* don't let the throttle slider go lower than this */
        float maxCursorRange = panelHeight * Const.SCALE_FOR_CURSOR_RANGE;
        float motorSpeed = 0;
        /* is the range acceptable? */
        if (0 <= eventYValue && eventYValue < maxCursorRange) {
            slider.setY(eventYValue);
            motorSpeed = 1 - (eventYValue / maxCursorRange);
            if (motorSpeed < 0) {
                motorSpeed = 0;
            }
            /* check if the slider tries to go above the control panel height */
        } else if (eventYValue < 0) {
            /* 0 corresponds to the max position, because the slider cannot go outside the
             * containing relative layout (the control panel)
             */
            slider.setY(0);
            motorSpeed = 1; // 100% throttle
        }

        BLESmartplaneService smartplaneService = bluetoothDelegate.getSmartplaneService();
        // The smartPlaneService might not be available
        if (smartplaneService == null) {
            return true;
        }

        // Adjust if flight assist is on
        if (planeState.isFlAssistEnabled()) {
            motorSpeed *= Const.SCALE_FASSIST_THROTTLE;
        }

        planeState.setMotorSpeed(motorSpeed);

        float adjustedMotorSpeed = planeState.getMotorSpeed();
        smartplaneService.setMotor((short) (adjustedMotorSpeed * Const.MAX_MOTOR_SPEED));
        Util.rotateImageView(throttleNeedle, adjustedMotorSpeed,
                Const.THROTTLE_NEEDLE_MIN_ANGLE, Const.THROTTLE_NEEDLE_MAX_ANGLE);
        throttleText.setText((short) (adjustedMotorSpeed * 100) + "%");
        return true; // the event was digested, keep listening for touch events
    }
}
