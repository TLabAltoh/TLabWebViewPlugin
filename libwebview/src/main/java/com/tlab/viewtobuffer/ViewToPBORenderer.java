package com.tlab.viewtobuffer;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;

public class ViewToPBORenderer extends ViewToBufferRenderer {

    private int[] mGLPboId;
    private boolean[] mGLPboReady;
    private int mNowWritePboId;
    private int mNowReadPboId;
    private byte[] mPixelBuffer;

    /**
     *
     */
    @Override
    protected void destroyBuffer() {
        if (mGLPboId != null) {
            GLES30.glDeleteBuffers(mGLPboId.length, mGLPboId, 0);
            mGLPboId = null;
        }
    }

    /**
     *
     */
    @Override
    protected void initBuffer() {
        if (mEglCore.getGlVersion() >= 3) {
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
                GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mTextureWidth * mTextureHeight * 4, null, GLES30.GL_STREAM_READ);
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
    }

    /**
     *
     */
    @Override
    public void CopySurfaceTextureToBuffer() {
        if (!mInitialized || mGLPboId == null) {
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

        GLES30.glUniform1i(mGLSamplerTexID, 0);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        int id = (mNowWritePboId + 1) % mGLPboId.length;
        if (mGLPboReady[id]) {
            mNowWritePboId = id;
            mNowReadPboId = (mNowReadPboId + 1) % mGLPboId.length;
            glUtilReadPixels(0, 0, mTextureWidth, mTextureHeight, mGLPboId[mNowWritePboId]);
            mGLPboReady[id] = false;
        }

        mPixelBuffer = glUtilGetPboBuffer(mTextureWidth, mTextureHeight, mGLPboId[mNowReadPboId]);
        mGLPboReady[mNowReadPboId] = true;

        GLES30.glDisableVertexAttribArray(mGLSamplerPositionID);
        GLES30.glDisableVertexAttribArray(mGLSamplerTexCoordID);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        //Log.i(TAG, "[VPBOR] [CopySurfaceTextureToBuffer]");
    }

    public byte[] getPixelBuffer() {
        return mPixelBuffer;
    }

    public native void glUtilReadPixels(int x, int y, int width, int height, int pboId);

    public native byte[] glUtilGetPboBuffer(int width, int height, int pboId);

    static {
        System.loadLibrary("glutil");
    }
}
