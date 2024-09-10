package com.tlab.viewtobuffer;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class GLLinearLayout extends LinearLayout implements GLRenderable {

    private ViewToBufferRenderer m_viewToBufferRenderer;

    public float ratioWidth = 1;
    public float ratioHeight = 1;

    /**
     * @param context
     * @param ratioWidth
     * @param ratioHeight
     */
    public GLLinearLayout(Context context, float ratioWidth, float ratioHeight) {
        super(context);
        this.ratioWidth = ratioWidth;
        this.ratioHeight = ratioHeight;
    }

    /**
     * @param context
     * @param attrs
     */
    public GLLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public GLLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param canvas The Canvas to which the View is rendered.
     */
    @Override
    public void draw(Canvas canvas) {
        if (m_viewToBufferRenderer == null) {
            return;
        }

        Canvas glAttachedCanvas = m_viewToBufferRenderer.onDrawViewBegin();

        if (glAttachedCanvas != null) {
            super.draw(glAttachedCanvas);
        }

        m_viewToBufferRenderer.onDrawViewEnd();
    }

    /**
     * @param viewToBufferRenderer
     */
    @Override
    public void setViewToGLRenderer(ViewToBufferRenderer viewToBufferRenderer) {
        m_viewToBufferRenderer = viewToBufferRenderer;
    }
}
