package com.tlab.viewtobuffer;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Surface;
import android.widget.LinearLayout;

public class ViewToSurfaceLayout extends LinearLayout {

    private Surface m_surface;

    public ViewToSurfaceLayout(Context context) {
        super(context);
    }

    public void setSurface(Surface surface) {
        synchronized (this) {
            m_surface = surface;
        }
    }

    public void removeSurface() {
        synchronized (this) {
            m_surface = null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (m_surface == null) {
            return;
        }

        synchronized (this) {
            Canvas target = m_surface.lockHardwareCanvas();

            if (target != null) {
                super.draw(target);
                m_surface.unlockCanvasAndPost(target);
            }
        }
    }
}
