package com.tobyrich.app.SmartPlane.dogfight;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;

import com.tobyrich.app.SmartPlane.AppState;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * @author Radu Hambasan
 * @date 31 Jul 2014
 */
public class OutputChannel {
    private Handler _handler;
    private AppState _appState;

    public OutputChannel(Activity activity) {
        HandlerThread handlerThread = new HandlerThread("OutputChannel");
        handlerThread.start();
        _handler = new Handler(handlerThread.getLooper());
        _appState = (AppState) activity.getApplication();
    }

    private void _postData(final DogfightData data) {
        OutputStream _outputStream = _appState.devOutput;
        if (_outputStream == null) {
            return;
        }
        try {
            ObjectOutputStream objOutStream = new ObjectOutputStream(_outputStream);
            objOutStream.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();  // shh
        }
    }

    public void postData(final DogfightData data) {
        _handler.post(new Runnable() {
            @Override
            public void run() {
                _postData(data);
            }
        });
    }
}
