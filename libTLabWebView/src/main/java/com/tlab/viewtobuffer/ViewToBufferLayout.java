package com.tlab.viewtobuffer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

@SuppressLint("ViewConstructor")
public class ViewToBufferLayout extends LinearLayout {

    private final ViewToBufferRenderer mRenderer;

    public ViewToBufferLayout(Context context, ViewToBufferRenderer renderer) {
        super(context);
        mRenderer = renderer;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mRenderer == null) return;

        Canvas target = mRenderer.onDrawViewBegin();

        if (target != null) super.draw(target);

        mRenderer.onDrawViewEnd();
    }
}
