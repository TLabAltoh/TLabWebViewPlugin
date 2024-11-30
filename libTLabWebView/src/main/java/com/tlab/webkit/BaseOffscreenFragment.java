package com.tlab.webkit;

import android.app.Activity;
import android.app.Fragment;
import android.hardware.HardwareBuffer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.robot9.shared.SharedTexture;
import com.tlab.viewtobuffer.ViewToBufferRenderer;
import com.tlab.viewtobuffer.ViewToHWBRenderer;
import com.tlab.viewtobuffer.ViewToPBORenderer;
import com.unity3d.player.UnityPlayer;

public abstract class BaseOffscreenFragment extends Fragment {

    private static final String TAG = "BaseOffscreenFragment";

    public enum CaptureMode {
        HARDWARE_BUFFER, BYTE_BUFFER, SURFACE
    }

    protected CaptureMode mCaptureMode = CaptureMode.HARDWARE_BUFFER;

    protected final Common.ResolutionState mResState = new Common.ResolutionState();

    protected ViewToBufferRenderer mViewToBufferRenderer;

    protected RelativeLayout mRootLayout;

    protected long[] mHwbTexID;
    protected boolean mIsVulkan;

    protected SharedTexture mSharedTexture;
    protected HardwareBuffer mSharedBuffer;

    protected boolean mCaptureThreadKeepAlive = true;
    protected final Object mCaptureThreadMutex = new Object();

    protected int mFps = 30;

    public boolean mInitialized = false;

    public boolean mDisposed = false;

    protected boolean mIsSharedBufferExchanged = true;

    @Override
    public void onPause() {
        super.onPause();
        if ((mCaptureMode != CaptureMode.SURFACE) && (mViewToBufferRenderer != null))
            mViewToBufferRenderer.disable();
        else if (mCaptureMode == CaptureMode.SURFACE) RemoveSurface();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void initParam(int viewWidth, int viewHeight, int texWidth, int texHeight, int screenWidth, int screenHeight, boolean isVulkan, CaptureMode captureMode) {
        mResState.view.update(viewWidth, viewHeight);
        mResState.tex.update(texWidth, texHeight);
        mResState.screen.update(screenWidth, screenHeight);
        mIsVulkan = isVulkan;
        mCaptureMode = captureMode;
    }

    public void abortCaptureThread() {
        synchronized (mCaptureThreadMutex) {
            mCaptureThreadKeepAlive = false;
        }
    }

    public void ReleaseSharedTexture() {
        //Log.i(TAG, "release (start)");
        mHwbTexID = null;
        if (mSharedTexture != null) {
            mSharedTexture.release();
            mSharedTexture = null;
        }
        //Log.i(TAG, "release (end)");
    }

    public void UpdateSharedTexture() {
        if (mViewToBufferRenderer instanceof ViewToHWBRenderer) {
            HardwareBuffer sharedBuffer = ((ViewToHWBRenderer) mViewToBufferRenderer).getHardwareBuffer();

            if (sharedBuffer == null) return;

            if (mSharedBuffer == sharedBuffer) {
                mSharedTexture.updateUnityTexture();
                return;
            }

            ReleaseSharedTexture();

            SharedTexture sharedTexture = new SharedTexture(sharedBuffer, mIsVulkan);

            mHwbTexID = new long[1];
            mHwbTexID[0] = sharedTexture.getPlatformTexture();

            mSharedTexture = sharedTexture;
            mSharedBuffer = sharedBuffer;

            mIsSharedBufferExchanged = false;
        }
    }

    public boolean ContentExists() {
        return mViewToBufferRenderer.contentExists();
    }

    public byte[] GetFrameBuffer() {
        if (mViewToBufferRenderer instanceof ViewToPBORenderer)
            return ((ViewToPBORenderer) mViewToBufferRenderer).getPixelBuffer();
        return new byte[0];
    }

    public void SetFps(int fps) {
        mFps = fps;
    }

    /**
     * Return the texture pointer of the WebView frame
     * (NOTE: In Vulkan, the VkImage pointer returned by this function could not be used for UpdateExternalTexture. This issue has not been fixed).
     *
     * @return texture pointer of the WebView frame (Vulkan: VkImage, OpenGLES: TexID)
     */
    public long GetPlatformTextureID() {
        if (mHwbTexID == null) return 0;
        return mHwbTexID[0];
    }

    public void SetUnityTextureID(long unityTexID) {
        if (mSharedTexture != null) mSharedTexture.setUnityTexture(unityTexID);
    }

    public abstract void SetSurface(Object surfaceObj, int width, int height);

    public abstract void RemoveSurface();

    public abstract void Dispose();

    public void Resize(int texWidth, int texHeight, int viewWidth, int viewHeight) {
        if (mViewToBufferRenderer != null) {
            mResState.tex.update(texWidth, texHeight);
            mViewToBufferRenderer.setTextureResolution(mResState.tex.x, mResState.tex.y);
            mViewToBufferRenderer.disable();
        }

        Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            mResState.view.update(viewWidth, viewHeight);
            ViewGroup.LayoutParams layoutParams = mRootLayout.getLayoutParams();
            layoutParams.width = mResState.view.x;
            layoutParams.height = mResState.view.y;
            mRootLayout.setLayoutParams(layoutParams);
        });
    }

    public void ResizeTex(int texWidth, int texHeight) {
        if (mViewToBufferRenderer != null) {
            mResState.tex.update(texWidth, texHeight);
            mViewToBufferRenderer.setTextureResolution(mResState.tex.x, mResState.tex.y);
            mViewToBufferRenderer.requestResizeTex();
        }
    }

    public void ResizeView(int viewWidth, int viewHeight) {
        if (mViewToBufferRenderer != null) mViewToBufferRenderer.disable();

        Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            mResState.view.update(viewWidth, viewHeight);
            ViewGroup.LayoutParams layoutParams = mRootLayout.getLayoutParams();
            layoutParams.width = mResState.view.x;
            layoutParams.height = mResState.view.y;
            mRootLayout.setLayoutParams(layoutParams);
        });
    }

    /**
     * Test
     */
    public void RenderContent2TmpSurface() {
        Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            SurfaceView surfaceView = new SurfaceView(a);
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    Log.e(TAG, "[RenderContent2TmpSurface] surfaceCreated");
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                    SetSurface(holder.getSurface(), width, height);
                    Log.e(TAG, "[RenderContent2TmpSurface] surfaceChanged");
                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                    RemoveSurface();
                    Log.e(TAG, "[RenderContent2TmpSurface] surfaceDestroyed");
                }
            });
            a.addContentView(surfaceView, new RelativeLayout.LayoutParams(mResState.view.x, mResState.view.y));

            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(1000 / mFps);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    surfaceView.postInvalidate();
                }
            }).start();
        });
    }
}