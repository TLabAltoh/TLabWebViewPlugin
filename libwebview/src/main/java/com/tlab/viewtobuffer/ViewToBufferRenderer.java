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

    protected EglCore m_eglCore;

    protected int m_textureWidth = DEFAULT_TEXTURE_WIDTH;
    protected int m_textureHeight = DEFAULT_TEXTURE_HEIGHT;

    protected int m_webWidth = DEFAULT_TEXTURE_WIDTH;
    protected int m_webHeight = DEFAULT_TEXTURE_HEIGHT;

    protected boolean m_forceResizeTex = false;
    protected boolean m_forceResizeWeb = false;

    protected boolean m_surfaceDestroyed = false;
    protected boolean m_contentExists = false;

    protected SurfaceTexture m_surfaceTexture;

    protected int[] m_surfaceTextureID;

    protected Surface m_surface;

    protected Canvas m_surfaceCanvas;

    /**
     *
     */

    protected int m_glSamplerProgram;
    protected int m_glSamplerPositionID;
    protected int m_glSamplerTexID;
    protected int m_glSamplerTexCoordID;

    protected FloatBuffer m_glCubeBuffer;
    protected FloatBuffer m_glTextureBuffer;

    protected int[] m_glCubeID;
    protected int[] m_glTexCoordID;

    protected boolean m_initialized;

    protected boolean m_frameAvailable = false;

    /**
     *
     */
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

        m_glSamplerProgram = GlUtil.createProgram(vertexShader, fragmentShader);

        if (m_glSamplerProgram == 0) {
            Log.e(TAG, "Load sampler shader filed");
            return;
        }

        m_glSamplerTexID = GLES30.glGetUniformLocation(m_glSamplerProgram, "inputTex");
        m_glSamplerPositionID = GLES30.glGetAttribLocation(m_glSamplerProgram, "position");
        m_glSamplerTexCoordID = GLES30.glGetAttribLocation(m_glSamplerProgram, "inputTexCoord");
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
        if (m_surface != null) {
            m_surface.release();
            m_surface = null;

            //Log.i(TAG, "[VHWBR] release surface");
        }

        if (m_surfaceTexture != null) {
            m_surfaceTexture.release();
            m_surfaceTexture = null;

            //Log.i(TAG, "[VHWBR] release surface texture");
        }

        if (m_surfaceTextureID != null) {
            GLES30.glDeleteTextures(m_surfaceTextureID.length, m_surfaceTextureID, 0);

            //Log.i(TAG, "[VHWBR] release surface texture id: " + m_surfaceTextureID[0]);

            m_surfaceTextureID = null;
        }
    }

    public boolean contentExists() {
        return m_contentExists;
    }

    /**
     * Generate the surface and surface texture to where android view will be rendered
     *
     * @param width  view's resolution x
     * @param height view's resolution y
     */
    public void createSurfaceAndSurfaceTexture(int width, int height) {
        m_surfaceTextureID = new int[1];

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glGenTextures(1, m_surfaceTextureID, 0);

        //Log.i(TAG, "[VHWBR] texture surface id: " + m_surfaceTextureID[0]);

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, m_surfaceTextureID[0]);

        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        m_frameAvailable = false;

        m_surfaceTexture = new SurfaceTexture(m_surfaceTextureID[0]);
        m_surfaceTexture.setDefaultBufferSize(width, height);
        m_surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                synchronized (this) {
                    m_frameAvailable = true;
                }
            }
        });
        m_surface = new Surface(m_surfaceTexture);
    }

    protected void destroyVbo() {
        if (m_glCubeID != null) {
            GLES30.glDeleteBuffers(1, m_glCubeID, 0);
            m_glCubeID = null;
        }
        if (m_glTexCoordID != null) {
            GLES30.glDeleteBuffers(1, m_glTexCoordID, 0);
            m_glTexCoordID = null;
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

        m_glCubeBuffer = ByteBuffer.allocateDirect(VEX_CUBE.length * SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_glCubeBuffer.put(VEX_CUBE).position(0);

        m_glTextureBuffer = ByteBuffer.allocateDirect(TEX_COORD.length * SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_glTextureBuffer.put(TEX_COORD).position(0);

        m_glCubeID = new int[1];
        m_glTexCoordID = new int[1];

        GLES30.glGenBuffers(1, m_glCubeID, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, m_glCubeID[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, m_glCubeBuffer.capacity() * SIZEOF_FLOAT, m_glCubeBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glGenBuffers(1, m_glTexCoordID, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, m_glTexCoordID[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, m_glTextureBuffer.capacity() * SIZEOF_FLOAT, m_glTextureBuffer, GLES30.GL_STATIC_DRAW);
    }

    protected void init() {
        //Log.i(TAG, "[VHWBR] [init] pass 0 (start)");
        EGLContext context = EGL14.eglGetCurrentContext();

        m_eglCore = new EglCore(context, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

        initVbo();

        initSamplerShader();

        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        m_initialized = true;

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
        if (m_initialized) {
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
        m_surfaceDestroyed = false;
        m_webWidth = width;
        m_webHeight = height;

        // I need to destroy both the surface and the FBO at once before
        // creating them. (At first I call destroy fbo after create
        // surface, but then both oes and fbo are not released propary).

        releaseSurfaceAndSurfaceTexture();
        destroyBuffer();

        createSurfaceAndSurfaceTexture(m_webWidth, m_webHeight);
        initBuffer();
    }

    /**
     * @param gl the GL interface. Use <code>instanceof</code> to
     *           test if the interface supports GL11 or higher interfaces.
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (m_forceResizeTex) {
                destroyBuffer();
                initBuffer();

                m_forceResizeTex = false;
            }

            m_surfaceTexture.updateTexImage();
            CopySurfaceTextureToBuffer();
            m_contentExists = !m_surfaceDestroyed && m_frameAvailable;
        }
    }

    /**
     * <a href="https://learn.microsoft.com/en-us/dotnet/api/android.views.surface.lockhardwarecanvas?view=xamarin-android-sdk-13">...</a>
     *
     * @return hardware canvas (this is the result of hardware acceleration).
     */
    public Canvas onDrawViewBegin() {
        m_surfaceCanvas = null;

        if (m_surface != null) {
            try {
                m_surfaceCanvas = m_surface.lockHardwareCanvas();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

        return m_surfaceCanvas;
    }

    /**
     *
     */
    public void onDrawViewEnd() {
        if (m_surfaceCanvas != null) {
            m_surface.unlockCanvasAndPost(m_surfaceCanvas);
        }

        m_surfaceCanvas = null;
    }

    /**
     * @param textureWidth
     * @param textureHeight
     */
    public void SetTextureResolution(int textureWidth, int textureHeight) {
        m_textureWidth = textureWidth;
        m_textureHeight = textureHeight;
    }

    public void disable() {
        synchronized (this) {
            m_contentExists = false;
            m_frameAvailable = false;
            m_surfaceDestroyed = true;
        }
    }

    public void requestResizeTex() {
        synchronized (this) {
            m_forceResizeTex = true;

            m_contentExists = false;
            m_frameAvailable = false;
            m_surfaceDestroyed = true;
        }
    }

    public void requestResizeWeb() {
        synchronized (this) {
            m_forceResizeWeb = true;

            m_contentExists = false;
            m_frameAvailable = false;
            m_surfaceDestroyed = true;
        }
    }

    /**
     *
     */
    public final void destroy() {
        m_initialized = false;

        destroyBuffer();
        destroyVbo();
        GLES30.glDeleteProgram(m_glSamplerProgram);

        if (m_eglCore != null) {
            m_eglCore.release();
            m_eglCore = null;
        }
    }
}
