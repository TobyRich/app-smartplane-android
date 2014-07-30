package com.tobyrich.app.SmartPlane.dogfight;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Radu Hambasan
 * @date 30 Jul 2014
 */
public class DeviceSelectionListener implements AdapterView.OnItemClickListener{
    public static final String TAG = "DeviceSelection";
    private static final int SERVER_TIMEOUT = 5000;  // ms

    final Activity _activity;
    BluetoothAdapter _bluetoothAdapter;
    final ArrayList<BluetoothDevice> _bluetoothDeviceArray;
    public DeviceSelectionListener(ArrayList<BluetoothDevice> bltDevArray,
                                   Activity activity, BluetoothAdapter bluetoothAdapter) {
        _bluetoothDeviceArray = bltDevArray;
        _bluetoothAdapter = bluetoothAdapter;
        _activity = activity;
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice dev = _bluetoothDeviceArray.get(position);
        BluetoothSocket socket;
        try {
            socket =
                    dev.createInsecureRfcommSocketToServiceRecord(DogfightActivity.APP_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Could not create socket");
            return;
        }
        final BT_Client_Task client =  new BT_Client_Task(_activity, socket, _bluetoothAdapter);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                client.cancel(true);
            }
        }, SERVER_TIMEOUT);
        client.execute();
    }
}