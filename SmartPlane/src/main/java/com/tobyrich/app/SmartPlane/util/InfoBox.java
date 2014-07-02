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

import android.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;

/**
 * @author Samit Vaidya
 * @date 04 March 2014
 * Refactored by: Radu Hambasan
 */

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