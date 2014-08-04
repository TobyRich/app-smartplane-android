package com.tobyrich.app.SmartPlane.TransportLayers;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.tobyrich.app.SmartPlane.dogfight.TEvent;

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
    private static final String TAG = "BluetoothTransportLayer";

    private Handler _outputHandler;
    private ObjectOutputStream _ostream;
    private ObjectInputStream _istream;

    public BluetoothTransportLayer(InputStream istream, OutputStream ostream) {
        try {
            _ostream = new ObjectOutputStream(ostream);
            _istream = new ObjectInputStream(istream);
        } catch (IOException ex) {
            Log.e(TAG, "You passed wrong streams");
            return;
        }

        final HandlerThread inputThread = new HandlerThread("BT_InputThread");
        inputThread.start();
        Handler _inputHandler = new Handler(inputThread.getLooper());
        _inputHandler.post(new Runnable() {
            @Override
            public void run() {
                inputLoop();
            }
        });

        HandlerThread outputThread = new HandlerThread("BT_OutputThread");
        outputThread.start();
        _outputHandler = new Handler(outputThread.getLooper());

    }

    public void send(final TEvent event) {
        _outputHandler.post(new Runnable() {
            @Override
            public void run() {
                _postData(event);
            }
        });
    }

    private void _postData(final TEvent event) {
        try {
            _ostream.writeObject(event);
        } catch (IOException e) {
            e.printStackTrace();  // shh
        }
    }

    private void inputLoop() {
        // noinspection InfiniteLoopStatement
        while(true) {
            try {
                TEvent obj = (TEvent) _istream.readObject();
                if (_receiveListener != null) {
                    _receiveListener.onReceive(obj);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
