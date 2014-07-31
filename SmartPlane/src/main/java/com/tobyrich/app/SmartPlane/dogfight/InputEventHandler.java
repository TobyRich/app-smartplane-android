package com.tobyrich.app.SmartPlane.dogfight;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;

import com.tobyrich.app.SmartPlane.AppState;

import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * @author Radu Hambasan
 * @date 31 Jul 2014
 */
public class InputEventHandler {
    private static final int EVENT_DELAY = 100;  // ms

    private InputEventListener _eventListener;
    private Handler _handler;
    private AppState _appState;

    public static interface InputEventListener {
        void onInputEvent(DogfightData data);
        void onListeningStop();
    }

    public InputEventHandler(Activity activity, InputEventListener eventListener) {
        _eventListener = eventListener;
        HandlerThread handlerThread = new HandlerThread("InputEventListener");
        handlerThread.start();
        _handler = new Handler(handlerThread.getLooper());
        _appState = (AppState) activity.getApplication();
    }

    private void checkForInput() {
        InputStream _inputStream = _appState.devInput;
        try {
            if (_inputStream == null) {
                _eventListener.onListeningStop();
                return;
            }
            if (_inputStream.available() == 0) {
                _handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkForInput();
                    }
                }, EVENT_DELAY);
                return;
            }
            ObjectInputStream inputObjStream = new ObjectInputStream(_inputStream);
            DogfightData obj = (DogfightData) inputObjStream.readObject();
            _eventListener.onInputEvent(obj);
            _handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkForInput();
                }
            }, EVENT_DELAY);
        } catch (Exception ex) {
            ex.printStackTrace();
            // shhhh
        }
    }

    public void start() {
        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForInput();
            }
        }, EVENT_DELAY);
    }



}
