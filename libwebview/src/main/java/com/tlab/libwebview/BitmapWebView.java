package com.tlab.libwebview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.webkit.WebView;

public class /**/BitmapWebView extends WebView {

    /**
     * @param context
     */
    public BitmapWebView(Context context) {
        super(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public BitmapWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public BitmapWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @return
     */
    @Override
    public boolean performClick() {
        super.performClick();
        return false;
    }

    /**
     * @param canvas the canvas on which the background will be drawn
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}