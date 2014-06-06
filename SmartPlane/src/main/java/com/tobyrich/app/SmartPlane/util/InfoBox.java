package com.tobyrich.app.SmartPlane.util;

import android.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;

/**
 * Dialog displayed when the info button is clicked
 */
public class InfoBox implements View.OnClickListener {
    String serialNumber;
    ImageView infoButton;
    String appVersion;

    public InfoBox(String serialNumber, ImageView infoButton, String appVersion) {
        this.serialNumber = serialNumber;
        this.infoButton = infoButton;
        this.appVersion = appVersion;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @Override
    public void onClick(View v) {
        // dialog box showing version info
        AlertDialog.Builder builder = new AlertDialog.Builder(infoButton.getContext());
        builder.setTitle("Version Info");
        String message = "Software: " + appVersion + "\n" + "Hardware: " + serialNumber;
        builder.setMessage(message).setPositiveButton("OK", null).show();
    }
}