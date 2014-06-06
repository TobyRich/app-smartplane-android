package com.tobyrich.app.SmartPlane;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Class in charge of maintaining the aspect ratio of the control panel
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
