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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ViewToBufferRenderer implements GLSurfaceView.Renderer {

    protected static final String TAG = "libwebview";

    protected static final int DEFAULT_TEXTURE_WIDTH = 512;
    protected static final int DEFAULT_TEXTURE_HEIGHT = 512;

    protected static final int SIZEOF_FLOAT = Float.SIZE / 8;

    protected EglCore mEglCore;

    protected int mTextureWidth = DEFAULT_TEXTURE_WIDTH;
    protected int mTextureHeight = DEFAULT_TEXTURE_HEIGHT;

    protected int mWebWidth = DEFAULT_TEXTURE_WIDTH;
    protected int mWebHeight = DEFAULT_TEXTURE_HEIGHT;

    protected boolean mForceResizeTex = false;
    protected boolean mForceResizeWeb = false;

    protected SurfaceTexture mSurfaceTexture;

    protected int[] mSurfaceTextureID;

    protected Surface mSurface;

    protected Canvas mSurfaceCanvas;

    /**
     *
     */

    protected int mGLSamplerProgram;
    protected int mGLSamplerPositionID;
    protected int mGLSamplerTexID;
    protected int mGLSamplerTexCoordID;

    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;

    protected int[] mGLCubeID;
    protected int[] mGLTexCoordID;

    protected boolean mInitialized;

    /**
     *
     */
    public void initSamplerShader() {
        String vertexShader = "attribute vec4 position;\n" +
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

        String fragmentShader = "#extension GL_OES_EGL_image_external : require\n" +
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

        mGLSamplerProgram = GlUtil.createProgram(vertexShader, fragmentShader);

        if (mGLSamplerProgram == 0) {
            Log.e(TAG, "Load sampler shader filed");
            return;
        }

        mGLSamplerTexID = GLES30.glGetUniformLocation(mGLSamplerProgram, "inputTex");
        mGLSamplerPositionID = GLES30.glGetAttribLocation(mGLSamplerProgram, "position");
        mGLSamplerTexCoordID = GLES30.glGetAttribLocation(mGLSamplerProgram, "inputTexCoord");
    }

    /**
     *
     */
    protected void destroyBuffer() {

    }

    /**
     *
     */
    protected void initBuffer() {

    }

    /**
     *
     */
    public void CopySurfaceTextureToBuffer() {

    }

    /**
     *
     */
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

        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureID[0]);
        mSurfaceTexture.setDefaultBufferSize(width, height);
        mSurface = new Surface(mSurfaceTexture);
    }

    protected void destroyVbo() {
        if (mGLCubeID != null) {
            GLES30.glDeleteBuffers(1, mGLCubeID, 0);
            mGLCubeID = null;
        }
        if (mGLTexCoordID != null) {
            GLES30.glDeleteBuffers(1, mGLTexCoordID, 0);
            mGLTexCoordID = null;
        }
    }

    protected void initVbo() {
        final float[] VEX_CUBE = {
                -1.0f, 1.0f, // Bottom left.
                1.0f, 1.0f, // Bottom right.
                -1.0f, -1.0f, // Top left.
                1.0f, -1.0f, // Top right.
        };

        final float[] TEX_COORD = {
                0.0f, 0.0f, // Bottom left.
                1.0f, 0.0f, // Bottom right.
                0.0f, 1.0f, // Top left.
                1.0f, 1.0f // Top right.
        };

        mGLCubeBuffer = ByteBuffer.allocateDirect(VEX_CUBE.length * SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLCubeBuffer.put(VEX_CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEX_COORD.length * SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTextureBuffer.put(TEX_COORD).position(0);

        mGLCubeID = new int[1];
        mGLTexCoordID = new int[1];

        GLES30.glGenBuffers(1, mGLCubeID, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGLCubeID[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mGLCubeBuffer.capacity() * SIZEOF_FLOAT, mGLCubeBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glGenBuffers(1, mGLTexCoordID, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGLTexCoordID[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mGLTextureBuffer.capacity() * SIZEOF_FLOAT, mGLTextureBuffer, GLES30.GL_STATIC_DRAW);
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

        if (mInitialized) {
            return;
        }

        init();
    }


    /**
     * @param gl     the GL interface. Use <code>instanceof</code> to
     *               test if the interface supports GL11 or higher interfaces.
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWebWidth = width;
        mWebHeight = height;

        // I need to destroy both the surface and the FBO at once before
        // creating them. (At first I call destroy fbo after create
        // surface, but then both oes and fbo are not released propary).

        releaseSurfaceAndSurfaceTexture();
        destroyBuffer();

        createSurfaceAndSurfaceTexture(mWebWidth, mWebHeight);
        initBuffer();
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

    /**
     *
     */
    public void onDrawViewEnd() {
        if (mSurfaceCanvas != null) {
            mSurface.unlockCanvasAndPost(mSurfaceCanvas);
        }

        mSurfaceCanvas = null;
    }

    /**
     * @param textureWidth
     * @param textureHeight
     */
    public void SetTextureResolution(int textureWidth, int textureHeight) {
        mTextureWidth = textureWidth;
        mTextureHeight = textureHeight;
    }

    public void requestResizeTex() {
        mForceResizeTex = true;
    }

    public void requestResizeWeb() {
        mForceResizeWeb = true;
    }

    /**
     *
     */
    public final void destroy() {
        mInitialized = false;

        destroyBuffer();
        destroyVbo();
        GLES30.glDeleteProgram(mGLSamplerProgram);

        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }
}
