package com.tobyrich.app.SmartPlane.dogfight;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.tobyrich.app.SmartPlane.R;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * @author Radu Hambasan
 * @date 30 Jul 2014
 */
public class DogfightActivity extends Activity {
    public static final int RESULT_CONNECTED = 1;
    public static final int RESULT_ERROR = 2;
    public static final UUID APP_UUID = UUID.fromString("75B64E51-5457-4ED1-921A-476090D80BA7");

    private BluetoothAdapter _bluetoothAdapter;
    private ArrayList<BluetoothDevice> _bluetoothDeviceArray = new ArrayList<BluetoothDevice>();
    private ListView _listView;
    private ArrayAdapter<String> _BTArrayAdapter;
    final private BroadcastReceiver _bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals(BluetoothDevice.ACTION_FOUND)) {
                return;  // not interested
            }
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) {
                return;
            }
            if (device.getName() == null) {
                return;  // we don't want to show null
            }
            _BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            _bluetoothDeviceArray.add(device);
            _BTArrayAdapter.notifyDataSetChanged();
            _listView.invalidateViews();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dogfight);

        _bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (_bluetoothAdapter == null) {
            setResult(RESULT_ERROR);
            finish();
            return;
        }

        _listView = (ListView) findViewById(R.id.dogfight_devices_listview);
        _BTArrayAdapter = new ArrayAdapter<String>(this, R.layout.dogfight_bt_list_item);
        _listView.setAdapter(_BTArrayAdapter);
        _listView.setOnItemClickListener(new DeviceSelectionListener(_bluetoothDeviceArray,
                this, _bluetoothAdapter));

        _bluetoothAdapter.cancelDiscovery();
        //registerReceiver(_bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        //_bluetoothAdapter.startDiscovery();
        if (_bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivity(discoverableIntent);
        }
        new BT_Server_Task(this, _bluetoothAdapter).execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(_bReceiver);
    }

    public void refreshList(View view) {
        _bluetoothAdapter.cancelDiscovery();
        _BTArrayAdapter.clear();
        _bluetoothDeviceArray.clear();
        _BTArrayAdapter.notifyDataSetChanged();
        _bluetoothAdapter.startDiscovery();
    }
}
