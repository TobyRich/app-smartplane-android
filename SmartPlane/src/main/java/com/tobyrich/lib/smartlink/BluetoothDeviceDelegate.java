package com.tobyrich.lib.smartlink;

/**
 * Created by pvaibhav on 13/02/2014.
 */
public interface BluetoothDeviceDelegate {
    public void didStartService(BluetoothDevice device, String serviceName, BLEService service);
    public void didUpdateSignalStrength(BluetoothDevice device, float signalStrength);
    public void didStartScanning(BluetoothDevice device);
    public void didStartConnectingTo(BluetoothDevice device, float signalStrength);
    public void didDisconnect(BluetoothDevice device);
}
