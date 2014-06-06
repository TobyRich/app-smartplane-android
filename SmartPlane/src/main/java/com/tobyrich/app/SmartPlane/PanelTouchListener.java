package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tobyrich.app.SmartPlane.R;
import com.tobyrich.app.SmartPlane.util.Const;
import com.tobyrich.app.SmartPlane.util.Util;
import lib.smartlink.driver.BLESmartplaneService;


public class PanelTouchListener implements View.OnTouchListener {
    private Activity activity;
    BLESmartplaneService smartPlaneService;

    public PanelTouchListener(Activity activity, BLESmartplaneService smartPlaneService) {
        this.activity = activity;
        this.smartPlaneService = smartPlaneService;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageView controlPanel = (ImageView) activity.findViewById(R.id.imgPanel);
        ImageView slider = (ImageView) activity.findViewById(R.id.throttleCursor);
        ImageView throttleNeedle = (ImageView) activity.findViewById(R.id.imgThrottleNeedle);
        TextView throttleText = (TextView) activity.findViewById(R.id.throttleValue);

        /* The current height of the control panel */
        final float panelHeight = controlPanel.getHeight();
        final float eventYValue = event.getY();

        float motorSpeed = 0;

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                /* don't let the throttle slider go lower than this */
                float maxCursorRange = panelHeight * Const.SCALE_FOR_CURSOR_RANGE;
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

                Util.rotateImageView(throttleNeedle, motorSpeed,
                        Const.THROTTLE_NEEDLE_MIN_ANGLE, Const.THROTTLE_NEEDLE_MAX_ANGLE);

                // The smartPlaneService might not be available
                if (smartPlaneService == null) {
                    break;
                }

                smartPlaneService.setMotor((short) (motorSpeed * Const.MAX_MOTOR_SPEED));
                throttleText.setText(String.valueOf((short) (motorSpeed * 100) + "%"));

                break;
            default:
                break;

        }
        return true; // the event was digested, keep listening for touch events
    }
}