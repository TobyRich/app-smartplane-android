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
import android.media.MediaPlayer;
import android.util.Log;


import com.tobyrich.app.SmartPlane.util.Const;
import com.tobyrich.app.SmartPlane.util.InfoBox;
import com.tobyrich.app.SmartPlane.util.Util;

import java.lang.ref.WeakReference;
import java.util.Timer;

import lib.smartlink.BLEService;
import lib.smartlink.BluetoothDevice;
import lib.smartlink.driver.BLEBatteryService;
import lib.smartlink.driver.BLEDeviceInformationService;
import lib.smartlink.driver.BLESmartplaneService;

import static com.tobyrich.app.SmartPlane.BluetoothTasks.*;
import static com.tobyrich.app.SmartPlane.BluetoothListeners.*;

/**
 * Class responsible for callbacks from bluetooth devices
 */

public class BluetoothDelegate
        implements BluetoothDevice.Delegate, BLESmartplaneService.Delegate,
        BLEDeviceInformationService.Delegate, BLEBatteryService.Delegate {
    // members specific to this class
    private final String TAG = "BluetoothDelegate";

    private BluetoothDevice device;
    private BLESmartplaneService smartplaneService;
    private BLEDeviceInformationService deviceInfoService;
    private BLEBatteryService batteryService;

    private PlaneState planeState;
    private Timer timer = new Timer();

    // Resources acquired in constructor
    private Activity activity;
    private InfoBox infoBox;

    public BluetoothDelegate(Activity activity, InfoBox infoBox) {
        this.activity = activity;
        this.infoBox = infoBox;
        this.planeState = (PlaneState) activity.getApplicationContext();

        try {
            device = new BluetoothDevice(activity.getResources().openRawResource(R.raw.services),
                    activity);
            device.delegate = new WeakReference<BluetoothDevice.Delegate>(this);
            device.automaticallyReconnect = true;
            device.connect();
        } catch (IllegalArgumentException e) {
            Log.wtf(TAG, "Could not create BluetoothDevice (maybe invalid plist?)");
            e.printStackTrace();
        }
    }

    public BluetoothDevice getBluetoothDevice() {
        return device;
    }

    public BLESmartplaneService getSmartplaneService() {
        return smartplaneService;
    }


    @Override
    public void didStartChargingBattery() {
        activity.runOnUiThread(new ChargeStatusTextChanger(activity,
                Const.IS_CHARGING));
    }

    @Override
    public void didStopChargingBattery() {
        activity.runOnUiThread(new ChargeStatusTextChanger(activity,
                Const.IS_NOT_CHARGING));
    }

    @Override
    public void didUpdateSerialNumber(BLEDeviceInformationService device, String serialNumber) {
        infoBox.setSerialNumber(serialNumber);
    }

    @Override
    public void didUpdateBatteryLevel(float percent) {
        final float R_batt = 0.520f;  // Ohm
        /* 0.5 Amps is the current through the motor at MAX_MOTOR_SPEED */
        final float I_motor = (planeState.getAdjustedMotorSpeed() / Const.MAX_MOTOR_SPEED) * 0.5f;  // Amps
        /* We don't consider the contribution of the rudder & chip if the motor is off */
        final float I_rudder = I_motor == 0 ? 0 : 0.013f;  // estimate
        final float I_chip = I_motor == 0 ? 0 : 0.019f;  // estimate

        final float Vdrop = (I_motor + I_rudder + I_chip) * R_batt;
        /* The voltage takes values between 3.0V and 3.75V */
        final float Vmeasured = 3.0f + (0.75f * percent / 100.0f);
        final float Vbatt = Vmeasured + Vdrop;

        int adjustedPercent = Math.round(100.0f * ((Vbatt - 3.0f) / 0.75f));
        if (adjustedPercent > 100)
            adjustedPercent = 100;

        activity.runOnUiThread(new BatteryLevelUIChanger(activity, adjustedPercent));
    }

    @Override
    public void didStartService(BluetoothDevice device, String serviceName, BLEService service) {
        // We are no longer "searching" for the device
        Util.showSearching(activity, false);

        if (serviceName.equalsIgnoreCase("smartplane") ||
                serviceName.equalsIgnoreCase("sml1test")) {

            smartplaneService = (BLESmartplaneService) service;
            smartplaneService.delegate = new WeakReference<BLESmartplaneService.Delegate>(this);

            activity.runOnUiThread(new ChargeStatusTextChanger(activity, Const.IS_NOT_CHARGING));

            ChargeTimerTask chargeTimerTask = new ChargeTimerTask(smartplaneService);
            timer.scheduleAtFixedRate(chargeTimerTask, Const.TIMER_DELAY, Const.TIMER_PERIOD);

            MediaPlayer engineSound = MediaPlayer.create(activity, R.raw.engine_sound);
            engineSound.start();

            SignalTimerTask sigTask = new SignalTimerTask(device);
            timer.scheduleAtFixedRate(sigTask, Const.TIMER_DELAY, Const.TIMER_PERIOD);
            return;

        }

        if (serviceName.equalsIgnoreCase("devinfo")) { // check for devinfo service
            deviceInfoService = (BLEDeviceInformationService) service;
            deviceInfoService.delegate = new WeakReference<BLEDeviceInformationService.Delegate>(this);
            return;
        }

        if (serviceName.equalsIgnoreCase("battery")) { // check for battery service
            batteryService = (BLEBatteryService) service;
            batteryService.delegate = new WeakReference<BLEBatteryService.Delegate>(this);
        }

    }

    @Override
    public void didUpdateSignalStrength(BluetoothDevice device, float signalStrength) {
        // scaling signalStrength to range from 0 to 1
        float scaledSignalStrength =
                ((signalStrength - Const.MIN_BLUETOOTH_STRENGTH) / (Const.MAX_BLUETOOTH_STRENGTH - Const.MIN_BLUETOOTH_STRENGTH));

        // for signal needle
        activity.runOnUiThread(new SignalLevelUIChanger(activity, scaledSignalStrength, signalStrength));
    }

    @Override
    public void didStartScanning(BluetoothDevice device) {
        Log.d(TAG, "started scanning");
        Util.showSearching(activity, true);
        infoBox.setSerialNumber(Const.UNKNOWN);
    }

    @Override
    public void didStartConnectingTo(BluetoothDevice device, float signalStrength) {
        Log.d(TAG, "did start connecting to " + device.toString());
    }

    @Override
    public void didDisconnect(BluetoothDevice device) {
        timer.cancel(); //stop timer
        // if the smartplane is disconnected, show hardware as "unknown"
        infoBox.setSerialNumber(Const.UNKNOWN);
    }

}
