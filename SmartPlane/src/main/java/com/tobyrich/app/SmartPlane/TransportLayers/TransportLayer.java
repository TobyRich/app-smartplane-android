package com.tobyrich.app.SmartPlane.TransportLayers;

import com.tobyrich.app.SmartPlane.TEvent;

/**
 * @author Radu Hambasan
 * @date 1 Aug 2014
 */
public abstract class TransportLayer {
    protected OnReceiveListener _receiveListener;

    public abstract void send(TEvent event);

    public static interface OnReceiveListener {
        void onReceive(TEvent event);
    }

    public void setOnReceiveListener(OnReceiveListener receiveListener) {
        _receiveListener = receiveListener;
    }
}
