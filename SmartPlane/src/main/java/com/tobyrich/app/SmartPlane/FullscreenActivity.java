package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.View;
import android.view.MotionEvent;

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
        implements BluetoothDevice.Delegate, SensorEventListener {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "SmartPlane";
    private static final int RSSI_THRESHOLD_TO_CONNECT = -100; // dB

    private final int MAX_ROLL_ANGLE = 45;
    private final int MAX_RUDDER_SPEED = 127;
    private final int MAX_MOTOR_SPEED = 254;
    private final double SCALE_FOR_CONTROL_PANEL = 0.2;
    private final double SCALE_LOWER_RANGE_OF_SLIDER = 0.3;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private BluetoothDevice device;
    private BLESmartplaneService mSmartplaneService;

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];

    private ImageView controlPanel;
    private ImageView slider;
    private float newcontrolPanelHeight;
    private ImageView imagePanel;

    private DisplayMetrics display = new DisplayMetrics();


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
    public void onResume() {
        super.onResume();
        findViewById(R.id.controlPanel).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imagePanel = (ImageView) findViewById(R.id.imgPanel);

                Drawable drawable = imagePanel.getDrawable(); //get the drawable of the imageView here, imgPanel

                float bitmapWidth = drawable.getIntrinsicWidth(); //this is the bitmap's width
                float bitmapHeight = drawable.getIntrinsicHeight(); //this is the bitmap's height

                // Change the height of the control panel section to maintain aspect ratio of image
                View controlPanel = findViewById(R.id.controlPanel);
                controlPanel.getLayoutParams().height = (int) (controlPanel.getWidth() / (bitmapWidth / bitmapHeight));
            }
        });
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    /* Here, the movement of the slider with touch is implemented. The dimension of the display, the fingerposition in the screen by the user
    and the control panel height are the values mostly used. For movement of the imageview (slider), the new control panel height is caluclated by reducing the control panel height by 20%
    which limits the movement of the slider inside the control panel in a required way. Checking the values of event.getY() which is the vertical movement of the finger, the movement of
    the slider is adjusted. But for the touch response on the screen, the variable diffFingerPosition is used to limit the touch events in the required area i.e the control panel. In one place 30%
    of the controlPanelHeight is used because the touch movement is made limited above the navigation bar.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        getWindowManager().getDefaultDisplay().getMetrics(display); //used to get the dimensions of the display


        Toast.makeText(FullscreenActivity.this,
                "Pull Up to Start the Motor",
                Toast.LENGTH_LONG).show();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        controlPanel = (ImageView) findViewById(R.id.imgPanel);
        slider = (ImageView) findViewById(R.id.imageView);


        controlPanel.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int eventId = event.getAction();
                float fingerPosition = event.getRawY(); //the fingerposition on the screen, here only the values in Y axis
                float motorSpeed = 0;
                float diffFingerPosition = display.heightPixels - fingerPosition; //gets the required finger position
                float controlpanelHeight = controlPanel.getHeight();

                switch (eventId) {

                    case MotionEvent.ACTION_MOVE:

                        newcontrolPanelHeight = (float) (controlPanel.getHeight() - (SCALE_FOR_CONTROL_PANEL * controlPanel.getHeight())); //this new controlpanel height which is the range for the slider to move

                        if (event.getY() > 0 && event.getY() < newcontrolPanelHeight) {
                            slider.setY(event.getY()); //movement of the slider with touch
                        } else if (event.getY() < 0) { //checking if the slider moves above the control panel height
                            slider.setY(0); //setting the slider to the max position if the user slides directly outside the control panel as the slider ranges from 0 to the control panel height
                        }

                        //calculations made so that the values of motorSpeed is only calculated in the controlpanel area
                        if ((diffFingerPosition < controlpanelHeight) && (diffFingerPosition > (SCALE_LOWER_RANGE_OF_SLIDER * controlPanel.getHeight()))) {
                            motorSpeed = ((diffFingerPosition / (controlpanelHeight)) * 100); //multiplying by 100 to get the values in percentage
                            if (motorSpeed < 0) {
                                motorSpeed = 0;
                            }
                        } else if (diffFingerPosition > controlpanelHeight) {
                            fingerPosition = controlpanelHeight;
                            motorSpeed = ((fingerPosition / controlpanelHeight) * 100); //multiplying by 100 to get the values in percentage
                            if (motorSpeed < 0) {
                                motorSpeed = 0;
                            }
                        }

                        short new_motor = (short) (motorSpeed * MAX_MOTOR_SPEED / 100); //converting motorSpeed in percentage values so that it ranges from 0 to MAX_MOTOR_SPEED

                        try {

                            mSmartplaneService.setMotor(new_motor);

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                        break;

                    default:
                        break;

                }
                return true; //keeps listening for touch events when returned true
            }
        });

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
        if (serviceName.equals("smartplane")) {
            mSmartplaneService = (BLESmartplaneService) service;
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

                float rollAngle = orientation[2] * (float) (180 / Math.PI); //radian to degrees

                short newRudder = (short) (rollAngle * -MAX_RUDDER_SPEED / MAX_ROLL_ANGLE);

                try {

                    mSmartplaneService.setRudder(newRudder);

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}

