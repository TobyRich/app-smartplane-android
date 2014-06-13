package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.tobyrich.app.SmartPlane.util.Const;
import com.tobyrich.app.SmartPlane.util.InfoBox;
import com.tobyrich.app.SmartPlane.util.Util;

import lib.smartlink.BluetoothDevice;

/**
 * @author Samit Vaidya
 * @date 04 March 2014
 * Refactored by: Radu Hambasan
 */

public class FullscreenActivity extends Activity {
    private static final String TAG = "SmartPlane";

    private BluetoothDelegate bluetoothDelegate;  // bluetooth events
    private SensorHandler sensorHandler;  // accelerometer & magnetometer
    private GestureDetector gestureDetector;  // touch events
    private PlaneState planeState;

    @Override
    public void onResume() {
        super.onResume();

        // The resolution might have changed while the app was paused
        ViewTreeObserver viewTree = findViewById(R.id.controlPanel).getViewTreeObserver();
        viewTree.addOnGlobalLayoutListener(new GlobalLayoutListener(this));

        sensorHandler.registerListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorHandler.unregisterListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        Util.inform(this, "Pull Up to Start the Motor");

        String appVersion = "";
        try {
            appVersion = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Could not locate package; needed to set appVersion.");
            e.printStackTrace();
        }

        ImageView infoButton = (ImageView) findViewById(R.id.imgInfo);
        InfoBox infoBox = new InfoBox(Const.UNKNOWN, infoButton, appVersion);
        infoButton.setOnClickListener(infoBox);

        bluetoothDelegate = new BluetoothDelegate(this, infoBox);
        sensorHandler = new SensorHandler(this, bluetoothDelegate);
        sensorHandler.registerListener();
        gestureDetector = new GestureDetector(this,
                new GestureListener(this, bluetoothDelegate));
        planeState = (PlaneState) getApplicationContext();

         /* setting the trivial listeners */
        ImageView horizonImage = (ImageView) findViewById(R.id.imageHorizon);
        horizonImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        // sound played when the user presses the "Control Tower" button
        final MediaPlayer atcSound = MediaPlayer.create(this, R.raw.atc_sounds1);

        final ImageView atcOffButton = (ImageView) findViewById(R.id.atcOff);
        final ImageView atcOnButton = (ImageView) findViewById(R.id.atcOn);

        atcOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                atcOnButton.setVisibility(View.VISIBLE);
                v.setVisibility(View.GONE);
                atcSound.start();
            }
        });

        atcOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                atcOffButton.setVisibility(View.VISIBLE);
                v.setVisibility(View.GONE);
                atcSound.pause();
            }
        });

        ImageView controlPanel = (ImageView) findViewById(R.id.imgPanel);
        controlPanel.setOnTouchListener(new PanelTouchListener(this,
                bluetoothDelegate));

        final ImageView settings = (ImageView) findViewById(R.id.settings);
        final Switch rudderReverse = (Switch) findViewById(R.id.rudderSwitch);
        final TextView revRudderText = (TextView) findViewById(R.id.revRudderText);
        final Switch flAssistSwitch = (Switch) findViewById(R.id.flAssistSwitch);
        final TextView flAssistText = (TextView) findViewById(R.id.flAssistText);
        settings.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                settings.setVisibility(View.INVISIBLE);
                rudderReverse.setVisibility(View.VISIBLE);
                revRudderText.setVisibility(View.VISIBLE);
                flAssistSwitch.setVisibility(View.VISIBLE);
                flAssistText.setVisibility(View.VISIBLE);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        rudderReverse.setVisibility(View.INVISIBLE);
                        revRudderText.setVisibility(View.INVISIBLE);
                        flAssistSwitch.setVisibility(View.INVISIBLE);
                        flAssistText.setVisibility(View.INVISIBLE);
                        settings.setVisibility(View.VISIBLE);
                    }
                }, Const.HIDE_SETTINGS_DELAY);
                return true;
            }
        });  // End  settings listener

        flAssistSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                planeState.enableFlightAssist(isChecked);
            }
        });  // end flAssist listener
        // Default:
        flAssistSwitch.setChecked(true);
    }  // End onCreate()

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            BluetoothDevice device = bluetoothDelegate.getBluetoothDevice();
            if (device != null) {
                device.connect(); // start scanning and connect
            }
        } else {
            Log.e(TAG, "Bluetooth enabling was canceled by user");
        }
    }
}

