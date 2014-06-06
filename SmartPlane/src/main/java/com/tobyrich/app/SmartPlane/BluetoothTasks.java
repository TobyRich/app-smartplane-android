package com.tobyrich.app.SmartPlane;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.TimerTask;

import lib.smartlink.BluetoothDevice;
import lib.smartlink.driver.BLESmartplaneService;

/*
 * Tasks that need bluetooth interaction
 */
public class BluetoothTasks {


    public static class SignalTimerTask extends TimerTask {
        // We have to avoid circular references, i.e.: the bluetooth device will hold a reference
        // to this activity and the activity will have a reference to the bluetooth device,
        // so without WeakReferences, none would get garbage collected.
        WeakReference<BluetoothDevice> weakDevice;

        public SignalTimerTask(BluetoothDevice device) {
            weakDevice = new WeakReference<BluetoothDevice>(device);
        }

        @Override
        public void run() {
            BluetoothDevice dev = weakDevice.get();
            if (dev != null) {
                weakDevice.get().updateSignalStrength();
            }
        }
    }

    public static class ChargeTimerTask extends TimerTask { // subclass for passing service in timer
        WeakReference<BLESmartplaneService> service; // using weakreference to BLESmartplaneService

        public ChargeTimerTask(BLESmartplaneService service) {
            this.service = new WeakReference<BLESmartplaneService>(service);
        }

        @Override
        public void run() {
            BLESmartplaneService serv = service.get();
            if (serv != null) {
                serv.updateChargingStatus();
            }
        }
    }
}
