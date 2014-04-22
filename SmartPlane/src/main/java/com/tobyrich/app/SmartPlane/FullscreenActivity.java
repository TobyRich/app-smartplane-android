package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.plist.PropertyListFormatException;
import com.tobyrich.lib.smartlink.BLEService;
import com.tobyrich.lib.smartlink.BluetoothDevice;
import com.tobyrich.lib.smartlink.driver.BLEBatteryService;
import com.tobyrich.lib.smartlink.driver.BLEDeviceInformationService;
import com.tobyrich.lib.smartlink.driver.BLESmartplaneService;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.ParserConfigurationException;

public class FullscreenActivity
        extends Activity
        implements BluetoothDevice.Delegate, BLESmartplaneService.Delegate, BLEDeviceInformationService.Delegate, BLEBatteryService.Delegate, SensorEventListener {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "SmartPlane";
    private static final int RSSI_THRESHOLD_TO_CONNECT = -100; // dB

    private static final int MAX_ROLL_ANGLE = 45;
    private static final int MAX_RUDDER_SPEED = 127;
    private static final int MAX_MOTOR_SPEED = 254;
    private static final double PITCH_ANGLE_MAX = 50;
    private static final double PITCH_ANGLE_MIN = -50;
    private static final long ANIMATION_DURATION_MILLISEC = 500;
    private static final double SCALE_FOR_CONTROL_PANEL = 0.2; // movement of slider only in 80% of the control panel height
    private static final double SCALE_LOWER_RANGE_OF_SLIDER = 0.1; // limiting the bottom slider movement
    private static final double SCALE_FOR_VERT_MOVEMENT_HORIZON = 4.5;
    private static float THROTTLE_NEEDLE_MAX_ANGLE = 40; // in degrees
    private static float THROTTLE_NEEDLE_MIN_ANGLE = -132; // in degrees
    private static float SIGNAL_NEEDLE_MIN_ANGLE = 0; // in degrees
    private static float SIGNAL_NEEDLE_MAX_ANGLE = 180; // in degrees
    private static float MAX_BLUETOOTH_STRENGTH = -20;
    private static float MIN_BLUETOOTH_STRENGTH = -100;
    private static float FUEL_NEEDLE_MIN_ANGLE = -90; // in degrees
    private static float FUEL_NEEDLE_MAX_ANGLE = 90; // in degrees
    private static float MAX_BATTERY_VALUE = 100;
    private static long TIMER_DELAY = 500; // the delay in milliseconds before task is to be executed
    private static long TIMER_PERIOD = 1000; // the time in milliseconds between successive task executions

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private BluetoothDevice device;
    private BLESmartplaneService mSmartplaneService;
    private BLEDeviceInformationService mDeviceInfoService;
    private BLEBatteryService mBatteryService;

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float[] prevOrientation = new float[3];
    private float[] rotateMatrix = new float[9];
    private float[] inclintationMatrix = new float[9];
    private float[] newOrientation = new float[3];

    private ImageView controlPanel;
    private ImageView slider;
    private ImageView throttleNeedleImageView;
    private ImageView fuelNeedleImageView;
    private ImageView signalNeedleImageView;
    private ImageView imagePanel;
    private ImageView horizonImageView;
    private ImageView infoButton;

    private TextView throttleText;
    private TextView signalText;
    private TextView batteryStatus;
    private TextView batteryLevelText;

    private String appVersion;

    Timer timer = new Timer();

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
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
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

        try {
            appVersion = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {

        }

        Toast.makeText(FullscreenActivity.this,
                "Pull Up to Start the Motor",
                Toast.LENGTH_LONG).show();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        controlPanel = (ImageView) findViewById(R.id.imgPanel);
        slider = (ImageView) findViewById(R.id.imageView);
        horizonImageView = (ImageView) findViewById(R.id.imageHorizon);
        throttleNeedleImageView = (ImageView) findViewById(R.id.imgThrottleNeedle);
        fuelNeedleImageView = (ImageView) findViewById(R.id.imgFuelNeedle);
        signalNeedleImageView = (ImageView) findViewById(R.id.imgSignalNeedle);
        infoButton = (ImageView) findViewById(R.id.imgInfo);

        controlPanel.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int eventId = event.getAction();
                float fingerPosition = event.getRawY(); //the fingerposition on the screen, here only the values in Y axis
                final float diffFingerPosition = display.heightPixels - fingerPosition; //gets the required finger position
                final float controlpanelHeight = controlPanel.getHeight();
                final float eventYValue = event.getY();
                float newcontrolPanelHeight = 0;
                float motorSpeed = 0;

                switch (eventId) {

                    case MotionEvent.ACTION_MOVE:

                        newcontrolPanelHeight = (float) (controlpanelHeight - (SCALE_FOR_CONTROL_PANEL * controlpanelHeight)); //this new controlpanel height which is the range for the slider to move

                        if (0 < eventYValue && eventYValue < newcontrolPanelHeight) { //range from top of control panel to bottom of control panel
                            slider.setY(eventYValue); //movement of the slider with touch
                        } else if (eventYValue < 0) { //checking if the slider moves above the control panel height
                            slider.setY(0); //setting the slider to the max position if the user slides directly outside the control panel as the slider ranges from 0 to the control panel height
                        }

                        //calculations made so that the values of motorSpeed is only calculated in the controlpanel area
                        if ((diffFingerPosition < controlpanelHeight) && (diffFingerPosition > (SCALE_LOWER_RANGE_OF_SLIDER * controlpanelHeight))) {
                            motorSpeed = diffFingerPosition / controlpanelHeight;
                            if (motorSpeed < 0) {
                                motorSpeed = 0;
                            }
                        } else if (diffFingerPosition >= controlpanelHeight) {
                            fingerPosition = controlpanelHeight;
                            motorSpeed = fingerPosition / controlpanelHeight;
                            if (motorSpeed < 0) {
                                motorSpeed = 0;
                            }
                        }

                        rotateImageView(throttleNeedleImageView, motorSpeed, THROTTLE_NEEDLE_MIN_ANGLE, THROTTLE_NEEDLE_MAX_ANGLE);

                        throttleText = (TextView) findViewById(R.id.throttleValue);

                        try {
                            throttleText.setText(String.valueOf((short) (motorSpeed * 100) + "%"));
                            mSmartplaneService.setMotor((short) (motorSpeed * MAX_MOTOR_SPEED));

                        } catch (NullPointerException e) {
                            //checking, because mSmartplaneService might not be available everytime
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
            device = new BluetoothDevice(getResources().openRawResource(R.raw.powerup_and_smartplane), this);
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
    public void didStartChargingBattery() {
        // if smartplane is charging
        runOnUiThread(new ChargeStatusTextChanger("CHARGING"));
    }

    @Override
    public void didStopChargingBattery() {
        // if smartplane is not in charge
        runOnUiThread(new ChargeStatusTextChanger("IN USE  "));
    }

    @Override
    public void didUpdateSerialNumber(BLEDeviceInformationService device, String serialNumber) {
        runOnUiThread(new infoBox(serialNumber));
    }

    @Override
    public void didUpdateBatteryLevel(float percent) {
        runOnUiThread(new BatteryLevelUIChanger(percent));
    }

    class infoBox implements Runnable, View.OnClickListener { // for information box
        String serialNumber;

        public infoBox(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        @Override
        public void run() {
            infoButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // dialog box showing version info
            AlertDialog.Builder builder = new AlertDialog.Builder(FullscreenActivity.this);
            builder.setTitle("Version Info");
            builder.setMessage("Software: " + appVersion + "\n" + "Hardware: " + serialNumber).setPositiveButton("OK", null).show();
        }
    }

    class SignalTimerTask extends TimerTask { // subclass for passing device in timer
        WeakReference<BluetoothDevice> weakDevice; // using weakreference to BluetoothDevice

        public SignalTimerTask(BluetoothDevice device) {
            weakDevice = new WeakReference<BluetoothDevice>(device);
        }

        @Override
        public void run() {
            weakDevice.get().updateSignalStrength();
        }
    }

    class ChargeTimerTask extends TimerTask { // subclass for passing service in timer
        WeakReference<BLESmartplaneService> service; // using weakreference to BLESmartplaneService

        public ChargeTimerTask(BLESmartplaneService service) {
            this.service = new WeakReference<BLESmartplaneService>(service);
        }

        @Override
        public void run() {
            service.get().updateChargingStatus();
        }
    }

    class ChargeStatusTextChanger implements Runnable { // updating the ui part when charging status changes
        String chargeStatus;

        public ChargeStatusTextChanger(String chargeStatus) {
            this.chargeStatus = chargeStatus;
        }

        @Override
        public void run() {
            batteryStatus = (TextView) findViewById(R.id.batteryStatus);
            batteryStatus.setText(chargeStatus);
        }
    }

    class SignalLevelUIChanger implements Runnable { // subclass for passing signalStrength
        float signalStrength;
        float signalInDb;

        public SignalLevelUIChanger(float signalStrength, float signalInDb) {
            this.signalStrength = signalStrength;
            this.signalInDb = signalInDb;
        }

        @Override
        public void run() {
            signalText = (TextView) findViewById(R.id.signalValue);

            signalText.setText(String.valueOf((int) (signalInDb)));

            rotateImageView(signalNeedleImageView, signalStrength, SIGNAL_NEEDLE_MIN_ANGLE, SIGNAL_NEEDLE_MAX_ANGLE);
        }
    }

    class BatteryLevelUIChanger implements Runnable { // subclass for passing battery level
        float batteryLevel;

        BatteryLevelUIChanger(float batteryLevel) {
            this.batteryLevel = batteryLevel;
        }

        @Override
        public void run() {
            batteryLevelText = (TextView) findViewById(R.id.batteryLevelText);

            batteryLevelText.setText(String.valueOf((int) batteryLevel + "%"));

            rotateImageView(fuelNeedleImageView, batteryLevel / MAX_BATTERY_VALUE, FUEL_NEEDLE_MIN_ANGLE, FUEL_NEEDLE_MAX_ANGLE);
        }

    }

    /*
       A method for rotation of ImageView from minAngle to maxAngle
       im: ImageView
       value: data ranging from 0 to 1
       minAngle: minimum angle for rotation
       maxAngle: maximum angle for rotation
     */
    public void rotateImageView(ImageView im, float value, float minAngle, float maxAngle) {
        final float rotationVal = (minAngle + (value * (maxAngle - minAngle)));

        im.setRotation(rotationVal);
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
        if (serviceName.equalsIgnoreCase("smartplane") || serviceName.equalsIgnoreCase("powerup")) { // check for smartplane or powerup service

            mSmartplaneService = (BLESmartplaneService) service;
            mSmartplaneService.delegate = new WeakReference<BLESmartplaneService.Delegate>(this);

            ChargeTimerTask chargeTimerTask = new ChargeTimerTask(mSmartplaneService);
            // update charging status at a fixed rate
            timer.scheduleAtFixedRate(chargeTimerTask, TIMER_DELAY, TIMER_PERIOD);

            SignalTimerTask sigTask = new SignalTimerTask(device);
            // update bluetooth signal strength at a fixed rate
            timer.scheduleAtFixedRate(sigTask, TIMER_DELAY, TIMER_PERIOD);

        }

        if (serviceName.equalsIgnoreCase("devinfo")) { // check for devinfo service
            mDeviceInfoService = (BLEDeviceInformationService) service;
            mDeviceInfoService.delegate = new WeakReference<BLEDeviceInformationService.Delegate>(this);
        }

        if (serviceName.equalsIgnoreCase("battery")) { // check for battery service
            mBatteryService = (BLEBatteryService) service;
            mBatteryService.delegate = new WeakReference<BLEBatteryService.Delegate>(this);
        }
    }

    @Override
    public void didUpdateSignalStrength(BluetoothDevice device, float signalStrength) {
        float signalInDB = signalStrength;
        //scaling signalStrength to range from 0 to 1
        float finalSignalStrength = ((signalStrength - MIN_BLUETOOTH_STRENGTH) / (MAX_BLUETOOTH_STRENGTH - MIN_BLUETOOTH_STRENGTH));

        //for signalneedle
        runOnUiThread(new SignalLevelUIChanger(finalSignalStrength, signalInDB));

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
        timer.cancel(); //stop timer
        runOnUiThread(new infoBox("Unknown")); // if the smartplane is disconnected then, show hardware as "unknown"
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

            final boolean success = SensorManager.getRotationMatrix(rotateMatrix, inclintationMatrix, mGravity, mGeomagnetic); //get rotation matrix

            if (success) {

                SensorManager.getOrientation(rotateMatrix, newOrientation);

                newOrientation = LowPassFilter.filter(newOrientation, prevOrientation);
                prevOrientation = newOrientation;

                //getting just the integer part of the angles for smooth data
                final int rollAngle = (int) Math.toDegrees(newOrientation[2]); //radian to degrees
                final int pitchAngle = (int) Math.toDegrees(newOrientation[1]);

                double horizonVerticalMovement = 0.0;

                //limiting the values of pitch angle for the vertical movement of the horizon
                if (PITCH_ANGLE_MIN < pitchAngle && pitchAngle < PITCH_ANGLE_MAX) {
                    horizonVerticalMovement = SCALE_FOR_VERT_MOVEMENT_HORIZON * pitchAngle;
                } else if (pitchAngle <= PITCH_ANGLE_MIN) {
                    horizonVerticalMovement = SCALE_FOR_VERT_MOVEMENT_HORIZON * PITCH_ANGLE_MIN;
                } else if (pitchAngle >= PITCH_ANGLE_MAX) {
                    horizonVerticalMovement = SCALE_FOR_VERT_MOVEMENT_HORIZON * PITCH_ANGLE_MAX;
                }
                final short newRudder = (short) (rollAngle * -MAX_RUDDER_SPEED / MAX_ROLL_ANGLE);

                //translation animation, translating the image in the vertical direction
                TranslateAnimation translateHorizon = new TranslateAnimation(0, 0, -(float) horizonVerticalMovement, (float) horizonVerticalMovement);
                translateHorizon.setDuration(ANIMATION_DURATION_MILLISEC);

                horizonImageView.startAnimation(translateHorizon);

                horizonImageView.setRotation(-rollAngle); // set rotation of horizonimageview


                try {

                    mSmartplaneService.setRudder(newRudder);

                } catch (NullPointerException e) {
                    //checking, because mSmartplaneService might not be available everytime
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}

