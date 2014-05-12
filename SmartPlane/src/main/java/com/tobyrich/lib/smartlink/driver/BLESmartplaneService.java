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

    private class TimestampedValue {
        short value;
        long timestamp;

        public TimestampedValue(short value) {
            this.value = value;
            this.timestamp = SystemClock.elapsedRealtime();
        }

        public TimestampedValue() {
            this.value = 0;
            this.timestamp = SystemClock.elapsedRealtime();
        }

        public String toString() {
            return "{" + this.value + ", " + (SystemClock.elapsedRealtime() - this.timestamp) + "ms ago}";
        }
    }

    private short extrapolatedValueForNow(TimestampedValue a, TimestampedValue b) {
        TimestampedValue t_2 = a.timestamp < b.timestamp ? a : b; // one with older timestamp
        TimestampedValue t_1 = a.timestamp < b.timestamp ? b : a; // one with later timestamp
        final double slope = ((double) (t_1.value - t_2.value))
                / ((double) (t_1.timestamp - t_2.timestamp));
        final long now = SystemClock.elapsedRealtime();
        final long t = now - t_1.timestamp;
        final short c = (short) (t_1.value + (slope * t));
        //Log.i("BLESmartPlaneService", "interpolated " + t_2 + " and " + t_1 + " -> {" + c + ", t=" + (now - t_2.timestamp) + "ms after t-2}");
        return c;
    }

    private TimestampedValue engine_t_1 = new TimestampedValue();
    private TimestampedValue engine_t_2 = new TimestampedValue();
    private TimestampedValue rudder_t_1 = new TimestampedValue();
    private TimestampedValue rudder_t_2 = new TimestampedValue();

    private long lastWriteTime = SystemClock.elapsedRealtime();

    public void setMotor(short value) {
        // clip
        if (value > 254)
            value = 254;
        if (value < 0)
            value = 0;

        // Shift
        engine_t_2 = engine_t_1;
        engine_t_1 = new TimestampedValue(value);
    }

    public void setRudder(short value) {
        // Shift
        rudder_t_2 = rudder_t_1;
        rudder_t_1 = new TimestampedValue(value);
    }

    public void updateChargingStatus() {
        updateField("chargestatus");
    }

    @Override
    protected void attached() {
        // Reset to zero
        writeUint8Value((short) 0, "engine");
        engine_t_1 = new TimestampedValue();
        engine_t_2 = new TimestampedValue();

        writeInt8Value((byte) 0, "rudder");
        rudder_t_1 = new TimestampedValue();
        rudder_t_2 = new TimestampedValue();

        TimerTask k = new TimerTask() {
            private short lastEngine = 0;
            private short lastRudder = 0;
            @Override
            public void run() {
                short engine = extrapolatedValueForNow(engine_t_2, engine_t_1);
                short rudder = extrapolatedValueForNow(rudder_t_2, rudder_t_1);

                if (lastRudder != rudder) {
                    // clip
                    if (rudder > 126)
                        rudder = 126;
                    if (rudder < -126)
                        rudder = 126;

                    writeInt8Value((byte) rudder, "rudder");
                    Log.i("smartplane timer", "RUDDER " + rudder);

                    lastRudder = rudder;
                }

                if (lastEngine != engine) {
                    // clip
                    if (engine > 254)
                        engine = 254;
                    if (engine < 0)
                        engine = 0;

                    writeUint8Value(engine, "engine");
                    Log.i("smartplane timer", "ENGINE " + engine);
                    lastEngine = engine;
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
