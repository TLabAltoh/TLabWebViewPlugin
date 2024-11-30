package com.tlab.webkit.gecko;

import android.app.Activity;
import android.view.Gravity;
import android.widget.RelativeLayout;

import com.tlab.webkit.BaseOffscreenBrowser;
import com.tlab.viewtobuffer.ViewToHWBRenderer;
import com.tlab.viewtobuffer.ViewToPBORenderer;
import com.unity3d.player.UnityPlayer;

public class OffscreenBrowser extends BaseOffscreenBrowser {
    protected RelativeLayout mRootLayout;

    private final static String TAG = "OffscreenBrowser (Gecko)";

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
        }

        a.addContentView(mRootLayout, new RelativeLayout.LayoutParams(mResState.view.x, mResState.view.y));
    }

    public void SetSurface(Object surfaceObj, int width, int height) {
    }

    public void RemoveSurface() {
    }

    @Override
    public void Dispose() {

    }
}
