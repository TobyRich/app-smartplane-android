package com.tobyrich.lib.smartlink.driver;

import android.os.SystemClock;
import android.util.Log;

import com.tobyrich.lib.smartlink.BLEService;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

/**
 * Created by pvaibhav on 13/02/2014.
 */
public class BLESmartplaneService
        extends BLEService {
    private static final int MAX_REFRESH_RATE_IN_HZ = 10;

    public interface Delegate {
        void didStartChargingBattery();

        void didStopChargingBattery();
    }

    public WeakReference<Delegate> delegate;

    private short lastEngine = 0;
    private short lastRudder = 0;

    public void setMotor(short value) {
        if (value > 254)
            value = 254;
        if (value < 0)
            value = 0;
        lastEngine = value;
    }

    public void setRudder(short value) {
        if (value > 126)
            value = 126;
        if (value < -126)
            value = -126;
        lastRudder = value;
    }

    public void updateChargingStatus() {
        updateField("chargestatus");
    }

    @Override
    protected void attached() {
        // Reset to zero
        setWriteNeedsResponse(false, "engine");
        setWriteNeedsResponse(false, "rudder");
        writeUint8Value((short) 0, "engine");
        writeInt8Value((byte) 0, "rudder");

        TimerTask k = new TimerTask() {
            private short lastlastengine = 0;
            private short lastlastrudder = 0;

            @Override
            public void run() {
                if (lastlastrudder != lastRudder) {
                    writeInt8Value((byte) lastRudder, "rudder");
                    lastlastrudder = lastRudder;
                }
                if (lastlastengine != lastEngine) {
                    writeUint8Value(lastEngine, "engine");
                    lastlastengine = lastEngine;
                }
            }
        };

        Timer t = new Timer();
        t.scheduleAtFixedRate(k, 0, 1000 / MAX_REFRESH_RATE_IN_HZ);
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
