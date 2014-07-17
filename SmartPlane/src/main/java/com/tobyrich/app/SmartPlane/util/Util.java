/*

Copyright (c) 2014, TobyRich GmbH
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/
package com.tobyrich.app.SmartPlane.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tobyrich.app.SmartPlane.PlaneState;
import com.tobyrich.app.SmartPlane.R;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author Radu Hambasan
 * @date 05 Jun 2014
 * Class which contains useful methods
 */
public class Util {
    private static final String TAG = "Util";
    public static final int PHOTO_REQUEST_CODE = 723;
    public static final int SHARE_REQUEST_CODE = 724;

    /* If not null, it contains the uri of a photo that can be shared */
    public static Uri photoUri;

    /**
     * @param activity the activity where we want to show the toast
     * @param message the message that will be displayed
     *                Display a toast to the user.
     */
    public static void inform(final Activity activity, final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        });
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
     * @brief          Displays or hide a message indicating that the app is searching for a bluetooth device
     */
    public static void showSearching(final Activity activity, boolean show) {
        final int visibility = show ? View.VISIBLE : View.GONE;
        activity.findViewById(R.id.txtSearching).post(new Runnable() {
            @Override
            public void run() {
                TextView msgSearching = (TextView) activity.findViewById(R.id.txtSearching);
                String defaultSearchingMsg = activity.getString(R.string.label_searching);
                msgSearching.setText(defaultSearchingMsg);
                msgSearching.setVisibility(visibility);

                activity.findViewById(R.id.progressBar).setVisibility(visibility);
            }
        });
    }

    public static void takePicture(Activity activity) {
        photoUri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new ContentValues());
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        activity.startActivityForResult(cameraIntent, PHOTO_REQUEST_CODE);
    }

    public static void socialShare(Activity activity, Uri photoUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        if (photoUri != null) {
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, photoUri);
        } else {
            shareIntent.setType("text/plain");
        }
        shareIntent.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.social_share_message));
        Intent mailer = Intent.createChooser(shareIntent, "Share");
        activity.startActivityForResult(mailer, SHARE_REQUEST_CODE);
    }

    public static void showSocialShareDialog(final Activity activity) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Util.takePicture(activity);
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        Util.socialShare(activity, null);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        String shareWithPictureMessage = activity.getString(R.string.shareWithPictureMessage);
        String shareWithPictureYes = activity.getString(R.string.shareWithPictureYes);
        String shareWithPictureNo = activity.getString(R.string.shareWithPictureNo);
        String shareWithPictureCancel = activity.getString(R.string.shareWithPictureCancel);
        builder.setMessage(shareWithPictureMessage)
                .setPositiveButton(shareWithPictureYes, dialogClickListener)
                .setNeutralButton(shareWithPictureNo, dialogClickListener)
                .setNegativeButton(shareWithPictureCancel, dialogClickListener).show();
    }

    public static String readUrl(String url) {
        BufferedReader reader = null;

        try {
            URL _url = new URL(url);
            reader = new BufferedReader(new InputStreamReader(_url.openStream()));
            StringBuilder buffer = new StringBuilder();
            int nrRead;
            char[] readChars = new char[1024];
            while((nrRead = reader.read(readChars)) != -1) {
                buffer.append(readChars, 0, nrRead);
            }
            return  buffer.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }  // end readURL()
}

