package com.tlab.viewtobuffer;

import android.hardware.HardwareBuffer;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import com.robot9.shared.SharedTexture;

public class ViewToHWBRenderer extends ViewToBufferRenderer {

    private HardwareBuffer m_sharedBuffer;

    private SharedTexture m_sharedTexture;

    /**
     * FBO (pixel data on GPU) for EGL_IMAGE_KHR (Hardware Buffer)
     */

    private int[] m_hwbFboID;
    private int[] m_hwbFboTexID;

    @Override
    protected void destroyBuffer() {
        if (m_hwbFboTexID != null) {
            GLES30.glDeleteTextures(m_hwbFboTexID.length, m_hwbFboTexID, 0);
            m_hwbFboTexID = null;
        }

        if (m_hwbFboID != null) {
            GLES30.glDeleteFramebuffers(m_hwbFboID.length, m_hwbFboID, 0);
            m_hwbFboID = null;
        }

        if (m_sharedTexture != null) {
            m_sharedTexture.release();
            m_sharedTexture = null;
        }
    }

    /**
     *
     */
    @Override
    protected void initBuffer() {
        m_hwbFboID = new int[1];
        m_hwbFboTexID = new int[1];

        m_sharedTexture = new SharedTexture(m_textureWidth, m_textureHeight, false);
        m_sharedBuffer = m_sharedTexture.getHardwareBuffer();

        // Plugin returns long variable, but in OpenGL, texture id can be used by int, so cast here.
        assert m_sharedTexture != null;
        m_hwbFboTexID[0] = (int) m_sharedTexture.getPlatformTexture();

        GLES30.glGenFramebuffers(1, m_hwbFboID, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, m_hwbFboID[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, m_hwbFboTexID[0], 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void CopySurfaceTextureToBuffer() {
        if (!m_initialized || m_hwbFboID == null) {
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

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glUniform1i(m_glSamplerTexID, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, m_hwbFboID[0]);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glFlush();

        GLES30.glDisableVertexAttribArray(m_glSamplerPositionID);
        GLES30.glDisableVertexAttribArray(m_glSamplerTexCoordID);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        //Log.i(TAG, "[VHWBR] [CopySurfaceTextureToBuffer]");
    }

    /**
     * @return
     */
    public HardwareBuffer getHardwareBuffer() {
        return m_sharedBuffer;
    }
}