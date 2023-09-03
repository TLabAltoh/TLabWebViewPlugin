package com.self.viewtoglrendering;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ViewToGLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = ViewToGLRenderer.class.getSimpleName();

    // ------------------------------------------------------------------------------------------------
    // Default texture resolution
    //

    private static final int DEFAULT_TEXTURE_WIDTH = 512;
    private static final int DEFAULT_TEXTURE_HEIGHT = 512;

    public int mTextureWidth = DEFAULT_TEXTURE_WIDTH;
    public int mTextureHeight = DEFAULT_TEXTURE_HEIGHT;
    public int mWebWidth = DEFAULT_TEXTURE_WIDTH;
    public int mWebHeight = DEFAULT_TEXTURE_HEIGHT;

    // ------------------------------------------------------------------------------------------------
    // Draw aspect
    //

    private static final float DEFAULT_SCALE = 1;
    public float scaleX = DEFAULT_SCALE;
    public float scaleY = DEFAULT_SCALE;

    // ------------------------------------------------------------------------------------------------
    // Surface and surface texture
    //

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    private int[] mGlSurfaceTexture = new int[1];
    private Canvas mSurfaceCanvas;

    // ------------------------------------------------------------------------------------------------
    // Texture capture
    //

    private TextureCapture textureCapture;
    private byte[] mCaptureData;

    // ------------------------------------------------------------------------------------------------
    // Surface
    //

    public void createSurface(int webWidth, int webHeight) {
        Log.i("libwebview", "libwebview---createSurface: create surface start");
        releaseSurface();
        Log.i("libwebview", "libwebview---createSurface: check point 0");

        // When used with Unity VR, commenting out this section made it work.
        // if (mGlSurfaceTexture[0] <= 0) return;

        Log.i("libwebview", "libwebview---createSurface: check point 1");

        //attach the texture to a surface.
        //It's a clue class for rendering an android view to gl level
        mSurfaceTexture = new SurfaceTexture(mGlSurfaceTexture[0]);
        mSurfaceTexture.setDefaultBufferSize(webWidth, webHeight);
        mSurface = new Surface(mSurfaceTexture);

        Log.i("libwebview", "libwebview---createSurface: check point 2");

        if (textureCapture == null) return;
        textureCapture.onInputSizeChanged(mTextureWidth, mTextureHeight);
        Log.i("libwebview", "libwebview---createSurface: create surface finish");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        final String extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS);
        Log.d(TAG, extensions);
        if (textureCapture != null) {
            textureCapture.init();
        }
        Log.i("libWebView", "libwebview---onSurfaceCreated: function called");
    }

    public void releaseSurface() {
        if(mSurface != null) mSurface.release();
        if(mSurfaceTexture != null) mSurfaceTexture.release();
        mSurface = null;
        mSurfaceTexture = null;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i("libwebview", "libwebview---onSurfaceChanged: surface change start");
        createSurface(width, height);
        Log.i("libwebview", "libwebview---onSurfaceChanged: surface change finish");
    }

    // ------------------------------------------------------------------------------------------------
    // Surface texture create
    //

    public void createTexture() {
        Log.i("tlabwebview", "create texture start");
        // Generate the texture to where android view will be rendered
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glGenTextures(1, mGlSurfaceTexture, 0);
        checkGlError("Texture generate");

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mGlSurfaceTexture[0]);
        checkGlError("Texture bind");

        // GLES20 --> GLES30

        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        Log.i("tlabwebview", "finish create texture");
    }

    // ------------------------------------------------------------------------------------------------
    // Draw frame (Call from texture capture)
    //

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            //Log.i("tlabwebview", "surface image update start");

            // update texture
            mSurfaceTexture.updateTexImage();

            if (textureCapture == null) return;

            textureCapture.onDrawFrame(getGLSurfaceTexture(), true);
            mCaptureData = textureCapture.getGLFboBuffer();

            //Log.i("tlabwebview", "surface image updated");
        }
    }

    public Canvas onDrawViewBegin() {
        mSurfaceCanvas = null;
        if (mSurface != null) {
            try {
                // mSurfaceCanvas = mSurface.lockCanvas(null);
                // https://learn.microsoft.com/en-us/dotnet/api/android.views.surface.lockhardwarecanvas?view=xamarin-android-sdk-13
                mSurfaceCanvas = mSurface.lockHardwareCanvas();
            }catch (Exception e){
                Log.e(TAG, "error while rendering view to gl: " + e);
            }
        }
        return mSurfaceCanvas;
    }

    public void onDrawViewEnd() {
        if(mSurfaceCanvas != null) mSurface.unlockCanvasAndPost(mSurfaceCanvas);
        mSurfaceCanvas = null;
    }

    public void checkGlError(String op) {
        int error;

        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR)
            Log.e(TAG, op + ": glError " + GLUtils.getEGLErrorString(error));
    }

    // ------------------------------------------------------------------------------------------------
    // Get surface texture
    //

    private int getGLSurfaceTexture(){
        return mGlSurfaceTexture[0];
    }

    // ------------------------------------------------------------------------------------------------
    // Get texture data
    //

    public byte[] getTexturePixels() {
        return mCaptureData;
    }

    // ------------------------------------------------------------------------------------------------
    // Texture resolution
    //

    public void SetTextureResolution(int textureWidth, int textureHeight) {
        mTextureWidth = textureWidth;
        mTextureHeight = textureHeight;
        ReCalcScale();
    }

    // ------------------------------------------------------------------------------------------------
    // Web resolution
    //

    public void SetWebResolution(int webWidth, int webHeight) {
        mWebWidth = webWidth;
        mWebHeight = webHeight;
        ReCalcScale();
    }

    // ------------------------------------------------------------------------------------------------
    // Re calc aspect
    //

    private void ReCalcScale() {
        scaleX = (float)mTextureWidth / (float)mWebWidth;
        scaleY = (float)mTextureHeight / (float)mWebHeight;
    }

    // ------------------------------------------------------------------------------------------------
    // Create texture capture
    //

    public void createTextureCapture(Context context, int vs, int fs) {
        textureCapture = new TextureCapture();
        textureCapture.flipY();
        textureCapture.loadSamplerShaderProg(context, vs, fs);
    }
}