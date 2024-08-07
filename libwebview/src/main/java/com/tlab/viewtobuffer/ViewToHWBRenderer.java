package com.tlab.viewtobuffer;

import android.hardware.HardwareBuffer;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import com.robot9.shared.SharedTexture;

public class ViewToHWBRenderer extends ViewToBufferRenderer {

    private HardwareBuffer mSharedBuffer;

    private SharedTexture mSharedTexture;

    /**
     * FBO (pixel data on GPU) for EGL_IMAGE_KHR (Hardware Buffer)
     */

    private int[] mHWBFboID;
    private int[] mHWBFboTexID;

    @Override
    protected void destroyBuffer() {
        if (mHWBFboTexID != null) {
            GLES30.glDeleteTextures(mHWBFboTexID.length, mHWBFboTexID, 0);
            mHWBFboTexID = null;
        }

        if (mHWBFboID != null) {
            GLES30.glDeleteFramebuffers(mHWBFboID.length, mHWBFboID, 0);
            mHWBFboID = null;
        }

        if (mSharedTexture != null) {
            mSharedTexture.release();
            mSharedTexture = null;
        }
    }

    /**
     *
     */
    @Override
    protected void initBuffer() {
        mHWBFboID = new int[1];
        mHWBFboTexID = new int[1];

        mSharedTexture = new SharedTexture(mTextureWidth, mTextureHeight, false);
        mSharedBuffer = mSharedTexture.getHardwareBuffer();

        // Plugin returns long variable, but in OpenGL, texture id can be used by int, so cast here.
        assert mSharedTexture != null;
        mHWBFboTexID[0] = (int) mSharedTexture.getBindedPlatformTexture();

        GLES30.glGenFramebuffers(1, mHWBFboID, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mHWBFboID[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mHWBFboTexID[0], 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void CopySurfaceTextureToBuffer() {
        if (!mInitialized || mHWBFboID == null) {
            return;
        }

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

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glUniform1i(mGLSamplerTexID, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mHWBFboID[0]);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glFlush();

        GLES30.glDisableVertexAttribArray(mGLSamplerPositionID);
        GLES30.glDisableVertexAttribArray(mGLSamplerTexCoordID);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        //Log.i(TAG, "[VHWBR] [CopySurfaceTextureToBuffer]");
    }

    /**
     * @return
     */
    public HardwareBuffer getHardwareBuffer() {
        return mSharedBuffer;
    }
}