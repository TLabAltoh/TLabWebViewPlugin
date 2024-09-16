package com.tlab.viewtobuffer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.widget.LinearLayout;

@SuppressLint("ViewConstructor")
public class ViewToBufferLayout extends LinearLayout {

    private final ViewToBufferRenderer m_renderer;

    public ViewToBufferLayout(Context context, ViewToBufferRenderer renderer) {
        super(context);
        m_renderer = renderer;
    }

    @Override
    public void draw(Canvas canvas) {
        if (m_renderer == null) {
            return;
        }

        Canvas glAttachedCanvas = m_renderer.onDrawViewBegin();

        if (glAttachedCanvas != null) {
            super.draw(glAttachedCanvas);
        }

        m_renderer.onDrawViewEnd();
    }
}
