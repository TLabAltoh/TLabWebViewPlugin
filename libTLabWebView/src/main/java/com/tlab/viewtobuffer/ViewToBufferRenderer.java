package com.tlab.viewtobuffer;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.android.grafika.gles.EglCore;
import com.android.grafika.gles.GlUtil;
import com.tlab.webkit.Common.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ViewToBufferRenderer implements GLSurfaceView.Renderer {

    public interface SurfaceEventListener {
        void onSurfaceChanged(Surface surface, int width, int height);
    }

    protected static final String TAG = "ViewToBufferRenderer";

    protected static final int DEFAULT_WIDTH = 512;
    protected static final int DEFAULT_HEIGHT = 512;

    protected static final int SIZEOF_FLOAT = Float.SIZE / 8;

    protected SurfaceEventListener mSurfaceEventListener;

    protected EglCore mEglCore;

    protected Vector2Int mTexSize = new Vector2Int(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    protected Vector2Int mViewSize = new Vector2Int(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    protected boolean mForceResizeTex = false;

    protected boolean mSurfaceEnabled = false;
    protected boolean mContentExists = false;

    protected SurfaceTexture mSurfaceTexture;

    protected int[] mSurfaceTextureID;

    protected Surface mSurface;

    protected Canvas mSurfaceCanvas;

    protected int mGlSamplerProgram;
    protected int mGlSamplerPositionID;
    protected int mGlSamplerTexID;
    protected int mGlSamplerTexCoordID;

    protected FloatBuffer mGlCubeBuffer;
    protected FloatBuffer mGlTextureBuffer;

    protected int[] mGlCubeID;
    protected int[] mGlTexCoordID;

    protected boolean mInitialized;

    protected boolean mFrameAvailable = false;

    public void initSamplerShader() {
        //@formatter:off
        String vertexShader =
                "attribute vec4 position;\n" +
                        "attribute vec4 inputTexCoord;\n" +
                        "\n" +
                        "varying vec2 texCoord;\n" +
                        "\n" +
                        "uniform mat4 textureTransform;\n" +
                        "\n" +
                        "void main() {\n" +
                        "    texCoord = inputTexCoord.xy;\n" +
                        "    gl_Position = position;\n" +
                        "}\n";

        String fragmentShader =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "\n" +
                        "precision mediump float;\n" +
                        "\n" +
                        "uniform samplerExternalOES inputTex;\n" +
                        "\n" +
                        "varying vec2 texCoord;\n" +
                        "\n" +
                        "void main() {\n" +
                        "    vec4 col = texture2D(inputTex, texCoord);\n" +
                        "    col = vec4(col.x, col.y, col.z, col.w);\n" +
                        "\n" +
                        "    // The EGL sharing method required gamma correction in the shaders.\n" +
                        "    col = pow(col, vec4(2.2));\n" +
                        "    gl_FragColor = col;\n" +
                        "}";
        //@formatter:on

        mGlSamplerProgram = GlUtil.createProgram(vertexShader, fragmentShader);

        if (mGlSamplerProgram == 0) {
            Log.e(TAG, "Load sampler shader filed");
            return;
        }

        mGlSamplerTexID = GLES30.glGetUniformLocation(mGlSamplerProgram, "inputTex");
        mGlSamplerPositionID = GLES30.glGetAttribLocation(mGlSamplerProgram, "position");
        mGlSamplerTexCoordID = GLES30.glGetAttribLocation(mGlSamplerProgram, "inputTexCoord");
    }

    protected void destroyBuffer() {

    }

    protected void initBuffer() {

    }

    public void CopySurfaceTextureToBuffer() {

    }

    public void releaseSurfaceAndSurfaceTexture() {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;

            //Log.i(TAG, "[VHWBR] release surface");
        }

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;

            //Log.i(TAG, "[VHWBR] release surface texture");
        }

        if (mSurfaceTextureID != null) {
            GLES30.glDeleteTextures(mSurfaceTextureID.length, mSurfaceTextureID, 0);

            //Log.i(TAG, "[VHWBR] release surface texture id: " + mSurfaceTextureID[0]);

            mSurfaceTextureID = null;
        }
    }

    public boolean contentExists() {
        return mContentExists;
    }

    /**
     * Generate the surface and surface texture to where android view will be rendered
     *
     * @param width  view's resolution x
     * @param height view's resolution y
     */
    public void createSurfaceAndSurfaceTexture(int width, int height) {
        mSurfaceTextureID = new int[1];

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glGenTextures(1, mSurfaceTextureID, 0);

        //Log.i(TAG, "[VHWBR] texture surface id: " + mSurfaceTextureID[0]);

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureID[0]);

        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        mFrameAvailable = false;

        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureID[0]);
        mSurfaceTexture.setDefaultBufferSize(width, height);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                synchronized (this) {
                    mFrameAvailable = true;
                }
            }
        });
        mSurface = new Surface(mSurfaceTexture);
    }

    protected void destroyVbo() {
        if (mGlCubeID != null) {
            GLES30.glDeleteBuffers(1, mGlCubeID, 0);
            mGlCubeID = null;
        }
        if (mGlTexCoordID != null) {
            GLES30.glDeleteBuffers(1, mGlTexCoordID, 0);
            mGlTexCoordID = null;
        }
    }

    protected void initVbo() {
        final float[] VEX_CUBE = {
                //@formatter:off
                -1.0f,  1.0f, // Bottom left.
                1.0f,  1.0f, // Bottom right.
                -1.0f, -1.0f, // Top left.
                1.0f, -1.0f, // Top right.
                //@formatter:on
        };

        final float[] TEX_COORD = {
                //@formatter:off
                0.0f,  0.0f, // Bottom left.
                1.0f,  0.0f, // Bottom right.
                0.0f,  1.0f, // Top left.
                1.0f,  1.0f, // Top right.
                //@formatter:on
        };

        mGlCubeBuffer = ByteBuffer.allocateDirect(VEX_CUBE.length * SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGlCubeBuffer.put(VEX_CUBE).position(0);

        mGlTextureBuffer = ByteBuffer.allocateDirect(TEX_COORD.length * SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGlTextureBuffer.put(TEX_COORD).position(0);

        mGlCubeID = new int[1];
        mGlTexCoordID = new int[1];

        GLES30.glGenBuffers(1, mGlCubeID, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGlCubeID[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mGlCubeBuffer.capacity() * SIZEOF_FLOAT, mGlCubeBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glGenBuffers(1, mGlTexCoordID, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGlTexCoordID[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mGlTextureBuffer.capacity() * SIZEOF_FLOAT, mGlTextureBuffer, GLES30.GL_STATIC_DRAW);
    }

    protected void init() {
        //Log.i(TAG, "[VHWBR] [init] pass 0 (start)");
        EGLContext context = EGL14.eglGetCurrentContext();

        mEglCore = new EglCore(context, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

        initVbo();

        initSamplerShader();

        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        mInitialized = true;

        //Log.i(TAG, "[VHWBR] [init] pass 0 (end)");
    }

    /**
     * @param gl     the GL interface. Use <code>instanceof</code> to
     *               test if the interface supports GL11 or higher interfaces.
     * @param config the EGLConfig of the created surface. Can be used
     *               to create matching pbuffers.
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated (gl, config)");
        if (mInitialized) return;
        init();
    }

    public void setSurfaceCallback(SurfaceEventListener callback) {
        mSurfaceEventListener = callback;
    }

    /**
     * @param gl     the GL interface. Use <code>instanceof</code> to
     *               test if the interface supports GL11 or higher interfaces.
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged (gl, width, height) " + width + ", " + height);

        // In case of GeckoView, onSurfaceChanged was called too many times after onResume and it caused HardwareBuffer's null pointer reference error.
        // But if I avoid calling OffscreenGeckoView.mDisplay.onSurfaceChanged (mSurfaceCallback) over twice, onSurfaceChange also called only
        // twice and it could avoid HardwareBuffer's null reference error. I don't know if this behaviour is only GeckoView's or not.
        // This has been tested in both cases of onPause/onResume and switching screen orientation and it has not caused any errors/crashes.
        if (mSurfaceEnabled) return;
        mSurfaceEnabled = true;

        mViewSize.update(width, height);

        // I need to destroy both the surface and the FBO at once before
        // creating them. (At first I call destroy fbo after create
        // surface, but then both oes and fbo are not released properly).

        releaseSurfaceAndSurfaceTexture();
        destroyBuffer();

        createSurfaceAndSurfaceTexture(mViewSize.x, mViewSize.y);
        initBuffer();

        if (mSurfaceEventListener != null)
            mSurfaceEventListener.onSurfaceChanged(mSurface, width, height);
    }

    /**
     * @param gl the GL interface. Use <code>instanceof</code> to
     *           test if the interface supports GL11 or higher interfaces.
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (mForceResizeTex) {
                destroyBuffer();
                initBuffer();

                mForceResizeTex = false;
            }

            mSurfaceTexture.updateTexImage();
            CopySurfaceTextureToBuffer();
            mContentExists = mSurfaceEnabled && mFrameAvailable;
        }
    }

    /**
     * <a href="https://learn.microsoft.com/en-us/dotnet/api/android.views.surface.lockhardwarecanvas?view=xamarin-android-sdk-13">...</a>
     *
     * @return hardware canvas (this is the result of hardware acceleration).
     */
    public Canvas onDrawViewBegin() {
        mSurfaceCanvas = null;

        if (mSurface != null) {
            try {
                mSurfaceCanvas = mSurface.lockHardwareCanvas();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

        return mSurfaceCanvas;
    }

    public void onDrawViewEnd() {
        if (mSurfaceCanvas != null) mSurface.unlockCanvasAndPost(mSurfaceCanvas);

        mSurfaceCanvas = null;
    }

    public void setTextureResolution(int texWidth, int texHeight) {
        mTexSize.update(texWidth, texHeight);
    }

    public void disable() {
        synchronized (this) {
            mContentExists = false;
            mFrameAvailable = false;
            mSurfaceEnabled = false;
        }
    }

    public void requestResizeTex() {
        synchronized (this) {
            mForceResizeTex = true;
        }
    }

    public Surface getSurface() {
        return mSurface;
    }

    public Vector2Int getTexSize() {
        return mTexSize;
    }

    public Vector2Int getViewSize() {
        return mViewSize;
    }

    public final void destroy() {
        disable();

        mInitialized = false;

        destroyBuffer();
        destroyVbo();
        GLES30.glDeleteProgram(mGlSamplerProgram);

        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }
}
