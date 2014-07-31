package com.tobyrich.app.SmartPlane.dogfight;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.tobyrich.app.SmartPlane.R;

/**
 * @author Radu Hambasan
 * @date 30 Jul 2014
 */
public class DogfightModeSelectActivity extends Activity {
    public static final String MODE_NAME = "MODE_NAME";
    public static final int SERVER_ONLINE_RESULT_CODE = 401;
    public static final int CLIENT_ONLINE_RESULT_CODE = 402;
    public static final int ERROR_RESULT_CODE = 403;

    static final int SERVER_REQUEST_CODE = 201;
    static final int CLIENT_REQUEST_CODE = 202;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_df_mode_select);
    }

    public void start_server(View view) {
        Intent serverIntent = new Intent(this, DogfightActivity.class);
        serverIntent.putExtra(MODE_NAME, "server");
        startActivityForResult(serverIntent, SERVER_REQUEST_CODE);
    }

    public void start_discovery(View view) {
        Intent clientIntent = new Intent(this, DogfightActivity.class);
        clientIntent.putExtra(MODE_NAME, "client");
        startActivityForResult(clientIntent, CLIENT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SERVER_REQUEST_CODE) {
            if (resultCode == DogfightActivity.RESULT_CONNECTED) {
                setResult(SERVER_ONLINE_RESULT_CODE);
                finish();
                return;
            } else {
                setResult(ERROR_RESULT_CODE);
                finish();
                return;
            }
        } else if (requestCode == CLIENT_REQUEST_CODE) {
            if (resultCode == DogfightActivity.RESULT_CONNECTED) {
                setResult(CLIENT_ONLINE_RESULT_CODE);
                finish();
                return;
            } else {
                setResult(ERROR_RESULT_CODE);
                finish();
                return;
            }
        }
        setResult(ERROR_RESULT_CODE);
        finish();
    }
}
