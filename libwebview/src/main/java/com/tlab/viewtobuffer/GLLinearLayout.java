package com.tlab.viewtobuffer;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class GLLinearLayout extends LinearLayout implements GLRenderable {

    private ViewToBufferRenderer mViewToBufferRenderer;

    public float mRatioWidth = 1;
    public float mRatioHeight = 1;

    /**
     * @param context
     * @param ratioWidth
     * @param ratioHeight
     */
    public GLLinearLayout(Context context, float ratioWidth, float ratioHeight) {
        super(context);
        mRatioWidth = ratioWidth;
        mRatioHeight = ratioHeight;
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
        if (mViewToBufferRenderer == null) {
            return;
        }

        Canvas glAttachedCanvas = mViewToBufferRenderer.onDrawViewBegin();

        if (glAttachedCanvas != null) {
            super.draw(glAttachedCanvas);
        }

        mViewToBufferRenderer.onDrawViewEnd();
    }

    /**
     * @param viewToBufferRenderer
     */
    @Override
    public void setViewToGLRenderer(ViewToBufferRenderer viewToBufferRenderer) {
        mViewToBufferRenderer = viewToBufferRenderer;
    }
}
