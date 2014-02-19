package com.tobyrich.lib.smartlink;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by pvaibhav on 13/02/2014.
 */
public abstract class BLEService {

    private static final String TAG = "lib-smartlink-BLEService";
    protected HashMap<String, BluetoothGattCharacteristic> mFields;
    protected BluetoothGatt mGatt;

    public void attach(BluetoothGatt gatt, HashMap<String, BluetoothGattCharacteristic> listOfFields) {
        mFields = listOfFields;
        mGatt = gatt;
        for (String s : listOfFields.keySet()) {
            Log.d(TAG, "Driver " + this.getClass().toString() + " got field " + s);
        }
    }

    public abstract void detach();

    protected abstract void didUpdateValueForCharacteristic(String c);

    protected void updateField(String name) {
        mGatt.readCharacteristic(mFields.get(name));
    }

    protected String getStringValueForCharacteristic(String characteristic) {
        BluetoothGattCharacteristic c = mFields.get(characteristic);
        return c.getStringValue(0);
    }

    protected Integer getUint8ValueForCharacteristic(String characteristic) {
        BluetoothGattCharacteristic c = mFields.get(characteristic);
        return c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
    }

    protected Integer getInt8ValueForCharacteristic(String characteristic) {
        BluetoothGattCharacteristic c = mFields.get(characteristic);
        return c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0);
    }

    protected void writeUint8Value(short value, String characteristic) {
        BluetoothGattCharacteristic c = mFields.get(characteristic);
        c.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mGatt.writeCharacteristic(c);
    }

    protected void writeInt8Value(byte value, String characteristic) {
        BluetoothGattCharacteristic c = mFields.get(characteristic);
        c.setValue(value, BluetoothGattCharacteristic.FORMAT_SINT8, 0);
        mGatt.writeCharacteristic(c);
    }
}
