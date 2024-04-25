package com.tlab.viewtohardwarebuffer;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.hardware.HardwareBuffer;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.android.grafika.gles.EglCore;
import com.android.grafika.gles.GlUtil;
import com.robot9.shared.SharedTexture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ViewToHWBRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "libwebview";

    private static final int DEFAULT_TEXTURE_WIDTH = 512;
    private static final int DEFAULT_TEXTURE_HEIGHT = 512;

    private static final int SIZEOF_FLOAT = Float.SIZE / 8;

    private EglCore mEglCore;

    private int mTextureWidth = DEFAULT_TEXTURE_WIDTH;
    private int mTextureHeight = DEFAULT_TEXTURE_HEIGHT;

    private boolean mForceResize = false;

    private SurfaceTexture mSurfaceTexture;

    private final int[] mSurfaceTextureID = new int[1];

    private Surface mSurface;

    private Canvas mSurfaceCanvas;

    /**
     *
     *
     */

    private HardwareBuffer mSharedBuffer;

    private SharedTexture mSharedTexture;

    /**
     *
     *
     */

    private int mGLSamplerProgram;
    private int mGLSamplerPositionID;
    private int mGLSamplerTexID;
    private int mGLSamplerTexCoordID;

    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;

    private int[] mGLCubeID;
    private int[] mGLTexCoordID;

    /**
     *
     * FBO (pixel data on GPU) for EGL_IMAGE_KHR (Hardware Buffer)
     */

    private int[] mHWBFboID;
    private int[] mHWBFboTexID;

    private boolean mInitialized;

    /**
     *
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

        if (mGLSamplerProgram == 0){
            Log.e(TAG, "Load sampler shader filed");
            return;
        }

        mGLSamplerTexID = GLES30.glGetUniformLocation(mGLSamplerProgram, "inputTex");
        mGLSamplerPositionID = GLES30.glGetAttribLocation(mGLSamplerProgram, "position");
        mGLSamplerTexCoordID = GLES30.glGetAttribLocation(mGLSamplerProgram,"inputTexCoord");
    }

    /**
     *
     *
     */
    private void destroyHWBFboTexture() {
        if (mHWBFboTexID != null) {
            GLES30.glDeleteTextures(mHWBFboTexID.length, mHWBFboTexID, 0);
            mHWBFboTexID = null;
        }

        if (mHWBFboID != null) {
            GLES30.glDeleteFramebuffers(mHWBFboID.length, mHWBFboID, 0);
            mHWBFboID = null;
        }

        if(mSharedTexture != null){
            mSharedTexture.release();
            mSharedTexture = null;
        }
    }

    /**
     *
     *
     */
    private void initHWBFboTexture() {
        destroyHWBFboTexture();

        mHWBFboID = new int[1];
        mHWBFboTexID = new int[1];

        GLES30.glGenTextures(1, mHWBFboTexID, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mHWBFboTexID[0]);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        mSharedTexture = new SharedTexture(mTextureWidth, mTextureHeight);
        mSharedBuffer = mSharedTexture.getHardwareBuffer();

        assert mSharedTexture != null;
        boolean result = mSharedTexture.bindTexture(mHWBFboTexID[0]);

        GLES30.glGenFramebuffers(1, mHWBFboID, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mHWBFboID[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mHWBFboTexID[0], 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    /**
     *
     *
     */
    public void releaseSurfaceAndSurfaceTexture() {
        if(mSurface != null) {
            mSurface.release();
            mSurface = null;
        }

        if(mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }

    /**
     * Generate the surface and surface texture to where android view will be rendered
     * @param width view's resolution x
     * @param height view's resolution y
     */
    public void createSurfaceAndSurfaceTexture(int width, int height) {
        releaseSurfaceAndSurfaceTexture();

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glGenTextures(1, mSurfaceTextureID, 0);

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureID[0]);

        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureID[0]);
        mSurfaceTexture.setDefaultBufferSize(width, height);
        mSurface = new Surface(mSurfaceTexture);
    }

    private void destroyVbo() {
        if (mGLCubeID != null) {
            GLES30.glDeleteBuffers(1, mGLCubeID, 0);
            mGLCubeID = null;
        }
        if (mGLTexCoordID != null) {
            GLES30.glDeleteBuffers(1, mGLTexCoordID, 0);
            mGLTexCoordID = null;
        }
    }

    private void initVbo() {
        final float[] VEX_CUBE = {
                -1.0f,  1.0f, // Bottom left.
                1.0f,  1.0f, // Bottom right.
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

    private void init() {
        EGLContext context = EGL14.eglGetCurrentContext();

        mEglCore = new EglCore(context, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

        initVbo();

        initSamplerShader();

        GLES30.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );

        mInitialized = true;
    }

    /**
     *
     * @param gl the GL interface. Use <code>instanceof</code> to
     * test if the interface supports GL11 or higher interfaces.
     * @param config the EGLConfig of the created surface. Can be used
     * to create matching pbuffers.
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        if (mInitialized) {
            return;
        }

        init();
    }

    /**
     *
     * @param gl the GL interface. Use <code>instanceof</code> to
     * test if the interface supports GL11 or higher interfaces.
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        createSurfaceAndSurfaceTexture(width, height);
        initHWBFboTexture();
    }

    public void CopySurfaceTextureToHWB() {
        if (!mInitialized || mHWBFboID == null) {
            return;
        }

        GLES30.glGetError();
        GLES30.glUseProgram(mGLSamplerProgram);

        GLES30.glViewport(0, 0, mTextureWidth, mTextureHeight);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureID[0]);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGLCubeID[0]);
        GLES30.glVertexAttribPointer(mGLSamplerPositionID, 2, GLES30.GL_FLOAT, false, 4 * 2, 0);
        GLES30.glEnableVertexAttribArray(mGLSamplerPositionID);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGLTexCoordID[0]);
        GLES30.glVertexAttribPointer(mGLSamplerTexCoordID, 2, GLES30.GL_FLOAT, false, 4 * 2, 0);
        GLES30.glEnableVertexAttribArray(mGLSamplerTexCoordID);

        GLES30.glBindBuffer( GLES30.GL_ARRAY_BUFFER, 0 );

        GLES30.glUniform1i(mGLSamplerTexID, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mHWBFboID[0]);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glFlush();

        GLES30.glDisableVertexAttribArray(mGLSamplerPositionID);
        GLES30.glDisableVertexAttribArray(mGLSamplerTexCoordID);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    /**
     *
     * @param gl the GL interface. Use <code>instanceof</code> to
     * test if the interface supports GL11 or higher interfaces.
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (mForceResize) {
                initHWBFboTexture();

                mForceResize = false;
            }

            mSurfaceTexture.updateTexImage();
            CopySurfaceTextureToHWB();
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
            }catch (Exception e){
                Log.e(TAG, e.toString());
            }
        }

        return mSurfaceCanvas;
    }

    /**
     *
     */
    public void onDrawViewEnd() {
        if(mSurfaceCanvas != null) {
            mSurface.unlockCanvasAndPost(mSurfaceCanvas);
        }

        mSurfaceCanvas = null;
    }

    /**
     *
     * @return
     */
    public HardwareBuffer getHardwareBuffer() {
        return mSharedBuffer;
    }

    /**
     *
     * @param textureWidth
     * @param textureHeight
     */
    public void SetTextureResolution(int textureWidth, int textureHeight) {
        mTextureWidth = textureWidth;
        mTextureHeight = textureHeight;
    }

    public void requestResize() {
        mForceResize = true;
    }

    /**
     *
     *
     */
    public final void destroy() {
        mInitialized = false;

        destroyHWBFboTexture();
        destroyVbo();
        GLES30.glDeleteProgram(mGLSamplerProgram);

        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }
}