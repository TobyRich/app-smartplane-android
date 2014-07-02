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
import android.widget.ImageView;
import android.widget.TextView;

import com.tobyrich.app.SmartPlane.R;
import com.tobyrich.app.SmartPlane.util.Const;
import com.tobyrich.app.SmartPlane.util.Util;

/**
 * Class containing various listeners that are notified by bluetooth devices
 */

public class BluetoothListeners {
    public static class ChargeStatusTextChanger implements Runnable {
        Activity activity;
        String chargeStatus;

        public ChargeStatusTextChanger(Activity activity, String chargeStatus) {
            this.activity = activity;
            this.chargeStatus = chargeStatus;
        }

        @Override
        public void run() {
            TextView batteryStatus = (TextView) activity.findViewById(R.id.batteryStatus);
            batteryStatus.setText(chargeStatus);
        }
    }

    public static class SignalLevelUIChanger implements Runnable { // subclass for passing signalStrength
        Activity activity;
        float signalStrength;
        float signalInDb;

        public SignalLevelUIChanger(Activity activity, float signalStrength, float signalInDb) {
            this.activity = activity;
            this.signalStrength = signalStrength;
            this.signalInDb = signalInDb;
        }

        @Override
        public void run() {
            TextView signalText = (TextView) activity.findViewById(R.id.signalValue);
            ImageView signalNeedle = (ImageView) activity.findViewById(R.id.imgSignalNeedle);

            signalText.setText(String.valueOf((int) (signalInDb)));

            Util.rotateImageView(signalNeedle, signalStrength,
                    Const.SIGNAL_NEEDLE_MIN_ANGLE, Const.SIGNAL_NEEDLE_MAX_ANGLE);
        }
    }

    public static class BatteryLevelUIChanger implements Runnable {
        Activity activity;
        float batteryLevel;

        public BatteryLevelUIChanger(Activity activity, float batteryLevel) {
            this.activity = activity;
            this.batteryLevel = batteryLevel;
        }

        @Override
        public void run() {
            TextView batteryLevelText = (TextView) activity.findViewById(R.id.batteryLevelText);
            ImageView fuelNeedle = (ImageView) activity.findViewById(R.id.imgFuelNeedle);

            batteryLevelText.setText(String.valueOf((int) batteryLevel + "%"));

            Util.rotateImageView(fuelNeedle, (batteryLevel / Const.MAX_BATTERY_VALUE),
                    Const.FUEL_NEEDLE_MIN_ANGLE, Const.FUEL_NEEDLE_MAX_ANGLE);
        }

    }
}
