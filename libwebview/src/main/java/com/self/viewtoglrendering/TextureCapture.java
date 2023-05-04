package com.self.viewtoglrendering;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.util.Log;

import com.android.grafika.gles.EglCore;
import com.android.grafika.gles.GlUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.EGL14;
import android.opengl.EGLContext;

public class TextureCapture
{
    private EglCore mEglCore;

    private boolean mIsInitialized;
    private String mVertexShaderProg;
    private String mFragmentShaderProg;

    private int mGLProgId;
    private int mGLPositionIndex;
    private int mGLInputImageTextureIndex;
    private int mGLTextureCoordinateIndex;

    // --------------------------------------------------------------------------------------------------------------------------
    // Texture resolution
    //

    protected int mInputWidth;
    protected int mInputHeight;

    // --------------------------------------------------------------------------------------------------------------------------
    //
    //

    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;
    private boolean mFlipY;
    private boolean mUsePBO;

    private int[] mGLCubeId;
    private int[] mGLTextureCoordinateId;

    // --------------------------------------------------------------------------------------------------------------------------
    // PBO (pixel data on GPU)
    //

    private int[] mGLPboId;
    private boolean[] mGLPboReady;

    // --------------------------------------------------------------------------------------------------------------------------
    // FBO (pixel data on GPU)
    //

    private int[] mGLFboId;
    private int[] mGLFboTexId;
    private Buffer mGLFboBuffer;

    private int mNowWritePboId;
    private int mNowReadPboId;

    public void flipY()
    {
        mFlipY = true;
    }

    // --------------------------------------------------------------------------------------------------------------------------
    // Sampler shader prog
    //

    public void setSamplerShaderProg(String vsProg, String fsProg) {
        mVertexShaderProg = vsProg;
        mFragmentShaderProg = fsProg;
    }

    public void loadSamplerShaderProg(Context context, int vs, int fs) {
        setSamplerShaderProg(readShaderFromRawResource(context, vs), readShaderFromRawResource(context, fs));
    }

    // --------------------------------------------------------------------------------------------------------------------------
    // Shader
    //

    private void loadSamplerShader() {
        mGLProgId = GlUtil.createProgram(mVertexShaderProg, mFragmentShaderProg);

        if (mGLProgId == 0) return;

        mGLPositionIndex = GLES30.glGetAttribLocation(mGLProgId, "position");
        mGLTextureCoordinateIndex = GLES30.glGetAttribLocation(mGLProgId,"inputTextureCoordinate");
        mGLInputImageTextureIndex = GLES30.glGetUniformLocation(mGLProgId, "inputImageTexture");
    }

