package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
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

    ImageView slider;
    ImageView throttleNeedle;
    TextView throttleText;

    /* constant only for a specific device */
   float maxCursorRange = -1;  // uninitialized

    public PanelTouchListener(Activity activity, BluetoothDelegate bluetoothDelegate) {
        this.activity = activity;
        this.planeState = (PlaneState) activity.getApplicationContext();
        this.bluetoothDelegate = bluetoothDelegate;

        slider = (ImageView) activity.findViewById(R.id.throttleCursor);
        throttleNeedle = (ImageView) activity.findViewById(R.id.imgThrottleNeedle);
        throttleText = (TextView) activity.findViewById(R.id.throttleValue);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if ((event.getAction() != MotionEvent.ACTION_MOVE) &&
                (event.getAction() != MotionEvent.ACTION_DOWN)) {
            // digest the event
            return true;
        }

        /* If uninitialized, initialize maxCursorRange */
        if (maxCursorRange == -1) {
            float panelHeight = activity.findViewById(R.id.imgPanel).getHeight();
            maxCursorRange = panelHeight * Const.SCALE_FOR_CURSOR_RANGE;
        }
        final float eventYValue = event.getY();

        /* Note: The coordinate system of the screen has the following properties:
         * (0,0) is in the upper-left corner.
         * There are no negative coordinates, i.e. X and Y always increase.
         */

        float motorSpeed = 0;
        /* check if the touch event went outside the bottom of the panel */
        if (eventYValue >= maxCursorRange) {
            slider.setY(maxCursorRange);
            motorSpeed = 0;
        /* check if the slider tries to go above the control panel height */
        } else if (eventYValue <= 0) {
            /* 0 corresponds to the max position, because the slider cannot go outside the
             * containing relative layout (the control panel)
             */
            slider.setY(0);
            motorSpeed = 1; // 100% throttle
        } else {
            slider.setY(eventYValue);
            motorSpeed = 1 - (eventYValue / maxCursorRange);
        }

        // Adjust if flight assist is on
        if (planeState.isFlAssistEnabled()) {
            motorSpeed *= Const.SCALE_FASSIST_THROTTLE;
        }

        planeState.setMotorSpeed(motorSpeed);
        float adjustedMotorSpeed = planeState.getAdjustedMotorSpeed();

        Util.rotateImageView(throttleNeedle, adjustedMotorSpeed,
                Const.THROTTLE_NEEDLE_MIN_ANGLE, Const.THROTTLE_NEEDLE_MAX_ANGLE);
        throttleText.setText((short) (adjustedMotorSpeed * 100) + "%");

        BLESmartplaneService smartplaneService = bluetoothDelegate.getSmartplaneService();
        // The smartPlaneService might not be available
        if (smartplaneService == null) {
            return true;
        }
        smartplaneService.setMotor((short) (adjustedMotorSpeed * Const.MAX_MOTOR_SPEED));
        return true; // the event was digested, keep listening for touch events
    }

}
