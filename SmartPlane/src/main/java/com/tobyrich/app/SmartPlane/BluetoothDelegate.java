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

    private Timer timer = new Timer();

    // Resources acquired in constructor
    private Activity activity;
    private InfoBox infoBox;

    public BluetoothDelegate(Activity activity, InfoBox infoBox) {
        this.activity = activity;
        this.infoBox = infoBox;

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
        activity.runOnUiThread(new BatteryLevelUIChanger(activity, percent));
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
        // TODO: why do we need this?
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
