package com.tobyrich.app.SmartPlane.BT;

import android.os.Handler;
import android.os.HandlerThread;

import com.tobyrich.app.SmartPlane.dogfight.TEvent;
import com.tobyrich.app.SmartPlane.dogfight.TransportLayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * @author Radu Hambasan
 * @date 1 Aug 2014
 */
public class BluetoothTransportLayer extends TransportLayer {
    private static final int EVENT_DELAY = 100;  // ms

    private Handler _handler;
    private OutputStream _ostream;
    private InputStream _istream;

    public BluetoothTransportLayer(InputStream istream, OutputStream ostream) {
        HandlerThread handlerThread = new HandlerThread("BluetoothTransportLayer");
        handlerThread.start();
        _handler = new Handler(handlerThread.getLooper());

        _ostream = ostream;
        _istream = istream;

        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForInput();
            }
        }, EVENT_DELAY);
    }

    public void send(final TEvent event) {
        _handler.post(new Runnable() {
            @Override
            public void run() {
                _postData(event);
            }
        });
    }

    private void _postData(final TEvent event) {
        try {
            ObjectOutputStream objOutStream = new ObjectOutputStream(_ostream);
            objOutStream.writeObject(event);
        } catch (IOException e) {
            e.printStackTrace();  // shh
        }
    }

    private void checkForInput() {
        try {
            if (_istream.available() == 0) {
                _handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkForInput();
                    }
                }, EVENT_DELAY);
                return;
            }
            ObjectInputStream inputObjStream = new ObjectInputStream(_istream);
            TEvent obj = (TEvent) inputObjStream.readObject();
            if (_receiveListener != null) {
                _receiveListener.onReceive(obj);
            }
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
}
