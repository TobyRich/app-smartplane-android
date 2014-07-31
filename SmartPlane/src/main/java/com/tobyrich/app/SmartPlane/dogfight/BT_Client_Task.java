package com.tobyrich.app.SmartPlane.dogfight;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.widget.Toast;

import com.tobyrich.app.SmartPlane.AppState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Radu Hambasan
 * @date 30 Jul 2014
 */
class BT_Client_Task extends AsyncTask<Void, Void, Boolean> {
    BluetoothAdapter _bluetoothAdapter;
    BluetoothSocket _socket;
    InputStream _inputStream;
    OutputStream _outputStream;

    Activity _activity;
    AppState _appState;
    ProgressDialog _connDialog;

    public BT_Client_Task(Activity activity, BluetoothSocket socket,
                          BluetoothAdapter bluetoothAdapter) {
        _connDialog = new ProgressDialog(activity);
        _connDialog.setIndeterminate(true);
        _connDialog.setTitle("Connecting...");
        _connDialog.setMessage("Attempting to connect to your opponent.");
        _connDialog.setCancelable(false);
        _connDialog.setCanceledOnTouchOutside(false);

        _activity = activity;
        _appState = (AppState) _activity.getApplication();
        _socket = socket;

        _bluetoothAdapter = bluetoothAdapter;
    }

    @Override
    protected void onPreExecute() {
        _connDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (_appState.isConnected) return true;
        try{
            _bluetoothAdapter.cancelDiscovery();
            _socket.connect();
            _inputStream = _socket.getInputStream();
            _outputStream = _socket.getOutputStream();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            _bluetoothAdapter.startDiscovery();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        _connDialog.dismiss();
        if (result) {
            if (_appState.isConnected) {
                _activity.setResult(DogfightActivity.RESULT_CONNECTED);
                _activity.finish();
                return;
            }
            _appState.isConnected = true;
            _appState.devInput = _inputStream;
            _appState.devOutput = _outputStream;
            _activity.setResult(DogfightActivity.RESULT_CONNECTED);
            _activity.finish();
        } else {
            _activity.setResult(DogfightActivity.RESULT_ERROR);
            Toast.makeText(_activity, "Error while connecting...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCancelled() {
        _activity.setResult(DogfightActivity.RESULT_ERROR);
        _connDialog.dismiss();
        _bluetoothAdapter.startDiscovery();
        Toast.makeText(_activity, "Timeout while connecting...", Toast.LENGTH_SHORT).show();
    }
}
