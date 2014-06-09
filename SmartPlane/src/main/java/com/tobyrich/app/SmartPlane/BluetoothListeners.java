package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.widget.ImageView;
import android.widget.TextView;

import com.tobyrich.app.SmartPlane.R;
import com.tobyrich.app.SmartPlane.util.Const;
import com.tobyrich.app.SmartPlane.util.Util;

/**
 * Class containing various listeners that are updated by bluetooth devices
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