    private String readShaderFromRawResource(Context context, int resourceId) {
        final InputStream inputStream = context.getResources().openRawResource(resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String nextLine;
        final StringBuilder body = new StringBuilder();

        try{
            while ((nextLine = bufferedReader.readLine()) != null){
                body.append(nextLine);
                body.append('\n');
            }
        }
        catch (IOException e){
            return null;
        }
        return body.toString();
    }

    // --------------------------------------------------------------------------------------------------------------------------
    // Initialize
    //

    public void init() {
        if (mIsInitialized) return;

        EGLContext con = EGL14.eglGetCurrentContext();
        mEglCore = new EglCore(con, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
        onInit();
        onInitialized();
    }

    public final void destroy() {
        mIsInitialized = false;
        destroyFboTexture();
        destoryVbo();
        GLES30.glDeleteProgram(mGLProgId);
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        onDestroy();
    }

    // --------------------------------------------------------------------------------------------------------------------------
    // On initialize
    //

    protected void onInit() {
        initVbo();
        loadSamplerShader();
    }

    protected void onDestroy() {}

    // --------------------------------------------------------------------------------------------------------------------------
    // Is this initialized ?
    //

    protected void onInitialized() {
        mIsInitialized = true;
    }

    // --------------------------------------------------------------------------------------------------------------------------
    // Vbo util (vertex buffer object)

    private void initVbo() {
        final float VEX_CUBE[] = {
                -1.0f, -1.0f, // Bottom left.
                1.0f, -1.0f, // Bottom right.
                -1.0f, 1.0f, // Top left.
                1.0f, 1.0f, // Top right.
        };
        final float VEX_CUBE_F[] = {
                -1.0f, 1.0f, // Bottom left.
                1.0f, 1.0f, // Bottom right.
                -1.0f, -1.0f, // Top left.
                1.0f, -1.0f, // Top right.
        };

        final float TEX_COORD[] = {
                0.0f, 0.0f, // Bottom left.
                1.0f, 0.0f, // Bottom right.
                0.0f, 1.0f, // Top left.
                1.0f, 1.0f // Top right.
        };

        mGLCubeBuffer = ByteBuffer.allocateDirect(VEX_CUBE.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLCubeBuffer.put(mFlipY? VEX_CUBE_F : VEX_CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEX_COORD.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTextureBuffer.put(TEX_COORD).position(0);

        mGLCubeId = new int[1];
        mGLTextureCoordinateId = new int[1];

        GLES30.glGenBuffers(1, mGLCubeId, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGLCubeId[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mGLCubeBuffer.capacity() * 4, mGLCubeBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glGenBuffers(1, mGLTextureCoordinateId, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGLTextureCoordinateId[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mGLTextureBuffer.capacity() * 4, mGLTextureBuffer, GLES30.GL_STATIC_DRAW);
    }

    private void destoryVbo() {
        if (mGLCubeId != null) {
            GLES30.glDeleteBuffers(1, mGLCubeId, 0);
            mGLCubeId = null;
        }
        if (mGLTextureCoordinateId != null) {
            GLES30.glDeleteBuffers(1, mGLTextureCoordinateId, 0);
            mGLTextureCoordinateId = null;
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------
    // Fbo util (frame buffer object)
    // link: http://www.slis.tsukuba.ac.jp/~fujisawa.makoto.fu/cgi-bin/wiki/index.php?OpenGL+-+FBO#:~:text=%E3%83%95%E3%83%AC%E3%83%BC%E3%83%A0%E3%83%90%E3%83%83%E3%83%95%E3%82%A1%E3%81%AF%EF%BC%8COpenGL%E3%81%AE%E3%83%AC%E3%83%B3%E3%83%80%E3%83%AA%E3%83%B3%E3%82%B0%E3%83%91%E3%82%A4%E3%83%97%E3%83%A9%E3%82%A4%E3%83%B3%E3%81%AB%E3%81%8A%E3%81%84%E3%81%A6%E6%9C%80%E7%B5%82%E7%9A%84%E3%81%AA%E3%83%AC%E3%83%B3%E3%83%80%E3%83%AA%E3%83%B3%E3%82%B0%E7%B5%90%E6%9E%9C%E3%82%92%E6%8F%8F%E7%94%BB%E3%81%99%E3%82%8B%E3%81%AE%E3%81%AB%E7%94%A8%E3%81%84%E3%82%89%E3%82%8C
    //

    // Re initialize fbo
    public void onInputSizeChanged(final int textureWidth, final int textureHeight) {
        mInputWidth = textureWidth;
        mInputHeight = textureHeight;
        initFboTexture(textureWidth, textureHeight);
    }

    private void initFboTexture(int textureWidth, int textureHeight) {
        if (mGLFboId != null && (mInputWidth != textureWidth || mInputHeight != textureHeight)) destroyFboTexture();

        mGLFboId = new int[1];
        mGLFboTexId = new int[1];

        GLES30.glGenFramebuffers(1, mGLFboId, 0);
        GLES30.glGenTextures(1, mGLFboTexId, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGLFboTexId[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, textureWidth, textureHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mGLFboId[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mGLFboTexId[0], 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        // Create PBO
        if (mEglCore.getGlVersion() >= 3 && mUsePBO) {
            GLES30.glGetError();
            boolean bError = false;
            int[] glPbos = new int[3];
            GLES30.glGenBuffers(glPbos.length, glPbos, 0);
            mGLPboReady = new boolean[glPbos.length];
            for (int i = 0; i < glPbos.length; i++) {
                GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, glPbos[i]);
                int errNo = GLES30.glGetError();
                if (errNo != 0) {
                    bError = true;
                    break;
                }
                GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, textureWidth * textureHeight * 4, null, GLES30.GL_STREAM_READ);
                mGLPboReady[i] = true;
            }
            if (bError) {
                GLES30.glDeleteBuffers(glPbos.length, glPbos, 0);
            } else {
                GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
                mGLPboId = glPbos;
            }
            mNowWritePboId = 0;
            mNowReadPboId = glPbos.length - 1;
        }

        if (mGLPboId == null) {
            mGLFboBuffer = ByteBuffer.allocate(textureWidth * textureHeight * 4);
        }
    }

    private void destroyFboTexture() {
        if (mGLFboTexId != null) {
            GLES30.glDeleteTextures(mGLFboTexId.length, mGLFboTexId, 0);
            mGLFboTexId = null;
        }
        if (mGLFboId != null) {
            GLES30.glDeleteFramebuffers(mGLFboId.length, mGLFboId, 0);
            mGLFboId = null;
        }
        if (mGLPboId != null) {
            GLES30.glDeleteBuffers(mGLPboId.length, mGLPboId, 0);
            mGLPboId = null;
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------
    // Call from ViewToGLRenderer.onDrawFrame()
    //

    public int onDrawFrame(int textureId, boolean external) {
        if (!mIsInitialized || mGLFboId == null) return -1;

        Log.i("TlabBrowser", "libwebview---onDrawFrameFrame: draw start");

        //Log.d("TextureCapture", "onDrawFrame:" + textureId);
        GLES30.glGetError();
        GLES30.glUseProgram(mGLProgId);
        //GlUtil.checkGlError("glUseProgram ProgId=" + mGLProgId);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGLCubeId[0]);
        //GlUtil.checkGlError("glBindBuffer");
        GLES30.glEnableVertexAttribArray(mGLPositionIndex);
        //GlUtil.checkGlError("glEnableVertexAttribArray");
        GLES30.glVertexAttribPointer(mGLPositionIndex, 2, GLES30.GL_FLOAT, false, 4 * 2, 0);
        //GlUtil.checkGlError("glVertexAttribPointer");

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGLTextureCoordinateId[0]);
        //GlUtil.checkGlError("glBindBuffer");
        GLES30.glEnableVertexAttribArray(mGLTextureCoordinateIndex);
        //GlUtil.checkGlError("glEnableVertexAttribArray");
        GLES30.glVertexAttribPointer(mGLTextureCoordinateIndex, 2, GLES30.GL_FLOAT, false, 4 * 2, 0);
        //GlUtil.checkGlError("glVertexAttribPointer");

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        //GlUtil.checkGlError("glActiveTexture");
        GLES30.glBindTexture(external? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES30.GL_TEXTURE_2D, textureId);
        //GlUtil.checkGlError("glBindTexture textureId=" + textureId);
        GLES30.glUniform1i(mGLInputImageTextureIndex, 0);
        //GlUtil.checkGlError("glUniform1i");

        GLES30.glViewport(0, 0, mInputWidth, mInputHeight);
        //GlUtil.checkGlError("glViewport");
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mGLFboId[0]);
        //GlUtil.checkGlError("glBindFramebuffer");
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        //GlUtil.checkGlError("glDisable");
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        //GlUtil.checkGlError("glDrawArrays");

        // GLSurface to buffer
        GLES30.glReadPixels(0, 0, mInputWidth, mInputHeight, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, mGLFboBuffer);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        GLES30.glBindTexture(external? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES30.GL_TEXTURE_2D, 0);

        GLES30.glDisableVertexAttribArray(mGLPositionIndex);
        GLES30.glDisableVertexAttribArray(mGLTextureCoordinateIndex);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        Log.i("TlabBrowser", "libwebview---onDrawFrameFrame: Frame draw end");

        return mGLFboTexId[0];
    }

    // --------------------------------------------------------------------------------------------------------------------------
    // Return texture buffer
    //

    public byte[] getGLFboBuffer() {
        return (mGLFboBuffer != null ? ((ByteBuffer)mGLFboBuffer).array() : new byte[0]);
    }
}
