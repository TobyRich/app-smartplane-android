package com.tobyrich.app.SmartPlane;

import android.app.Application;

import com.tobyrich.app.SmartPlane.util.Const;

/**
 * @author Radu Hambasan
 * @date Jun 13 2014
 *
 * Singleton handling data shared at global level
 */
public class PlaneState extends Application{
    private float motorSpeed;
    private double scaler = 0;
    private boolean flAssistEnabled = false;
    private boolean screenLocked = false;

    public void setScaler(double scaler) {
        this.scaler = scaler;
    }
    public boolean isScreenLocked() {
        return this.screenLocked;
    }
    public void setScreenLock(boolean screenLocked) {
        this.screenLocked = screenLocked;
    }
    /**
     * If flight assist is enabled, it returns the scaled motorSpeed.
     */
    public float getAdjustedMotorSpeed() {
        if (flAssistEnabled) {
            float adjustedMotorSpeed = (float) (motorSpeed * (1 + scaler));
            if (adjustedMotorSpeed > 1)
                adjustedMotorSpeed = 1;
            return adjustedMotorSpeed;
        } else {
            return motorSpeed;
        }
    }

    public void setMotorSpeed(float motorSpeed) {
        this.motorSpeed = motorSpeed;
    }

    public void enableFlightAssist(boolean flAssistEnabled) {
        // Cutoff speed for flight assist
        if (!this.flAssistEnabled && motorSpeed > Const.SCALE_FASSIST_THROTTLE) {
            motorSpeed = (float) Const.SCALE_FASSIST_THROTTLE;
        }
        this.flAssistEnabled = flAssistEnabled;
    }

    public boolean isFlAssistEnabled() {
        return this.flAssistEnabled;
    }
}
