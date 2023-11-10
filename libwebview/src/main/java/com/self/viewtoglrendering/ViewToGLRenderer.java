package com.self.viewtoglrendering;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGL15;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ViewToGLRenderer implements GLSurfaceView.Renderer {

    // EGL shared_context
    // https://eaglesakura.hatenablog.com/entry/2013/12/28/235121

    private static final String TAG = "libewbview";

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
    private int mTexturePtr;

    // ------------------------------------------------------------------------------------------------
    // EGL context util
    //

    // Saved eglcontext from "unitumain"
    private EGLContext mSavedContext = EGL14.EGL_NO_CONTEXT;
    private EGLDisplay mSavedDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLSurface mSavedDSurface = EGL14.EGL_NO_SURFACE;
    private EGLSurface mSavedRSurface = EGL14.EGL_NO_SURFACE;

    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLSurface mEGLDSurface = EGL14.EGL_NO_SURFACE;
    private EGLSurface mEGLRSurface = EGL14.EGL_NO_SURFACE;

    public void saveEGLContext(EGLContext context, EGLDisplay display, EGLSurface dSurface, EGLSurface rSurface){
        mSavedContext = context;
        mSavedDisplay = display;
        mSavedDSurface = dSurface;
        mSavedRSurface = rSurface;
    }

    private void checkEGLContext(){
        Log.i(TAG, "thread name: " + Thread.currentThread().getName());
        Log.i(TAG, "context class name: " + mEGLContext.getClass().getName());
        Log.i(TAG, "same context: " + (mEGLContext == EGL14.eglGetCurrentContext()));

        if(mSavedContext == EGL14.EGL_NO_CONTEXT)
            Log.i(TAG, "check exist but egl context is not created");
        else
            Log.i(TAG, "check exist and egl context created !!");
    }

    /**
     * https://stackoverflow.com/questions/40787339/unity3d-render-opengl-fbo-to-texture-in-android-java
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
     */
    private void eglSetup() {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.d(TAG, "unable to get EGL14 display");
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            Log.d(TAG, "unable to initialize EGL14");
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Configure EGL for recording and OpenGL ES 2.0.
        // TODO: siwtch 2.0 --> 3.0
        int EGL_RECORDABLE_ANDROID = 0x3142;
        int[] attribList;
        attribList = new int[]{
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL15.EGL_OPENGL_ES3_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
        };
        android.opengl.EGLConfig[] configs = new android.opengl.EGLConfig[1];
        int[] numConfigs = new int[1];

        EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0);
        checkEglError("eglCreateContext RGB888+recordable ES2");

        // Configure context for OpenGL ES 2.0.
        // TODO: siwtch 2.0 --> 3.0
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
        };

        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], mSavedContext, attrib_list, 0);
        checkEglError("eglCreateContext");

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = { EGL14.EGL_NONE };
        mEGLDSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        mEGLRSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
        checkEglError("eglCreateWindowSurface");

        EGL14.eglMakeCurrent(mEGLDisplay, mEGLDSurface, mEGLRSurface, mEGLContext);
        checkEglError("eglMakeCurrent");

        Log.i(TAG, "make eglcontext done !");
    }

    /**
     * Checks for EGL errors. Throws an exception if one is found.
     */
    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            Log.d(TAG, msg + ": EGL error: 0x" + Integer.toHexString(error));
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Surface
    //

    public void createSurface(int webWidth, int webHeight) {
        //setEGLContext();
        //eglSetup();

        Log.i(TAG, "createSurface: create surface start");
        releaseSurface();
        Log.i(TAG, "createSurface: check point 0");

        // When used with Unity VR, commenting out this section made it work.
        // if (mGlSurfaceTexture[0] <= 0) return;

        Log.i(TAG, "createSurface: check point 1");

        //attach the texture to a surface.
        //It's a clue class for rendering an android view to gl level
        mSurfaceTexture = new SurfaceTexture(mGlSurfaceTexture[0]);
        mSurfaceTexture.setDefaultBufferSize(webWidth, webHeight);
        mSurface = new Surface(mSurfaceTexture);

        Log.i(TAG, "createSurface: check point 2");

        if (textureCapture == null) return;
        textureCapture.onInputSizeChanged(mTextureWidth, mTextureHeight);
        Log.i(TAG, "createSurface: create surface finish");

        checkEGLContext();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        final String extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS);
        Log.d(TAG, extensions);
        eglSetup();
        checkEGLContext();
        if (textureCapture != null) textureCapture.init();
        Log.i(TAG, "onSurfaceCreated: function called");
    }

    public void releaseSurface() {
        if(mSurface != null) mSurface.release();
        if(mSurfaceTexture != null) mSurfaceTexture.release();
        mSurface = null;
        mSurfaceTexture = null;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged: surface change start");
        createSurface(width, height);
        createTexture();
        Log.i(TAG, "onSurfaceChanged: surface change finish");
    }

    // ------------------------------------------------------------------------------------------------
    // Surface texture create
    //

    private void createTexture() {
        Log.i(TAG, "create texture start");
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
        Log.i(TAG, "finish create texture");
    }

    // ------------------------------------------------------------------------------------------------
    // Draw frame (Call from texture capture)
    //

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            //Log.i(TAG, "surface image update start");

            // update texture
            mSurfaceTexture.updateTexImage();

            if (textureCapture == null) return;

            mTexturePtr = textureCapture.onDrawFrame(getGLSurfaceTexture());

            //Log.i(TAG, "surface image updated");
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

    public int getTexturePtr(){
        return mTexturePtr;
    }

    public byte[] getTexturePixels() {
        mCaptureData = textureCapture.getGLFboBuffer();
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

    public void createTextureCapture(Context context, int texId, int vs, int fs) {
        textureCapture = new TextureCapture();
        textureCapture.flipY();
        textureCapture.setTexId(texId);
        textureCapture.loadSamplerShaderProg(context, vs, fs);
    }
}