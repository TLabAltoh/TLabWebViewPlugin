package com.tlab.webkit.chromium;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.Surface;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.tlab.webkit.BaseOffscreenBrowser;
import com.tlab.viewtobuffer.CustomGLSurfaceView;
import com.tlab.viewtobuffer.ViewToBufferLayout;
import com.tlab.viewtobuffer.ViewToHWBRenderer;
import com.tlab.viewtobuffer.ViewToPBORenderer;
import com.tlab.viewtobuffer.ViewToSurfaceLayout;
import com.unity3d.player.UnityPlayer;

public class OffscreenBrowser extends BaseOffscreenBrowser {
    protected CustomGLSurfaceView mGlSurfaceView;

    protected LinearLayout mCaptureLayout;

    private final static String TAG = "OffscreenBrowser (Chromium)";

    public void init() {
        Activity a = UnityPlayer.currentActivity;
        mRootLayout = new RelativeLayout(a);
        mRootLayout.setGravity(Gravity.TOP);
        mRootLayout.setX(mResState.screen.x);
        mRootLayout.setY(mResState.screen.y);
        mRootLayout.setBackgroundColor(0xFFFFFFFF);

        if ((mCaptureMode == CaptureMode.HARDWARE_BUFFER) || (mCaptureMode == CaptureMode.BYTE_BUFFER)) {
            switch (mCaptureMode) {
                case HARDWARE_BUFFER:
                    mViewToBufferRenderer = new ViewToHWBRenderer();
                    break;
                case BYTE_BUFFER:
                    mViewToBufferRenderer = new ViewToPBORenderer();
                    break;
            }
            mViewToBufferRenderer.setTextureResolution(mResState.tex.x, mResState.tex.y);

            mGlSurfaceView = new CustomGLSurfaceView(a);
            mGlSurfaceView.setEGLContextClientVersion(3);
            mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
            mGlSurfaceView.setPreserveEGLContextOnPause(true);
            mGlSurfaceView.setRenderer(mViewToBufferRenderer);
            mGlSurfaceView.setBackgroundColor(0x00000000);

            mCaptureLayout = new ViewToBufferLayout(a, mViewToBufferRenderer);
        } else if (mCaptureMode == CaptureMode.SURFACE) {
            mCaptureLayout = new ViewToSurfaceLayout(a);
        }

        mCaptureLayout.setOrientation(ViewToBufferLayout.VERTICAL);
        mCaptureLayout.setGravity(Gravity.START);
        mCaptureLayout.setBackgroundColor(Color.WHITE);

        a.addContentView(mRootLayout, new RelativeLayout.LayoutParams(mResState.view.x, mResState.view.y));
        if ((mCaptureMode == CaptureMode.HARDWARE_BUFFER) || (mCaptureMode == CaptureMode.BYTE_BUFFER)) {
            mRootLayout.addView(mGlSurfaceView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            mRootLayout.addView(mCaptureLayout, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        } else if (mCaptureMode == CaptureMode.SURFACE) {
            mRootLayout.addView(mCaptureLayout, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        }

        new Thread(() -> {
            while (mCaptureThreadKeepAlive) {
                try {
                    Thread.sleep(1000 / mFps);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                synchronized (mCaptureThreadMutex) {
                    mCaptureLayout.postInvalidate();
                }
            }
        }).start();
    }

    public void SetSurface(Object surfaceObj, int width, int height) {
        Surface surface = (Surface) surfaceObj;
        if (mCaptureLayout instanceof ViewToSurfaceLayout)
            ((ViewToSurfaceLayout) mCaptureLayout).setSurface(surface);
    }

    public void RemoveSurface() {
        if (mCaptureLayout instanceof ViewToSurfaceLayout)
            ((ViewToSurfaceLayout) mCaptureLayout).removeSurface();
    }

    @Override
    public void Dispose() {

    }
}
