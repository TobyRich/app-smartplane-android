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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import static org.apache.commons.lang3.ArrayUtils.reverse;

/*
 * Created by pvaibhav on 13/02/2014.
 */
@SuppressWarnings({"ConstantConditions", "AccessStaticViaInstance"})
// because we are already checking for null pointers for delegate
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
    private android.bluetooth.BluetoothDevice mDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private UUID[] mPrimaryServices;

    private Queue<BluetoothGattCharacteristic> mReadQueue = new LinkedList<BluetoothGattCharacteristic>(); // FIFO of chars to read in order

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

        // Now build our HashMap of uuid -> name.
        uuidToName = new HashMap<String, String>();
        // And also one to store which driver handles a particular service
        mServiceNameToDriverClass = new HashMap<String, String>();

        for (String serviceName : services.allKeys()) {

            // Get service information dictionary
            NSDictionary service = (NSDictionary) services.objectForKey(serviceName);

            // Add service name, its uuid and driver class
            String uuid = uuidHarmonize(service.objectForKey("UUID").toString());
            uuidToName.put(uuid, serviceName);
            mServiceNameToDriverClass.put(serviceName, service.objectForKey("DriverClass").toString());

            Log.i(TAG, "service " + serviceName + " ->" + uuid);

            // Now iterate over its characteristics
            NSDictionary fields = (NSDictionary) service.objectForKey("Fields");
            for (String charName : fields.allKeys()) {
                String charUuid = uuidHarmonize(fields.objectForKey(charName).toString());
                uuidToName.put(charUuid, serviceName + "/" + charName);
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
        try {
            delegate.get().didStartScanning(this);
        } catch (NullPointerException ex) {
            Log.w(TAG, "No delegate set");
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
        if (scanRecord.length < 3)
            return false; // cuz we need at least 3 bytes: len, type, data

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
        } while (offset < scanRecord.length - 1); // len-1 cuz each time we read at least 2 bytes
        return false;
    }

    @Override
    public void onLeScan(android.bluetooth.BluetoothDevice d, int rssi, byte[] scanRecord) {
        // When scan results are received
        mDevice = d;
        if (mDevice.getName() == null) // some sort of error happened
            return;

        Log.d(TAG, mDevice.getName() + " found");
        if (mDevice.getName().equalsIgnoreCase("TailorToys PowerUp"))
            mDevice.connectGatt(mOwner.getApplicationContext(), false, this);

        // Android BLE api has a bug which does not filter on 128 bit UUIDs (only 16 bit works).
        // To workaround it, we will manually check if this device is not supported, and skip it
//        if (includesPrimaryService(scanRecord)) {
//            // So this is a supported device, connect to it if signal strength is high enough
//            if (rssiLow < rssi && rssi < rssiHigh) {
//                mDevice.connectGatt(mOwner.getApplicationContext(), false, this);
//                if (delegate.get() != null) {
//                    delegate.get().didStartConnectingTo(this, rssi);
//                }
//            }
//        } else {
//            Log.d(TAG, "Primary service is NOT included by above device");
//        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
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
                charToDriver.clear();

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
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "services discovered");
        List<BluetoothGattService> gattServiceList = mBluetoothGatt.getServices();

        charToDriver.clear(); // start afresh

        for (BluetoothGattService s : gattServiceList) {
            // Find service name corresponding to this service's UUID, as per plist file
            String sName = uuidToName.get(uuidHarmonize(s.getUuid().toString()));

            // If it was not found (in plist), just continue to next
            if (sName == null)
                continue;

            BLEService driver;
            try {
                // Try to instantiate an instance of the driver's class (as specified in the plist)
                driver = (BLEService) Class.forName("com.tobyrich.lib.smartlink.driver."
                        + mServiceNameToDriverClass.get(sName)).newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
                return;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "No driver class " + mServiceNameToDriverClass.get(sName) + " was found");
                return;
            }

            // ----- Now that we created the driver, process this service's fields.
            Log.d(TAG, "service driver: " + mServiceNameToDriverClass.get(sName));

            // Create a hashmap to store the mapping of field names to chars.
            // This will be sent to the driver so it can output data to chars directly.
            HashMap<String, BluetoothGattCharacteristic> listOfFields = new HashMap<String, BluetoothGattCharacteristic>();

            // Iterate over each char, and if it's in the plist, add it to our global
            // and driver-specific lists.
            for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                // Find the name for this char based on its uuid, as specified in the plist
                String cName = uuidToName.get(uuidHarmonize(c.getUuid().toString()));
                if (cName == null) {
                    // this field was not in plist so skip it
                    continue;
                }
                Log.i(TAG, "found " + cName + " on device");

                // Remove the service name before sending to the driver, as they only get
                // the field names
                String cNameWithoutSname = cName.substring(cName.indexOf("/") + 1);
                listOfFields.put(cNameWithoutSname, c);

                // Also store the mapping of char to its designated driver
                charToDriver.put(c, driver);
            }

            // Attach this information to the driver instance, and notify the app
            driver.attach(mBluetoothGatt, listOfFields, this);
            try {
                delegate.get().didStartService(this, sName, driver);
            } catch (NullPointerException ex) {
                Log.w(TAG, "No delegate set");
            }
        }
    }

    protected void updateField(BluetoothGattCharacteristic c) {
        // Android ignores read requests if any previous requests are pending. So here we serialize
        // all read requests using a FIFO, and also ignore read requests for fields that are already
        // pending to be read in the queue.
        Log.d(TAG, "Adding to read queue, size " + mReadQueue.size() + "->" + (mReadQueue.size() + 1));
        if (mReadQueue.contains(c)) {
            Log.w(TAG, "Read queue already had char " + c);
        } else {
            mReadQueue.add(c);
            kickReadQueue();
        }
    }

    private void kickReadQueue() {
        // This function will read a field if it's the only one on the queue, otherwise it'll not
        // do anything and wait till the queue is kicked again next time (e.g. a value finished
        // reading and was removed from the queue)
        if (mReadQueue.size() == 1) {
            // queue has only one item so nothing else is pending, read it
            Log.d(TAG, "Reading from read queue (size=1)");
            BluetoothGattCharacteristic c = mReadQueue.peek();
            mBluetoothGatt.readCharacteristic(c);
        } else {
            Log.w(TAG, "Read queue was of size " + mReadQueue.size() + ", ignoring kick.");
            if (mReadQueue.size() > 15) {
                // queue too long, flush and start over
                // This is not strictly necessary, but this is Android so who knows?
                mReadQueue.clear();
                Log.w(TAG, "Read queue was too large --> FLUSHED");
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        String charName = uuidToName.get(uuidHarmonize(characteristic.getUuid().toString()));
        Log.d(TAG, "Received value for char " + charName);
        // Find which driver handles it and send it a message
        BLEService driver = charToDriver.get(characteristic);
        Log.i(TAG, "Driver " + driver + " was found to be informed");
        driver.didUpdateValueForCharacteristic(charName.substring(charName.indexOf("/") + 1));
        // deque
        mReadQueue.poll(); // remove top item
        kickReadQueue(); // read next
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        try {
            delegate.get().didUpdateSignalStrength(this, rssi);
        } catch (NullPointerException ex) {
            Log.w(TAG, "No delegate set");
        }
    }
}
