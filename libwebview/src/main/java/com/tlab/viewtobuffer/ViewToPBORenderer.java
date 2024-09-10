package com.tlab.viewtobuffer;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;

public class ViewToPBORenderer extends ViewToBufferRenderer {

    private int[] m_glPboId;
    private boolean[] m_glPboReady;
    private int m_nowWritePboId;
    private int m_nowReadPboId;
    private byte[] m_pixelBuffer;

    /**
     *
     */
    @Override
    protected void destroyBuffer() {
        if (m_glPboId != null) {
            GLES30.glDeleteBuffers(m_glPboId.length, m_glPboId, 0);
            m_glPboId = null;
        }
    }

    /**
     *
     */
    @Override
    protected void initBuffer() {
        if (m_eglCore.getGlVersion() >= 3) {
            GLES30.glGetError();
            boolean bError = false;
            int[] glPbos = new int[3];
            GLES30.glGenBuffers(glPbos.length, glPbos, 0);
            m_glPboReady = new boolean[glPbos.length];
            for (int i = 0; i < glPbos.length; i++) {
                GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, glPbos[i]);
                int errNo = GLES30.glGetError();
                if (errNo != 0) {
                    bError = true;
                    break;
                }
                GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, m_textureWidth * m_textureHeight * 4, null, GLES30.GL_STREAM_READ);
                m_glPboReady[i] = true;
            }
            if (bError) {
                GLES30.glDeleteBuffers(glPbos.length, glPbos, 0);
            } else {
                GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
                m_glPboId = glPbos;
            }
            m_nowWritePboId = 0;
            m_nowReadPboId = glPbos.length - 1;
        }
    }

    /**
     *
     */
    @Override
    public void CopySurfaceTextureToBuffer() {
        if (!m_initialized || m_glPboId == null) {
            return;
        }

        GLES30.glUseProgram(m_glSamplerProgram);

        GLES30.glViewport(0, 0, m_textureWidth, m_textureHeight);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, m_surfaceTextureID[0]);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, m_glCubeID[0]);
        GLES30.glVertexAttribPointer(m_glSamplerPositionID, 2, GLES30.GL_FLOAT, false, 4 * 2, 0);
        GLES30.glEnableVertexAttribArray(m_glSamplerPositionID);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, m_glTexCoordID[0]);
        GLES30.glVertexAttribPointer(m_glSamplerTexCoordID, 2, GLES30.GL_FLOAT, false, 4 * 2, 0);
        GLES30.glEnableVertexAttribArray(m_glSamplerTexCoordID);

        GLES30.glUniform1i(m_glSamplerTexID, 0);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        int id = (m_nowWritePboId + 1) % m_glPboId.length;
        if (m_glPboReady[id]) {
            m_nowWritePboId = id;
            m_nowReadPboId = (m_nowReadPboId + 1) % m_glPboId.length;
            glUtilReadPixels(0, 0, m_textureWidth, m_textureHeight, m_glPboId[m_nowWritePboId]);
            m_glPboReady[id] = false;
        }
        m_pixelBuffer = glUtilGetPboBuffer(m_textureWidth, m_textureHeight, m_glPboId[m_nowReadPboId]);
        m_glPboReady[m_nowReadPboId] = true;

        GLES30.glDisableVertexAttribArray(m_glSamplerPositionID);
        GLES30.glDisableVertexAttribArray(m_glSamplerTexCoordID);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        //Log.i(TAG, "[VPBOR] [CopySurfaceTextureToBuffer]");
    }

    public byte[] getPixelBuffer() {
        return m_pixelBuffer;
    }

    public native void glUtilReadPixels(int x, int y, int width, int height, int pboId);

    public native byte[] glUtilGetPboBuffer(int width, int height, int pboId);

    static {
        System.loadLibrary("glutil");
    }
}
