package com.tobyrich.lib.smartlink.driver;

import com.tobyrich.lib.smartlink.BLEService;

/**
 * Created by pvaibhav on 13/02/2014.
 */
public class BLESmartplaneService
    extends BLEService
{
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

    @Override
    public void detach() {

    }

    @Override
    protected void didUpdateValueForCharacteristic(String c) {

    }
}
