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

    private int[] mHwbFboID;
    private int[] mHwbFboTexID;

    @Override
    protected void destroyBuffer() {
        if (mHwbFboTexID != null) {
            GLES30.glDeleteTextures(mHwbFboTexID.length, mHwbFboTexID, 0);
            mHwbFboTexID = null;
        }

        if (mHwbFboID != null) {
            GLES30.glDeleteFramebuffers(mHwbFboID.length, mHwbFboID, 0);
            mHwbFboID = null;
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
        mHwbFboID = new int[1];
        mHwbFboTexID = new int[1];

        mSharedTexture = new SharedTexture(mTexSize.x, mTexSize.y, false);
        mSharedBuffer = mSharedTexture.getHardwareBuffer();

        // Plugin returns long variable, but in OpenGL, texture id can be used by int, so cast here.
        assert mSharedTexture != null;
        mHwbFboTexID[0] = (int) mSharedTexture.getPlatformTexture();

        GLES30.glGenFramebuffers(1, mHwbFboID, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mHwbFboID[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mHwbFboTexID[0], 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void CopySurfaceTextureToBuffer() {
        if (!mInitialized || mHwbFboID == null) return;

        GLES30.glUseProgram(mGlSamplerProgram);

        GLES30.glViewport(0, 0, mTexSize.x, mTexSize.y);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureID[0]);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGlCubeID[0]);
        GLES30.glVertexAttribPointer(mGlSamplerPositionID, 2, GLES30.GL_FLOAT, false, 4 * 2, 0);
        GLES30.glEnableVertexAttribArray(mGlSamplerPositionID);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mGlTexCoordID[0]);
        GLES30.glVertexAttribPointer(mGlSamplerTexCoordID, 2, GLES30.GL_FLOAT, false, 4 * 2, 0);
        GLES30.glEnableVertexAttribArray(mGlSamplerTexCoordID);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glUniform1i(mGlSamplerTexID, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mHwbFboID[0]);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glFlush();

        GLES30.glDisableVertexAttribArray(mGlSamplerPositionID);
        GLES30.glDisableVertexAttribArray(mGlSamplerTexCoordID);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        //Log.i(TAG, "[VHWBR] [CopySurfaceTextureToBuffer]");
    }

    public HardwareBuffer getHardwareBuffer() {
        return mSharedBuffer;
    }
}