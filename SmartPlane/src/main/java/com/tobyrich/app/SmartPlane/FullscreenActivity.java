package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import com.dd.plist.PropertyListFormatException;
import com.tobyrich.lib.smartlink.BLEService;
import com.tobyrich.lib.smartlink.BluetoothDevice;
import com.tobyrich.lib.smartlink.driver.BLESmartplaneService;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

public class FullscreenActivity
        extends Activity
        implements BluetoothDevice.Delegate, SensorEventListener
{
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "SmartPlane";
    private static final int RSSI_THRESHOLD_TO_CONNECT = -100; // dB

    private final int MAX_ROLL_ANGLE = 45;
    private final int MAX_PITCH_ANGLE = 90;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private BluetoothDevice device;
    private BLESmartplaneService mSmartplaneService;

    private float[] mGravity = new float[3];
   
    private float[] mGeomagnetic = new float[3];


    private void showSearching(boolean show) {
        final int visibility = show ? View.VISIBLE : View.GONE;
        findViewById(R.id.txtSearching).post(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.txtSearching).setVisibility(visibility);
            }
        });
        findViewById(R.id.progressBar).post(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progressBar).setVisibility(visibility);
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        findViewById(R.id.controlPanel).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                // Change the height of the control panel section to maintain aspect ratio of image
                View controlPanel = findViewById(R.id.controlPanel);
                controlPanel.getLayoutParams().height = (int) (controlPanel.getWidth() / (640.0 / 342.0));
            }
        });
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (mAccelerometer != null && mMagnetometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
        } else {
            Log.e(TAG, "no Accelerometer!");
        }


        try {
            device = new BluetoothDevice(getResources().openRawResource(R.raw.powerup), this);
            device.delegate = new WeakReference<BluetoothDevice.Delegate>(this);
            device.automaticallyReconnect = true;
            device.connect();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (PropertyListFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (device != null) {
                device.connect(); // start scanning and connect
            }
        } else {
            Log.e(TAG, "Bluetooth enabling was canceled by user");
        }
    }

    @Override
    public void didStartService(BluetoothDevice device, String serviceName, BLEService service) {
        showSearching(false);
        if(serviceName.equals("smartplane")){
             mSmartplaneService = (BLESmartplaneService)service;
        }
    }

    @Override
    public void didUpdateSignalStrength(BluetoothDevice device, float signalStrength) {

    }

    @Override
    public void didStartScanning(BluetoothDevice device) {
        Log.d(TAG, "started scanning");
        showSearching(true);
    }

    @Override
    public void didStartConnectingTo(BluetoothDevice device, float signalStrength) {

    }

    @Override
    public void didDisconnect(BluetoothDevice device) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic); //get rotation matrix

            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation); //get orientation
                float pitch_angle = orientation[1] * (float)(180/Math.PI); //radian to degrees
                float roll_angle = orientation[2] * (float)(180/Math.PI);

                short new_motor = (short)(pitch_angle * -254/MAX_PITCH_ANGLE);
                short new_rudder = (short)(roll_angle * -127/MAX_ROLL_ANGLE);

                try{


                    if(mSmartplaneService != null){
                        mSmartplaneService.setRudder(new_rudder);
                        mSmartplaneService.setMotor(new_motor);
                    }

                }catch(NullPointerException e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
