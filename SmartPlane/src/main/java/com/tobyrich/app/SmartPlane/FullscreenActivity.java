package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import com.dd.plist.PropertyListFormatException;
import com.tobyrich.lib.smartlink.BLEService;
import com.tobyrich.lib.smartlink.BluetoothDevice;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

public class FullscreenActivity
        extends Activity
        implements BluetoothDevice.Delegate
{
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "SmartPlane";
    private static final int RSSI_THRESHOLD_TO_CONNECT = -100; // dB

    private BluetoothDevice device;

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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        try {
            device = new BluetoothDevice(getResources().openRawResource(R.raw.powerup), this);
            device.delegate = this;
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

    }

    @Override
    public void didUpdateSignalStrength(BluetoothDevice device, float signalStrength) {

    }

    @Override
    public void didStartScanning(BluetoothDevice device) {
        Log.d(TAG, "started scanning");
    }

    @Override
    public void didStartConnectingTo(BluetoothDevice device, float signalStrength) {

    }

    @Override
    public void didDisconnect(BluetoothDevice device) {

    }
}
