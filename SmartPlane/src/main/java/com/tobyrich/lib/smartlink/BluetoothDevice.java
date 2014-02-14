package com.tobyrich.lib.smartlink;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;

import org.apache.commons.lang3.ArrayUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import static org.apache.commons.lang3.ArrayUtils.*;

/*
 * Created by pvaibhav on 13/02/2014.
 */
public class BluetoothDevice extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = "lib-smartlink-BluetoothDevice";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int ADV_128BIT_UUID_ALL = 0x06;
    private static final int ADV_128BIT_UUID_MORE = 0x07;

    public BluetoothDeviceDelegate delegate;
    public boolean automaticallyReconnect = false;
    public int rssiHigh = -25;
    public int rssiLow = -96;

    private Activity mOwner;
    private NSDictionary mPlist;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private UUID[] mPrimaryServices;


    public BluetoothDevice(InputStream plistFile, Activity owner) throws ParserConfigurationException, ParseException, SAXException, PropertyListFormatException, IOException {
        mOwner = owner;
        mPlist = (NSDictionary) PropertyListParser.parse(plistFile);

        // Collect basic settings
        rssiHigh = ((NSNumber) mPlist.objectForKey("rssi high")).intValue();
        rssiLow = ((NSNumber) mPlist.objectForKey("rssi low")).intValue();


        // Build a list of all primary services to scan for
        NSDictionary services = (NSDictionary) mPlist.objectForKey("Services");
        List<UUID> uuidList = new ArrayList<UUID>();

        for (String serviceName : services.allKeys()) { // enumerate over all keys

            NSDictionary service = (NSDictionary) services.objectForKey(serviceName);

            NSNumber isPrimary = ((NSNumber) service.objectForKey("Primary"));
            if (isPrimary == null)
                continue;

            if (isPrimary.boolValue()) {
                UUID uuid = UUID.fromString(service.objectForKey("UUID").toString());
                uuidList.add(uuid);
                Log.d(TAG, "Found primary uuid " + uuid + " for service " + serviceName);
            }
        }

        // Pre-convert to an array because it will be used quite often (on every scan start)
        mPrimaryServices = new UUID[uuidList.size()]; // allocate array with just enough elements
        uuidList.toArray(mPrimaryServices);
    }

    public void connect() {
        initializeBluetooth();
    }

    public void disconnect() {

    }

    public void updateSignalStrength() {

    }

    private void initializeBluetooth() {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mOwner.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth was not enabled, showing intent to enable");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mOwner.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        mBluetoothAdapter.stopLeScan(this); // in case scan was already running
        mBluetoothAdapter.startLeScan(this);
        if (this.delegate != null) {
            this.delegate.didStartScanning(this);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private boolean uuidEqualToByteArray(UUID uuid, byte[] b) {
        // http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation
        final String s = uuid.toString().replace("-", "");
        final String given = bytesToHex(b);
        return given.equalsIgnoreCase(s);
    }

    private boolean includesPrimaryService(byte[] scanRecord) {
        int offset = 0;
        int len = 0;
        int type = 0;
        do {
            len = scanRecord[offset++];
            type = scanRecord[offset++];
            if (type == ADV_128BIT_UUID_ALL || type == ADV_128BIT_UUID_MORE) {
                byte[] uuidbytes = ArrayUtils.subarray(scanRecord, offset, offset + len - 1);
                reverse(uuidbytes);

                for (UUID primary : mPrimaryServices) {
                    if (uuidEqualToByteArray(primary, uuidbytes))
                        return true;
                }

            } else {
                offset += len - 1;
            }
        } while (offset <= scanRecord.length);
        return false;
    }

    @Override
    public void onLeScan(android.bluetooth.BluetoothDevice device, int rssi, byte[] scanRecord) {
        // When scan results are received
        Log.d(TAG, device.getName() + " found");

        // Android BLE api has a bug which does not filter on 128 bit UUIDs (only 16 bit works).
        // To workaround it, we will manually check if this device is not supported, and skip it
        if (!includesPrimaryService(scanRecord)) {
            Log.d(TAG, "Primary service is NOT included by above device");
            return;
        }

        // So this is a supported device, connect to it if signal strength is high enough
        if (rssiLow < rssi && rssi < rssiHigh) {
            device.connectGatt(mOwner.getApplicationContext(), true, this);
            if (this.delegate != null) {
                this.delegate.didStartConnectingTo(this, rssi);
            }
        }
    }

    @Override
    public void onConnectionStateChange (BluetoothGatt gatt, int status, int newState) {
        Log.d(TAG, "connection state changed " + status + " -> " + newState);
    }

}
