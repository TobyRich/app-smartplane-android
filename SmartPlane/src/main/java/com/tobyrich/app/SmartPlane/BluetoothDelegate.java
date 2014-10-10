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
import android.widget.TextView;

import com.tobyrich.app.SmartPlane.util.Const;
import com.tobyrich.app.SmartPlane.util.Util;

import java.lang.ref.WeakReference;
import java.util.Timer;

import lib.smartlink.BLEService;
import lib.smartlink.BluetoothDevice;
import lib.smartlink.BluetoothDisabledException;
import lib.smartlink.driver.BLEBatteryService;
import lib.smartlink.driver.BLEDeviceInformationService;
import lib.smartlink.driver.BLESmartplaneService;

import static com.tobyrich.app.SmartPlane.BluetoothTasks.*;
import static com.tobyrich.app.SmartPlane.UIChangers.*;

/**
 * Abstraction Layer for the Bluetooth device
 * It mediates the interaction between the app the BLE services advertised by the peripheral:
 * i.e. it responds to callbacks and sets characteristics (motor, rudder)
 *
 * @author Radu Hambasan
 * @date 05 Jun 2014
 */

public class BluetoothDelegate
        implements BluetoothDevice.Delegate, BLESmartplaneService.Delegate,
        BLEDeviceInformationService.Delegate, BLEBatteryService.Delegate {
    private final String TAG = "BluetoothDelegate";

    private BluetoothDevice device;
    private BLESmartplaneService smartplaneService;
    @SuppressWarnings("FieldCanBeLocal")
    private BLEDeviceInformationService deviceInfoService;
    @SuppressWarnings("FieldCanBeLocal")
    private BLEBatteryService batteryService;

    private PlaneState planeState;
    private Timer timer;

    private Activity activity;

    /**
     * Create a BluetoothDelegate owned by <code>activity</code>
     * @param activity the activity that will use this delegate
     */
    public BluetoothDelegate(Activity activity) {
        this.activity = activity;
        this.planeState = (PlaneState) activity.getApplicationContext();

        try {
            device = new BluetoothDevice(activity.getResources().openRawResource(R.raw.services),
                    activity);
            device.delegate = new WeakReference<BluetoothDevice.Delegate>(this);
            device.automaticallyReconnect = true;
        } catch (IllegalArgumentException e) {
            Log.wtf(TAG, "Could not create BluetoothDevice (maybe invalid plist?)");
            e.printStackTrace();
        }
    }

    /**
     * Sets the motor speed to the <code>value</code>. If the value is out of the allowed range,
     * it will be trimmed to be inside it. I.e. if <code>value</code> is less than 0,
     * the motor will be set to 0. If <code>value</code> is more than 255, the motor
     * will be set to 255.
     * @param value new speed for the motor, it should be in the range [0, 255]
     *              0 corresponds to no motor movement and 255 to max throttle
     */
    public void setMotor(short value) {
        if (smartplaneService != null) {
            smartplaneService.setMotor(value);
        }
    }

    /**
     * Sets the rudder angle to <code>value</code>. If the value is out of the allowed range,
     * it will be trimmed to be inside it. I.e. if <code>value</code> is less than -127,
     * the angle will be set to -127. If <code>value</code> is more than 127, the rudder angle
     * will be set to 127.
     * @param value new speed for the motor, it should be in the range [0, 255]
     *              0 corresponds to no motor movement and 255 to max throttle
     */
    public void setRudder(short value) {
        if (smartplaneService != null) {
            smartplaneService.setRudder(value);
        }
    }

    /**
     * Look for peripherals and try to connect
     * @throws BluetoothDisabledException
     */
    public void connect() throws BluetoothDisabledException {
        device.connect();
    }

    /**
     * Callback when battery starts charging
     * We just update the UI status
     */
    @Override
    public void didStartChargingBattery() {
        activity.runOnUiThread(new ChargeStatusTextChanger(activity,
                Const.IS_CHARGING));
    }

    /**
     * Callback when battery stops charging
     * We just update the UI status
     */
    @Override
    public void didStopChargingBattery() {
        activity.runOnUiThread(new ChargeStatusTextChanger(activity,
                Const.IS_NOT_CHARGING));
    }

    /**
     * Callback when the serial number is read
     * @param service a reference to the service
     * @param serialNumber the new serial number
     */
    @Override
    public void didUpdateSerialNumber(BLEDeviceInformationService service, String serialNumber) {
        final String hardwareDataInfo = "Hardware: " + serialNumber;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) activity.findViewById(R.id.hardwareInfoData)).setText(hardwareDataInfo);
            }
        });
    }

    /**
     * Callback invoked when the battery level changes
     * Due to the varying voltage drop over the motor, we need to do additional computation
     * to get the correct battery level.
     * Afterwards, we just update the UI.
     * @param percent the new battery level, as reported by the peripheral
     */
    @Override
    public void didUpdateBatteryLevel(float percent) {
        Log.i(TAG, "did update battery level");
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

    /**
     * Callback invoked when a new service was discovered
     * In this implementation, we just get a reference to that service and set the delegate
     * @param device the connected devuce
     * @param serviceName as given in the plist config file
     * @param service a reference to the service
     */
    @Override
    public void didStartService(BluetoothDevice device, String serviceName, BLEService service) {
        Log.i(TAG, "did start service: " + service.toString());
        // We are no longer "searching" for the device
        Util.showSearching(activity, false);

        if (serviceName.equalsIgnoreCase("smartplane") ||
                serviceName.equalsIgnoreCase("sml1test")) {

            smartplaneService = (BLESmartplaneService) service;
            smartplaneService.delegate = new WeakReference<BLESmartplaneService.Delegate>(this);

            activity.runOnUiThread(new ChargeStatusTextChanger(activity, Const.IS_NOT_CHARGING));

            // if disconnected, or not initialized
            if (timer == null) {
                timer = new Timer();
            }

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

    /**
     * Callback invoked when the signal strength changes
     * We just update the UI signal level.
     * @param device the connected device
     * @param signalStrength the new signal strength
     */
    @Override
    public void didUpdateSignalStrength(BluetoothDevice device, float signalStrength) {
        // scaling signalStrength to range from 0 to 1
        float scaledSignalStrength =
                ((signalStrength - Const.MIN_BLUETOOTH_STRENGTH) / (Const.MAX_BLUETOOTH_STRENGTH - Const.MIN_BLUETOOTH_STRENGTH));

        // for signal needle
        activity.runOnUiThread(new SignalLevelUIChanger(activity, scaledSignalStrength, signalStrength));
    }

    /**
     * Callback invoked when scanning started
     * We just show a message to the user
     * @param device the device which is scanning
     */
    @Override
    public void didStartScanning(BluetoothDevice device) {
        Log.i(TAG, "started scanning");
        Util.showSearching(activity, true);
    }

    /**
     * Callback invoked when a connection is initiated
     * We just show a message to the user
     * @param device the device which is initiating the connection
     * @param signalStrength the signal strength
     */
    @Override
    public void didStartConnectingTo(BluetoothDevice device, float signalStrength) {
        Log.i(TAG, "did start connecting to " + device.toString());
        activity.runOnUiThread(new SearchingStatusChanger(activity));
    }

    /**
     * Callback invoked at disconnection
     * Clears all data associated with the old peripheral, then displays a message to the user.
     * @param device the device to which the bluetooth delegate is attached
     */
    @Override
    public void didDisconnect(BluetoothDevice device) {
        Log.i(TAG, "did disconnect from" + device.toString());
        if (timer != null) {
            timer.cancel();
        }
        // resetting all fields
        timer = null;
        smartplaneService = null;
        batteryService = null;
        deviceInfoService = null;
        // if the smartplane is disconnected, show hardware as "unknown"
        final String hardwareDataInfo = "Hardware: unknown";
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) activity.findViewById(R.id.hardwareInfoData)).setText(hardwareDataInfo);
            }
        });
        Util.showSearching(activity, true);
    }

}
