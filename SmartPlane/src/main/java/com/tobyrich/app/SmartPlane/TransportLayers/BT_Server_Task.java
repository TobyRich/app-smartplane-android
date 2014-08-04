package com.tobyrich.app.SmartPlane.TransportLayers;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import com.tobyrich.app.SmartPlane.AppState;
import com.tobyrich.app.SmartPlane.dogfight.DogfightActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Radu Hambasan
 * @date 30 Jul 2014
 */
public class BT_Server_Task extends AsyncTask<Void, Void, Boolean> {
    public static final String TAG = "BT_Server";

    private Activity _activity;
    private AppState _appState;
    private BluetoothAdapter _bluetoothAdapter;

    private InputStream _inputStream;
    private OutputStream _outputStream;

    public BT_Server_Task(Activity activity, BluetoothAdapter bluetoothAdapter) {
        _activity = activity;
        _appState = (AppState) activity.getApplication();
        _bluetoothAdapter = bluetoothAdapter;
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "starting BT server");
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (_appState.isConnected) return true;
        try {
            BluetoothServerSocket _serverSocket =
                    _bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("UNUSED",
                            DogfightActivity.APP_UUID);
            BluetoothSocket _socket = _serverSocket.accept();
            _serverSocket.close();
            _inputStream = _socket.getInputStream();
            _outputStream = _socket.getOutputStream();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (_appState.isConnected) return;
        if (result) {
            _appState.devInput = _inputStream;
            _appState.devOutput = _outputStream;
            _appState.isConnected = true;
            _activity.setResult(DogfightActivity.RESULT_CONNECTED);
            _activity.finish();
        } else {
            Log.e(TAG, "A connection was attempted, but failed.");
            new BT_Server_Task(_activity, _bluetoothAdapter).execute();
        }
    }
}
