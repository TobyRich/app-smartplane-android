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
import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.TextView;

import com.tobyrich.app.SmartPlane.util.Const;
import com.tobyrich.app.SmartPlane.util.Util;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Timer;

import lib.smartlink.BLEService;
import lib.smartlink.BluetoothDevice;
import lib.smartlink.BluetoothDisabledException;
import lib.smartlink.driver.BLEBatteryService;
import lib.smartlink.driver.BLEDeviceInformationService;
import lib.smartlink.driver.BLEFirmwareUploadService;
import lib.smartlink.driver.BLESmartplaneService;

import static com.tobyrich.app.SmartPlane.BluetoothTasks.*;
import static com.tobyrich.app.SmartPlane.UIChangers.*;

/**
 * Class responsible for callbacks from bluetooth devices
 */

public class BluetoothDelegate
        implements BluetoothDevice.Delegate, BLESmartplaneService.Delegate,
        BLEDeviceInformationService.Delegate, BLEBatteryService.Delegate,
        BLEFirmwareUploadService.Delegate {
    private final String TAG = "BluetoothDelegate";

    private BluetoothDevice device;
    private BLESmartplaneService smartplaneService;
    @SuppressWarnings("FieldCanBeLocal")
    private BLEDeviceInformationService deviceInfoService;
    @SuppressWarnings("FieldCanBeLocal")
    private BLEBatteryService batteryService;
    @SuppressWarnings("FieldCanBeLocal")
    private BLEFirmwareUploadService firmwareUploadService;

    private PlaneState planeState;
    private Timer timer;

    private Activity activity;
    private ProgressDialog updateDialog;

    public BluetoothDelegate(Activity activity) {
        this.activity = activity;
        updateDialog = new ProgressDialog(activity);

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

    public BLESmartplaneService getSmartplaneService() {
        return smartplaneService;
    }

    public void connect() throws BluetoothDisabledException {
        device.connect();
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
        final String hardwareDataInfo = "Hardware: " + serialNumber;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) activity.findViewById(R.id.hardwareInfoData)).setText(hardwareDataInfo);
            }
        });
    }

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

    @Override
    public void didStartService(BluetoothDevice device, String serviceName, BLEService service) {
        Log.i(TAG, "did start service: " + service.toString());
        // We are no longer "searching" for the device
        Util.showSearching(activity, false);
        Util.inform(activity, "Pull Up to Start the Motor");

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

        if (serviceName.equalsIgnoreCase("firmware")) {
            //noinspection ConstantConditions
            firmwareUploadService = (BLEFirmwareUploadService) service;
            firmwareUploadService.delegate =
                    new WeakReference<BLEFirmwareUploadService.Delegate>(this);
            InputStream imgA_inputStream = activity.getResources()
                    .openRawResource(R.raw.smartplane_204_a);
            InputStream imgB_InputStream = activity.getResources()
                    .openRawResource(R.raw.smartplane_204_b);
            firmwareUploadService.uploadFirmware(imgA_inputStream);
            firmwareUploadService.uploadFirmware(imgB_InputStream);
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
        Log.i(TAG, "started scanning");
        Util.showSearching(activity, true);
    }

    @Override
    public void didStartConnectingTo(BluetoothDevice device, float signalStrength) {
        Log.i(TAG, "did start connecting to " + device.toString());
        activity.runOnUiThread(new SearchingStatusChanger(activity));
    }

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
                ((TextView) activity.findViewById(R.id.hardwareInfoData))
                        .setText(hardwareDataInfo);
            }
        });
        Util.showSearching(activity, true);
    }

    @Override
    public void didGetFirmwareRejected(BLEFirmwareUploadService bleFirmwareUploadService,
                                       String s) {
        Log.i(TAG, "Firmware was rejected: " + s);
    }

    @Override
    public void didUploadFirmwareUpto(BLEFirmwareUploadService bleFirmwareUploadService,
                                      final float v) {
        Log.i("Progress!!", "Made progress: " + v);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDialog.setProgress((int) v);
            }
        });
    }

    @Override
    public void didFinishUploadingFirmware(BLEFirmwareUploadService bleFirmwareUploadService) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDialog.dismiss();
            }
        });
    }

    @Override
    public void didReceiveFirmwareVersion(BLEFirmwareUploadService bleFirmwareUploadService,
                                          String s) {
        Log.i(TAG, "received firmware version: " + s);
    }

    @Override
    public boolean shouldStartUploadingFirmware(BLEFirmwareUploadService bleFirmwareUploadService,
                                                String s) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDialog.setTitle("Uploading firmware");
                updateDialog.setMessage("Preparing the plane for its maiden flight...");
                updateDialog.setCancelable(false);
                updateDialog.setIndeterminate(false);
                updateDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                updateDialog.setMax(100);
                updateDialog.show();
            }
        });

        return true;
    }
}
