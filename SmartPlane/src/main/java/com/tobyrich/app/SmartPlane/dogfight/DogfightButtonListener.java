package com.tobyrich.app.SmartPlane.dogfight;

import android.app.Activity;
import android.content.Intent;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.tobyrich.app.SmartPlane.AppState;
import com.tobyrich.app.SmartPlane.R;
import com.tobyrich.app.SmartPlane.util.Util;

/**
 * @author Radu Hambasan
 * @date 30 Jul 2014
 */
public class DogfightButtonListener implements CompoundButton.OnCheckedChangeListener{
    private Activity _activity;
    private AppState _appState;

    public DogfightButtonListener(Activity activity) {
        _activity = activity;
        _appState = (AppState) activity.getApplication();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!_appState.isBTEnabled && isChecked) {
            String noBTMessage = _activity.getString(R.string.dogfight_no_BT);
            Toast.makeText(_activity, noBTMessage, Toast.LENGTH_SHORT).show();
            buttonView.setChecked(false);
            return;
        }
        if (!isChecked) {
            // handle dogfight disable
            return;
        }

        _appState.isConnected = false;
        _appState.devInput = null;
        _appState.devOutput = null;
        Intent dogfightActivity = new Intent(_activity, DogfightActivity.class);
        _activity.startActivityForResult(dogfightActivity, Util.DOGFIGHT_REQUEST_CODE);
    }
}
