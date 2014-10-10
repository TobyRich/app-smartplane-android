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
package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Class in charge of maintaining the aspect ratio of the control panel.
 */
public class GlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
    private Activity activity;

    public GlobalLayoutListener (Activity activity) {
        this.activity = activity;
    }
    @Override
    public void onGlobalLayout() {
        ImageView imagePanel = (ImageView) activity.findViewById(R.id.imgPanel);
        Drawable drawable = imagePanel.getDrawable();
        float bitmapWidth = drawable.getIntrinsicWidth();
        float bitmapHeight = drawable.getIntrinsicHeight();
        // Change the height of the control panel section to maintain aspect ratio of image
        View controlPanel = activity.findViewById(R.id.controlPanel);
        controlPanel.getLayoutParams().height =
                (int) (controlPanel.getWidth() / (bitmapWidth / bitmapHeight));
    }
}
