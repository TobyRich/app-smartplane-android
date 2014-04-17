package com.tobyrich.lib.smartlink.driver;

import android.util.Log;

import com.tobyrich.lib.smartlink.BLEService;

import java.lang.ref.WeakReference;

/**
 * Created by pvaibhav on 13/02/2014.
 */
public class BLESmartplaneService
        extends BLEService {
    public interface Delegate {
        void didStartChargingBattery();

        void didStopChargingBattery();
    }

    public WeakReference<Delegate> delegate;

    private short oldSpeed = 0;
    private short oldRudder = 0;

    public void setMotor(short speed) {
        if (oldSpeed == speed)
            return;
        if (speed > 254)
            speed = 254;
        if (speed < 0)
            speed = 0;
        writeUint8Value(speed, "engine");
        oldSpeed = speed;
    }

    public void setRudder(short value) {
        if (oldRudder == value)
            return;
        if (value < -127)
            value = -127;
        if (value > 126)
            value = 126;
        writeInt8Value((byte) value, "rudder");
        oldRudder = value;
    }

    public void updateChargingStatus() {
        updateField("chargestatus");
    }

    @Override
    protected void attached() {
        // Reset to zero
        writeUint8Value((short) 0, "engine");
        writeInt8Value((byte) 0, "rudder");
    }

    @Override
    protected void didUpdateValueForCharacteristic(String c) {
        if (delegate.get() == null)
            return;

        if (c.equalsIgnoreCase("chargestatus")) {
            int status = getUint8ValueForCharacteristic("chargestatus");
            try {
                if (status == 0)
                    delegate.get().didStopChargingBattery();
                else
                    delegate.get().didStartChargingBattery();
            } catch (NullPointerException ex) {
                Log.w(this.getClass().getName(), "No delegate set");
            }
        }
    }
}
