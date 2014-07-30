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
import android.view.GestureDetector;
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

public class GestureListener extends GestureDetector.SimpleOnGestureListener {
    private Activity activity;
    private BluetoothDelegate bluetoothDelegate;
    private AppState appState;
    private boolean tapped;

    public GestureListener(Activity activity, BluetoothDelegate bluetoothDelegate) {
        this.activity = activity;
        appState = (AppState) activity.getApplicationContext();
        this.bluetoothDelegate = bluetoothDelegate;
        tapped = false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // consume event
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        final ImageView controlPanel = (ImageView) activity.findViewById(R.id.imgPanel);
        final ImageView throttleLock = (ImageView) activity.findViewById(R.id.lockThrottle);
        final ImageView slider = (ImageView) activity.findViewById(R.id.throttleCursor);
        final float currHeight = controlPanel.getHeight();
        final float newHeight = Const.SCALE_FOR_CURSOR_RANGE * currHeight;

        tapped = !tapped;

        // if double tapped, show the locked image, move the slider to min position,
        // move throttle needle to min, stop ontouch events on slider and controlpanel
        // and set motor value to 0
        if (tapped) {
            ImageView throttleNeedle = (ImageView) activity.findViewById(R.id.imgThrottleNeedle);
            TextView throttleText = (TextView) activity.findViewById(R.id.throttleValue);
            throttleLock.setVisibility(View.VISIBLE);

            slider.setY(newHeight);
            Util.rotateImageView(throttleNeedle, 0, Const.THROTTLE_NEEDLE_MIN_ANGLE,
                    Const.THROTTLE_NEEDLE_MAX_ANGLE);
            throttleText.setText("0" + "%");

            // turning the ontouch listener off
            slider.setEnabled(false);
            controlPanel.setEnabled(false);

            BLESmartplaneService smartplaneService = bluetoothDelegate.getSmartplaneService();
            if (smartplaneService != null) {
                smartplaneService.setMotor((short) 0);
            }
            appState.screenLocked = true;
        } else {
            throttleLock.setVisibility(View.INVISIBLE);
            slider.setEnabled(true);
            controlPanel.setEnabled(true);
            appState.screenLocked = false;
            appState.setMotorSpeed(0);
        }

        return true;
    }
}