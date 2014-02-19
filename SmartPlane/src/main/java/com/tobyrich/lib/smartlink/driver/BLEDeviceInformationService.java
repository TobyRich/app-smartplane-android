package com.tobyrich.lib.smartlink.driver;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.tobyrich.lib.smartlink.BLEService;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Created by pvaibhav on 17/02/2014.
 */
public class BLEDeviceInformationService
    extends BLEService
{
    interface Delegate {
        void didUpdateSerialNumber(BLEDeviceInformationService device, String serialNumber);
    }

    private String mSerialNumber;
    public WeakReference<Delegate> delegate;

    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public void attach(BluetoothGatt gatt, HashMap<String, BluetoothGattCharacteristic> listOfFields) {
        super.attach(gatt, listOfFields);
        updateField("devinfo/serialnumber");
    }

    @Override
    public void detach() {

    }

    @Override
    protected void didUpdateValueForCharacteristic(String c) {
        if (c.equalsIgnoreCase("devinfo/serialnumber")) {
            mSerialNumber = getStringValueForCharacteristic("devinfo/serialnumber").trim();
            Log.d("lib-smartlink-devinfo", "Serial number updated: " + mSerialNumber + " (len=" + mSerialNumber.length() + ")");
        }
    }
}
