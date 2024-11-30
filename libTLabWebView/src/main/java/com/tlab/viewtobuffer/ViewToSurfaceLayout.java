package com.tlab.viewtobuffer;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Surface;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

public class ViewToSurfaceLayout extends LinearLayout {

    private static final String TAG = "ViewToSurfaceLayout";

    private Surface mSurface;

    public ViewToSurfaceLayout(Context context) {
        super(context);
    }

    public void setSurface(Surface surface) {
        synchronized (this) {
            mSurface = surface;
        }
    }

    public void removeSurface() {
        synchronized (this) {
            mSurface = null;
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mSurface == null) {
            return;
        }

        synchronized (this) {
            Canvas target = mSurface.lockHardwareCanvas();

            if (target != null) {
                super.draw(target);
                mSurface.unlockCanvasAndPost(target);
            }
        }
    }
}
