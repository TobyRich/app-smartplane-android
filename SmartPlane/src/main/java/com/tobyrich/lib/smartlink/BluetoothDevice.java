package com.tobyrich.lib.smartlink;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import static org.apache.commons.lang3.ArrayUtils.*;

/*
 * Created by pvaibhav on 13/02/2014.
 */
@SuppressWarnings("ConstantConditions") // because we are already checking for null pointers for delegate
public class BluetoothDevice extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback {

    public interface Delegate {
        public void didStartService(BluetoothDevice device, String serviceName, BLEService service);
        public void didUpdateSignalStrength(BluetoothDevice device, float signalStrength);
        public void didStartScanning(BluetoothDevice device);
        public void didStartConnectingTo(BluetoothDevice device, float signalStrength);
        public void didDisconnect(BluetoothDevice device);
    }

    private static final String TAG = "lib-smartlink-BluetoothDevice";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int ADV_128BIT_UUID_ALL = 0x06;
    private static final int ADV_128BIT_UUID_MORE = 0x07;

    public WeakReference<Delegate> delegate;
    public boolean automaticallyReconnect = false;
    private int rssiHigh = -25;
    private int rssiLow = -96;

    private final Activity mOwner;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private UUID[] mPrimaryServices;

    private HashMap<String, String> uuidToName;
    private HashMap<String, String> mServiceNameToDriverClass;
    private final HashMap<BluetoothGattCharacteristic, BLEService> charToDriver = new HashMap<BluetoothGattCharacteristic, BLEService>();

    private static String uuidHarmonize(String old) {
        // takes a 16 or 128 bit uuid string (with dashes) and harmonizes it into a 128 bit uuid
        String uuid = old.toUpperCase();
        if (uuid.length() == 4) {
            // 16 bit UUID. Convert to 128 bit using Bluetooth base UUID.
            uuid = "0000" + uuid + "-0000-1000-8000-00805F9B34FB";
        }
        return uuid;
    }

    public BluetoothDevice(InputStream plistFile, Activity owner) throws ParserConfigurationException, ParseException, SAXException, PropertyListFormatException, IOException {
        mOwner = owner;
        NSDictionary mPlist = (NSDictionary) PropertyListParser.parse(plistFile);

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

        // Now build our HashMap of uuid -> name and name -> uuid. Done separately for clarity.
        uuidToName = new HashMap<String, String>();
        HashMap<String, String> nameToUuid = new HashMap<String, String>();
        mServiceNameToDriverClass = new HashMap<String, String>();

        for (String serviceName: services.allKeys()) {

            // Get service information dictionary
            NSDictionary service = (NSDictionary) services.objectForKey(serviceName);

            // Add service name, its uuid and driver class
            String uuid = uuidHarmonize(service.objectForKey("UUID").toString());
            uuidToName.put(uuid, serviceName);
            nameToUuid.put(serviceName, uuid);
            mServiceNameToDriverClass.put(serviceName, service.objectForKey("DriverClass").toString());

            Log.i(TAG, "service " + serviceName + " ->" + uuid);

            // Now iterate over its characteristics
            NSDictionary fields = (NSDictionary) service.objectForKey("Fields");
            for (String charName : fields.allKeys()) {
                String charUuid = uuidHarmonize(fields.objectForKey(charName).toString());
                uuidToName.put(charUuid, serviceName + "/" + charName);
                nameToUuid.put(serviceName + "/" + charName, charUuid);
                Log.i(TAG, "  char " + charName + " -> " + charUuid);
            }

        }
    }

    public void connect() {
        initializeBluetooth();
    }

    public void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    public void updateSignalStrength() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.readRemoteRssi();
        }
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
        if (delegate.get() != null) {
            delegate.get().didStartScanning(this);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static boolean uuidEqualToByteArray(UUID uuid, byte[] b) {
        // http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation
        final String s = uuid.toString().replace("-", "");
        final String given = bytesToHex(b);
        return given.equalsIgnoreCase(s);
    }

    private boolean includesPrimaryService(byte[] scanRecord) {
        int offset = 0;
        do {
            final int len = scanRecord[offset++];
            final int type = scanRecord[offset++];
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
        if (includesPrimaryService(scanRecord)) {
            // So this is a supported device, connect to it if signal strength is high enough
            if (rssiLow < rssi && rssi < rssiHigh) {
                device.connectGatt(mOwner.getApplicationContext(), true, this);
                if (delegate.get() != null) {
                    delegate.get().didStartConnectingTo(this, rssi);
                }
            }
        } else {
            Log.d(TAG, "Primary service is NOT included by above device");
        }
    }

    @Override
    public void onConnectionStateChange (BluetoothGatt gatt, int status, int newState) {
        Log.d(TAG, "connection state changed " + status + " -> " + newState);
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                Log.d(TAG, "connected to device");
                mBluetoothAdapter.stopLeScan(this);
                mBluetoothGatt = gatt;
                mBluetoothGatt.discoverServices();
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                mBluetoothGatt = null;
                if (delegate.get() != null) {
                    delegate.get().didDisconnect(this);
                }
                if (automaticallyReconnect)
                    startScanning();
                break;
            default:
                break;
        }
    }

    @Override
    public void onServicesDiscovered (BluetoothGatt gatt, int status) {
        Log.d(TAG, "services discovered");
        List<BluetoothGattService> gattServiceList = mBluetoothGatt.getServices();
        for (BluetoothGattService s: gattServiceList) {
            String sName = uuidToName.get(uuidHarmonize(s.getUuid().toString()));
            if (sName != null) {
                // process this service's fields
                Log.d(TAG, "service driver: " + mServiceNameToDriverClass.get(sName));
                HashMap<String, BluetoothGattCharacteristic> listOfFields = new HashMap<String, BluetoothGattCharacteristic>();
                for (BluetoothGattCharacteristic c: s.getCharacteristics()) {
                    String cName = uuidToName.get(uuidHarmonize(c.getUuid().toString()));
                    if (cName == null) {
                        // this field was not in plist so skip it
                        continue;
                    }
                    Log.d(TAG, "found " + cName + " on device");
                    listOfFields.put(cName, c);
                }

                BLEService driver;
                try {
                    driver = (BLEService) Class.forName("com.tobyrich.lib.smartlink.driver." + mServiceNameToDriverClass.get(sName)).newInstance();
                    driver.attach(mBluetoothGatt, listOfFields);
                    // set the driver for each field to this instance of BLEService
                    for (String charName: listOfFields.keySet()) {
                        charToDriver.put(listOfFields.get(charName), driver);
                    }
                    if (delegate.get() != null) {
                        delegate.get().didStartService(this, sName, driver);
                    }
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    // if the driver class was not found, it's alright, just ignore.
                }
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        BLEService driver = charToDriver.get(characteristic);
        driver.didUpdateValueForCharacteristic(uuidToName.get(uuidHarmonize(characteristic.getUuid().toString())));
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        if (delegate.get() != null) {
            delegate.get().didUpdateSignalStrength(this, rssi);
        }
    }
}
