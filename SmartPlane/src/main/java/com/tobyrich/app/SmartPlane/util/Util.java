package com.tobyrich.app.SmartPlane.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.tobyrich.app.SmartPlane.R;

/**
 * @author Radu Hambasan
 * @date 05 Jun 2014
 * Class which contains useful methods
 */
public class Util {
    /**
     * @param context the context in which the message will be displayed
     * @param message the message that will be displayed
     *                Display a toast to the user.
     */
    public static void inform(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * @param im       ImageView to be rotated
     * @param value    percentage of rotation (between 0 and 1) between the the limits
     * @param minAngle minimum angle for rotation
     * @param maxAngle maximum angle for rotation
     *                 rotate im from minAngle to maxAngle by value percentages
     */
    public static void rotateImageView(ImageView im, float value, float minAngle, float maxAngle) {
        final float rotationVal = (minAngle + (value * (maxAngle - minAngle)));

        im.setRotation(rotationVal);
    }

    /**
     * @param activity the activity where we want to show the message
     * @param show     true if we want to show the message, false if we want to hide it
     *                 Displays or hide a message indicating that the app is searching for a bluetooth device
     */
    public static void showSearching(final Activity activity, boolean show) {
        final int visibility = show ? View.VISIBLE : View.GONE;
        activity.findViewById(R.id.txtSearching).post(new Runnable() {
            @Override
            public void run() {
                activity.findViewById(R.id.txtSearching).setVisibility(visibility);
            }
        });
        activity.findViewById(R.id.progressBar).post(new Runnable() {
            @Override
            public void run() {
                activity.findViewById(R.id.progressBar).setVisibility(visibility);
            }
        });
    }
}

