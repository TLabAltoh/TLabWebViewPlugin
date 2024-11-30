package com.robot9.shared;

import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class SharedTexture {

    private static final String TAG = "SharedTexture";
    private static final int EGL_NO_NATIVE_FENCE_FD_ANDROID = -1;
    private static boolean available = false;

    static {
        //Log.i(TAG, "[sharedtex-jni] load shared texture plugin");
        System.loadLibrary("shared-texture");
    }

    static {
        try {
            available = available();
        } catch (Throwable t) {
            Log.e(TAG, "SharedTexture check available error: " + t);
        }

        Log.d(TAG, "SharedTexture available: " + available);
        if (!available) {
            Log.e(TAG, "SharedTexture not available");
        }
    }

    private boolean mIsVulkan = false;
    private long mNativeContext = 0;
    private HardwareBuffer mBuffer = null;

    public SharedTexture(HardwareBuffer buffer, boolean isVulkan) {
        Log.d(TAG, "create SharedTexture from buffer");
        mNativeContext = createFromBuffer(buffer, isVulkan);
        mBuffer = buffer;
        mIsVulkan = isVulkan;
    }

    public SharedTexture(int width, int height, boolean isVulkan) {
        Log.d(TAG, "create new SharedTexture:(" + width + "," + height + ")");
        mNativeContext = create(width, height, isVulkan);
        mIsVulkan = isVulkan;
    }

    public static boolean isAvailable() {
        return available;
    }

    public HardwareBuffer getHardwareBuffer() {
        if (mNativeContext != 0) {
            if (mBuffer != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mBuffer.close();
                }
                mBuffer = null;
            }
            mBuffer = getBuffer(mNativeContext);
            return mBuffer;
        }
        return null;
    }

    public void downloadBuffer() {
        if (mNativeContext != 0) {
            downloadBuffer(mNativeContext);
        }
    }

    public int getBufferWidth() {
        if (mNativeContext != 0) {
            return getWidth(mNativeContext);
        }
        return 0;
    }

    public int getBufferHeight() {
        if (mNativeContext != 0) {
            return getHeight(mNativeContext);
        }
        return 0;
    }

    public long getPlatformTexture() {
        if (mNativeContext != 0) {
            return getPlatformTexture(mNativeContext);
        }
        return 0;
    }

    public void setUnityTexture(long unityTexID) {
        if (mNativeContext != 0) {
            setUnityTexture(mNativeContext, unityTexID);
        }
    }

    public void updateUnityTexture() {
        if (mNativeContext != 0) {
            updateUnityTexture(mNativeContext);
        }
    }

    public boolean isVulkan() {
        return mIsVulkan;
    }

    //@TODO:
//    public ParcelFileDescriptor createFence() {
//        int fd = createEGLFence();
//        if (fd != EGL_NO_NATIVE_FENCE_FD_ANDROID) {
//            return ParcelFileDescriptor.adoptFd(fd);
//        }
//        return null;
//    }
//
//    public boolean waitFence(ParcelFileDescriptor pfd) {
//        if (pfd != null) {
//            int fd = pfd.detachFd();
//            if (fd != EGL_NO_NATIVE_FENCE_FD_ANDROID) {
//                return waitEGLFence(fd);
//            }
//        }
//
//        return false;
//    }

    public void release() {
        if (mNativeContext != 0) {
            Log.d(TAG, "destroy");
            if (mBuffer != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mBuffer.close();
                }
                mBuffer = null;
            }
            destroy(mNativeContext);
            mNativeContext = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    private static native boolean available();

//    private static native int createEGLFence();
//
//    private static native boolean waitEGLFence(int fenceFd);

    private native long create(int width, int height, boolean isVulkan);

    private native long createFromBuffer(HardwareBuffer buffer, boolean isVulkan);

    private native HardwareBuffer getBuffer(long ctx);

    private native void downloadBuffer(long ctx);

    private native int getWidth(long ctx);

    private native int getHeight(long ctx);

    private native long getPlatformTexture(long ctx);

    private native void setUnityTexture(long ctx, long unityTexID);

    private native void updateUnityTexture(long ctx);

    private native void destroy(long ctx);
}
