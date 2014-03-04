package com.tobyrich.lib.smartlink.driver;

import com.tobyrich.lib.smartlink.BLEService;

import java.lang.ref.WeakReference;

/**
 * Created by pvaibhav on 13/02/2014.
 */
public class BLESmartplaneService
    extends BLEService
{
    public interface Delegate {
        void didStartChargingBattery();
        void didStopChargingBattery();
    }

    public WeakReference<Delegate> delegate;

    public void setMotor(short speed) {
        if (speed > 254)
            speed = 254;
        if (speed < 0)
            speed = 0;
        writeUint8Value(speed, "smartplane/engine");
    }

    public void setRudder(short value) {
        if (value < -127)
            value = -127;
        if (value > 126)
            value = 126;
        writeInt8Value((byte) value, "smartplane/rudder");
    }

    public void updateChargingStatus() {
        updateField("smartplane/chargestatus");
    }

    @Override
    public void detach() {

    }

    @Override
    protected void didUpdateValueForCharacteristic(String c) {
        if (delegate.get() == null)
            return;

        if (c.equalsIgnoreCase("smartplane/chargestatus")) {
            int status = getUint8ValueForCharacteristic("smartplane/chargestatus");
            if (status == 0)
                delegate.get().didStopChargingBattery();
            else
                delegate.get().didStartChargingBattery();
        }
    }
}
