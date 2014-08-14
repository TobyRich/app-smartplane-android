package com.tobyrich.app.SmartPlane.TransportLayers;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.tobyrich.app.SmartPlane.TEvent;

import java.util.HashSet;
import java.util.Set;


/**
 * @author Radu Hambasan
 * @date 4 Aug 2014
 */
public class FirebaseTransportLayer extends TransportLayer {
    private final String FIREBASE_URL;
    private final String GAME_URL;
    private Firebase _firebaseClient;
    private FirebaseDataListener _dataListener;
    private Set<String> _uniqueListeners;

    public FirebaseTransportLayer(String base_url, String game, String nr) {
        FIREBASE_URL = base_url;
        GAME_URL = FIREBASE_URL + game + "/" + nr;
        _firebaseClient = new Firebase(GAME_URL);
        _dataListener = new FirebaseDataListener();
        _uniqueListeners = new HashSet<String>();
    }


    public void send(final TEvent event) {
        _firebaseClient.child(event.address).push().setValue(event.value);
    }

    /**
     * @param eventAddress event for which you want callbacks
     */
    public void registerForEvent(String eventAddress) {
        if (!_uniqueListeners.contains(eventAddress)) {
            _uniqueListeners.add(eventAddress);
            _firebaseClient.child(eventAddress).addValueEventListener(_dataListener);
        }
    }

    private class FirebaseDataListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            TEvent event = new TEvent();
            event.address = dataSnapshot.getName();
            if (_receiveListener!= null) {
                _receiveListener.onReceive(event);
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
        }
    }
}
